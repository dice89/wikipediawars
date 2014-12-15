package controllers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import play.Logger;
import play.cache.Cache;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.util.CombinedResponses;
import de.w4.analyzer.WikiAnalyzer;
import de.w4.analyzer.util.RevisionAnalysisResultObject;
import de.w4.analyzer.util.RevisionList;

public class WikiController extends Controller {

	public static final String CACHE_WIKI_PREFIX = "wiki";

	public static final int TOP_K_DISCUSSED_TERMS = 30;
	private static final long TIMEOUT = 5000;

	// Enum for timeScoe
	public enum TimeScope {
		MONTH, SIXMONTH, YEAR
	}

	public static final List<String> nations() {
		List<String> nations = null;
		try {
			if (nations == null) nations = Files.readAllLines(new File("public/data/un_nations.txt").toPath(), Charset.forName("UTF-8"));
			return nations;
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}


	public static Promise<Result> guessAnalyzeTime(final String article,
			final String time_scope) {

		String end_date_utc_string = getUTCDateStringForScope(time_scope);

		final Promise<Result> resultPromise = chainGetRevisionsGeoWS(null,
				article, new ArrayList<WSResponse>(), end_date_utc_string, true)
				.map(new Function<CombinedResponses, Result>() {

					@Override
					public Result apply(CombinedResponses responses)
							throws Throwable {
						JsonNode jsonbody = null;
						int number = 0;
						for (WSResponse response : responses.getResponses()) {

							try {
								jsonbody = response.asJson();
								// System.out.println(jsonbody.toString());
							} catch (Exception e) {
								ok(response.toString());
							}

							// prevent json error
							try {
								// navigate through tree and get first page
								// discard
								// the other ones
								number = number
										+ jsonbody.findPath("pages").elements()
												.next().findPath("revisions")
												.size();

							} catch (Exception e) {
								System.out.println(number);
								internalServerError(e.getMessage());
							}

							// now turn this to the revisions_analyzer
						}

						ok(Json.toJson("{count: " + number + "}"));
						return null;
					}
				});

		return resultPromise;

	}
	
	/**
	 * Wrapper service for Wikipedia Autosuggest
	 * @param search
	 * @return
	 */
	public static Promise<Result> suggest(final String search, final int limit){
		
		WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php")
				.setQueryParameter("action", "opensearch")
				.setQueryParameter("format", "json")
				.setQueryParameter("search",search)
				.setQueryParameter("limit", limit+"")
				.setQueryParameter("namespace", "0")
				.setQueryParameter("suggest", "");
		
		return holder.get().map(new Function<WSResponse, Result>() {
			@Override
			public Result apply(WSResponse response) throws Throwable {
				return ok(response.asJson());
			}
		});
		
	}
	
	/*http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=Mann&namespace=0&suggest=*/

	/**
	 * Route for analyzing an Article
	 * 
	 * @return
	 */
	public static Promise<Result> analyze(final String article,
			final String time_scope, final String aggregation_string) {

		// format timely scope
		
		String end_date_utc_string = getUTCDateStringForScope(time_scope);
		final int aggregation_type = getAggregationType(aggregation_string);

		// check cache
		final String cachekey = getCacheKey(article, time_scope,
				aggregation_type);
		final String cache = (String) Cache.get(cachekey);

		if (cache != null) {
			Promise<Result> promiseOfResult = Promise
					.promise(new Function0<Result>() {
						public Result apply() {
							System.out.println("served from cache");
							return ok(Json.parse(cache));
						}
					});

			return promiseOfResult;
		}
		final Promise<Result> resultPromise = chainGetRevisionsGeoWS(null,
				article, new ArrayList<WSResponse>(), end_date_utc_string,
				false).map(new Function<CombinedResponses, Result>() {
			public Result apply(CombinedResponses responses) {
				// now let the magic begin

				JsonNode jsonbody = null;

				List<JsonNode> revision_array = new ArrayList<JsonNode>();
				for (WSResponse response : responses.getResponses()) {

					try {
						jsonbody = response.asJson();
					} catch (Exception e) {
						
						System.out.println(e);
						return returnEmptyResult();
					}

					try {

						// navigate through tree and get first page
						// discard
						// the other ones
						revision_array.add(jsonbody.findPath("pages")
								.elements().next().findPath("revisions"));
					} catch (Exception e) {
						return internalServerError(e.getMessage());
					}
					
					// now turn this to the revisions_analyzer
				}
				System.out.println("Data retrieved now analyse it!");
				// first merge revisions arrays together to only have one
				RevisionAnalysisResultObject revision_summary_object = null;
				try {
					revision_summary_object = WikiAnalyzer.getWikiAnalyzer()
							.analyzeGeoOrigin(revision_array,
									TOP_K_DISCUSSED_TERMS, aggregation_type);
				} catch (IOException | ParseException e) {
					System.out.println("fail");
					return internalServerError(e.getMessage());
				}

				if (revision_summary_object == null) {

					return internalServerError("Wikipedia API Error");
				}
				JsonNode resp = Json.toJson(revision_summary_object);
				Cache.set(cachekey, Json.stringify(resp));
				return ok(resp);
			}
		});

		return resultPromise;
	}

	private static String getCacheKey(final String article,
			final String time_scope, final int aggregation_type) {
		final String cachekey = CACHE_WIKI_PREFIX + "-" + time_scope + "-" + aggregation_type + "-"
				+ article;
		return cachekey;
	}

	private static Status returnEmptyResult() {

		JsonNode resp = Json.toJson(new RevisionAnalysisResultObject());
		return ok(resp);
	}
	
	
	private static int getAggregationType(String query_string){
		
		if(query_string.equals("d")){
			return RevisionList.AGGREGATE_DAY;
		}else if (query_string.equals("w")) {
			return RevisionList.AGGREGATE_WEEK;
		}else if(query_string.equals("m")){
			return RevisionList.AGGREGATE_MONTH;
		}else {
			return RevisionList.AGGREGATE_DAY;
		}
		
	}

	private static String getUTCDateStringForScope(String scope) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(tz);

		Date cur_date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(cur_date);
		
		
		if(scope.equals("") || scope == null ){
			c.add(Calendar.MONTH, -12);
		}else if(scope.contains("m")){
			int mindex = scope.indexOf("m");	
			int month_inc = Integer.parseInt(scope.substring(0, mindex));
			c.add(Calendar.MONTH, -month_inc);
			
		}else if(scope.contains("y")){
			int mindex = scope.indexOf("y");
			int year_inc = Integer.parseInt(scope.substring(0, mindex));
			c.add(Calendar.YEAR, -year_inc);
		}


		return df.format(c.getTime());

	}

	private static Promise<CombinedResponses> chainGetRevisionsGeoWS(
			final String continue_revision, final String article,
			final List<WSResponse> responses, final String end_date_utc_string,
			final boolean count_request) {

		// create parameters

		WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php");
		holder = holder.setQueryParameter("format", "json")
				.setQueryParameter("action", "query")
				.setQueryParameter("titles", article)
				.setQueryParameter("prop", "revisions");
		if (!count_request) {
			holder = holder
					.setQueryParameter("rvprop",
							"user|timestamp|size|ids|userid")
					.setQueryParameter("rvend", end_date_utc_string)
					.setQueryParameter("rvdiffto", "prev")
					.setQueryParameter("rvlimit", "max");
			;
		} else {
			holder = holder.setQueryParameter("rvprop", "userid|timestamp")
					.setQueryParameter("rvend", end_date_utc_string)
					.setQueryParameter("rvlimit", "max");
			;
		}
		System.out.println("Date String:" + end_date_utc_string);

		if (continue_revision != null) {
			holder = holder.setQueryParameter("rvcontinue", continue_revision);
		}

		holder = holder.setHeader("User-Agent", "Wikpedia Wars Application");

		// crazy functional shit to chain ws promises after each other and
		// finally create a new promise based on the results of the others
		return holder.get().flatMap(
				new Function<WSResponse, Promise<CombinedResponses>>() {

					@Override
					public Promise<CombinedResponses> apply(WSResponse response)
							throws Throwable {
						if (!response.asJson().has("query-continue")) {
							// System.out.println(response.getBody());
							// all revisions retrieved finally done
							responses.add(response);
							Promise<CombinedResponses> response_promise = Promise
									.promise(new Function0<CombinedResponses>() {
										public CombinedResponses apply() {
											return new CombinedResponses(
													responses);
										}
									});
							return response_promise;

						} else {

							// more revisions to retrieve
							String continue_value = response.asJson()
									.findPath("query-continue")
									.findPath("rvcontinue").toString();
							System.out.println(continue_value);
							responses.add(response);
							return chainGetRevisionsGeoWS(continue_value,
									article, responses, end_date_utc_string,
									count_request);
						}

					}
				});
	}

	/**
	 * Heuristic to extract the nationality form a wikipedia user page
	 *
	 * @return
	 */
	public static Result geoForRegisteredUsers(String userName) {
		return ok(tryGeoHeuristicRegisteredUsers(userName).orElse("not nation found"));
	}

	/**
	 * Heuristic to extract the nationality form a wikipedia user page
	 *
	 * @return
	 */
	public static Optional<String> tryGeoHeuristicRegisteredUsers(String userName) {
		String url = "http://en.wikipedia.org/wiki/User:"+userName;
		return WS.url(url).get().map(response -> {
					Document doc = Jsoup.parse(response.getBody());
					Elements contentLinks = doc.getElementById("bodyContent").getElementsByTag("a");
					String s = "";
					List<String> candidates = new ArrayList<>();

					for (Element e: contentLinks) {

						String[] parts = e.attr("href").split("/");
						if (parts[parts.length-1].contains(":")) continue;
						if (parts[parts.length-1].contains("#")) continue;

						s += parts[parts.length-1];
//						Logger.debug(s);
						candidates.add(parts[parts.length-1].trim());
					}

					Optional<String> nation = candidates.stream().filter(can ->
									nations().stream().anyMatch(na -> na.equalsIgnoreCase(can))
					).findFirst();

					return nation;
				}
		).get(TIMEOUT);
	}

}
