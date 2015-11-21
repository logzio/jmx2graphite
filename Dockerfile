FROM ubuntu/
MAINTAINER Asaf Mesika <asaf.mesika@gmail.com>

RUN apt-get install -y unzip

# Add the package
ADD build/distributions/*.tar /

# JAVA HOME
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle/jre
ENV PATH $PATH:$JAVA_HOME/bin

RUN mkdir -p /var/log/jmx2graphite

# Default Start
CMD /opt/jmx2graphite/bin/jmx2graphite
