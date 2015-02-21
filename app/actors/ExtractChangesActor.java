package actors;


import akka.actor.UntypedActor;
import controllers.WikiController;
import de.w4.analyzer.WikiAnalyzer;
import play.Logger;

public class ExtractChangesActor extends UntypedActor {

    public void onReceive(Object message) {
        if (message.equals("startJobToGetMostEditedArticles")) {
            Logger.info("About to run scheduled: startJobToGetMostEditedArticles");
            try {
                WikiController.startJobToGetMostEditedArticles();
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }
        } else {
            unhandled(message);
        }
    }
}