package controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

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
import com.maxmind.geoip2.exception.GeoIp2Exception;

import controllers.util.CombinedResponses;
import de.w4.analyzer.WikiAnalyzer;
import de.w4.analyzer.ipgeoloc.IPLocExtractor;
import de.w4.analyzer.util.RevisionSummaryObjectGroup;

public class WikiController extends Controller {
	
	public static final String CACHE_GEO_PREFIX = "geo";

	public static Result index() throws ClientProtocolException,
			URISyntaxException, IOException, GeoIp2Exception {

		IPLocExtractor ip = null;
		try {
			ip = new IPLocExtractor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String code = ip.getGeoLocationForIP("134.106.98.44").getCountryCode();

		return ok(code);

	}

	/**
	 * Route for analyzing an Article
	 * 
	 * @return
	 */
	public static Promise<Result> analyze(final String article) {

		// check cache
		final String cache = (String) Cache.get(CACHE_GEO_PREFIX + article);

		if (cache != null) {
			Promise<Result> promiseOfResult = Promise
					.promise(new Function0<Result>() {
						public Result apply() {
							System.out.println("cache served 123");
							
							
							return ok(Json.parse(cache));
						}
					});

			return promiseOfResult;
		}
		final Promise<Result> resultPromise = chainGetRevisionsGeoWS(null,
				article, new ArrayList<WSResponse>()).map(
				new Function<CombinedResponses, Result>() {
					public Result apply(CombinedResponses responses) {
						// now let the magic begin

						JsonNode jsonbody;

						List<JsonNode> revision_array = new ArrayList<JsonNode>();
						for (WSResponse response : responses.getResponses()) {
							jsonbody = response.asJson();

							// navigate through tree and get first page discard
							// the other ones
							revision_array.add(jsonbody.findPath("pages")
									.elements().next().findPath("revisions"));
							// now turn this to the revisions_analyzer
						}
						// first merge revisions arrays together to only have
						// one
						ArrayList<RevisionSummaryObjectGroup> revision_summary_object = null;
						try {
							revision_summary_object = WikiAnalyzer
									.getWikiAnalyzer().analyzeGeoOrigin(
											revision_array);
						} catch (IOException | ParseException e) {
							System.out.println("fail");
							internalServerError(e.getMessage());
						}
						
						
						JsonNode resp = Json.toJson(revision_summary_object);
						Cache.set(CACHE_GEO_PREFIX + article, Json.stringify(resp));
						return ok(Json.toJson(revision_summary_object));
					}
				});

		return resultPromise;
	}

	public static Promise<CombinedResponses> chainGetRevisionsGeoWS(
			final String continue_revision, final String article,
			final List<WSResponse> responses) {

		// create parameters
		WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php");
		holder = holder.setQueryParameter("test", "test")
				.setQueryParameter("format", "json")
				.setQueryParameter("action", "query")
				.setQueryParameter("titles", article)
				.setQueryParameter("prop", "revisions")
				.setQueryParameter("rvprop", "user|timestamp|size|ids|userid")
				.setQueryParameter("rvlimit", "max");

		if (continue_revision != null) {
			holder = holder.setQueryParameter("rvcontinue", continue_revision);
		}

		// crazy functional shit to chain ws promises after each other and
		// finally create a new promise based on the results of the others
		return holder.get().flatMap(
				new Function<WSResponse, Promise<CombinedResponses>>() {

					@Override
					public Promise<CombinedResponses> apply(WSResponse response)
							throws Throwable {

						if (!response.asJson().has("query-continue")) {

							// all revisions retrieved finally done
							System.out.println("done");
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
							responses.add(response);
							return chainGetRevisionsGeoWS(continue_value,
									article, responses);
						}

					}
				});

	}

}
