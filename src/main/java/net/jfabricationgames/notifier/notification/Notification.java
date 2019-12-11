package net.jfabricationgames.notifier.notification;

import java.util.Arrays;
import java.util.List;

public class Notification {
	
	public Notification() {
		//default constructor for serialization
	}
	public Notification(String message, String sender, String... receivers) {
		this.message = message;
		this.sender = sender;
		this.receivers = Arrays.asList(receivers);
	}
	
	private String message;
	private String sender;
	private List<String> receivers;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	
	public List<String> getReceivers() {
		return receivers;
	}
	public void setReceivers(List<String> receivers) {
		this.receivers = receivers;
	}
}