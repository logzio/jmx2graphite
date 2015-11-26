#!/bin/bash
HOMEDIR=`pwd`
docker run -i -t -d --name jmx2graphite \
   -e "JOLOKIA_URL=http://your-java-app-host:port/jolokia/" \
   -e "SERVICE_NAME=MyApp" \
   -e "GRAPHITE_HOST=your-graphite-host" \
   --restart=always \
   -v $HOMEDIR/log/jmx2graphite:/var/log/jmx2graphite \
   -v $HOMEDIR/graphite/storage:/opt/graphite/storage \
   -v $HOMEDIR/log:/var/log \
   -v $HOMEDIR/graphite/conf:/opt/graphite/conf \
   -v $HOMEDIR/graphite/webapp/graphite:/opt/graphite/webapp/graphite \
   logzio/jmx2graphite

# Environment variables left out
#   -e "GRAPHITE_PORT=2004" \
#   -e "SERVICE_HOST=172_1_1_2" \
#   -e "INTERVAL_IN_SEC=30" \
