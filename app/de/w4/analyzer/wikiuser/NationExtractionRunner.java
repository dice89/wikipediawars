package de.w4.analyzer.wikiuser;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinPool;
import de.w4.analyzer.wikiuser.messages.ExtractionStart;
import de.w4.analyzer.wikiuser.messages.ExtractionTask;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskDone;

public class NationExtractionRunner extends UntypedActor {

	private int no_of_worker;

	private int size_of_user_list_chunk;

	private List<String> user_names;

	private final ActorRef listener;
	private final ActorRef workerRouter;

	public NationExtractionRunner(int no_of_worker,
			int size_of_user_list_chunk, ActorRef listener,
			List<String> user_names) {
		super();
		this.no_of_worker = no_of_worker;
		this.size_of_user_list_chunk = size_of_user_list_chunk;

		workerRouter = getContext().actorOf(
				new RoundRobinPool(this.no_of_worker).props(Props
						.create(NationExtractionWorker.class)), "router");

		this.listener = listener;

		this.user_names = user_names;

		getSelf().tell(new ExtractionStart(), getSelf());

	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ExtractionStart) {
			// register at listener by telling them the analysis has started
			listener.tell(new ExtractionStart(), getSelf());

			// start calculating

			int group_size = ((int) Math
					.floor((((double) user_names.size()) / ((double) size_of_user_list_chunk))));

			Logger.debug("Configuration results in chunk size of: "
					+ group_size);

			List<String> sub_group = new ArrayList<String>();

			for (int i = 0; i < user_names.size(); i++) {
				sub_group.add(user_names.get(i));
				// check group_size + not first element
				// and do it for the rest if groupsize will not be reached for
				// the last group
				if ((((i % group_size) == 0) && i > 0)
						|| (i == user_names.size() - 1)) {
					
					workerRouter.tell(new ExtractionTask(sub_group), getSelf());
					sub_group = new ArrayList<String>();
				}
			}

		} else if (message instanceof ExtractionTaskDone) {
			Logger.debug("Stop Extraction Task Runner and Router");

			getContext().stop(getSelf());

		}

	}

}
