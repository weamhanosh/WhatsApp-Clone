
import akka.actor.ActorRef;

import java.io.Serializable;

public class Messages {

    public static final class Connect implements Serializable {

        public String username;
        public ActorRef actorRef;

        public Connect(String username, ActorRef actorRef) {
            this.username = username;
            this.actorRef = actorRef;
        }

    }

    public static final class Disconnect implements Serializable {

        public String username;

        public Disconnect(String username) {
            this.username = username;

        }

    }

    public static final class GetActorRef implements Serializable {

        public String target;

        public GetActorRef(String target) {
            this.target = target;
        }

    }

    public static final class SendTextToUser implements Serializable {

        public String username;
        public String target;
        public String text;

        public SendTextToUser(String username, String target, String text) {
            this.username = username;
            this.target = target;
            this.text = text;
        }

    }

    public static final class SendFileToUser implements Serializable {

        public String username;
        public String target;
        public String sourceFilePath;

        public SendFileToUser(String username, String target, String sourceFilePath) {
            this.username = username;
            this.target = target;
            this.sourceFilePath = sourceFilePath;
        }

    }

    public static final class CreateGroup implements Serializable {

        public String username;
        public String groupname;

        public CreateGroup(String username, String groupname) {
            this.username = username;
            this.groupname = groupname;
        }

    }

    public static final class LeaveGroup implements Serializable {

        public String username;
        public String groupname;

        public LeaveGroup(String username, String groupname) {
            this.username = username;
            this.groupname = groupname;
        }

    }

    public static final class SendTextToGroup implements Serializable {

        public String username;
        public String groupname;
        public String text;

        public SendTextToGroup(String username, String groupname, String text) {
            this.username = username;
            this.groupname = groupname;
            this.text = text;
        }

    }

    public static final class SendFileToGroup implements Serializable {

        public String username;
        public String groupname;
        public String sourceFilePath;

        public SendFileToGroup(String username, String groupname, String sourceFilePath) {
            this.username = username;
            this.groupname = groupname;
            this.sourceFilePath = sourceFilePath;
        }

    }

    public static final class SendFileDataToGroup implements Serializable {

        public String username;
        public String groupname;
        public String source;
        public byte[] data;

        public SendFileDataToGroup(String username, String groupname, String source, byte[] data) {
            this.username = username;
            this.groupname = groupname;
            this.source = source;
            this.data = data;
        }

    }

    public static final class InviteUserToGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public InviteUserToGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class InvitePending implements Serializable {

        public String username;
        public String groupname;
        public String target;
        public String message;

        public InvitePending(String username, String groupname, String target, String message) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
            this.message = message;
        }

    }

    public static final class AddUserToGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public AddUserToGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class InviteAnswer implements Serializable {

        public String username;
        public String groupname;
        public String target;
        public String answer;

        public InviteAnswer(String username, String groupname, String target, String answer) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
            this.answer = answer;
        }

    }

    public static final class RemoveUserFromGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public RemoveUserFromGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class MuteUserInGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;
        public long timeout;

        public MuteUserInGroup(String username, String groupname, String target, long timeout) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
            this.timeout = timeout;
        }

    }

    public static final class UnmuteUserInGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public UnmuteUserInGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class AutomaticUnmuteUserInGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public AutomaticUnmuteUserInGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class PromoteCoAdminInGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public PromoteCoAdminInGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class DemoteCoAdminInGroup implements Serializable {

        public String username;
        public String groupname;
        public String target;

        public DemoteCoAdminInGroup(String username, String groupname, String target) {
            this.username = username;
            this.groupname = groupname;
            this.target = target;
        }

    }

    public static final class ReceiveTextFromUser implements Serializable {

        public String sender;
        public String text;

        public ReceiveTextFromUser(String sender, String text) {
            this.sender = sender;
            this.text = text;
        }

    }

    public static final class ReceiveFileFromUser implements Serializable {

        public String sender;
        public byte[] data;
        public String sourceFilePath;


        public ReceiveFileFromUser(String sender, byte[] data, String sourceFilePath) {
            this.sender = sender;
            this.data = data;
            this.sourceFilePath = sourceFilePath;
        }

    }

    public static final class ReceiveTextFromGroup implements Serializable {

        public String sender;
        public String groupname;
        public String text;

        public ReceiveTextFromGroup(String sender, String groupname, String text) {
            this.sender = sender;
            this.groupname = groupname;
            this.text = text;
        }

    }

    public static final class ReceiveFileFromGroup implements Serializable {

        public String sender;
        public String groupname;
        public String source;
        public byte[] data;

        public ReceiveFileFromGroup(String sender, String groupname, String source, byte[] data) {
            this.sender = sender;
            this.groupname = groupname;
            this.source = source;
            this.data = data;
        }

    }

    public static final class EmptyMessage implements Serializable {

        public EmptyMessage() {
        }

    }


}
