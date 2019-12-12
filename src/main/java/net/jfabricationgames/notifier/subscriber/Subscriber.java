package net.jfabricationgames.notifier.subscriber;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Subscriber {
	
	private static final Logger LOGGER = LogManager.getLogger(Subscriber.class);
	
	private static final String USERNAME_REQUEST = "<<send_username>>";
	private static final String MESSAGE_END = "<<notification_end>>";
	
	private SubscriberReceiver receiver;
	
	private String name;
	private Socket socket;
	private InputStream inStream;
	private OutputStream outStream;
	
	public Subscriber(Socket socket, SubscriberReceiver receiver) throws IOException {
		this.socket = socket;
		this.inStream = new BufferedInputStream(socket.getInputStream());
		this.outStream = new BufferedOutputStream(socket.getOutputStream());
		this.receiver = receiver;
		
		//send a name request and receive result (afterwards register this subscriber to the manager)
		sendNameRequest();
	}
	
	/**
	 * Send a request to the user to make him tell his name (no verification)
	 * 
	 * @throws IOException
	 */
	private void sendNameRequest() throws IOException {
		LOGGER.debug("sending username request to subscriber");
		sendMessageToSubscriber(USERNAME_REQUEST);
		
		Thread usernameRequestListenerThread = new Thread(() -> {
			Scanner scanner = new Scanner(inStream);
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(10);
					//wait for the subscriber to enter a username
					if (scanner.hasNext()) {
						LOGGER.debug("reading scanner input");
						String username = scanner.next();
						LOGGER.debug("subscriber registered with username: {}", username);
						//set the username to the subscriber
						Subscriber.this.name = username;
						
						//stop listening to the subscriber (subscribers only receive from now on)
						Thread.currentThread().interrupt();
					}
				}
				catch (InterruptedException ie) {
					//reset interrupted flag
					Thread.currentThread().interrupt();
				}
			}
			scanner.close();
			
			//register the subscriber to the receiver (which registers it to the manager)
			receiver.registerSubscriber(Subscriber.this);
		}, "username_request_listener_thread");
		
		//start the thread for listening to the username
		usernameRequestListenerThread.setDaemon(true);
		usernameRequestListenerThread.start();
	}
	
	/**
	 * Send the given message to the subscriber (a notification end tag will be added to the end of the message).
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendMessageToSubscriber(String message) throws IOException {
		LOGGER.debug("sending message to subscriber (subscriber: {}; message: {}{})", this, message, MESSAGE_END);
		//send the message
		outStream.write(message.getBytes());
		//add the notification end tag
		outStream.write(MESSAGE_END.getBytes());
		outStream.flush();
	}
	
	public boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed() && outStream != null;
	}
	public boolean isConnectionClosed() {
		return socket.isClosed();
	}
	public void closeConnection() throws IOException {
		LOGGER.debug("closing connection of subscriber {}", this);
		socket.close();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}