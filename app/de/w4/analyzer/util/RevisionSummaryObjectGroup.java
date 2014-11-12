package de.w4.analyzer.util;

import java.util.ArrayList;
import java.util.Date;

public class RevisionSummaryObjectGroup implements Comparable<RevisionSummaryObjectGroup>{
	
	private Date timeStamp;
	
	private ArrayList<RevisionSummaryObject> data;

	public RevisionSummaryObjectGroup(Date timeStamp) {
		super();
		this.timeStamp = timeStamp;
		this.data = new ArrayList<RevisionSummaryObject>();
	}

	public RevisionSummaryObjectGroup() {
		this.data = new ArrayList<RevisionSummaryObject>();
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public ArrayList<RevisionSummaryObject> getSummary() {
		return data;
	}
	
	public void addSummary(RevisionSummaryObject obj){
		this.data.add(obj);
	}

	public int compareTo(RevisionSummaryObjectGroup o) {
	    return getTimeStamp().compareTo(o.getTimeStamp());
	}
}

	
