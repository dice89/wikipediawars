package de.w4.analyzer.wikiuser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import scala.Option;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import de.w4.analyzer.wikiuser.messages.ExtractionTask;
import de.w4.analyzer.wikiuser.messages.ExtractionTaskResult;

public class NationExtractionWorker extends UntypedActor {
	
	private Map<String,String> nation_map = nations();

	
	@Override
	public void onReceive(Object message) throws Exception {
		
		if (message instanceof ExtractionTask) {

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

					//String s = "";
					List<String> candidates = new ArrayList<>();

					for (Element e : contentLinks) {

						String[] parts = e.attr("href").split("/");
						if (parts[parts.length - 1].contains(":"))
							continue;
						if (parts[parts.length - 1].contains("#"))
							continue;

						//s += parts[parts.length - 1];
						// Logger.debug(s);
						candidates.add(parts[parts.length - 1].trim());
					}

					Optional<String> nation = candidates
							.stream()
							.filter(can -> nation_map.keySet().stream().anyMatch(
									na -> na.equalsIgnoreCase(can)))
							.findFirst();
					
					if (nation.isPresent()){
						nation = Optional.of(nation_map.get(nation.get()));
					}
					
					return nation;
			
				}).get(5000);
	}
	
	public static final Map<String,String> nations() {
		try {
			
			InputStream stream = Play.application().classloader().getResourceAsStream("public/data/combined_iso.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader( stream));
			Map<String,String> nations = new HashMap<String,String>();
			
			String theLine = null;
			while((theLine = br.readLine())!= null){
				String[] elements = theLine.split(";");
				if (elements.length > 0 ){
					nations.put(elements[0], elements[1]);
				}

			}
			return nations;
			
		} catch (Exception e){
			Logger.debug("No File Found");
			System.out.println(e);
			return new HashMap<String,String>();
		}
	}
	
}
