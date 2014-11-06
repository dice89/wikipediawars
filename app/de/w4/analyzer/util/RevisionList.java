/**
 * 
 */
package de.w4.analyzer.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author apfelbaum24
 *
 */
public class RevisionList extends ArrayList<Revision> {

	private static final long serialVersionUID = 4646829403244344628L;

	public HashMap<String, String> aggregateComments() {
		HashMap<String, String> result = new HashMap<String, String>();
		for(Revision rev: this) {
			String key = rev.getTime_stamp().substring(0, 10);//Date
			System.out.println(key);
			String current;
			if (result.get(key) != null) {
				current = result.get(key);
			} else
				current = "";
			result.put(key, current + " " + rev.getComment().replaceAll("\\<.*?>",""));
		}
		return result;
	}
	
	public HashMap<String, ArrayList<Revision>> aggregateRevisionsOverTime() {
		HashMap<String, ArrayList<Revision>> result = new HashMap<String, ArrayList<Revision>>();
		for(Revision rev: this) {
			ArrayList<Revision> part = new ArrayList<Revision>();
			String key = rev.getTime_stamp().substring(0, 10);//Date
			
			if(result.containsKey(key)){
				part = result.get(key);
			}else {
				result.put(key, part);
			}
			
			part.add(rev);
			result.put(key, part);
		}
		return result;
	}
	
	public HashMap<String,HashMap<String,ArrayList<Revision>>> aggregateRevisionsOverTimeAndOrigin(){
		HashMap<String, ArrayList<Revision>> timely = this.aggregateRevisionsOverTime();
		
		HashMap<String,HashMap<String,ArrayList<Revision>>> result = new HashMap<String, HashMap<String,ArrayList<Revision>>>();
		
		for(String date : timely.keySet()){
			ArrayList<Revision> revs= timely.get(date);
			//init country map
			HashMap<String, ArrayList<Revision>> map = new HashMap<String, ArrayList<Revision>>();
			if(result.containsKey(date)){
				map = result.get(date);
			}
			
			
			//for loop countries
			for (Revision revision : revs) {
				ArrayList<Revision> parts; 
				if(map.containsKey(	revision.getGeo().getCountryCode())){
					parts = map.get(revision.getGeo().getCountryCode());
				}else {
					parts = new ArrayList<Revision>();
				}
				
				parts.add(revision);
				map.put(revision.getGeo().getCountryCode(),parts);
			}
			
			result.put(date, map);
			
		}
		return result;
		
	}

}
