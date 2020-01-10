#create port and password enviroment variables
export NOTIFIER_PORT_REST=5714
export NOTIFIER_PORT_SOCKET=5716

#make the logs reachable for everyone (because they are created by root, which can cause problems)
sudo chmod 777 logs/*

#run docker-compose
docker-compose build
docker-compose up &
#the docker container will be started in the background
#use 'docker ps' to see running containers
#use 'docker stop <name_of_container>' to stop the container

#start the service by requesting anything (otherwhise the socket connection can not be established)
#wait 20 seconds before the request to give the docker time to start
sleep 20
curl localhost:${NOTIFIER_PORT_REST}/JFG_Notifier/notifier/notifier/hello 

#unset the variables
unset NOTIFIER_PORT_REST
unset NOTIFIER_PORT_SOCKET
