package controllers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

public class WikiController extends Controller {

	public static final String CACHE_WIKI_PREFIX = "wiki";

	public static final int TOP_K_DISCUSSED_TERMS = 30;

	// Enum for timeScoe
	public enum TimeScope {
		MONTH, SIXMONTH, YEAR
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
	 * Route for analyzing an Article
	 * 
	 * @return
	 */
	public static Promise<Result> analyze(final String article,
			final String time_scope) {

		// format timely scope

		String end_date_utc_string = getUTCDateStringForScope(time_scope);

		// check cache
		final String cachekey = CACHE_WIKI_PREFIX + "-" + time_scope + "-"
				+ article;
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
				// first merge revisions arrays together to only have one
				RevisionAnalysisResultObject revision_summary_object = null;
				try {
					revision_summary_object = WikiAnalyzer.getWikiAnalyzer()
							.analyzeGeoOrigin(revision_array,
									TOP_K_DISCUSSED_TERMS);
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

	public static Status returnEmptyResult() {

		JsonNode resp = Json.toJson(new RevisionAnalysisResultObject());
		return ok(resp);
	}

	public static String getUTCDateStringForScope(String time_scope) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(tz);

		Date cur_date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(cur_date);

		if (time_scope.equals(TimeScope.MONTH.toString())) {
			c.add(Calendar.MONTH, -1);
		} else if (time_scope.equals(TimeScope.SIXMONTH.toString())) {
			c.add(Calendar.MONTH, -6);
		} else if (time_scope.equals(TimeScope.YEAR.toString())) {
			c.add(Calendar.YEAR, -1);
		} else {
			// default scope is month
			c.add(Calendar.YEAR, -1);
		}

		return df.format(c.getTime());

	}

	public static Promise<CombinedResponses> chainGetRevisionsGeoWS(
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

}
