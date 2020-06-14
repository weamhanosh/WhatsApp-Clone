
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Server {
    public static void main(String[] args) {
        // create the actor system for the server
        ActorSystem serverSystem = ActorSystem.create("WhatsAppServerSystem");
        // create our server (which is ManagingServer)
        ActorRef managingServer = serverSystem.actorOf(Props.create(ManagingServer.class), "WhatsAppManagingServer");
        System.out.println("Server Started");
    }
}