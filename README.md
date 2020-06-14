# WhatsApp-Clone
A textual-based WhatsApp clone, implemented in Akka using Java.
University assignment in coursework "Functional and Reactive Programming".
All commands for using this application are in the PDF file "Assignment 2 - The Actor Model using Akka.pdf".

Java version: jre-8u231 (Java 8) (You can download it here: https://www.java.com/en/download/windows-64bit.jsp)
Maven: 2.12.1

Instructions:
	
	1. File -> Open -> choose pom.xml file provided in each sub-directory, Open as Project,
		choose New Window in the pop-up (if it appears) and repeat for each of the sub-directories provided
		(You need to open an instance of IntelliJ for each sub-directory)
		
	2. Change JRE in 'Edit Configurations' to JAVA 8 for each instance of IntelliJ
		(default install location of Java 8 is C:\Program Files\Java\jre1.8.0_231)

	3. Run the server (Server.java file)

	4. Run the clients (ClientUI.java file)
		(We have provided 3 clients. If you wish to add more clients you need to create a copy of a Client folder,
		change the port in Client\src\main\resources\application.conf to a port number that has not been used before,
		and do instructions for the new client.
		Used port numbers: 3553-Server, 2552-Client1, 2553-Client2, 2554-Client3)

If you encounter this error, then make sure you are running Java 8:
	java.lang.ClassCastException: class [B cannot be cast to class [C ([B and [C are in module java.base of loader 'bootstrap')


Design of the Actor Model using Akka:
	
	Server extends AbstractActor:
		We have created an ActorSystem called whatsAppServerSystem, and from that system we created our managingServer called WhatsAppManagingServer.

		In the ManagingServer we receive messages related to the server only:
			1. Connect
			2. Disconnect
			3. Create group
			4. Leave group
			5. Send text to group
			6. Send file to group
			7. Invite user to group
			8. Remove user from group
			9. Mute user in group
			10. Unmute user in group
			11. Promote user to co-admin in group
			11. Demote co-admin to user in group
			12. Get actorRef

		In the ManagingServer we can send these types of messages:
			1. String
			2. ActorRef
			3. InvitePending
			4. broadcast using router to group

		The server contains multiple data structures in order to maintain it:
			1. Hashmap of <username, ActorRef>
			2. List of <groupnames>
			3. Hashmap of <groupname, List of all users in groupname>
			4. Hashmap of <groupname, Hashmap of <mutedUsers, timeout>>
			5. Hashmap of <groupname, List of <RegularUsers>>
			6. Hashmap of <groupname, List of <CoAdmins>>
			7. List of <groupname, admin>
			8. HashMap of <groupname, groupRouter>


	Client extends AbstractActor:
		We have created an ActorSystem called whatsAppClientSystem, and when we connect to the server, we create a client named after the given username.

		In the Client we send these messages to the server:
			1. Connect
			2. Disconnect
			3. Create group
			4. Leave group
			5. Send text to group
			6. Send file to group
			7. Invite user to group
			8. Remove user from group
			9. Mute user in group
			10. Unmute user in group
			11. Promote user to co-admin in group
			11. Demote co-admin to user in group
			12. Get actorRef
			13. String (from server)
			14. ReceiveTextFromGroup (from server router)
			15. ReceiveFileFromGroup (from server router)

		We send and receive these messages between clients:
			1. Send text to user
			2. Receive text from user
			3. Send file to user
			4. Receive file from user
