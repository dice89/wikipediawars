package de.w4.analyzer.wikiuser;

import play.Logger;
import controllers.WikiController;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskDone;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskResult;
import redis.clients.jedis.Jedis;
import akka.actor.UntypedActor;

public class NationExtractionSaver extends UntypedActor {
	
	private Jedis jedis;

	
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ExtractionTaskResult) {
			
			this.jedis = new Jedis(WikiController.REDIS_HOST, WikiController.REDIS_PORT);
			ExtractionTaskResult result = (ExtractionTaskResult) message;
			Logger.debug("Received Save Task");
	
			result.getExtracted_nations_per_user()
					.forEach((name, nation) -> {

						if (nation.isPresent()) {
							// Jedis add received_nations_real
							Logger.debug("Saved: " +name +"---" +nation.get());
					
							jedis.hset(WikiController.REDIS_NATION_HASH_NAME, name, nation.get());
						}
					});
			
			getContext().actorSelection("/user/listener").tell(new ExtractionTaskDone(result.getTime_used(), result.getExtracted_nations_per_user().size()), getSelf());

		}
		
	}

}
