package de.w4.analyzer.wikiuser.messages;

import java.io.Serializable;

public class ExtractionTaskDone implements Serializable{

	private static final long serialVersionUID = 2056154037456436087L;

	private long totaltime;
	
	private int user_extracted;

	public ExtractionTaskDone(long totaltime, int user_extracted) {
		super();
		this.totaltime = totaltime;
		this.user_extracted = user_extracted;
	}

	public long getTotaltime() {
		return totaltime;
	}

	public void setTotaltime(long totaltime) {
		this.totaltime = totaltime;
	}

	public int getUser_extracted() {
		return user_extracted;
	}

	public void setUser_extracted(int user_extracted) {
		this.user_extracted = user_extracted;
	}
	
	
	
}
