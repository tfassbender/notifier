# JFG Notifier

A simple notification service for push messages (using a producer - substriber pattern). Applications can subscribe to the notifier, send messages to notify other registered subscribers or receive the notifications. The receiving 
is done via streams (to enable push messages). The notifications for other subscribers can be added using REST.
