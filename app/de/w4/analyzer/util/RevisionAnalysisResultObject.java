package de.w4.analyzer.util;

import java.util.List;

public class RevisionAnalysisResultObject {
	List<TFIDFWord> most_specfic_terms;

	List<RevisionSummaryObjectGroup> revisions;
	
	public RevisionAnalysisResultObject(List<TFIDFWord> most_specfic_terms,
			List<RevisionSummaryObjectGroup> revisions) {
		super();
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
	
	


}