import actors.ExtractChangesActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;

public class Global extends GlobalSettings {

    public void onStart(Application app) {

        startPeriodicExtractTask();

    }

    public void onStop(Application app) {
        Logger.info("Global.onStop() callback");
    }


//    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
//        return F.Promise.<Result> pure(internalServerError(
//                Messages.get("error.global", t.getMessage())
//        ));
//    }
//
//
//    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
//        return F.Promise.<Result>pure(notFound(
//                Messages.get("error.routeNotFound")
//        ));
//    }


    private void startPeriodicExtractTask() {
        Logger.info("Starting periodic extract task");

        ActorRef timeoutsActor = Akka.system().actorOf(Props.create(ExtractChangesActor.class), "ExtractChangesActor");

        Akka.system().scheduler().schedule(
                Duration.create(30, TimeUnit.SECONDS), //Initial delay
                Duration.create(24, TimeUnit.HOURS),     //Frequency
                timeoutsActor,
                "startJobToGetMostEditedArticles",
                Akka.system().dispatcher(),
                null
        );

    }

}