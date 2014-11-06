package de.w4.analyzer.util;

public class RevisionSummaryObject {
	private int frequency;
	private double editSize;
	private String country;
	public RevisionSummaryObject(int frequency, double editSize, String country, String date) {
		super();
		this.frequency = frequency;
		this.editSize = editSize;
		this.country = country;
		this.date = date;
	}
	public int getFrequency() {
		return frequency;
	}
	public double getEditSize() {
		return editSize;
	}
	public String getCountry() {
		return country;
	}
	
	private String date;
}