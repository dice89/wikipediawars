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

	public ResultListener(int size) {
		super();
		this.size = size;
		this.received_size = 0;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		
		if(message instanceof ExtractionStart) {
			master = getSender();
		}else if (message instanceof ExtractionTaskDone) {
			ExtractionTaskDone result = (ExtractionTaskDone) message;
			
			//update received size
			received_size = received_size + result.getUser_extracted();
			
			Logger.debug("Total Extracted Users:" + received_size);
			//Stopping Condition
			if(received_size >= size){
				Logger.debug("Extraction done:" + received_size);
				master.tell(new ExtractionTaskDone(0, received_size), getSelf());
				
			}			
		}
	}

}
