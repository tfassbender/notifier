package net.jfabricationgames.notifier.subscriber;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.jfabricationgames.notifier.notification.Notification;

class SubscriberManagerTest {
	
	@Test
	public void testMatchesUsernameRegex() {
		String regex = "project_name/.*";
		String matching = "project_name/a_username";
		String matching2 = "project_name/";
		String notMatching = "another_project_name/a_username";
		String notMatching2 = "project_name_user";
		
		assertTrue(SubscriberManager.matchesUsernameRegex(regex, matching));
		assertTrue(SubscriberManager.matchesUsernameRegex(regex, matching2));
		assertFalse(SubscriberManager.matchesUsernameRegex(regex, notMatching));
		assertFalse(SubscriberManager.matchesUsernameRegex(regex, notMatching2));
	}
	
	@Test
	public void testMatchesAnyUsernameRegex() {
		List<String> regex = Arrays.asList("project_name/.*", "a_username", ".*user42.*");
		
		String matching = "project_name/a_username";
		String matching2 = "project_name/";
		String matching3 = "a_username";
		String matching4 = "user42";
		String matching5 = "___user42__some_more_text";
		
		String notMatching = "another_project_name/a_username";
		String notMatching2 = "project_name_user";
		String notMatching3 = "a_different_username";
		String notMatching4 = "__user_42__";
		
		assertTrue(SubscriberManager.matchesAnyUsernameRegex(regex, matching));
		assertTrue(SubscriberManager.matchesAnyUsernameRegex(regex, matching2));
		assertTrue(SubscriberManager.matchesAnyUsernameRegex(regex, matching3));
		assertTrue(SubscriberManager.matchesAnyUsernameRegex(regex, matching4));
		assertTrue(SubscriberManager.matchesAnyUsernameRegex(regex, matching5));
		assertFalse(SubscriberManager.matchesAnyUsernameRegex(regex, notMatching));
		assertFalse(SubscriberManager.matchesAnyUsernameRegex(regex, notMatching2));
		assertFalse(SubscriberManager.matchesAnyUsernameRegex(regex, notMatching3));
		assertFalse(SubscriberManager.matchesAnyUsernameRegex(regex, notMatching4));
	}
	
	@Test
	public void testSendNotification() throws IOException {
		SubscriberManager manager = mock(SubscriberManager.class);
		Subscriber subscriber1 = getMockedSubscriber("project_name/user42");
		Subscriber subscriber2 = getMockedSubscriber("project_name/another_user");
		Subscriber subscriber3 = getMockedSubscriber("project_name/unique_username_10564845138");
		Subscriber subscriber4 = getMockedSubscriber("a_different_project/user42");
		Subscriber subscriber5 = getMockedSubscriber("user42");
		
		doCallRealMethod().when(manager).sendNotification(any(Notification.class));
		when(manager.getSubscribers()).thenReturn(Arrays.asList(subscriber1, subscriber2, subscriber3, subscriber4, subscriber5));
		
		String message1 = "a_message";
		String message2 = "a_different_message";
		Notification toAllProjectUsers = new Notification(message1, "me", "project_name/.*");
		Notification toUser42 = new Notification(message2, "also_me", "user42");
		
		manager.sendNotification(toAllProjectUsers);
		manager.sendNotification(toUser42);
		
		verify(subscriber1, times(1)).sendMessageToSubscriber(message1);
		verify(subscriber2, times(1)).sendMessageToSubscriber(message1);
		verify(subscriber3, times(1)).sendMessageToSubscriber(message1);
		verify(subscriber4, times(0)).sendMessageToSubscriber(any(String.class));
		verify(subscriber5, times(1)).sendMessageToSubscriber(message2);
		
		verify(subscriber1, times(1)).sendMessageToSubscriber(any(String.class));
		verify(subscriber2, times(1)).sendMessageToSubscriber(any(String.class));
		verify(subscriber3, times(1)).sendMessageToSubscriber(any(String.class));
		verify(subscriber5, times(1)).sendMessageToSubscriber(any(String.class));
	}
	
	private Subscriber getMockedSubscriber(String name) {
		Subscriber subscriber = mock(Subscriber.class);
		when(subscriber.getName()).thenReturn(name);
		when(subscriber.isConnected()).thenReturn(true);
		when(subscriber.isConnectionClosed()).thenReturn(false);
		
		return subscriber;
	}
}