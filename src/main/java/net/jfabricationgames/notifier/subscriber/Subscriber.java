package net.jfabricationgames.notifier.subscriber;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

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
		
		LOGGER.info(">> Subscriber started");
	}
	
	@Override
	public String toString() {
		return "Subscriber [receiver=" + receiver + ", name=" + name + ", socket=" + socket + ", inStream=" + inStream + ", outStream=" + outStream
				+ "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscriber other = (Subscriber) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (receiver == null) {
			if (other.receiver != null)
				return false;
		}
		else if (!receiver.equals(other.receiver))
			return false;
		return true;
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
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(10);
					//wait for the subscriber to enter a username
					int available;
					if ((available = inStream.available()) > 0) {
						LOGGER.debug("reading scanner input");
						byte[] buffer = new byte[available];
						inStream.read(buffer);
						String username = new String(buffer);
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
				catch (IOException ioe) {
					LOGGER.error("error while trying to read from input stream", ioe);
				}
			}
			
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
		boolean connected = isConnected();
		boolean removeSubscriber = false;
		LOGGER.debug("sending message to subscriber (message: {}{}   connected: {})", message, MESSAGE_END, connected);
		
		if (connected) {
			try {
				//send the message
				outStream.write(message.getBytes());
				//add the notification end tag
				outStream.write(MESSAGE_END.getBytes());
				outStream.flush();
			}
			catch (SocketException se) {
				LOGGER.warn("SocketException caught: {}\nthe subscriber seems to be disconnected. notification is not send", se.getMessage());
				removeSubscriber = true;
			}
		}
		else {
			LOGGER.warn("the subscriber disconnected. notification is not send");
			removeSubscriber = true;
		}
		
		if (removeSubscriber) {
			LOGGER.info("removing this subscriber");
			receiver.removeSubscriber(this);
		}
	}
	
	public boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed() && outStream != null;
	}
	public boolean isConnectionClosed() {
		return socket.isClosed();
	}
	public void closeConnection() throws IOException {
		LOGGER.debug("closing connection of this subscriber");
		socket.close();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}