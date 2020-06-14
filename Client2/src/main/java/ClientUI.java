
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;

import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientUI {

    private static String[] input = null;
    // create the actor system for the client
    private static final ActorSystem clientSystem = ActorSystem.create("WhatsAppClientSystem");
    private static ActorRef client = null;
    private static String username = null;
    public static ConcurrentLinkedQueue<Messages.InvitePending> invitesList = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (invitesList.isEmpty()) {
                System.out.println("Enter command");
            } else {
                System.out.println("You have pending invites to groups. Enter command or Accept/Decline invites");
            }
            input = scanner.nextLine().split(" ");
            switch (input[0]) {

                case "/user":
                    userCommand();
                    break;
                case "/group":
                    groupCommand();
                    break;
                case "Yes":
                case "No":
                    if (!invitesList.isEmpty()) {
                        Messages.InvitePending invite = invitesList.remove();
                        invitesList.removeIf(invitePending -> invitePending.groupname.equals(invite.groupname));
                        client.tell(new Messages.InviteAnswer(invite.username, invite.groupname, invite.target, input[0]), ActorRef.noSender());
                    } else {
                        System.out.println("Invalid command");
                    }
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void userCommand() {
        if (input.length < 2) {
            System.out.println("Invalid command");
        } else {
            switch (input[1]) {
                case "connect":
                    userConnect();
                    break;
                case "disconnect":
                    if (!isConnected()){
                        System.out.println("You are not connected!");
                        break;
                    }
                    userDisconnect();
                    break;
                case "text":
                    if (!isConnected()){
                        System.out.println("You are not connected!");
                        break;
                    }
                    userSendText();
                    break;
                case "file":
                    if (!isConnected()){
                        System.out.println("You are not connected!");
                        break;
                    }
                    userSendFile();
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void userConnect() {
        if (input.length != 3) {
            System.out.println("Invalid command");
        } else {
            username = input[2];
            // create the client actor (under the given username)
            client = clientSystem.actorOf(Props.create(Client.class), username);
            client.tell(new Messages.Connect(username, client), ActorRef.noSender());
        }
    }

    private static void userDisconnect() {
        client.tell(new Messages.Disconnect(username), ActorRef.noSender());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.tell(PoisonPill.getInstance(), ActorRef.noSender());
        client = null;
    }

    private static void userSendText() {
        if (input.length < 4) {
            System.out.println("Invalid command");
        } else {
            String target = input[2];

            StringBuilder text = new StringBuilder(input[3]);
            for (int i = 4; i < input.length; i++) {
                text.append(" ").append(input[i]);
            }
            try {
                client.tell(new Messages.SendTextToUser(username, target, text.toString()), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void userSendFile() {
        if (input.length != 4) {
            System.out.println("Invalid command");
        } else {
            String target = input[2];
            String sourcefilePath = input[3];
            try {
                client.tell(new Messages.SendFileToUser(username, target, sourcefilePath), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void groupCommand() {
        if (input.length < 2) {
            System.out.println("Invalid command");
        } else {
            if (!isConnected()){
                System.out.println("You are not connected!");
                return;
            }
            switch (input[1]) {
                case "create":
                    groupCreate();
                    break;
                case "leave":
                    groupLeave();
                    break;
                case "send":
                    groupSend();
                    break;
                case "user":
                    groupUser();
                    break;
                case "coadmin":
                    groupCoAdmin();
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void groupCreate() {
        if (input.length != 3) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[2];
            try {
                client.tell(new Messages.CreateGroup(username, groupname), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupLeave() {
        if (input.length != 3) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[2];
            try {
                client.tell(new Messages.LeaveGroup(username, groupname), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupSend() {
        if (input.length < 3) {
            System.out.println("Invalid command");
        } else {
            switch (input[2]) {
                case "text":
                    groupSendText();
                    break;
                case "file":
                    groupSendFile();
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void groupSendText() {
        if (input.length < 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            StringBuilder text = new StringBuilder(input[4]);
            for (int i = 5; i < input.length; i++) {
                text.append(" ").append(input[i]);
            }
            try {
                client.tell(new Messages.SendTextToGroup(username, groupname, text.toString()), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupSendFile() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String sourcefilePath = input[4];
            try {
                client.tell(new Messages.SendFileToGroup(username, groupname, sourcefilePath), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupUser() {
        if (input.length < 3) {
            System.out.println("Invalid command");
        } else {
            switch (input[2]) {
                case "invite":
                    groupUserInvite();
                    break;
                case "remove":
                    groupUserRemove();
                    break;
                case "mute":
                    groupUserMute();
                    break;
                case "unmute":
                    groupUserUnmute();
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void groupUserInvite() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            try {
                client.tell(new Messages.InviteUserToGroup(username, groupname, target), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupUserRemove() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            try {
                client.tell(new Messages.RemoveUserFromGroup(username, groupname, target), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupUserMute() {
        if (input.length != 6) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            long timeInSeconds = Long.parseLong(input[5]);
            try {
                client.tell(new Messages.MuteUserInGroup(username, groupname, target, timeInSeconds), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupUserUnmute() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            try {
                client.tell(new Messages.UnmuteUserInGroup(username, groupname, target), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupCoAdmin() {
        if (input.length < 3) {
            System.out.println("Invalid command");
        } else {
            switch (input[2]) {
                case "add":
                    groupCoAdminAdd();
                    break;
                case "remove":
                    groupCoAdminRemove();
                    break;
                default:
                    System.out.println("Invalid command");
                    break;
            }
        }
    }

    private static void groupCoAdminAdd() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            try {
                client.tell(new Messages.PromoteCoAdminInGroup(username, groupname, target), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static void groupCoAdminRemove() {
        if (input.length != 5) {
            System.out.println("Invalid command");
        } else {
            String groupname = input[3];
            String target = input[4];
            try {
                client.tell(new Messages.DemoteCoAdminInGroup(username, groupname, target), ActorRef.noSender());
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isConnected(){
        return (client != null);
    }

}
