import java.io.*;
import java.net.*;
import java.util.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ChatServer {

	protected int serverPort = 1234;
	public List<String> usernames = new ArrayList<String>(); // list of usernames of clients
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		Iterator<Socket> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			Socket socket = (Socket) i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void sendAPrivateMessage(String message, String username, Socket s) throws Exception //username that we want to send a message t, socket of username that send a message
	{
		Iterator<Socket> i=clients.iterator();
		Iterator<String> j=usernames.iterator();
		boolean clientExists=false;

		while(j.hasNext() && i.hasNext()){
			String user = j.next();
			Socket socket = i.next();

			if(user.equals(username))
			{
				clientExists=true;
				//String pMsg=""
				//Socket socket = (Socket) i.next(); // do we get the right i here?
													// so if you do it my way, you dont need that line
				try {
					DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
					out.writeUTF(message); // send message to the client
					DataOutputStream out1= new DataOutputStream(s.getOutputStream());
					out1.writeUTF(message);

				} catch (Exception e) {
					System.err.println("[system] could not send message to " + user);
					e.printStackTrace(System.err);
				}
			}
			else
				continue;

		}

		if (clientExists==false)
		{
			//System.out.println("The client with the username that you want to send a message to does not exist !");
			String msg="The client with the username that you want to send a message to does not exist !";
			DataOutputStream out3 = new DataOutputStream(s.getOutputStream());
			out3.writeUTF(msg);
		}
			 //does it print this everywhere(chatServer and clients)
			// or only in the chatClient console from where we tried to send a message?
			// NO: now we are in the ChatServer, so that will be printed only in console of ChatServer

	}

	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket; //socket of client
	private String username;

	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;

	}

	public void run() {
		
		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		
		try {
			String firstMessage=in.readUTF(); // NEW
			server.usernames.add(firstMessage);
			username=firstMessage;

		}catch (Exception e) {
			
			System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
			e.printStackTrace(System.err);
			this.server.removeClient(this.socket);
			return;
		}

		System.out.println("[system] connected with " + username);

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client
			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

			Date date = new Date();
		    String strDateFormat = "hh:mm:ss a";
		    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
		    String time=dateFormat.format(date);

		    if (msg_received.length() == 0) // invalid message
				continue;

		    String[] result=msg_received.split(" ", 2);
		    String first=result[0];
		    char letter=first.charAt(0);
			
			System.out.println("[RKchat] [" + username + "] at " + time +": " + msg_received); // print the incoming message in the console of ChatServer
			String msg_send = username + " said at "+ time +": " + msg_received.toUpperCase();

			if(letter == '@'){
				String user = first.substring(1, first.length());
				
				String sentence="[private message] : " + msg_send;

				try {
					this.server.sendAPrivateMessage(sentence, user, socket);
				} catch (Exception e) {
					System.err.println("[system] there was a problem while sending the message to " + user);
					e.printStackTrace(System.err);
					continue;
				}

			}else{
				try {
					this.server.sendToAllClients(msg_send); // send message to all clients
				} catch (Exception e) {
					System.err.println("[system] there was a problem while sending the message to all clients");
					e.printStackTrace(System.err);
					continue;
				}

			}
			
		}
	}
}
