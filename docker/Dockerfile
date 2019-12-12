FROM tomcat:9.0-alpine
LABEL maintainer="tobias-fassbender@gmx.de"

ADD JFG_Notifier.war /usr/local/tomcat/webapps/
ADD notifier_config.properties /usr/local/tomcat/lib/

EXPOSE 8080
CMD ["catalina.sh", "run"]