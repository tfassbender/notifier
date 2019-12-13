package net.jfabricationgames.notifier.subscriber;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriberReceiver {
	
	private static final Logger LOGGER = LogManager.getLogger(SubscriberReceiver.class);
	
	private SubscriberManager manager;
	
	private ServerSocket serverSocket;
	
	/**
	 * The port to listen for subscribers
	 */
	private int port;
	
	public SubscriberReceiver(SubscriberManager manager) throws IOException {
		this.manager = manager;
		
		try {
			//load the configuration (for port)
			loadConfiguration();
		}
		catch (IOException ioe) {
			LOGGER.error("configurations couldn't be loaded", ioe);
			throw ioe;
		}
		
		//start receiving socket requests
		startReceiver();
		
		LOGGER.info(">> SubscriberReceiver started");
	}
	
	@Override
	public String toString() {
		return "SubscriberReceiver [manager=" + manager + ", serverSocket=" + serverSocket + ", port=" + port + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((manager == null) ? 0 : manager.hashCode());
		result = prime * result + port;
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
		SubscriberReceiver other = (SubscriberReceiver) obj;
		if (manager == null) {
			if (other.manager != null)
				return false;
		}
		else if (!manager.equals(other.manager))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	/**
	 * Load the port configuration
	 */
	private void loadConfiguration() throws IOException {
		String resourceName = "notifier_config.properties";
		LOGGER.debug("loading configuration from config file: {}", resourceName);
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties configProperties = new Properties();
		try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
			configProperties.load(resourceStream);
		}
		
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
		LOGGER.info("configuration loaded. port is: {}", port);
	}
	
	private void startReceiver() {
		LOGGER.info("starting subscriber receiver");
		Thread subscriberReceiverThread = new Thread(() -> {
			try {
				serverSocket = new ServerSocket(port);
				LOGGER.info(">> ServerSocket started on port {}", port);
			}
			catch (IOException ioe) {
				LOGGER.fatal("an error occured while creating a server socket (ending application)", ioe);
				//end the program because without a subscriber listener that listens on a port the whole application is quite useless
				System.exit(1);
			}
			
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Socket socket = serverSocket.accept();
					LOGGER.debug("creating a new subscriber from the accepted socket connection: {}", socket);
					//only create the subscriber here and pass on the reference to this object
					//(the subscriber will register itself as soon as it received a name from the user)
					new Subscriber(socket, this);
				}
				catch (IOException ioe) {
					LOGGER.error("an error occured while trying to accept a new socket", ioe);
				}
			}
		}, "subscriber_receiver_thread");
		
		//start the receiver
		subscriberReceiverThread.setDaemon(true);
		subscriberReceiverThread.start();
	}
	
	public void registerSubscriber(Subscriber subscriber) {
		//test whether the subscriber is valid
		if (subscriber.getName() == null || subscriber.getName().equals("")) {
			throw new IllegalArgumentException("A subscribers name can't be empty");
		}
		else if (!subscriber.isConnected()) {
			throw new IllegalArgumentException("The subscriber is not connected");
		}
		
		//add the subscriber to the manager
		manager.addSubscriber(subscriber);
	}
	public void removeSubscriber(Subscriber subscriber) {
		manager.removeSubscriber(subscriber);
	}
}