package de.w4.analyzer.wikiuser.messages;

import java.io.Serializable;
import java.util.List;

public class ExtractionTask implements Serializable {

	private static final long serialVersionUID = 4646605510175745244L;

	private List<String> wikipedia_user_names;

	public ExtractionTask(List<String> wikipedia_user_names) {
		super();
		this.wikipedia_user_names = wikipedia_user_names;
	}

	public List<String> getWikipedia_user_names() {
		return wikipedia_user_names;
	}

	public void setWikipedia_user_names(List<String> wikipedia_user_names) {
		this.wikipedia_user_names = wikipedia_user_names;
	}

}
