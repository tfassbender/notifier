package net.jfabricationgames.notifier.subscriber;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Subscriber {
	
	private static final Logger LOGGER = LogManager.getLogger(Subscriber.class);
	
	private String name;
	private Socket socket;
	private InputStream inStream;
	private OutputStream outStream;
	
	public Subscriber(String name, Socket socket) throws IOException {
		this.name = name;
		this.socket = socket;
		this.inStream = new BufferedInputStream(socket.getInputStream());
		this.outStream = new BufferedOutputStream(socket.getOutputStream());
		
		//TODO send name request and receive result
	}
	
	public void sendMessageToSubscriber(String message) throws IOException {
		LOGGER.debug("sending message to subscriber (subscriber: {}; message: {}", this, message);
		outStream.write(message.getBytes());
		outStream.flush();
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