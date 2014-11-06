package de.w4.analyzer.util;

public class TFIDFWord  implements Comparable<TFIDFWord> {
	private double tfidf;
	private String word;
	public TFIDFWord(double tfidf, String word){
		super();
		this.tfidf = tfidf;
		this.word = word;
	}
	public double getTfidf() {
		return tfidf;
	}
	public String getWord() {
		return word;
	}
	public int compareTo(TFIDFWord o) {
		if(o.tfidf < this.tfidf) return -1;
		if(o.tfidf > this.tfidf) return 1;
		return 0;
	}
}
