# jmx2graphite

jmx2graphite is a one liner tool for polling JMX and writes into Graphite (every 30 seconds by default). You install & run it on every machine you want to poll its JMX.

Currently it has two flavors:
1. Docker image which reads JMX from a jolokia agent running on a JVM, since exposing JMX is the simplest and easiest through Jolokia agent (1 liner - see below).
2. Run as a java agent, and get metrics directly from MBean Platform

The metrics reported have the following names template:

[service-name].[service-host].[metric-name]

- service-name is a parameter you supply when you run jmx2graphite. For example "LogReceiever", or "FooBackend"
- service-host is a parameter you supply when you run jmx2graphite. If not supplied it's the hostname of the 
  jolokia URL. For example: "172_1_1_2" or "log-receiver-1_foo_com"
- metric-name the name of the metric taken when polling Jolokia. For example: java_lang.type_Memory.HeapMemoryUsage.used

# How to run?

## Using Docker (preferred)
If you don't have docker, install it first - instructions [here](http://docs.docker.com/engine/installation/).

Run the docker either with environment variables or with a configuration file.

### Docker with env variables
```bash
docker run -i -t -d --name jmx2graphite \
   -e "JOLOKIA_URL=http://172.1.1.2:11001/jolokia/" \
   -e "SERVICE_NAME=MyApp" \
   -e "GRAPHITE_HOST=graphite.foo.com" \
   -e "GRAPHITE_PROTOCOL=pickled" \
   -v /var/log/jmx2graphite:/var/log/jmx2graphite \
   --rm=true \
   logzio/jmx2graphite
```

**Environment variables**
- JOLOKIA_URL: The full URL to jolokia on the JVM you want to sample. When jolokia (and the java app) is running inside a docker container
there are two ways to specify the host in the jolokia URL so this URL will be reachable by jmx2graphite which also runs inside a docker instance:
  - The easy one: On the docker running your java app and Jolokia, makes sure to expose the jolokia port (using -v), and then use the *host* IP of the machine
    running the dockers.
  - Container linking: You can use a hostname you invent like "myapp.com", and then when running jmx2graphite using Docker, add the option: 
  ``` --link myservice-docker-name:myapp.com"```. So if your app is running in docker named "crazy_service" then you would write jolokia URL 
  as "http://myapp.com:8778/jolokia", and when running jmx2graphite using docker add the option "--link crazy_service:myapp.com". 
  What this does is add mapping between the host name myapp.com to the internal IP of the docker running your service to the /etc/hosts file.
- SERVICE_NAME: The name of the service (it's role).
- GRAPHITE_HOST: The hostname/IP of graphite
- GRAPHITE_PROTOCOL: Protocol for graphite communication. Possible values: udp, tcp, pickled

**Optional environment variables**

- GRAPHITE_PORT: Protocol port of graphite. Defaults to 2004.
- SERVICE_HOST: By default the host is taken from Jolokia URL and serves as the service host, unless you use this variable.
- INTERVAL_IN_SEC: By default 30 seconds unless you use this variable.

### Docker with config file
Create a .conf file, set the input parameter and provide it to the docker.
See our [sample config file](https://github.com/logzio/jmx2graphite/blob/master/application.conf)].

You can find a complete list of the required parameters [here](#using-bash--jolokia-agent)
```bash
docker run -d -name jmx2graphtite -v path/to/config/myConfig.conf:application.conf logizo/jmx2graphite
```
**Note**: The config file at the docker end must be name application.conf

*Rest of command*
- `--rm=true`: removes the docker image created upon using `docker run` command, so you can just call `docker run` command again.

## Using bash + Jolokia agent
1. get the java agent jar from the releases page
2. Create a config file that will contain the input parameters, see [our sample config file](https://github.com/logzio/jmx2graphite/blob/master/application.conf) - The mandatory items are:
   1. service.jolokiaFullUrl - Fill in the full URL to the JVM running Jolokia (It exposes your JMX as a REST service, normally under port 8778).
   2. service.name - The role name of the service.
   3. graphite.hostname  - Graphite host name the metrics will be sent to
   4. graphite.port - The port which Graphite listen to.
   5. graphite.connectTimeout - Timeout in seconds for the connection with graphite.
   6. graphite.socketTimeout - Timeout in seconds for the socket.
3. Run your app with Jolokia agent (instructions below)

4. run the jar: ```java -jar jmx2graphite.jar```

5. If you wish to run this as a service you need to create a service wrapper for it. Any pull requests for making it are welcome!

   
## As Java Agent
This lib can also get the metrics from MBean Platform instead of jolokia. In order to do so, we need to run inside the JVM.
- First, get the java agent jar from the releases page
- Modify your app JVM arguments and add the following:  java -javaagent:/path/to/jmx2graphite-1.3.1-javaagent.jar=GRAPHITE_HOSTNAME=graphite.host,SERVICE_NAME=Myservice ...
- The parameters are key-value pairs, in the format of key=value;key=value;... or key=value,key=value,...
- The parameters names and functions are exactly as described in Environment Variables section. (Except no need to specify JOLOKIA_URL of course)
- The javaagent.jar is an "Uber-Jar" that shades all of its dependencies inside, to prevent class collisions
- For example: java -javaagent:jmx2graphite.jar=GRAPHITE_HOSTNAME=graphite.example.com,SERVICE_NAME=PROD.MyAwesomeCategory example.jar

   

# How to expose JMX Metrics using Jolokia Agent

1. Download Jolokia JVM Agent JAR file [here](http://search.maven.org/remotecontent?filepath=org/jolokia/jolokia-jvm/1.3.2/jolokia-jvm-1.3.2-agent.jar).
2. Add the following command line option to your line running java:
 ```
 -javaagent:path-to-jolokia-jar-file.jar
 ```
 For example:
 ```
 -javaagent:/opt/jolokia/jolokia-jvm-1.3.2-agent.jar
 ```

By default it exposes an HTTP REST interface on port 8778. See [here](https://jolokia.org/reference/html/agents.html#jvm-agent) if you want to change it and configure it more.
We run all of ours apps using Docker, so to avoid clashes when we map the 8778 port to a unique external port belonging only to this application. 


# Installing and Configuring Graphite
If you never installed Graphite, this small guide below might be a good place to start. I'm using Docker since it's very easy to install this way.

## Installing Graphite
We will install Graphite using a great docker image by [hopsoft](https://github.com/hopsoft/docker-graphite-statsd). I tried several and it was by far the easiest to work with.

1. Run the following to get basic Graphite up and running
    ```
    docker run -d \
      --name graphite \
      -p 80:80 \
      -p 2003:2003 \
      -p 2004:2004 \
      -p 8125:8125/udp \
      -p 8126:8126 
    ```
2. Now, let's copy out all of its existing configuration files so it will be easy to modify. I will assume you will place it at `/home/ubuntu`
    ```
    cd /home/ubuntu
    mkdir graphite
    docker cp graphite:/opt/graphite/conf graphite
    docker cp graphite:/opt/graphite/webapp/graphite graphite/webapp
    ```

3. Stop graphite by running `docker stop graphite`

4. Configuring Graphite: Now edit the following files:
  1. `/home/ubuntu/graphite/conf/carbon.conf`:
     - MAX_CREATES_PER_MINUTE: Make sure to place high values - for example 10000. The default of 50 means that
        the first time you run jmx2graphite, all of your metrics are reported at once. If you have more than 50, all 
        other metrics will be dropped.
     - MAX_UPDATES_PER_SECOND: I read a lot there should be a formula for calcualting the value for this field, but
        that is once you reach high I/O disk utilization. For now, simply place 10000 value there. Otherwise you will
        get a 1-2 minute lag from the moment jmx2graphite pushes the metric to Graphite until it is viewable in 
        Graphite dashboard
  2. `/home/ubuntu/graphite/conf/storage-schemas.conf`:
      - in the default section (default_1min_for_1day) make sure retentions is set to the same interval as you are using
      in jmx2graphite (30seconds by default). Here is an example
      ```
      [default_1min_for_1day]
      pattern = .*
      retentions = 30s:24h,1min:7d,10min:1800d
      ```
      If you have 10s:24h then when doing derivative, you will get null values for each missing 2 points in the 30sec window
      and the graph will be empty

5. Create some directories which normally are created by the docker image but since we're mounting `/var/log` to an empty directory of ours in the host, they don't exists:
    ```bash
    mkdir -p /home/ubuntu/log/nginx
    mkdir -p /home/ubuntu/log/carbon
    mkdir -p /home/ubuntu/log/graphite
    ```

6. Run Graphite. I use the following short bash script `run-graphite.sh`:
    ```bash
    #!/bin/bash
     docker run -d \
      --name graphite \
      --rm=true \
      --restart=always \
      -p 80:80 \
      -p 2003:2003 \
      -p 2004:2004 \
      -p 8125:8125/udp \
      -p 8126:8126 \
      -v /home/ubuntu/graphite/storage:/opt/graphite/storage \
      -v /home/ubuntu/log:/var/log \
      -v /home/ubuntu/graphite/conf:/opt/graphite/conf \
      -v /home/ubuntu/graphite/webapp/graphite:/opt/graphite/webapp/graphite \
      hopsoft/graphite-statsd
    ```

## Configuring Graphite

If you have an existing Graphite installation see the section above "configuring Graphite: Now edit the following files:".

# Motivation

I was looking for a tool I can just drop in place, have a 1-liner run command which will then run every 10 seconds, poll my JVM JMX entirely and dump it to Graphite.
Of course I started Googling and saw the following:

- [JMXTrans](https://github.com/jmxtrans/jmxtrans)
   I had several issues which got me stomped: 
     - You can't instruct it to sample all JMX metrics. Instead you have to specify exactly which MBeans which you want and also their attributes - this can be quite a long list. In order to compose this list you have to fire up JMX Console, find the bean you are interested at, extract its name and add several lines of config to your config file. Then you have to copy the attribute names you want from this mbean. Rinse and repeat for every bean. For me, I just wanted all, since when you benchmark a JVM you don't know where the problem is so you want to start with everything at hand. From my handful experience with JMX, polling all beans doesn't impact the running JVM. Graphite can be boasted with hardware if it will become the bottleneck. Essentially I would like to add blacklist/whitelist to jmx2graphite, but it should be straightforward wildcard expession and not regular expression based.
     - I had trouble understanding how to configure it polling several JVMs. It invovles writing a YAML file and then running a CLI for generating the configuration file for JMXTrans. Too complicated in my opinion. 
- [jmxproxy](https://github.com/mk23/jmxproxy)
   It's an HTTP REST server allowing you to fetch mbeans from a given JVM using REST to it. You are supposed to have one per your cluster. Great work there. The biggest drawback here was that you have to specify a predefined list of mbeans to retrieve - I wanted it all - it's too much work to compose the list of mbeans for: Camel, Kafka, Zookeeper, your own, etc.
  
- [Sensu plugin](https://github.com/sensu/sensu-community-plugins/blob/master/plugins/http/http-json-graphite.rb) - Aside from the prequisite of Sensu, again you must supply a predefined list of beans. 

- [Collectd plugin](https://collectd.org/wiki/index.php/Plugin:GenericJMX) - Must have collectd and also, same as before, specify a list of mbeans and their attributes in a quite complicated config file. This also requires installing another collectd plugin.

- [Fluentd JMX plugin](https://github.com/niyonmaruz/fluent-plugin-jmx) - Must have fluentd installed. Must specify list of mbeans and their attributes. Works against Jolokia only (same as jmx2graphite)

   
So after spending roughly 1.5 days fighting with those tools and not getting what I wanted, I sat down to write my own.

## Why Docker?
Docker enables jmx2graphite to install and run in one command line! Just about any other solution will requires more steps for installation, and not to mention the development efforts.

## Why Jolokia?
- When running JVM application inside docker it is sometime quite complex getting JMX to work, especially around ports.
- Composing JMX URI seems very complicated and not intuitive. Jolokia REST endpoint is straight forward.
- Can catch reading several MBeans into one call (not using this feature yet though)

# Features Roadmap

- Add Integration Tests using Vagrant
- Add support for reading using JMX RMI protocol for those not using Jolokia.
- Support whiltelisting/blacklisting for metrics

# Contributing 

We welcome any contribution! You can help in the following way:
- Open an issue (Bug, Feature request, etc)
- Pull requests for any addition you can think of

## Building and Deploying
# Build

Build Java Agent
```
mvn clean install
```

# Deploy
```
docker login 
docker push logzio/jmx2graphite
```


# Changelog
- v1.3.1
  - support external config file when using with jolokia agent 
  - provide docker for jmx2graphite when using with jolokia agent
- v1.3
  - jmx2graphite is now a maven project, Hooray!
- v1.2.5
  - This release adds support for commas as argument delimiters when using as a Java agent. If you experience issues when using semicolons as argument delimiters, try using a comma.
- v1.2.3
  - Fixed an NPE when poll() resulted in MBeanClient.MBeanClientPollingFailure
- v1.2.1
  - Fixed a bug when no protocol was provided
  - Fixed log4j dependencies
- v1.2.0
  - Changed Docker image to be based upon Alpine and OpenJDK 
- v1.1.1 
  - Added support for 2 additional protocols when sending metrics to Graphite: tcp, udp. This is
    in addition to the existing Pickle protocol (Contributed by: jdavisonc)
- v1.1.0
  - Major refactoring - jmx2graphite now comes in two flavors: standalone using docker as it was in 1.0.x, and as a Java Agent running alongside you app. This is useful if your app is running inside Docker on Mesos and coupling it with another container just to read its metrics contradicts the Mesos paradigm.
  - Added java agent capabilities, through MBeans Platform
  - Changed logback to log4j
- v1.0.8
  - First migration step to Kotlin language
- v1.0.7
  - Issue #2: Log file appender will show TRACE debug level as well
- v1.0.6
  - Fixes #4: logback will save history for 7 days
- v1.0.5
  - logback.xml now scan it self every 10 seconds instead of 30 to get that fast feedback loop
  - Added an XML element sample to logback.xml to trace the metrics that are sent to Graphite
- v1.0.4
  - logback.xml now scan it self every 30 seconds. Improved error message printed to the log
- v1.0.3
  - Wouldn't recover from Graphite server restart (failed on broken pipe for a long time)
- v1.0.2 
  - MBean name properties (the part that is after the ':') retrieved from jolokia were sorted lexically by property name. 
    This removed any creation order of those properties which actually represent a tree path, thus the fix is to 
    maintain the creation order.
- v1.0.1 
  - MBean name got its dots converted into _ which results in flattening your beans too much. Now the dot is kept.

#License

See the LICENSE file for license rights and limitations (MIT).
