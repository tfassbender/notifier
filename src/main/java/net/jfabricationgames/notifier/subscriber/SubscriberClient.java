package net.jfabricationgames.notifier.subscriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple implementation of a client side subscriber that subscribes to the service and listens to notifications.
 */
public class SubscriberClient {
	
	private static final Logger LOGGER = LogManager.getLogger(SubscriberClient.class);
	
	/**
	 * The string that the service will send to request a name from the user.
	 */
	private static final String USERNAME_REQUEST = "<<send_username>>";
	/**
	 * A string that indicates the end of a notification message.
	 */
	private static final String MESSAGE_END = "<<notification_end>>";
	
	/**
	 * The name (URL) of the host.
	 */
	private String host;
	/**
	 * The port to send the notifications (via REST).
	 */
	private int portRest;
	/**
	 * The port to connect to (via socket).
	 */
	private int portSocket;
	/**
	 * The name, this client will use when subscribing to the notification service
	 */
	private String username;
	
	/**
	 * The socket that makes the connection to the notifier service
	 */
	private Socket socket;
	/**
	 * The input stream that reads from the socket connection
	 */
	private InputStream inStream;
	/**
	 * The output stream to send to the notification
	 */
	private OutputStream outStream;
	/**
	 * The thread that listens to notifications
	 */
	private Thread notificationListenerThread;
	
	/**
	 * Main method for testing
	 */
	public static void main(String[] args) throws IOException {
		new SubscriberClient();
	}
	
	/**
	 * Create a new client that loads the configurations (host, port and username), creates the connection and subscribes to the notification service.
	 * 
	 * Also a listener thread is started to handle the notifications from the service.
	 * 
	 * @throws IOException
	 *         An {@link IOException} is thrown if the creation of the client fails.
	 */
	public SubscriberClient() throws IOException {
		loadConfig();
		LOGGER.info("SubscriberClient: loaded configuration: [host: {}   port: {}   username: {}]", host, portRest, username);
		
		subscribeToNotifierService();
		startNotificationListener();
	}
	
	@Override
	public String toString() {
		return "SubscriberClient [host=" + host + ", portRest=" + portRest + ", portSocket=" + portSocket + ", username=" + username + ", socket="
				+ socket + ", inStream=" + inStream + ", outStream=" + outStream + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + portRest;
		result = prime * result + portSocket;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
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
		SubscriberClient other = (SubscriberClient) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
			return false;
		if (portRest != other.portRest)
			return false;
		if (portSocket != other.portSocket)
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	/**
	 * Create an start a notification listener to handle messages from the service.
	 */
	private void startNotificationListener() {
		if (notificationListenerThread != null && notificationListenerThread.isAlive()) {
			throw new IllegalStateException("A notification listener thread has already been started");
		}
		notificationListenerThread = new Thread(() -> {
			int available = 0;
			StringBuilder sb = new StringBuilder();
			while (!Thread.currentThread().isInterrupted()) {
				try {
					if ((available = inStream.available()) > 0) {
						LOGGER.trace("Received input (bytes available: {})", available);
						//read the input into a string
						byte[] buffer = new byte[available];
						inStream.read(buffer);
						String str = new String(buffer);
						
						//append the new text till the end of the message is reached
						sb.append(str);
						
						//check for message end(s)
						String rest;//rest of a next message
						//checks for multiple messages in one buffer
						while ((rest = handleMessageEnd(sb)) != null) {
							sb = new StringBuilder(rest);
						}
					}
				}
				catch (IOException ioe) {
					//this exception should be logged...
					ioe.printStackTrace();
				}
			}
		}, "notification_listener_thread");
		
		//start the listener thread
		notificationListenerThread.start();
	}
	
	/**
	 * Stop this subscriber and close the connection.
	 */
	public void closeConnection() throws IOException {
		if (notificationListenerThread != null) {
			notificationListenerThread.interrupt();
			socket.close();
			notificationListenerThread = null;
		}
		else {
			throw new IllegalStateException("The notification listener thread has either not yet been started or was already closed");
		}
	}
	
	/**
	 * Check for the end of a message.<br>
	 * If a message end is contained the message is handled and the rest of the string builder content is returned. Otherwise null is returned.
	 */
	private String handleMessageEnd(StringBuilder sb) {
		int messageEnd = 0;
		if ((messageEnd = sb.indexOf(MESSAGE_END)) > 0) {
			//split message in message content, message end identifier and rest (of a next message)
			String wholeMessage = sb.toString();
			String message = wholeMessage.substring(0, messageEnd);
			String rest = wholeMessage.substring(messageEnd + MESSAGE_END.length());
			
			LOGGER.debug("received message: {}", message);
			
			//handle the message content
			handleMessage(message);
			
			//put the rest of the message back in the string builder
			return rest;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Handle the message
	 */
	private void handleMessage(String message) {
		LOGGER.debug("handling message: {}", message);
		if (message.equals(USERNAME_REQUEST)) {
			//notification service requests a name for this user -> send the name
			try {
				LOGGER.debug("received name request. answering with username: {}", username);
				outStream.write(username.getBytes());
				outStream.flush();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
				//if the name can't be send the program can't go on
				LOGGER.fatal("failed to send username to service. ending programm");
				System.exit(1);
			}
		}
		else {
			LOGGER.debug("received notification message from service: {}", message);
			//do whatever you want with the message
		}
	}
	
	/**
	 * Open the connection to subscribe to the notification service
	 */
	private void subscribeToNotifierService() throws UnknownHostException, IOException {
		socket = new Socket(host, portSocket);
		inStream = socket.getInputStream();
		outStream = socket.getOutputStream();
	}
	
	/**
	 * Load configuration (host, port and username)
	 */
	private void loadConfig() throws IOException {
		String resourceName = "client_config.properties";
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties configProperties = new Properties();
		try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
			configProperties.load(resourceStream);
		}
		
		host = configProperties.getProperty("host", "localhost");
		username = configProperties.getProperty("username", "user");
		String portValue = null;
		String portValueSocket = null;
		try {
			portValue = configProperties.getProperty("port.rest", "<<not_found>>");
			portValueSocket = configProperties.getProperty("port.socket", "<<not_found>>");
			portRest = Integer.parseInt(portValue);
			portSocket = Integer.parseInt(portValueSocket);
		}
		catch (NumberFormatException nfe) {
			throw new IOException("port couldn't be interpreted as integer value (was: " + portValue + ")", nfe);
		}
		if (portRest < 1024) {
			throw new IOException("the port can't be a \"well known port\" (port number < 1024)");
		}
		if (host.equals("")) {
			throw new IOException("host mussn't be an empty string");
		}
		if (username.equals("")) {
			throw new IOException("username mussn't be an empty string");
		}
	}
}