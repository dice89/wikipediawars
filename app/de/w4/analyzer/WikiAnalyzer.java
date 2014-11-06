package de.w4.analyzer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.maxmind.geoip2.exception.GeoIp2Exception;

import de.w4.analyzer.ipgeoloc.GeoObject;
import de.w4.analyzer.ipgeoloc.IPLocExtractor;
import de.w4.analyzer.util.Revision;
import de.w4.analyzer.util.RevisionList;
import de.w4.analyzer.util.RevisionSummaryObject;
import de.w4.analyzer.util.RevisionSummaryObjectGroup;

/**
 * Class for a Singleton Object, that manages the whole wikipedia content
 * analysis.
 * 
 * Implements and origin analysis of wikipedia revisions based on underlying ips
 * and and an analysis what the most changed N-Gramms of an article are
 * 
 * @author Alexander C. Mueller
 *
 */
public class WikiAnalyzer {

	// singleton
	private static WikiAnalyzer singleton;

	private IPLocExtractor ipExtractor;

	public static synchronized WikiAnalyzer getWikiAnalyzer()
			throws IOException {
		if (singleton == null) {
			singleton = new WikiAnalyzer();
		}
		return singleton;
	}

	// constructor
	private WikiAnalyzer() throws IOException {
		this.ipExtractor = new IPLocExtractor();
	}

	/**
	 * Method that analyzes the origin of Wikipedia Revisions, no text analysis
	 * here
	 * 
	 * @param revision_arrays
	 * @throws ParseException
	 */
	public ArrayList<RevisionSummaryObjectGroup> analyzeGeoOrigin(
			List<JsonNode> revision_arrays) throws ParseException {
		// list of json answers looping over them

		RevisionList rev_list = parseJSON(revision_arrays);
		System.out.println("Revisions parsed");
		analyzeDifferences(rev_list);
		return this.aggregateOverGeoLocAndTime(rev_list);

	}

	/**
	 * Method that analysis the text difference between different revisions
	 * 
	 * @param revision_arrays
	 */
	public void analyzeDifferences(RevisionList revisions) {
		
		for (Revision revision : revisions) {
			if(revision.getDiffhtml().length()==0) continue;
			
			//do some replacements

			Document doc = Jsoup.parse(revision.getDiffhtml());
			Elements elements = doc.select(".diffchange-inline");
			System.out.println(elements.size());
			for (Element element : elements) {
				System.out.println(element.nodeName());
				
				
				System.out.println(cleanDiffText(element.html()));
			}
		}
		// TODO
	}
	
	private String cleanDiffText(String diff){
		String toreturn = diff.replace("]]", "")
				.replace("[[", "")
				.replace("''", "")
				.replace("\"", "")
				.replace("|", " ")
				.replace("_"," ");
		return toreturn;
	}

	/**
	 * Parses An Array of Json Revision Arrays
	 * 
	 * @param revision_arrays
	 * @return
	 */
	private RevisionList parseJSON(List<JsonNode> revision_arrays) {

		RevisionList rev_list = new RevisionList();
		for (JsonNode revisions : revision_arrays) {
			JsonNode revision = null;
			Revision revision_obj = null;

			// loop over different revision elements
			for (int i = 0; i < revisions.size(); i++) {
				revision = revisions.get(i);
				try {
					// parse JSON object
					revision_obj = parseJSONSingleRevision(revision);

					// set edited size

					rev_list.add(revision_obj);
				} catch (Exception e) {
					System.out.println("sick entry");
					System.out.println(revision.toString());
				}

			}

		}

		return calcEditSize(rev_list);
	}

	/**
	 * Method that calculates the edit size of a revisions as a the current size
	 * - the size of the revision before, so it's positive for adding content
	 * and negative when more content is delete then added
	 * 
	 * @param rev_list
	 * @return
	 */
	private RevisionList calcEditSize(RevisionList rev_list) {
		Collections.sort(rev_list);
		int new_rev_size;
		int old_rev_size;
		for (int i = 0; i < rev_list.size(); i++) {

			if (i == 0) {
				rev_list.get(i).setEditSize(rev_list.get(i).getSize());
			} else {
				new_rev_size = rev_list.get(i).getSize();
				old_rev_size = rev_list.get(i - 1).getSize();

				rev_list.get(i).setEditSize(new_rev_size - old_rev_size);
			}
		}
		return rev_list;
	}

	/**
	 * Method that parses an single revision JSON to the internal representation
	 * on an Revision
	 * 
	 * @param revision
	 * @return
	 * @throws GeoIp2Exception
	 */
	private Revision parseJSONSingleRevision(JsonNode revision)
			throws GeoIp2Exception {

		String user = revision.findValue("user").asText();
		String user_id = revision.findValue("userid").asInt() + "";
		String timestamp = revision.findValue("timestamp").asText();
		String diffhtml ="";
		try{
			 diffhtml = revision.findPath("diff").findValue("*").asText().replace("\\", "");
		}catch(Exception e){
			//Difference from wiki api not cached ignore it!
		}
		int size = revision.findValue("size").asInt();

		Revision rev = new Revision(user, user_id, timestamp, size, diffhtml);

		// verify if is IP
		if (rev.userIsIP()) {
			try {
				// retrieve Geo Location
				rev.setGeo(ipExtractor.getGeoLocationForIP(rev.getUser_name()));
			} catch (URISyntaxException | IOException e) {
				rev.setGeo(new GeoObject(0.0, 0.0, ""));
			}
		} else {
			// no location
			rev.setGeo(new GeoObject(0.0, 0.0, ""));
		}

		return rev;
	}

	/**
	 * Aggregates the Revisions over the location and by time, so that for each
	 * country and for each day only one aggregate is available
	 * 
	 * @param revisions
	 * @return
	 * @throws ParseException
	 */
	private ArrayList<RevisionSummaryObjectGroup> aggregateOverGeoLocAndTime(
			RevisionList revisions) throws ParseException {

		HashMap<String, HashMap<String, ArrayList<Revision>>> aggregates = revisions
				.aggregateRevisionsOverTimeAndOrigin();
		ArrayList<RevisionSummaryObject> summaryObject = new ArrayList<RevisionSummaryObject>();
		ArrayList<RevisionSummaryObjectGroup> summaryObjectList = new ArrayList<RevisionSummaryObjectGroup>();
		HashMap<Date, ArrayList<RevisionSummaryObject>> datemap = new HashMap<Date, ArrayList<RevisionSummaryObject>>();

		for (String date : aggregates.keySet()) {

			HashMap<String, ArrayList<Revision>> map = aggregates.get(date);

			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Date result = df.parse(date);

			RevisionSummaryObjectGroup sog = new RevisionSummaryObjectGroup(
					result);

			for (String code : map.keySet()) {
				ArrayList<Revision> reflist = map.get(code);
				String country = code;
				int frequency = reflist.size();
				int editSize = 0;
				for (Revision revision : reflist) {
					editSize = editSize + Math.abs(revision.getEditSize());
				}

				double avg_editSize = editSize / frequency;
				RevisionSummaryObject rso = new RevisionSummaryObject(
						frequency, avg_editSize, country, date);
				sog.addSummary(rso);
			}

			summaryObjectList.add(sog);
		}

		Collections.sort(summaryObjectList);

		return summaryObjectList;
	}

}