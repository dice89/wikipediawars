package de.w4.analyzer.wikiuser;

import play.Logger;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import de.w4.analyzer.wikiuser.messages.ExtractionStart;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskDone;

public class ResultListener extends UntypedActor {

	final private int size;

	private int received_size;
	
	private ActorRef master;

	final private long starttime;
	
	private long aggregated_time;
	public ResultListener(int size) {
		super();
		this.size = size;
		this.received_size = 0;
		starttime = System.currentTimeMillis();
		aggregated_time = 0;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		
		if(message instanceof ExtractionStart) {
			master = getSender();
		}else if (message instanceof ExtractionTaskDone) {
			long time_used = System.currentTimeMillis() - starttime;
			ExtractionTaskDone result = (ExtractionTaskDone) message;

			//update received size
			received_size = received_size + result.getUser_extracted();
			
			aggregated_time = aggregated_time+ result.getTotaltime();
			
			Logger.debug("Total Extracted Users:" + received_size + "in total tim " + time_used + " with working time "+ aggregated_time);
			//Stopping Condition
			if(received_size >= size){
			
				Logger.debug("Extraction done:" + received_size);
				master.tell(new ExtractionTaskDone(time_used, received_size), getSelf());
				
			}			
		}
	}

}
