FROM openjdk:8u121-alpine

MAINTAINER Asaf Mesika <amesika@logz.io>

RUN apk add --no-cache --update bash curl vim

ADD target/jmx2graphite-*-javaagent.jar /jmx2graphite.jar
ADD slf4j-simple-1.7.25.jar /slf4j-simple-1.7.25.jar
ADD application.conf /application.conf
# Default Start
CMD java -cp jmx2graphite.jar:slf4j-simple-1.7.25.jar io.logz.jmx2graphite.Jmx2GraphiteJolokia application.conf