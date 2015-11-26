FROM debian:jessie
MAINTAINER Asaf Mesika <asaf.mesika@gmail.com>

# Install Java.
ENV DEBIAN_FRONTEND noninteractive
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
RUN echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN apt-get update
RUN apt-get install -y --fix-missing \
	apt-utils \
	cpanminus
RUN cpanm Term::ReadLine

RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

RUN apt-get install -y \
	oracle-java8-installer \
	oracle-java8-set-default

RUN apt-get update
RUN apt-get install -y --fix-missing\
	vim \
	bc

# JAVA HOME
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle/jre
ENV PATH $PATH:$JAVA_HOME/bin

# Misc
RUN apt-get install -y sudo

# Add UTF-8 support
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen
RUN locale-gen
RUN dpkg-reconfigure locales
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8


# Add the package
ADD build/distributions/*.tar /

RUN mkdir -p /var/log/jmx2graphite

# Default Start
CMD /opt/jmx2graphite/bin/jmx2graphite
