package net.jfabricationgames.notifier.subscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	}
	
	public void sendNotification(Notification notification) {
		LOGGER.debug("sending notification: " + notification);
		//send the notification message to all subscribers
		subscribers.stream().filter(s -> notification.getReceivers().contains(s.getName()))
				.forEach(s -> sendNotificationToSubscriber(s, notification));
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
}