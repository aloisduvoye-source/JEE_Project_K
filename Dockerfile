# Tomcat 11 : nécessaire à spring-app (Spring Boot 4, Servlet 6.1), et compatible avec jee-app (Servlet 6.0)
FROM tomcat:11.0-jdk17

# Version déployée : jee-app (par défaut) ou spring-app, choisie par APP dans docker-compose.yml
ARG APP=jee-app

RUN rm -rf /usr/local/tomcat/webapps/ROOT

COPY ${APP}/target/assurance.war /usr/local/tomcat/webapps/ROOT.war


EXPOSE 8080


CMD ["catalina.sh", "run"]
