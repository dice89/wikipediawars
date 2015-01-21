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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.JsonNode;
import com.maxmind.geoip2.exception.GeoIp2Exception;

import controllers.WikiController;
import de.w4.analyzer.ipgeoloc.GeoObject;
import de.w4.analyzer.ipgeoloc.IPLocExtractor;
import de.w4.analyzer.util.Revision;
import de.w4.analyzer.util.RevisionAnalysisResultObject;
import de.w4.analyzer.util.RevisionList;
import de.w4.analyzer.util.RevisionSummaryObject;
import de.w4.analyzer.util.RevisionSummaryObjectGroup;
import de.w4.analyzer.util.TFIDFWord;

/**
 * Class for a Singleton Object, that manages the whole Wikipedia content
 * analysis.
 * 
 * Implements and origin analysis of Wikipedia revisions based on underlying ips
 * and an analysis what the most changed N-Grams of an article are
 * 
 * @author Alexander C. Mueller, Michael Dell
 *
 */
public class WikiAnalyzer {
	
	public static final String NO_USER_LOCATION_FOUND = "NK";
	// singleton
	private static WikiAnalyzer singleton;

	private IPLocExtractor ipExtractor;
	
	private Jedis jedis;

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
		
		this.jedis = new Jedis(WikiController.REDIS_HOST, WikiController.REDIS_PORT);
	}

	/**
	 * Method that analyzes the origin of Wikipedia Revisions, no text analysis
	 * here
	 * 
	 * @param revision_arrays
	 * @throws ParseException
	 * @throws IOException
	 */
	public RevisionAnalysisResultObject analyzeGeoOrigin(
			List<JsonNode> revision_arrays, int topKTerms, int aggregation_type)
			throws ParseException, IOException {

		long startime = System.currentTimeMillis();
		// pasre JSON to internal revision representation
		RevisionList rev_list = parseJSON(revision_arrays);

		// Analysis of the difference
		List<TFIDFWord> words = analyzeDifferences(rev_list, topKTerms);
		ArrayList<RevisionSummaryObjectGroup> grouped = this
				.aggregateOverGeoLocAndTime(rev_list, aggregation_type);

		// some time capturing
		long endtime = System.currentTimeMillis();
		long analysistime = (endtime - startime);

		// create result object
		return new RevisionAnalysisResultObject(rev_list.get(
				rev_list.size() - 1).getRev_id(), rev_list.size(), rev_list
				.get(rev_list.size() - 1).getTime_stamp(), analysistime, words,
				grouped);

	}

	/**
	 * Method that analysis the text difference between different revisions
	 * 
	 * @param revision_arrays
	 * @return
	 * @throws IOException
	 */
	public List<TFIDFWord> analyzeDifferences(RevisionList revisions, int k)
			throws IOException {

		RAMDirectory idx = new RAMDirectory();

		IndexWriter writer = new IndexWriter(idx, new IndexWriterConfig(
				Version.LATEST, new StandardAnalyzer()));

		Revision revision;
		for (int i = 0; i < revisions.size(); i++) {
			revision = revisions.get(i);
			if (revision.getDiffhtml().length() == 0)
				continue;

			// parsee html document

			Document doc = Jsoup.parseBodyFragment("<table>"
					+ revision.getDiffhtml() + "</table>");

			// get all changes via css query
			Elements ins_elements = doc.select(".diff-addedline");
			Elements del_elements = doc.select(".diff-deletedline");

			// extract the content per category
			String ins_content = getContent(ins_elements, "ins");
			String del_content = getContent(del_elements, "del");

			// add lucene documents
			org.apache.lucene.document.Document lucene_doc_ins = getLuceneDoc(
					ins_content, "ins", i);
			writer.addDocument(lucene_doc_ins);

			org.apache.lucene.document.Document lucene_doc_del = getLuceneDoc(
					del_content, "del", i);
			writer.addDocument(lucene_doc_del);

		}

		writer.close();

		IndexReader reader = DirectoryReader.open(idx);

		Map<String, Float> docFrequencies = getIDF(reader);

		List<TFIDFWord> wordVector = tfidf(reader, docFrequencies, revisions);
		reader.close();
		idx.close();

		if (wordVector.size() >= k) {
			return wordVector.subList(0, k);
		}
		return wordVector;
	}

	/**
	 * Method creates a lucene document for TF and TFIDF computation
	 * 
	 * @param content
	 * @param node_type
	 * @param doc_index
	 * @return
	 */
	private org.apache.lucene.document.Document getLuceneDoc(String content,
			String node_type, int doc_index) {
		org.apache.lucene.document.Document lucene_doc = new org.apache.lucene.document.Document();

		FieldType type = new FieldType();

		type.setIndexed(true);
		type.setStored(true);
		type.setStoreTermVectors(true);

		// stsore cleaned content
		lucene_doc.add(new Field("content", content, type));

		FieldType type2 = new FieldType();
		type2.setIndexed(false);
		type2.setStored(true);
		type2.setStoreTermVectors(false);
		// store meta info fields
		lucene_doc.add(new Field("internal_id", doc_index + "", type2));
		lucene_doc.add(new Field("interal_type", node_type, type2));
		return lucene_doc;

	}

	/**
	 * Extracts the html content from the diff html
	 * 
	 * @param elements
	 * @param type
	 * @return
	 */
	private String getContent(Elements elements, String type) {
		String content = "";
		// loop over them
		for (Element main_element : elements) {
			Elements subelements = main_element.select(".diffchange-inline");

			if (subelements.size() == 0) {
				content = content + " " + cleanDiffText(main_element.html());
			} else {
				for (Element element : subelements) {

					if (element.nodeName().equals(type)) {
						content = content + " " + cleanDiffText(element.html());
					}
				}
			}
		}
		return content;
	}

	/**
	 * This method cleans a given difference string from, not parsed mark-up
	 * parts
	 * 
	 * @param diff
	 * @return
	 */
	private String cleanDiffText(String diff) {
		String toreturn = diff
				.replaceAll(
						"\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
						"")
				// urls
				.replaceAll("/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*/?", " ")
				// sick urls
				.replaceAll("&[a-z]+;", " ")
				// special charactesr
				.replaceAll("[a-z]+\\s*=", "")
				.replaceAll("(([0-9]*)(px))", "")
				// remove pixel entries
				// meta fields like date =
				.replace("((\\-*[a-z,A-Z,0-9]*\\-*)*).html", "")
				// only html links
				.replace("JPG", "")
				.replace("jpg", "")
				.replace("ref", " ")
				// ref
				.replace("]]", "").replace("[[", "").replace("''", "")
				.replace("\"", "").replace("|", " ").replace("_", " ")
				.replace("{{cite", "").replace("{{Citation needed}}", "")
				.replace("{{", " ").replace("}}", " ").replace("[", "")
				.replace("]", "").replace("\\", "");

		return toreturn;
	}

	/**
	 * Method to get IDF of Terms based on Lucene handling
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	protected Map<String, Float> getIDF(IndexReader reader) throws IOException {
		Fields fields = MultiFields.getFields(reader);

		TFIDFSimilarity tfidfsim = new DefaultSimilarity();

		Map<String, Float> docFrequencies = new HashMap<>();
		for (String field : fields) {
			TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(
					null);
			BytesRef bytesRef;
			while ((bytesRef = termEnum.next()) != null) {
				if (termEnum.seekExact(bytesRef)) {
					String term = bytesRef.utf8ToString();
					float idf = tfidfsim.idf(termEnum.docFreq(),
							reader.numDocs());
					// play here a little bit
					docFrequencies.put(term, (float) Math.log(idf));
				}
			}
		}
		return docFrequencies;
	}

	/**
	 * 
	 * Calculated TFIDF Values with help of lucene
	 * 
	 * @param reader
	 * @param docFrequencies
	 * @param revisions
	 * @param termFrequencies
	 * @param tf_Idf_Weights
	 * @throws IOException
	 */
	protected List<TFIDFWord> tfidf(IndexReader reader,
			Map<String, Float> docFrequencies, RevisionList revisions)
			throws IOException {
		TFIDFSimilarity tfidfsim = new DefaultSimilarity();
		Map<String, Float> termFrequencies = new HashMap<>();

		Map<String, Float> tf_Idf_Weights = new HashMap<>();

		List<TFIDFWord> wordList = new ArrayList<TFIDFWord>();
		for (int docID = 0; docID < reader.maxDoc(); docID++) {
			// get all fields, but in general should just be one "content"
			Fields fields = MultiFields.getFields(reader);
			for (String field : fields) {

				// if not the content field then continue
				if (!field.equals("content"))
					continue;

				// String field = "content";
				TermsEnum termsEnum = MultiFields.getTerms(reader, field)
						.iterator(null);
				DocsEnum docsEnum = null;
				// get term vectors

				// get Revision for this document

				Terms vector = reader.getTermVector(docID, field);

				// nothing stored so continue
				if (vector == null)
					continue;
				termsEnum = vector.iterator(termsEnum);

				BytesRef bytesRef = null;
				// set containing all terms
				Set<String> terms = new HashSet<String>();
				// enumerate over terms
				while ((bytesRef = termsEnum.next()) != null) {
					if (termsEnum.seekExact(bytesRef)) {
						String term = bytesRef.utf8ToString();
						terms.add(term);
						float tf = 0;
						// pretty inefficient from here one because the tfidf is
						// computed multiple times per term

						docsEnum = termsEnum.docs(null, null,
								DocsEnum.FLAG_FREQS);
						while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

							// get term frequency
							tf = tfidfsim.tf(docsEnum.freq());
							termFrequencies.put(term, tf);
						}

						float idf = docFrequencies.get(term);
						float w = tf * idf;

						if (!tf_Idf_Weights.containsKey(term)) {
							tf_Idf_Weights.put(term, w);

							wordList.add(new TFIDFWord(w, term));
						}

					}
				}

				// handle revisions deleted and inserted terms
				int rev_id = Integer.parseInt(reader.document(docID)
						.getField("internal_id").stringValue().trim());

				String type = reader.document(docID).getField("interal_type")
						.stringValue().trim();
				Revision rev = revisions.get(rev_id);

				if (type.equals("ins")) {
					rev.setInsertedTerms(terms);
				} else if (type.equals("del")) {
					rev.setDeletedTerms(terms);
				}

			}
		}
		Collections.sort(wordList);

		return wordList;
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
					Logger.error("sick_entry" + revision.toString());
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
	 * @throws ParseException
	 */
	private Revision parseJSONSingleRevision(JsonNode revision)
			throws GeoIp2Exception, ParseException {

		String user = revision.findValue("user").asText();

		int rev_id = revision.findValue("revid").asInt();
		String user_id = revision.findValue("userid").asInt() + "";

		String timestamp = revision.findValue("timestamp").asText();
		
		String diffhtml = "";
		try {
			diffhtml = revision.findPath("diff").findValue("*").asText()
					.replace("\\", "").replace("==", "");
		} catch (Exception e) {
			// Difference from wiki api not cached ignore it!
		}
		int size = revision.findValue("size").asInt();

		Revision rev = new Revision(user, user_id, timestamp, size, diffhtml,
				rev_id);

		// verify if is IP
		if (rev.userIsIP()) {
			try {
				// retrieve Geo Location
				rev.setGeo(ipExtractor.getGeoLocationForIP(rev.getUser_name()));
			} catch (URISyntaxException | IOException e) {
				rev.setGeo(new GeoObject(0.0, 0.0, NO_USER_LOCATION_FOUND));
			}

		} else {
			// no location
			String user_name = jedis.hget(WikiController.REDIS_NATION_HASH_NAME,rev.getUser_name());
			if(user_name != null) {
				rev.setGeo(new GeoObject(0.0, 0.0, user_name));
			}else {
				//nothing found
				rev.setGeo(new GeoObject(0.0, 0.0, NO_USER_LOCATION_FOUND));
			}
			
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
			RevisionList revisions, int aggregation_type) throws ParseException {

		HashMap<String, HashMap<String, ArrayList<Revision>>> aggregates = revisions
				.aggregateRevisionsOverTimeAndOrigin(aggregation_type);
		ArrayList<RevisionSummaryObjectGroup> summaryObjectList = new ArrayList<RevisionSummaryObjectGroup>();

		for (String date : aggregates.keySet()) {

			HashMap<String, ArrayList<Revision>> map = aggregates.get(date);
			DateFormat df = null;

			switch (aggregation_type) {
			case RevisionList.AGGREGATE_DAY:
				df = new SimpleDateFormat("yyyy-MM-dd");
				break;
			case RevisionList.AGGREGATE_WEEK:
				df = new SimpleDateFormat("yyyy-ww");
				break;
			case RevisionList.AGGREGATE_MONTH:
				df = new SimpleDateFormat("yyyy-MM");
				break;
			}

			// DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Date result = df.parse(date);

			
			RevisionSummaryObjectGroup sog = new RevisionSummaryObjectGroup(result);

			for (String code : map.keySet()) {
				ArrayList<Revision> reflist = map.get(code);
				Set<String> inserted_terms = new HashSet<String>();
				Set<String> deleted_terms = new HashSet<String>();
				String country = code;
				int frequency = reflist.size();
				int editSize = 0;

				for (Revision revision : reflist) {
					editSize = editSize + Math.abs(revision.getEditSize());
					inserted_terms.addAll(revision.getInsertedTerms());
					deleted_terms.addAll(revision.getDeletedTerms());
				}

				double avg_editSize = editSize / frequency;
				RevisionSummaryObject rso = new RevisionSummaryObject(
						frequency, avg_editSize, country, date, inserted_terms,
						deleted_terms);
				sog.addSummary(rso);
			}

			summaryObjectList.add(sog);
		}

		Collections.sort(summaryObjectList);

		return summaryObjectList;
	}

}