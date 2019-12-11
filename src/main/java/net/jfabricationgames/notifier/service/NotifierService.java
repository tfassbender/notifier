package net.jfabricationgames.notifier.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
	private SubscriberManager manager = new SubscriberManager();
	
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
	
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response notifySubscribers(Notification notification) {
		LOGGER.info("Received notification: " + notification);
		try {
			manager.sendNotification(notification);
			return Response.status(Status.OK).build();
		}
		catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}