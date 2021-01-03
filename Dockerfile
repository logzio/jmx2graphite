FROM maven:3.6.3-jdk-8-slim AS build
RUN mkdir -p /workspace
WORKDIR /workspace
COPY pom.xml /workspace
COPY src /workspace/src
RUN mvn -B -f pom.xml clean package -DskipTests

FROM openjdk:8u121-alpine

MAINTAINER Asaf Mesika <amesika@logz.io>

RUN apk add --no-cache --update bash curl vim
COPY --from=build /workspace/target/jmx2graphite-*-javaagent.jar /jmx2graphite.jar
ADD slf4j-simple-1.7.25.jar /slf4j-simple-1.7.25.jar
ADD application.conf /application.conf
# Default Start
CMD java -cp jmx2graphite.jar:slf4j-simple-1.7.25.jar io.logz.jmx2graphite.Jmx2GraphiteJolokia application.conf