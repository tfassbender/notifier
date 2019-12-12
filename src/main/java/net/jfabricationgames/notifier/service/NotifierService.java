package net.jfabricationgames.notifier.service;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.jfabricationgames.notifier.notification.Notification;
import net.jfabricationgames.notifier.subscriber.SubscriberManager;

@Path("notifier")
public class NotifierService {
	
	private static final Logger LOGGER = LogManager.getLogger(NotifierService.class);
	
	/**
	 * The subscriber manager that handles all subscribers and passes on the notifications
	 */
	private SubscriberManager manager;
	
	public NotifierService() {
		try {
			manager = new SubscriberManager();
		}
		catch (IOException ioe) {
			LOGGER.fatal("the subscriber manager couldn't be initialized (ending program)", ioe);
			//end the program because without a subscriber manager there is nothing to do
			System.exit(1);
		}
	}
	
	/**
	 * A simple hello world to test whether the service is reachable
	 */
	@GET
	@Path("/hello")
	@Produces(MediaType.APPLICATION_JSON)
	public Response processHelloRequestGet() {
		LOGGER.info("Received 'hello' request (HTTP GET)");
		String answer = "Hello there!";
		return Response.status(Status.OK).entity(answer).build();
	}
	
	/**
	 * Send a notification to registered listeners (using HTTP POST)
	 */
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response notifySubscribers(Notification notification) {
		LOGGER.info("Received notification: {}", notification);
		try {
			manager.sendNotification(notification);
			return Response.status(Status.OK).build();
		}
		catch (Exception e) {
			LOGGER.error("an error occured while trying to send the notification (sending HTTP 500 to producer)", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Send a notification to registered listeners (using HTTP GET)
	 */
	@GET
	@Path("/notify/{to_user}/{message}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response notifySubscribersHttpGet(@PathParam("to_user") String user, @PathParam("message") String message) {
		LOGGER.info("Received notification via HTTP GET request: [user: {} message: {}]", user, message);
		try {
			Notification notification = new Notification(message, "", user);
			manager.sendNotification(notification);
			return Response.status(Status.OK).build();
		}
		catch (Exception e) {
			LOGGER.error("an error occured while trying to send the notification (sending HTTP 500 to producer)", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}