
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ManagingServer extends AbstractActor {

    // key: userA
    // value: userA's actorRef
    private ConcurrentHashMap<String, ActorRef> usersList = new ConcurrentHashMap<>();

    // concurrent list of group names
    private ConcurrentLinkedQueue<String> groupsList = new ConcurrentLinkedQueue<>();

    // key: groupA's name
    // value: groupA's Users (admin is part of groupA's users)
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> groupAllUsers = new ConcurrentHashMap<>();

    // key: groupA's name
    // value: groupA's admin
    private ConcurrentHashMap<String, String> groupAdmins = new ConcurrentHashMap<>();

    // key: groupA's name
    // value: groupA's list of coAdmins (not including the admin himself)
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> groupCoAdmins = new ConcurrentHashMap<>();

    // key: groupA's name
    // value: groupA's list of mutedUsers
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> groupMutedUsers = new ConcurrentHashMap<>();

    // key: groupA's name
    // value: groupA's list of regularUsers
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> groupRegularUsers = new ConcurrentHashMap<>();

    // key: groupA's name
    // value: groupA's Router
    private ConcurrentHashMap<String, Router> groupRouters = new ConcurrentHashMap<>();


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.Connect.class, this::onConnect)
                .match(Messages.Disconnect.class, this::onDisconnect)
                .match(Messages.GetActorRef.class, this::onGetActorRef)
                .match(Messages.CreateGroup.class, this::onCreateGroup)
                .match(Messages.LeaveGroup.class, this::onLeaveGroup)
                .match(Messages.SendTextToGroup.class, this::onSendTextToGroup)
                .match(Messages.SendFileDataToGroup.class, this::onSendFileDataToGroup)
                .match(Messages.InviteUserToGroup.class, this::onInviteUserToGroup)
                .match(Messages.AddUserToGroup.class, this::onAddUserToGroup)
                .match(Messages.RemoveUserFromGroup.class, this::onRemoveUserFromGroup)
                .match(Messages.MuteUserInGroup.class, this::onMuteUserInGroup)
                .match(Messages.AutomaticUnmuteUserInGroup.class, this::onAutomaticUnmuteUserInGroup)
                .match(Messages.UnmuteUserInGroup.class, this::onUnmuteUserInGroup)
                .match(Messages.PromoteCoAdminInGroup.class, this::onPromoteCoAdminInGroup)
                .match(Messages.DemoteCoAdminInGroup.class, this::onDemoteCoAdminInGroup)
                .build();
    }


    private void onConnect(Messages.Connect message) {
        if (isUserInSystem(message.username)) {
            getSender().tell(message.username + " is in use!", ActorRef.noSender());
        } else {
            addUserToSystem(message.username, message.actorRef);
            getSender().tell(message.username + " has connected successfully!", ActorRef.noSender());
        }
    }

    private void onDisconnect(Messages.Disconnect message) {
        if (isUserInSystem(message.username)) {
            leaveAllGroups(message.username);
            removeUserFromSystem(message.username);
            getSender().tell(message.username + " has been disconnected successfully!", ActorRef.noSender());
        } else {
            getSender().tell(message.username + " is not connected!", ActorRef.noSender());
        }
    }

    private void onGetActorRef(Messages.GetActorRef message) {
        if (isUserInSystem(message.target)) {
            getSender().tell(getUserRef(message.target), ActorRef.noSender());
        } else {
            getSender().tell(new Messages.EmptyMessage(), ActorRef.noSender());
        }
    }

    private void onCreateGroup(Messages.CreateGroup message) {
        if (isGroupInSystem(message.groupname)) {
            getSender().tell(message.groupname + " already exists!", ActorRef.noSender());
        } else {
            // add groupname to the list of groups
            addGroupToSystem(message.groupname);
            // add groupname to the groupRouters
            ArrayList<Routee> routees = new ArrayList<>();
            Router router = new Router(new BroadcastRoutingLogic(), routees);
            groupRouters.put(message.groupname, router.addRoutee(getUserRef(message.username)));
            // set username as the group's admin
            groupAdmins.put(message.groupname, message.username);
            // initialize the list of users
            groupAllUsers.put(message.groupname, new ConcurrentLinkedQueue<>());
            // add username to the list of users in groupname
            groupAllUsers.get(message.groupname).add(message.username);

            // initialize the list of mutedUsers
            groupMutedUsers.put(message.groupname, new ConcurrentHashMap<>());
            // initialize the list of regularUsers
            groupRegularUsers.put(message.groupname, new ConcurrentLinkedQueue<>());
            // initialize the list of coAdmins
            groupCoAdmins.put(message.groupname, new ConcurrentLinkedQueue<>());
            getSender().tell(message.groupname + " created successfully!", ActorRef.noSender());
        }
    }

    private void onLeaveGroup(Messages.LeaveGroup message) {
        if (!isUserInGroup(message.groupname, message.username)) {
            getSender().tell(message.username + " is not in " + message.groupname + "!", ActorRef.noSender());
        } else {

            // BROADCAST TO GROUP
            broadcastFromServer(message.groupname, message.username + " has left " + message.groupname + "!");

            // if the user is the admin of the group then we delete the group
            if (isAdminInGroup(message.groupname, message.username)) {
                broadcastFromServer(message.groupname, message.groupname + " admin has closed " + message.groupname + "!");
                // remove groupname from the groupRouters
                groupRouters.remove(message.groupname);
                deleteGroupFromSystem(message.groupname);
            }

            // remove user from the group
            if (isUserInGroup(message.groupname, message.username)) {
                Router prevRouter = groupRouters.get(message.groupname);
                Router newRouter = prevRouter.removeRoutee(getUserRef(message.username));
                groupRouters.replace(message.groupname, newRouter);
                groupAllUsers.get(message.groupname).remove(message.username);
            }
            removeMutedUserFromGroup(message.groupname, message.username);
            removeRegularUserFromGroup(message.groupname, message.username);
            removeCoAdminFromGroup(message.groupname, message.username);
        }
    }

    private void onSendTextToGroup(Messages.SendTextToGroup message) {
        if (validMessageToGroup(message.groupname, message.username)) {
            // successful text send
            broadcastFromUser(message.groupname, message.username, message.text);
        }
    }

    private void onSendFileDataToGroup(Messages.SendFileDataToGroup message) {
        if (validMessageToGroup(message.groupname, message.username)) {
            broadcastFileFromServer(message);
        }
    }

    private void onInviteUserToGroup(Messages.InviteUserToGroup message) {
        if (validInvite(message.groupname, message.username)) {
            // target is not connected
            if (!isUserInSystem(message.target)) {
                getSender().tell(message.target + " is not connected!", ActorRef.noSender());
            }
            // target is already in group
            if (isUserInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is already in " + message.groupname + "!", ActorRef.noSender());
            } else {
                if (isUserInSystem(message.username) && isUserInSystem(message.target)) {
                    getUserRef(message.target).tell(new Messages.InvitePending(message.username, message.groupname, message.target, "You have been invited to " + message.groupname + ", Accept?"), getUserRef(message.username));
                }
            }
        }
    }

    private void onAddUserToGroup(Messages.AddUserToGroup message) {
        if (isUserInSystem(message.target)) {
            broadcastFromServer(message.groupname, message.target + " joined group " + message.groupname + "!");
            Router prevRouter = groupRouters.get(message.groupname);
            Router newRouter = prevRouter.addRoutee(getUserRef(message.target));
            groupRouters.replace(message.groupname, newRouter);
            groupAllUsers.get(message.groupname).add(message.target);
            groupRegularUsers.get(message.groupname).add(message.target);
        } else {
            getSender().tell(message.target + " is not connected!", ActorRef.noSender());
        }
    }

    private void onRemoveUserFromGroup(Messages.RemoveUserFromGroup message) {
        if (validRemovalMuteUnmuteInGroup(message.groupname, message.username, message.target)) {
            // target is not in group
            if (!isUserInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is not in " + message.groupname + "!", ActorRef.noSender());
            }
            // target is the group's admin
            else if (isAdminInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is an admin thus can't be removed!", ActorRef.noSender());
            }
            // target removed
            else {
                // remove user from the group
                if (isUserInGroup(message.groupname, message.target)) {
                    groupAllUsers.get(message.groupname).remove(message.target);
                    Router prevRouter = groupRouters.get(message.groupname);
                    Router newRouter = prevRouter.removeRoutee(getUserRef(message.target));
                    groupRouters.replace(message.groupname, newRouter);
                }
                removeRegularUserFromGroup(message.groupname, message.target);
                removeMutedUserFromGroup(message.groupname, message.target);
                // if the user is a coAdmin then remove this privilege
                removeCoAdminFromGroup(message.groupname, message.target);

                if (isUserInSystem(message.target)) {
                    getUserRef(message.target).tell("You have been removed from " + message.groupname + " by " + message.username + "!", ActorRef.noSender());
                }
            }
        }
    }

    private void onMuteUserInGroup(Messages.MuteUserInGroup message) {
        if (validRemovalMuteUnmuteInGroup(message.groupname, message.username, message.target)) {
            // target is a co-admin and username is a co-admin
            if (isCoAdminInGroup(message.groupname, message.target) && isCoAdminInGroup(message.groupname, message.username)) {
                getSender().tell(message.target + " is a co-admin thus can't mute the target co-admin" + message.target + "!", ActorRef.noSender());
            }
            // already muted user
            else if (isMutedUserInGroup(message.groupname, message.username)) {
                groupMutedUsers.get(message.groupname).replace(message.username, message.timeout);
                if (isUserInSystem(message.target)) {
                    getUserRef(message.target).tell("You have been muted for " + message.timeout + " seconds in " + message.groupname + " by " + message.username + "!", ActorRef.noSender());
                }
            }
            // target is the group's admin
            else if (isAdminInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is the admin thus can't be muted!", ActorRef.noSender());
            }
            // target muted
            else {
                removeCoAdminFromGroup(message.groupname, message.target);
                removeRegularUserFromGroup(message.groupname, message.target);
                addMutedUserToGroup(message.groupname, message.target, message.timeout);
                if (isUserInSystem(message.target)) {
                    getUserRef(message.target).tell("You have been muted for " + message.timeout + " seconds in " + message.groupname + " by " + message.username + "!", ActorRef.noSender());
                }

                // support automatic unmute
                getContext().getSystem().scheduler().scheduleOnce(new FiniteDuration(message.timeout, TimeUnit.SECONDS), getSelf(), new Messages.AutomaticUnmuteUserInGroup(message.username, message.groupname, message.target), getContext().getSystem().dispatcher(), ActorRef.noSender());
            }

        }
    }

    private void onAutomaticUnmuteUserInGroup(Messages.AutomaticUnmuteUserInGroup message) {
        if (isMutedUserInGroup(message.groupname, message.target)) {
            // target unmuted automatically (new privileges are of a regular user)
            removeMutedUserFromGroup(message.groupname, message.target);
            groupRegularUsers.put(message.groupname, new ConcurrentLinkedQueue<>());
            addRegularUserToGroup(message.groupname, message.target);
            if (isUserInSystem(message.target))
                getUserRef(message.target).tell("You have been unmuted in " + message.groupname + "! Muting time is up!", ActorRef.noSender());
        }
    }

    private void onUnmuteUserInGroup(Messages.UnmuteUserInGroup message) {
        if (validRemovalMuteUnmuteInGroup(message.groupname, message.username, message.target)) {
            if (isMutedUserInGroup(message.groupname, message.target)) {
                // target unmuted
                removeMutedUserFromGroup(message.groupname, message.target);
                groupRegularUsers.put(message.groupname, new ConcurrentLinkedQueue<>());
                addRegularUserToGroup(message.groupname, message.target);
                if (isUserInSystem(message.target))
                    getUserRef(message.target).tell("You have been unmuted in " + message.groupname + " by " + message.username + "!", ActorRef.noSender());
            } else {
                getSender().tell(message.target + " is not muted !", ActorRef.noSender());
            }
        }
    }

    private void onPromoteCoAdminInGroup(Messages.PromoteCoAdminInGroup message) {
        // username is the admin
        if (validPromoteDemoteInGroup(message.groupname, message.username, message.target)) {
            // target is the admin
            if (isAdminInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is an admin thus can't be promoted!", ActorRef.noSender());
            }
            // target is a co-admin
            else if (isCoAdminInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is a co-admin thus can't be promoted!", ActorRef.noSender());
            } else {
                // target is a regular user
                if (isRegularUserInGroup(message.groupname, message.target)) {
                    removeRegularUserFromGroup(message.groupname, message.target);
                    addCoAdminToGroup(message.groupname, message.target);
                }
                // target is a muted user
                else if (isMutedUserInGroup(message.groupname, message.target)) {
                    removeMutedUserFromGroup(message.groupname, message.target);
                    addCoAdminToGroup(message.groupname, message.target);
                }
                if (isUserInSystem(message.target)) {
                    getUserRef(message.target).tell("You have been promoted to co-admin in " + message.groupname + "!", ActorRef.noSender());
                }
            }
        }
    }

    private void onDemoteCoAdminInGroup(Messages.DemoteCoAdminInGroup message) {
        // username is the admin
        if (validPromoteDemoteInGroup(message.groupname, message.username, message.target)) {
            // target is the admin
            if (isAdminInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is an admin thus can't be demoted!", ActorRef.noSender());
            }
            // target is a regular user
            else if (isRegularUserInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is a user thus can't be demoted!", ActorRef.noSender());
            }
            // target is a muted user
            else if (isMutedUserInGroup(message.groupname, message.target)) {
                getSender().tell(message.target + " is a muted user thus can't be demoted!", ActorRef.noSender());
            }
            // target is a co-admin
            else if (isCoAdminInGroup(message.groupname, message.target)) {
                removeCoAdminFromGroup(message.groupname, message.target);
                addRegularUserToGroup(message.groupname, message.target);
                // target demoted
                if (isUserInSystem(message.target)) {
                    getUserRef(message.target).tell("You have been demoted to user in " + message.groupname + "!", ActorRef.noSender());
                }
            }
        }
    }

    private void leaveAllGroups(String username) {
        for (String groupname : groupsList) {
            if (isUserInGroup(groupname, username)) {
                onLeaveGroup(new Messages.LeaveGroup(username, groupname));
            }
        }
    }

    private void broadcastFromServer(String groupname, String message) {
        if (isGroupInSystem(groupname)) {
            groupRouters.get(groupname).route(message, ActorRef.noSender());
        }
    }

    private void broadcastFileFromServer(Messages.SendFileDataToGroup message) {
        if (isGroupInSystem(message.groupname)) {
            groupRouters.get(message.groupname).route(new Messages.ReceiveFileFromGroup(message.username, message.groupname, message.source, message.data), ActorRef.noSender());
        }
    }

    private void broadcastFromUser(String groupname, String sender, String message) {
        if (isUserInSystem(sender) && isGroupInSystem(groupname)) {
            groupRouters.get(groupname).route(new Messages.ReceiveTextFromGroup(sender, groupname, message), ActorRef.noSender());
        }
    }

    private boolean validMessageToGroup(String groupname, String username) {
        // group does not exist
        if (!isGroupInSystem(groupname)) {
            getSender().tell(groupname + " does not exists!", ActorRef.noSender());
            return false;
        }
        // user is not in group
        else if (!isUserInGroup(groupname, username)) {
            getSender().tell("You are not part of " + groupname + "!", ActorRef.noSender());
            return false;
        }
        // user is muted in group
        else if (isMutedUserInGroup(groupname, username)) {
            getSender().tell("You are muted for " + groupMutedUsers.get(groupname).get(username) + " seconds in " + groupname + "!", ActorRef.noSender());
            return false;
        } else {
            return true;
        }
    }

    private boolean validInvite(String groupname, String username) {
        // group does not exist
        if (!isGroupInSystem(groupname)) {
            getSender().tell(groupname + " does not exists!", ActorRef.noSender());
            return false;
        }
        // group does not exist
        if (!isUserInSystem(username)) {
            getSender().tell(username + " does not exists!", ActorRef.noSender());
            return false;
        }
        // user is not admin/co-admin in group
        else if (!isCoAdminInGroup(groupname, username) && !isAdminInGroup(groupname, username)) {
            getSender().tell("You are neither an admin nor a co-admin of " + groupname + "!", ActorRef.noSender());
            return false;
        } else
            return true;
    }

    private boolean validRemovalMuteUnmuteInGroup(String groupname, String username, String target) {
        // group does not exist
        if (!isGroupInSystem(groupname)) {
            getSender().tell(groupname + " does not exists!", ActorRef.noSender());
            return false;
        }
        // user is not admin/co-admin in group
        else if (!isCoAdminInGroup(groupname, username) && !isAdminInGroup(groupname, username)) {
            getSender().tell("You are neither an admin nor a co-admin of " + groupname + "!", ActorRef.noSender());
            return false;
        }
        // target doesn't exist
        else if (!isUserInGroup(groupname, target)) {
            getSender().tell(target + " does not exists!", ActorRef.noSender());
            return false;
        } else {
            return true;
        }
    }

    private boolean validPromoteDemoteInGroup(String groupname, String username, String target) {
        // group does not exist
        if (!isGroupInSystem(groupname)) {
            getSender().tell(groupname + " does not exists!", ActorRef.noSender());
            return false;
        }
        // user is not admin/co-admin in group
        else if (!isAdminInGroup(groupname, username)) {
            getSender().tell("You are not an admin of " + groupname + "!", ActorRef.noSender());
            return false;
        }
        // target doesn't exist
        else if (!isUserInGroup(groupname, target)) {
            getSender().tell(target + " does not exists!", ActorRef.noSender());
            return false;
        } else {
            return true;
        }
    }

    private boolean isUserInSystem(String username) {
        return usersList.containsKey(username);
    }

    private ActorRef getUserRef(String username) {
        return usersList.get(username);
    }

    private void addUserToSystem(String username, ActorRef actorRef) {
        usersList.put(username, actorRef);
    }

    private void removeUserFromSystem(String username) {
        usersList.remove(username);
    }

    private boolean isGroupInSystem(String groupname) {
        return groupsList.contains(groupname);
    }

    private void addGroupToSystem(String groupname) {
        groupsList.add(groupname);
    }

    private void deleteGroupFromSystem(String groupname) {
        groupsList.remove(groupname);
        groupAllUsers.remove(groupname);
        groupMutedUsers.remove(groupname);
        groupRegularUsers.remove(groupname);
        groupCoAdmins.remove(groupname);
        groupAdmins.remove(groupname);
    }

    private boolean isUserInGroup(String groupname, String username) {
        if (groupAllUsers.containsKey(groupname)) {
            return groupAllUsers.get(groupname).contains(username);
        } else {
            return false;
        }
    }

    private boolean isMutedUserInGroup(String groupname, String username) {
        if (groupMutedUsers.containsKey(groupname)) {
            return groupMutedUsers.get(groupname).containsKey(username);
        } else {
            return false;
        }
    }

    private void addMutedUserToGroup(String groupname, String username, long timeout) {
        if (groupMutedUsers.containsKey(groupname)) {
            groupMutedUsers.get(groupname).put(username, timeout);
        }
    }

    private void removeMutedUserFromGroup(String groupname, String username) {
        if (groupMutedUsers.containsKey(groupname)) {
            groupMutedUsers.get(groupname).remove(username);
        }
    }

    private boolean isRegularUserInGroup(String groupname, String username) {
        if (groupRegularUsers.containsKey(groupname)) {
            return groupRegularUsers.get(groupname).contains(username);
        } else {
            return false;
        }
    }

    private void addRegularUserToGroup(String groupname, String username) {
        if (groupRegularUsers.containsKey(groupname)) {
            groupRegularUsers.get(groupname).add(username);
        }
    }

    private void removeRegularUserFromGroup(String groupname, String username) {
        if (groupRegularUsers.containsKey(groupname)) {
            groupRegularUsers.get(groupname).remove(username);
        }
    }

    private boolean isCoAdminInGroup(String groupname, String username) {
        if (groupCoAdmins.containsKey(groupname)) {
            return groupCoAdmins.get(groupname).contains(username);
        } else {
            return false;
        }
    }

    private void addCoAdminToGroup(String groupname, String username) {
        if (groupCoAdmins.containsKey(groupname)) {
            groupCoAdmins.get(groupname).add(username);
        }
    }

    private void removeCoAdminFromGroup(String groupname, String username) {
        if (groupCoAdmins.containsKey(groupname)) {
            groupCoAdmins.get(groupname).remove(username);
        }
    }

    private boolean isAdminInGroup(String groupname, String username) {
        if (groupAdmins.containsKey(groupname)) {
            return groupAdmins.get(groupname).equals(username);
        } else {
            return false;
        }
    }

    private void systemPrinter() {
        System.out.println("usersList: " + usersList);
        System.out.println("groupsList: " + groupsList);
        System.out.println("groupAllUsers: " + groupAllUsers);
        System.out.println("groupAdmins: " + groupAdmins);
        System.out.println("groupCoAdmins: " + groupCoAdmins);
        System.out.println("groupMutedUsers: " + groupMutedUsers);
        System.out.println("groupRegularUsers: " + groupRegularUsers);
        System.out.println("groupRouters: " + groupRouters);
    }

}
