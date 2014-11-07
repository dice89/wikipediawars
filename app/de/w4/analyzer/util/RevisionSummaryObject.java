package de.w4.analyzer.util;

import java.util.Set;

public class RevisionSummaryObject {
	private int frequency;
	private double editSize;
	private String country;
	private String date;
	
	private Set<String> insertedTerms;
	
	private Set<String> deletedTerms;
	
	public RevisionSummaryObject(int frequency, double editSize,
			String country, String date, Set<String> insertedTerms,
			Set<String> deletedTerms) {
		super();
		this.frequency = frequency;
		this.editSize = editSize;
		this.country = country;
		this.date = date;
		this.insertedTerms = insertedTerms;
		this.deletedTerms = deletedTerms;
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
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public Set<String> getInsertedTerms() {
		return insertedTerms;
	}
	public void setInsertedTerms(Set<String> insertedTerms) {
		this.insertedTerms = insertedTerms;
	}
	public Set<String> getDeletedTerms() {
		return deletedTerms;
	}
	public void setDeletedTerms(Set<String> deletedTerms) {
		this.deletedTerms = deletedTerms;
	}
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
	public void setEditSize(double editSize) {
		this.editSize = editSize;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	
	
}