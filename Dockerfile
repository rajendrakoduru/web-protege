FROM registry1.dso.mil/ironbank/opensource/maven/maven-openjdk-11:3.8.5 AS build
USER root
COPY . /webprotege
RUN dnf install -y git
WORKDIR /webprotege
RUN  mvn clean install -X

FROM registry1.dso.mil/ironbank/opensource/apache/tomcat9-openjdk11:9.0.60
USER root
RUN dnf install -y unzip
RUN rm -rf /usr/local/tomcat/webapps/*  \
 && mkdir -p /srv/webprotege \
    && mkdir -p /usr/local/tomcat/webapps/ROOT

WORKDIR /usr/local/tomcat/webapps/ROOT

# Here WEBPROTEGE_VERSION is coming from the custom build args WEBPROTEGE_VERSION=$DOCKER_TAG hooks/build script.
# Ref: https://docs.docker.com/docker-hub/builds/advanced/
ARG WEBPROTEGE_VERSION=5.0.0-SNAPSHOT
ENV WEBPROTEGE_VERSION 5.0.0-SNAPSHOT
COPY --from=build /webprotege/webprotege-cli/target/webprotege-cli-${WEBPROTEGE_VERSION}.jar /webprotege-cli.jar
COPY --from=build /webprotege/webprotege-server/target/webprotege-server-${WEBPROTEGE_VERSION}.war ./webprotege.war
RUN unzip webprotege.war \
    && rm webprotege.war

