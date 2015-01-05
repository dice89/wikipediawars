package de.w4.analyzer;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanResult;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import controllers.WikiController;
import de.w4.analyzer.wikiuser.NationExtractionRunner;
import de.w4.analyzer.wikiuser.ResultListener;

/**
 * Triggers an Akka task that extracts for each user stored in a redis DB a call
 * to retrieve the origin based on their user profile in Wikipedia (currently
 * only for english wikipedia)
 * 
 * @author Alexander C. Mueller
 *
 */
public class UserAnalyzer {
	private static Jedis jedis = new Jedis(WikiController.REDIS_HOST,
			WikiController.REDIS_PORT);

	/**
	 * Extract Data from Jedis
	 * 
	 * @return
	 */
	private static List<String> getUserNames() {

		List<String> user_names = new LinkedList<String>();

		ScanResult<String> res = null;
		Optional<String> cursor = Optional.empty();

		do {
			res = jedis.sscan(WikiController.REDIS_USER_SET_NAME,
					cursor.orElse(SCAN_POINTER_START));
			// System.out.println(res.getStringCursor());
			user_names.addAll(res.getResult());
			cursor = Optional.of(res.getStringCursor());
		} while (!cursor.get().equals("0"));

		System.out.println(user_names.size());

		return user_names;
	}

	/**
	 * Starts the analysis
	 */
	public static void analyze() {
		// Start akka job for analysis
		List<String> user_list = getUserNames();

		// List<String> user_list = new ArrayList<String>();
		//
		// user_list.add("Test1"); user_list.add("Test2");
		// user_list.add("Test3"); user_list.add("Test4");
		// user_list.add("Test5"); user_list.add("Test6");
		// user_list.add("Test7"); user_list.add("Test8");
		// user_list.add("Test9"); user_list.add("Test10");
		// user_list.add("Test11"); user_list.add("Test12");
		//

		ActorSystem system = ActorSystem.create("WikiNationExtractionSystem");

		final ActorRef listener = system.actorOf(
				Props.create(ResultListener.class, user_list.size()),
				"listener");

		ActorRef master = system.actorOf(Props.create(
				NationExtractionRunner.class, 10, 100000, listener, user_list),
				"master");
		
		

	}

}