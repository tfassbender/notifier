# JFG Notifier

A simple notification service for push messages (using a producer - substriber pattern). Applications can subscribe to the notifier, send messages to notify other registered subscribers or receive the notifications. The receiving 
is done via streams (to enable push messages). The notifications for other subscribers can be added using REST.

### Starting the service

To start the service a docker can be used. Therefore you need to copy the files from the directory `/docker` and a compiled .war file to a directory and execute the build script `build_and_run.sh` (docker and docker-compose need to be installed).

### Subscribing a client

A client can be subscribed to the service by creating a socket connection to the service. An example implementation of a client can be found here: [SubscriberClient example implementation](https://github.com/tfassbender/notifier/blob/master/src/main/java/net/jfabricationgames/notifier/subscriber/SubscriberClient.java).

### Sending a notification

Notifications can be send using REST (either HTTP GET or HTTP POST can be used). The URL to call is (using HTTP GET):

    url_to_your_host:<used_port_from_the_config_files>/JFG_Notification/notification/notification/notify/<from_user>/<to_user>/<the_message_you_want_to_send>

or when using HTTP POST:

    url_to_your_host:<used_port_from_the_config_files>/JFG_Notification/notification/notification/notify
    
using a notification object in json form (see the [Notification implementation](https://github.com/tfassbender/notifier/blob/master/src/main/java/net/jfabricationgames/notifier/notification/Notification.java)).