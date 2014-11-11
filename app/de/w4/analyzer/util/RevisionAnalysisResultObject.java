package de.w4.analyzer.util;

import java.util.List;

/**
 * 
 * Object wrapping JSON Response Object
 * @author Alexander C. Mueller
 *
 */
public class RevisionAnalysisResultObject {
	
	private int last_revision_id;
	private int revisions_total;
	private String newest_revision_time;
	private long analysis_time;
	

	List<TFIDFWord> most_specfic_terms;

	List<RevisionSummaryObjectGroup> revisions;
	
	
	
	
	public RevisionAnalysisResultObject() {
		super();
	}

	public RevisionAnalysisResultObject(int last_revision_id,
			int revisions_total, String newest_revision_time,
			long analysis_time, List<TFIDFWord> most_specfic_terms,
			List<RevisionSummaryObjectGroup> revisions) {
		super();
		this.last_revision_id = last_revision_id;
		this.revisions_total = revisions_total;
		this.newest_revision_time = newest_revision_time;
		this.analysis_time = analysis_time;
		this.most_specfic_terms = most_specfic_terms;
		this.revisions = revisions;
	}

	public List<TFIDFWord> getMost_specfic_terms() {
		return most_specfic_terms;
	}

	public void setMost_specfic_terms(List<TFIDFWord> most_specfic_terms) {
		this.most_specfic_terms = most_specfic_terms;
	}

	public List<RevisionSummaryObjectGroup> getRevisions() {
		return revisions;
	}

	public void setRevisions(List<RevisionSummaryObjectGroup> revisions) {
		this.revisions = revisions;
	}

	public int getLast_revision_id() {
		return last_revision_id;
	}

	public void setLast_revision_id(int last_revision_id) {
		this.last_revision_id = last_revision_id;
	}

	public int getRevisions_total() {
		return revisions_total;
	}

	public void setRevisions_total(int revisions_total) {
		this.revisions_total = revisions_total;
	}

	public String getNewest_revision_time() {
		return newest_revision_time;
	}

	public void setNewest_revision_time(String newest_revision_time) {
		this.newest_revision_time = newest_revision_time;
	}

	public long getAnalysis_time() {
		return analysis_time;
	}

	public void setAnalysis_time(long analysis_time) {
		this.analysis_time = analysis_time;
	}
	
	
	


}