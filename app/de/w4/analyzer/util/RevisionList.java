/**
 * 
 */
package de.w4.analyzer.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * @author Michael Dell and Alexander C. Mueller
 *
 */
public class RevisionList extends ArrayList<Revision> {

	private static final long serialVersionUID = 4646829403244344628L;
	
	public static final int AGGREGATE_MONTH = 1;
	public static final int AGGREGATE_WEEK = 2;
	public static final int AGGREGATE_DAY = 3;
	
	/**
	 * Method that aggregates the revisions of a specific time bucket (Month,Week,Day)
	 * 
	 * @param aggregation_type
	 * @return
	 */
	private HashMap<String, ArrayList<Revision>> aggregateRevisionsOverTime(int aggregation_type) {
		HashMap<String, ArrayList<Revision>> result = new HashMap<String, ArrayList<Revision>>();
		for(Revision rev: this) {
			ArrayList<Revision> part = new ArrayList<Revision>();
			String aggregation_key = getAggregationKey(rev, aggregation_type);
			
			if(result.containsKey(aggregation_key)){
				part = result.get(aggregation_key);
			}else {
				result.put(aggregation_key, part);
			}
			
			part.add(rev);
			result.put(aggregation_key, part);
		}
		return result;
	}
	
	/**
	 * Helper Method to build the correct aggregation string
	 * 
	 * @param rev
	 * @param aggregation_type
	 * @return
	 */
	private String getAggregationKey(Revision rev, int aggregation_type){
		
		Calendar c = Calendar.getInstance();
		c.setTime(rev.getDate());
		
		String return_key ="";
		switch (aggregation_type) {
		case AGGREGATE_MONTH:
			return_key = c.get(Calendar.YEAR) +"-" + c.get(Calendar.MONTH);
			break;
		case AGGREGATE_WEEK:
			return_key =c.get(Calendar.YEAR) +"-"+ c.get(Calendar.WEEK_OF_YEAR);
			break;
		case AGGREGATE_DAY:
			return_key = c.get(Calendar.YEAR) +"-" + c.get(Calendar.MONTH) +"-"+c.get(Calendar.DAY_OF_MONTH);
			break;
		}
		
		return return_key;
	}
	

	
	/**
	 * 
	 * Methods to aggregate the Revisions
	 * @param aggregation_type
	 * @return
	 */
	public HashMap<String,HashMap<String,ArrayList<Revision>>> aggregateRevisionsOverTimeAndOrigin(int aggregation_type){
		HashMap<String, ArrayList<Revision>> timely = this.aggregateRevisionsOverTime(aggregation_type);
		
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
