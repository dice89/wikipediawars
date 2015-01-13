package controllers;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.jdbc.JDBCCategoryDataset;
import org.jfree.data.jdbc.JDBCXYDataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.db.DB;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.util.CombinedJSONResponse;
import controllers.util.CombinedResponses;
import de.w4.analyzer.UserAnalyzer;
import de.w4.analyzer.WikiAnalyzer;
import de.w4.analyzer.util.RevisionAnalysisResultObject;
import de.w4.analyzer.util.RevisionList;



public class WikiController extends Controller {

	public static final String REDIS_HOST = "localhost";
	public static final int REDIS_PORT = 6379;

	public static final String REDIS_USER_SET_NAME = "W_USER";
	public static final String REDIS_NATION_HASH_NAME = "USER_NATION_HASH";

	public static final String CACHE_WIKI_PREFIX = "wiki";

	private static Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);

	public static final int TOP_K_DISCUSSED_TERMS = 30;
	private static final long TIMEOUT = 40000;

	// Enum for timeScoe
	public enum TimeScope {
		MONTH, SIXMONTH, YEAR
	}

	public static final List<String> nations() {
		try {
			
			InputStream stream = Play.application().classloader().getResourceAsStream("public/data/un_nations.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader( stream));
			List<String> nations = new ArrayList<String>();
			
			String theLine = null;
			while((theLine = br.readLine())!= null){
				nations.add(theLine);
			}
			return nations;
			
		} catch (Exception e){
			System.out.println(e);
			return new ArrayList<String>();
		}
	}
	
	
	public static Result testFile(){
		
		try {
		
			InputStream stream = Play.application().classloader().getResourceAsStream("public/data/un_nations.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader( stream));
			List<String> nations = new ArrayList<String>();
			
			String theLine = null;
			while((theLine = br.readLine())!= null){
				nations.add(theLine);
			}
			
		} catch (Exception e){

			System.out.println(e);
		}
			//Files.readAllLines(new File(Play.application().classloader().getResource("data/un_nations.txt").toString()).toPath(), Charset.forName("UTF-8"));
			return ok("found");
		
		
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
	 * 
	 * @param search
	 * @return
	 */
	public static Promise<Result> suggest(final String search, final int limit) {

		WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php")
				.setQueryParameter("action", "opensearch")
				.setQueryParameter("format", "json")
				.setQueryParameter("search", search)
				.setQueryParameter("limit", limit + "")
				.setQueryParameter("namespace", "0")
				.setQueryParameter("suggest", "");

		return holder.get().map(new Function<WSResponse, Result>() {
			@Override
			public Result apply(WSResponse response) throws Throwable {
				return ok(response.asJson());
			}
		});

	}

	/*
	 * http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=Mann
	 * &namespace=0&suggest=
	 */

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
							Logger.debug("served from cache");
							
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
						
						Logger.error(e.getMessage());
						
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
				Logger.debug("Data retrieved now analyse it!");

				// first merge revisions arrays together to only have one
				RevisionAnalysisResultObject revision_summary_object = null;
				try {
					revision_summary_object = WikiAnalyzer.getWikiAnalyzer()
							.analyzeGeoOrigin(revision_array,
									TOP_K_DISCUSSED_TERMS, aggregation_type);
				} catch (IOException | ParseException e) {
					Logger.error("Analysis Failed");
					return internalServerError(e.getMessage());
				}

				if (revision_summary_object == null) {

					return internalServerError("Wikipedia API Error");
				}
				JsonNode resp = Json.toJson(revision_summary_object);

				Cache.set(cachekey, Json.stringify(resp), 60 * 24);
				return ok(resp);
			}
		});

		return resultPromise;
	}
	
	

	private static String getCacheKey(final String article,
			final String time_scope, final int aggregation_type) {
		final String cachekey = CACHE_WIKI_PREFIX + "-" + time_scope + "-"
				+ aggregation_type + "-" + article;
		return cachekey;
	}

	private static Status returnEmptyResult() {

		JsonNode resp = Json.toJson(new RevisionAnalysisResultObject());
		return ok(resp);
	}

	private static int getAggregationType(String query_string) {

		if (query_string.equals("d")) {
			return RevisionList.AGGREGATE_DAY;
		} else if (query_string.equals("w")) {
			return RevisionList.AGGREGATE_WEEK;
		} else if (query_string.equals("m")) {
			return RevisionList.AGGREGATE_MONTH;
		} else {
			return RevisionList.AGGREGATE_DAY;
		}

	}

	public static String getWikipediaUsers() {

		return "";
	}

	private static String getUTCDateStringForScope(String scope) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(tz);

		Date cur_date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(cur_date);

		if (scope.equals("") || scope == null) {
			c.add(Calendar.MONTH, -12);
		} else if (scope.contains("m")) {
			int mindex = scope.indexOf("m");
			int month_inc = Integer.parseInt(scope.substring(0, mindex));
			c.add(Calendar.MONTH, -month_inc);

		} else if (scope.contains("y")) {
			int mindex = scope.indexOf("y");
			int year_inc = Integer.parseInt(scope.substring(0, mindex));
			c.add(Calendar.YEAR, -year_inc);
		}

		return df.format(c.getTime());

	}

	public static Result extractUserNation() {

		UserAnalyzer.analyze();
		return ok("Nations Extraction Tasks triggered");
	}

	public static Result extractWikiUsers() {

		// action=query&list=allusers&aufrom=Y&aulimit=max
		WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php");
		holder = holder.setQueryParameter("format", "json")
				.setQueryParameter("action", "query")
				.setQueryParameter("list", "allusers")
				.setQueryParameter("aufrom", "!")
				.setQueryParameter("aulimit", "max")
				.setQueryParameter("auprop","editcount|blockinfo")
				.setQueryParameter("auwitheditsonly", "true");

		// .setQueryParameter("auactiveusers", "true")

		holder = holder.setHeader("User-Agent", "Wikpedia Wars Application");

		extractWikiUsers(Optional.empty(), "aufrom", holder);

		return ok("Wikipedia Users Extraction Task triggered");
	}

	private static Promise<Object> extractWikiUsers(
			final Optional<String> query_continue_value,
			final String continue_field, final WSRequestHolder holder) {

		// http://en.wikipedia.org/w/api.php?action=query&list=allusers&aufrom=Ba&aulimit=max
		/*
		 * WSRequestHolder holder = WS.url("http://en.wikipedia.org/w/api.php");
		 * holder = holder.setQueryParameter("format", "json")
		 * .setQueryParameter("action", "query") .setQueryParameter("list",
		 * "allusers") .setQueryParameter("aufrom", "A"); holder =
		 * holder.setHeader("User-Agent", "Wikpedia Wars Application");
		 */
		WSRequestHolder holder_new = WS.url(holder.getUrl());
		holder_new.setHeader("User-Agent", "Wikpedia Wars Application");
		holder.getQueryParameters()
				.keySet()
				.forEach(
						key -> {
							holder_new.setQueryParameter(key, holder
									.getQueryParameters().get(key).iterator()
									.next());
						});
		if (query_continue_value.isPresent()) {
			holder_new.setQueryParameter(continue_field,
					query_continue_value.get());
		}

		return holder_new.get().flatMap(
				response -> {
					JsonNode json_response = response.asJson();
					try {
						saveToPostgreAndJedis(json_response
								.get("query")
								.findPath("allusers"));
					}catch (Exception e){
						Logger.error(e.getMessage());
					}
			
//					json_response
//							.get("query")
//							.findPath("allusers")
//							.forEach(
//									user -> {
//										jedis.sadd(REDIS_USER_SET_NAME, user
//												.findValue("name").asText());
//									});

					// responses.add(json_response);
					if (!json_response.has("query-continue")) {
						Promise<Object> response_promise = Promise
								.promise(() -> new CombinedJSONResponse(null));
						return response_promise;
					} else {
						
						Optional<String> continue_value = Optional
								.ofNullable(json_response
										.findPath("query-continue")
										.findPath("allusers")
										.findPath(continue_field).asText()
										.toString().replace(" ", "_"));

						if(continue_value.isPresent()){
							Logger.debug("Continue with "+ continue_value );
						}
						return extractWikiUsers(continue_value, continue_field, holder);
					}

				});
	}
	
	private static void saveToPostgreAndJedis(JsonNode users) throws SQLException {
		//TODO
		/* Typical User
		"userid": "13586119",
		"name": "\"V\" for Vashon",
		"blockid": "2210638",
		"blockedby": "Prolog",
		"blockedbyid": "1614547",
		"blockedtimestamp": "2010-12-08T19:37:11Z",
		"blockreason": "Abusing [[WP:Sock puppetry|multiple accounts]]: sock of [[User:Grundle2600]]",
		"blockexpiry": "infinity",
		"editcount": 6
		*/
		Connection connec =null;

		try {
		 connec = DB.getConnection();
		 try {
			 String insert_sql = "INSERT INTO wikiwars.users (userid, name, blocked, editcount,country) VALUES  (?,?,?,?,?)";
				
			 PreparedStatement  prepared_statement  = connec.prepareStatement(insert_sql);
			 
				 
					users.forEach(user -> {
						String userid = user.findValue("userid").asText();
						String name = user.findValue("name").asText();

						boolean blocked = user.has("blockexpiry");
						int editcount = user.findValue("editcount").asInt();
						
						String user_nation = jedis.hget(WikiController.REDIS_NATION_HASH_NAME,name);
			
						try {
							
							prepared_statement.setString(1, userid);
							prepared_statement.setString(2, name);
							prepared_statement.setBoolean(3,blocked);
							prepared_statement.setInt(4, editcount);
							prepared_statement.setString(5,user_nation);
							
							prepared_statement.executeUpdate();
							
							//Logger.debug("Inserted " + changed_size + " Dataset");
						} catch (Exception e) {
							Logger.error("Fail");
							e.printStackTrace();
						}
						
						
						//save to jedis
						jedis.sadd(REDIS_USER_SET_NAME, user
								.findValue("name").asText());
					});
					
					Logger.debug("Inserted " +users.size()  + " datasets");
					
					connec.close();
			
			} catch (Exception e) {
				Logger.error("Failed to get prepare statement");
				e.printStackTrace();
				connec.close();
			}
		
		} catch (Exception e) {
			Logger.error("Failed to get connection");
			e.printStackTrace();
			connec.close();
		}
		
		/*INSERT INTO wikiwars.users (userid, name, blocked, editcount) VALUES ('test','test',true,12) */



		
	}
	
	public static Result createBarChart() throws Exception{
	 	Connection connec = DB.getConnection();
	 	String query = "Select sum(editcount) as edits, country from wikiwars.users where country is not null group by country order by edits desc limit 20;";
		
	 	
	 	DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	 	
	 	Statement statement = connec.createStatement();
	 	
	 	ResultSet set = statement.executeQuery(query);
	 
	 	while(set.next()){
	 		
	 		dataset.addValue(set.getInt(1), set.getString(2),"Countries");
	 	}
	 	set.close();
	 	statement.close();
	 	System.out.println(dataset.getColumnCount());
	 	System.out.println(dataset.getRowCount());
	 	JFreeChart chart = ChartFactory.createBarChart("Sum of edits per country", "country", "sum of edits", dataset);
	    BufferedImage bi = chart.createBufferedImage(1800, 900);
	    File f1 = new File("top_20_edit_per_country.png");
	    ImageIO.write(bi, "png", f1);
	 	
	 	connec.close();
	 	
	 	return ok("Chart Created");
	}
	
	
	public static Result createChart() throws IOException, SQLException {

		JDBCXYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        
        BufferedImage bi = chart.createBufferedImage(1800, 900);
        File f1 = new File("log_all_users.png");
        ImageIO.write(bi, "png", f1);
		return ok("Triggered");
	}
	
	 private static JDBCXYDataset createDataset() throws SQLException {
		 	Connection connec = DB.getConnection();
		 	String query = "select round(log(editcount)::numeric,1) as bucket, count(*) as count from wikiwars.users group by bucket order by bucket;";
			

		 	JDBCXYDataset dataset = new JDBCXYDataset(connec, query);
		 	System.out.println(dataset.getItemCount());
		 
		 	connec.close();

	        return dataset;
	    }
	 
	 private static JFreeChart createChart(JDBCXYDataset dataset) {
		

		  final JFreeChart chart = ChartFactory.createScatterPlot("Power Law distribution Edit count to number of users", "log (Edit Count) +1 ", " Count of Users in Buckets", dataset);
				  
				  //ChartFactory.createXYAreaChart("WikiUsers", "X", "Y", dataset,  PlotOrientation.VERTICAL, true, false, false);

	        XYPlot plot = (XYPlot) chart.getPlot();
	        /*final IntervalMarker target = new IntervalMarker(400.0, 700.0);
	        target.setLabel("Target Range");
	        target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
	        target.setLabelAnchor(RectangleAnchor.LEFT);
	        target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
	        target.setPaint(new Color(222, 222, 255, 128));
	        plot.addRangeMarker(target, Layer.BACKGROUND);*/
	        return chart;    
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
		return ok(tryGeoHeuristicRegisteredUsers(userName).orElse(
				"no nation found"));
	}

	/**
	 * Heuristic to extract the nationality form a wikipedia user page
	 *
	 * @return
	 */
	@Deprecated
	public static Optional<String> tryGeoHeuristicRegisteredUsers(
			String userName) {
		String url = "http://en.wikipedia.org/wiki/User:" + userName;
		return WS
				.url(url)
				.get()
				.map(response -> {
					Document doc = Jsoup.parse(response.getBody());
					Elements contentLinks = doc.getElementById("bodyContent")
							.getElementsByTag("a");
					String s = "";
					List<String> candidates = new ArrayList<>();

					for (Element e : contentLinks) {

						String[] parts = e.attr("href").split("/");
						if (parts[parts.length - 1].contains(":"))
							continue;
						if (parts[parts.length - 1].contains("#"))
							continue;

						s += parts[parts.length - 1];
						// Logger.debug(s);
						candidates.add(parts[parts.length - 1].trim());
					}

					Optional<String> nation = candidates
							.stream()
							.filter(can -> nations().stream().anyMatch(
									na -> na.equalsIgnoreCase(can)))
							.findFirst();

					return nation;
				}).get(TIMEOUT);
	}

}
