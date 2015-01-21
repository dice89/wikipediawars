package de.w4.analyzer.wikiuser.messages;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;


public class ExtractionTaskResult implements Serializable {

	private static final long serialVersionUID = 3863491495585189193L;

	private Map<String,Optional<String>> extracted_nations_per_user;

	private long time_used;
	
	public ExtractionTaskResult(
			Map<String, Optional<String>> extracted_nations_per_user,
			long time_used) {
		super();
		this.extracted_nations_per_user = extracted_nations_per_user;
		this.time_used = time_used;
	}

	public Map<String, Optional<String>> getExtracted_nations_per_user() {
		return extracted_nations_per_user;
	}


	public long getTime_used() {
		return time_used;
	}


	
	
	
	
}
