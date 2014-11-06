package de.w4.analyzer.util;

import java.util.Date;

import de.w4.analyzer.ipgeoloc.GeoObject;



public class Revision  implements Comparable<Revision> {
	public String getHtml() {
		return html;
	}


	public void setHtml(String html) {
		this.html = html;
	}


	public String getPlainText() {
		return plainText;
	}


	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}


	public String getUser_name() {
		return user_name;
	}


	public String getUser_id() {
		return user_id;
	}


	public String getTime_stamp() {
		return time_stamp;
	}


	public int getSize() {
		return size;
	}


	public String getWikitext() {
		return wikitext;
	}


	public String getComment() {
		return comment;
	}


	public GeoObject getGeo() {
		return geo;
	}


	private String user_name;
	private String user_id;
	private String time_stamp;
	private int size;
	private String wikitext;
	private String html;
	private String plainText;
	private String comment;
	private GeoObject geo;
	private String diffhtml;
	
	private int editSize;
	
	
	public Revision(String user_name, String user_id, String time_stamp,
			int size, String diffhtml) {
		super();
		this.user_name = user_name;
		this.user_id = user_id;
		this.time_stamp = time_stamp;
		this.size = size;
		this.diffhtml = diffhtml;
	}
	
	
	public boolean userIsIP(){
		return this.user_name.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
	}


	public void setGeo(GeoObject geo) {
		this.geo = geo;
	}


	public int getEditSize() {
		return editSize;
	}


	public void setEditSize(int editSize) {
		this.editSize = editSize;
	}

	public String getDiffhtml() {
		return diffhtml;
	}


	@Override
	public int compareTo(Revision o) {
		
		Date this_date = javax.xml.bind.DatatypeConverter.parseDateTime(this.time_stamp).getTime();	
		Date compare_date = javax.xml.bind.DatatypeConverter.parseDateTime(o.time_stamp).getTime();
	
		return this_date.compareTo(compare_date);
	}
	
	
	
	 
}
