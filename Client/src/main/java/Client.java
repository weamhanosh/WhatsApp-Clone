import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.ByteString;
import akka.util.Timeout;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Client extends AbstractActor {

    public ActorSelection whatsAppManagingServer = getContext().actorSelection("akka.tcp://WhatsAppServerSystem@127.0.0.1:3553/user/WhatsAppManagingServer");
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::onServerReply)
                .match(Messages.Connect.class, this::onConnect)
                .match(Messages.Disconnect.class, this::onDisconnect)
                .match(Messages.CreateGroup.class, this::onCreateGroup)
                .match(Messages.LeaveGroup.class, this::onLeaveGroup)
                .match(Messages.SendTextToUser.class, this::onSendTextToUser)
                .match(Messages.ReceiveTextFromUser.class, this::onReceiveTextFromUser)
                .match(Messages.SendFileToUser.class, this::onSendFileToUser)
                .match(Messages.ReceiveFileFromUser.class, this::onReceiveFileFromUser)
                .match(Messages.SendTextToGroup.class, this::onSendTextToGroup)
                .match(Messages.ReceiveTextFromGroup.class, this::onReceiveTextFromGroup)
                .match(Messages.ReceiveFileFromGroup.class, this::onReceiveFileFromGroup)
                .match(Messages.SendFileToGroup.class, this::onSendFileToGroup)
                .match(Messages.InviteUserToGroup.class, this::onInviteUserToGroup)
                .match(Messages.InvitePending.class, this::onInvitePending)
                .match(Messages.InviteAnswer.class, this::onInviteAnswer)
                .match(Messages.AddUserToGroup.class, this::onAddUserToGroup)
                .match(Messages.RemoveUserFromGroup.class, this::onRemoveUserFromGroup)
                .match(Messages.MuteUserInGroup.class, this::onMuteUserInGroup)
                .match(Messages.UnmuteUserInGroup.class, this::onUnmuteUserInGroup)
                .match(Messages.PromoteCoAdminInGroup.class, this::onPromoteCoAdminInGroup)
                .match(Messages.DemoteCoAdminInGroup.class, this::onDemoteCoAdminInGroup)
                .build();
    }

    private void onServerReply(String message) {
        System.out.println(message);
    }

    private void onConnect(Messages.Connect message) {
        String result = askServer(whatsAppManagingServer, message);
        if (result != null) {
            String[] resultArr = result.split(" ");
            if (resultArr.length == 4 && resultArr[1].equals("is") && resultArr[2].equals("in") && resultArr[3].equals("use!")) {
                getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
            System.out.println(result);
        } else {
            System.out.println("server is offline! try again later!");
            getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    private void onDisconnect(Messages.Disconnect message) {
        String result = askServer(whatsAppManagingServer, message);
        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onCreateGroup(Messages.CreateGroup message) {
        String result = askServer(whatsAppManagingServer, message);
        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onLeaveGroup(Messages.LeaveGroup message) {
        whatsAppManagingServer.tell(message, getSelf());
    }

    private void onSendTextToUser(Messages.SendTextToUser message) {
        Object result = getActorRef(whatsAppManagingServer, new Messages.GetActorRef(message.target));
        if (result != null) {
            if (result instanceof ActorRef) {
                ActorRef actor = (ActorRef) result;
                // send message directly to the other actor's mailbox
                actor.tell(new Messages.ReceiveTextFromUser(message.username, message.text), getSelf());
            } else if (result instanceof Messages.EmptyMessage) {
                System.out.println(message.target + " does not exist!");
            }
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onReceiveTextFromUser(Messages.ReceiveTextFromUser message) {
        System.out.println("[" + timeFormat.format(LocalDateTime.now()) + "][user][" + message.sender + "] " + message.text);
    }

    private void onSendFileToUser(Messages.SendFileToUser message) {
        Object result = getActorRef(whatsAppManagingServer, new Messages.GetActorRef(message.target));
        if (result != null) {
            if (result instanceof ActorRef) {
                ActorRef actor = (ActorRef) result;
                byte[] data = new byte[0];
                try {
                    data = loadBinaryFile(message.sourceFilePath);
                } catch (FileNotFoundException e) {
                    System.out.println(message.sourceFilePath + " does not exist!");
                } catch (IOException ignored) {
                }
                if (data != null) {
                    actor.tell(new Messages.ReceiveFileFromUser(message.username, data, message.sourceFilePath), getSelf());
                } else {
                    System.out.println(message.sourceFilePath + " does not exist!");
                }
            } else if (result instanceof Messages.EmptyMessage) {
                System.out.println(message.target + " is not connected!");
            }
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onReceiveFileFromUser(Messages.ReceiveFileFromUser message) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(System.getProperty("user.dir") + "/" + "copy" + getFileName(message.sourceFilePath));
        } catch (FileNotFoundException e) {
            System.out.println(message.sourceFilePath + " does not exist!");
        }
        try {
            if (fos != null) {
                fos.write(message.data);
                fos.close();
            }
        } catch (IOException ignored) {
        }
        System.out.println("[" + timeFormat.format(LocalDateTime.now()) + "]" + "[user][" + message.sender + "] " + " File received: " + System.getProperty("user.dir") + "/" + "copy" + getFileName(message.sourceFilePath));
    }

    private void onSendTextToGroup(Messages.SendTextToGroup message) {
        whatsAppManagingServer.tell(message, getSelf());
    }

    private void onReceiveTextFromGroup(Messages.ReceiveTextFromGroup message) {
        System.out.println("[" + timeFormat.format(LocalDateTime.now()) + "][" + message.groupname + "][" + message.sender + "] " + message.text);
    }

    private void onSendFileToGroup(Messages.SendFileToGroup message) throws IOException {
        byte[] data = loadBinaryFile(message.sourceFilePath);
        if (data != null) {
            whatsAppManagingServer.tell(new Messages.SendFileDataToGroup(message.username, message.groupname, message.sourceFilePath, data), getSelf());
        } else {
            System.out.println(message.sourceFilePath + " does not exist!");
        }
    }

    private void onReceiveFileFromGroup(Messages.ReceiveFileFromGroup message) throws IOException {
        FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/" + "copy" + getFileName(message.source));
        fos.write(message.data);
        fos.close();
        System.out.println("File received: " + System.getProperty("user.dir") + "/" + "copy" + getFileName(message.source));
    }

    private void onInviteUserToGroup(Messages.InviteUserToGroup message) {
        whatsAppManagingServer.tell(message, getSelf());
    }

    private void onInvitePending(Messages.InvitePending message) {
        System.out.println(message.message);
        ClientUI.invitesList.add(message);
    }

    private void onInviteAnswer(Messages.InviteAnswer message) {
        Object result = getActorRef(whatsAppManagingServer, new Messages.GetActorRef(message.username));
        if (result != null) {
            if (result instanceof ActorRef) {
                ActorRef actor = (ActorRef) result;
                // send message invite answer directly to the inviting actor's mailbox
                if (message.answer.equals("Yes")) {
                    actor.tell(new Messages.AddUserToGroup(message.username, message.groupname, message.target), getSelf());
                }
            } else if (result instanceof Messages.EmptyMessage) {
                System.out.println(message.username + " is not connected!");
            }
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onAddUserToGroup(Messages.AddUserToGroup message) {
        Object result = getActorRef(whatsAppManagingServer, new Messages.GetActorRef(message.target));
        if (result != null) {
            if (result instanceof ActorRef) {
                whatsAppManagingServer.tell(new Messages.AddUserToGroup(message.username, message.groupname, message.target), getSelf());
                ((ActorRef) result).tell("Welcome to " + message.groupname + "!", getSelf());
            } else {
                System.out.println(result);
            }
        } else {
            System.out.println("server is offline! try again later!");
        }
    }

    private void onRemoveUserFromGroup(Messages.RemoveUserFromGroup message) {
        whatsAppManagingServer.tell(new Messages.RemoveUserFromGroup(message.username, message.groupname, message.target), getSelf());
    }

    private void onMuteUserInGroup(Messages.MuteUserInGroup message) {
        whatsAppManagingServer.tell(new Messages.MuteUserInGroup(message.username, message.groupname, message.target, message.timeout), getSelf());
    }

    private void onUnmuteUserInGroup(Messages.UnmuteUserInGroup message) {
        whatsAppManagingServer.tell(new Messages.UnmuteUserInGroup(message.username, message.groupname, message.target), getSelf());
    }

    private void onPromoteCoAdminInGroup(Messages.PromoteCoAdminInGroup message) {
        whatsAppManagingServer.tell(new Messages.PromoteCoAdminInGroup(message.username, message.groupname, message.target), getSelf());
    }

    private void onDemoteCoAdminInGroup(Messages.DemoteCoAdminInGroup message) {
        whatsAppManagingServer.tell(new Messages.DemoteCoAdminInGroup(message.username, message.groupname, message.target), getSelf());
    }

    public Object getActorRef(ActorSelection actorRef, Messages.GetActorRef Message) {
        final Timeout timeout = new Timeout(Duration.create(500, TimeUnit.MILLISECONDS));
        Future<Object> answer = Patterns.ask(actorRef, Message, timeout);
        try {
            return Await.result(answer, timeout.duration());
        } catch (Exception ignored) {
            return null;
        }
    }

    public String askServer(ActorSelection actorRef, Serializable Message) {
        final Timeout timeout = new Timeout(Duration.create(500, TimeUnit.MILLISECONDS));
        Future<Object> answer = Patterns.ask(actorRef, Message, timeout);
        try {
            return (String) Await.result(answer, timeout.duration());
        } catch (Exception ignored) {
            return null;
        }
    }

    public byte[] loadBinaryFile(String filename) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(filename));
        byte[] theBytes = new byte[dis.available()];
        dis.read(theBytes, 0, dis.available());
        dis.close();
        return theBytes;
    }

    public static String getFileName(String fileName) {
        for (int i = fileName.length() - 1; i >= 0; i--)
            if (fileName.charAt(i) == '\\')
                return fileName.substring(i + 1);
        return fileName;
    }

}