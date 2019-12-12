#create port and password enviroment variables
export TOMCAT_EXTERNAL_PORT=5713
export NOTIFIER_PORT=5712

#make the logs reachable for everyone (because they are created by root, which can cause problems)
sudo chmod 777 logs/*

#run docker-compose
docker-compose build
docker-compose up

#unset the variables
unset TOMCAT_EXTERNAL_PORT
unset NOTIFIER_PORT