#!/bin/bash
docker stop jmx2graphite
docker rm jmx2graphite
./run-jmx2graphite.sh
