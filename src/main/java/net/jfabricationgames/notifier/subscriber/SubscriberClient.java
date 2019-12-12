package net.jfabricationgames.notifier.subscriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * A simple implementation of a client side subscriber that subscribes to the service and listens to notifications.
 */
public class SubscriberClient {
	
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
	 * The port to contact for the notification service.
	 */
	private int port;
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
		subscribeToNotifierService();
		startNotificationListener();
	}
	
	/**
	 * Create an start a notification listener to handle messages from the service.
	 */
	private void startNotificationListener() {
		Thread notificationListenerThread = new Thread(() -> {
			int available = 0;
			StringBuilder sb = new StringBuilder();
			while (!Thread.currentThread().isInterrupted()) {
				try {
					if ((available = inStream.available()) > 0) {
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
		notificationListenerThread.setDaemon(true);
		notificationListenerThread.start();
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
		if (message.equals(USERNAME_REQUEST)) {
			//notification service requests a name for this user -> send the name
			try {
				outStream.write(username.getBytes());
				outStream.flush();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
				//if the name can't be send the program can't go on
				System.err.println("FATAL: failed to send username to service. ending programm");
				System.exit(1);
			}
		}
		else {
			//do whatever you want with the message
			System.out.println(message);
		}
	}
	
	/**
	 * Open the connection to subscribe to the notification service
	 */
	private void subscribeToNotifierService() throws UnknownHostException, IOException {
		socket = new Socket(host, port);
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
		username = configProperties.getProperty("user", "user");
		String portValue = null;
		try {
			portValue = configProperties.getProperty("port", "<<not_found>>");
			port = Integer.parseInt(portValue);
		}
		catch (NumberFormatException nfe) {
			throw new IOException("port couldn't be interpreted as integer value (was: " + portValue + ")", nfe);
		}
		if (port < 1024) {
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