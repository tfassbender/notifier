package net.jfabricationgames.notifier.subscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.jfabricationgames.notifier.notification.Notification;

public class SubscriberManager {
	
	private static final Logger LOGGER = LogManager.getLogger(SubscriberManager.class);
	
	private List<Subscriber> subscribers;
	
	public SubscriberManager() throws IOException {
		subscribers = new ArrayList<Subscriber>();
		
		//the receiver is just created with a reference and not saved as reference
		new SubscriberReceiver(this);
		
		LOGGER.info(">> SubscriberManager started");
	}
	
	@Override
	public String toString() {
		return "SubscriberManager [subscribers=" + subscribers + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subscribers == null) ? 0 : subscribers.hashCode());
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
		SubscriberManager other = (SubscriberManager) obj;
		if (subscribers == null) {
			if (other.subscribers != null)
				return false;
		}
		else if (!subscribers.equals(other.subscribers))
			return false;
		return true;
	}
	
	public void sendNotification(Notification notification) {
		LOGGER.debug("sending notification: {}", notification);
		
		//copy the list of subscribers to prevent concurrent modifications
		List<Subscriber> currentSubscribers = new ArrayList<Subscriber>(getSubscribers());
		
		//send the notification message to all subscribers
		currentSubscribers.stream().filter(s -> matchesAnyUsernameRegex(notification.getReceivers(), s.getName()))
				.forEach(s -> sendNotificationToSubscriber(s, notification));
	}
	
	protected static boolean matchesAnyUsernameRegex(List<String> receiverRegex, String testedName) {
		boolean matches = false;
		for (String receiver : receiverRegex) {
			matches |= matchesUsernameRegex(receiver, testedName);
		}
		return matches;
	}
	protected static boolean matchesUsernameRegex(String usernameRegex, String testedName) {
		return Pattern.matches(usernameRegex, testedName);
	}
	
	private void sendNotificationToSubscriber(Subscriber subscriber, Notification notification) {
		try {
			subscriber.sendMessageToSubscriber(notification.getMessage());
		}
		catch (IOException ioe) {
			LOGGER.warn("problems occured while trying to send a message to the subscriber (closing connection to subscriber)", ioe);
			if (subscriber.isConnectionClosed()) {
				LOGGER.info("subscriber connection was already closed (subscriber: {})", subscriber);
			}
			else {
				LOGGER.debug("closing connection to subscriber: {}", subscriber);
				try {
					subscriber.closeConnection();
				}
				catch (IOException e) {
					LOGGER.error("couldn't close connection to subscriber", e);
				}
			}
			removeSubscriber(subscriber);
		}
	}
	
	public void addSubscriber(Subscriber subscriber) {
		LOGGER.debug("adding subscriber: {}", subscriber);
		subscribers.add(subscriber);
	}
	public void removeSubscriber(Subscriber subscriber) {
		LOGGER.debug("removing subscriber: {}", subscriber);
		subscribers.remove(subscriber);
	}
	
	protected List<Subscriber> getSubscribers() {
		return subscribers;
	}
}