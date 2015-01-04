package de.w4.analyzer.wikiuser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import play.Play;
import play.libs.ws.WS;
import redis.clients.jedis.Jedis;
import scala.Option;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import controllers.WikiController;
import de.w4.analyzer.wikiuser.messages.ExtractionTask;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskResult;

public class NationExtractionWorker extends UntypedActor {
	
	private List<String> nation_list = nations();
	private Jedis jedis;
	
	@Override
	public void onReceive(Object message) throws Exception {
		
		if (message instanceof ExtractionTask) {
			this.jedis = new Jedis(WikiController.REDIS_HOST, WikiController.REDIS_PORT);
		
			ExtractionTask task = (ExtractionTask) message;	
			
			Logger.debug("Got Extraction Task for size of: " + task.getWikipedia_user_names().size());
			ExtractionTaskResult result = extractionNationsForUsers(task);
			
			ActorRef saver = null;
			Option<ActorRef> saver_ref = getContext().child("saver");
			
			if(saver_ref.isDefined()){
				saver = saver_ref.get();
			}else {
				 saver = getContext().actorOf(
						Props.create(NationExtractionSaver.class), "saver");
			}
			
			saver.tell(result, getSelf());
	
		}
	}

	/**
	 * @param task
	 * @return
	 */
	public ExtractionTaskResult extractionNationsForUsers(ExtractionTask task) {
		long starttime = System.currentTimeMillis();
		final Map<String, Optional<String>> found_users = new HashMap<String, Optional<String>>();

		// get for all
		task.getWikipedia_user_names().forEach(user -> {
			
			String clean_string = user.replace(" ", "_");

			found_users.put(user, extractNationCall(clean_string));
		});

		long usedtime = System.currentTimeMillis() - starttime;

		return new ExtractionTaskResult(found_users, usedtime);
	}

	/**
	 * @param wiki_user_name
	 * @return
	 */
	public Optional<String> extractNationCall(final String wiki_user_name) {

		final String clean_element = wiki_user_name.replace(" ", "_");

		try {

			Optional<String> result = extractWS(clean_element);
			

			if (result.isPresent()) {
				Logger.debug("found nation for " + wiki_user_name +" " + result.get());
				//System.out.println("found nation for " + wiki_user_name +" " + result.get() );
			
			} else {
				//Logger.debug("no nation found for " + wiki_user_name);
			}

			return result;

		} catch (Exception e) {
			System.out.println(e);
			Optional<String> opt = Optional.empty();
			return opt;
		}

	}
	
	public Optional<String> extractWS(
			String userName) {
		String url = "http://en.wikipedia.org/wiki/User:" + userName;
		return WS
				.url(url)
				.get()
				.map(response -> {
					Document doc = Jsoup.parse(response.getBody());
					Elements contentLinks = doc.getElementById("bodyContent")
							.getElementsByTag("a");
					String s = "";
					List<String> candidates = new ArrayList<>();

					for (Element e : contentLinks) {

						String[] parts = e.attr("href").split("/");
						if (parts[parts.length - 1].contains(":"))
							continue;
						if (parts[parts.length - 1].contains("#"))
							continue;

						s += parts[parts.length - 1];
						// Logger.debug(s);
						candidates.add(parts[parts.length - 1].trim());
					}

					Optional<String> nation = candidates
							.stream()
							.filter(can -> nation_list.stream().anyMatch(
									na -> na.equalsIgnoreCase(can)))
							.findFirst();
					return nation;
			
				}).get(5000);
	}
	
	public static final List<String> nations() {
		try {
			
			InputStream stream = Play.application().classloader().getResourceAsStream("public/data/un_nations.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader( stream));
			List<String> nations = new ArrayList<String>();
			
			String theLine = null;
			while((theLine = br.readLine())!= null){
				nations.add(theLine);
			}
			return nations;
			
		} catch (Exception e){
			Logger.debug("No File Found");
			System.out.println(e);
			return new ArrayList<String>();
		}
	}
	
}
