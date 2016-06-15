package io.logz.jmx2graphite;

import com.typesafe.config.Config;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * @author amesika
 */
public class Jmx2GraphiteConfiguration {
    /* Jolokia full URL */
    private String jolokiaUrl;

    // Graphite server related params
    private  String graphiteHostname;
    private int graphitePort;

    /* Short name of the sampled service (exposed by the Jolokia URL)", required = false */
    private String serviceName = null;

    /* host of the sampled service (exposed by the Jolokia URL) */
    private String serviceHost = null;

    /* Metrics polling interval in seconds */
    private int intervalInSeconds;
    private int graphiteConnectTimeout;
    private int graphiteSocketTimeout;
    private int graphiteWriteTimeoutMs;

    // Which client should we use
    private MetricClientType metricClientType;

    public enum MetricClientType {
        JOLOKIA,
        MBEAN_PLATFORM
    }

    public Jmx2GraphiteConfiguration(Config config, MetricClientType metricClientType) throws IllegalConfiguration {

        this.metricClientType = metricClientType;

        if (config.hasPath("service.host")) {
            serviceHost = config.getString("service.host");
        }

        if (this.metricClientType == MetricClientType.JOLOKIA) {
            jolokiaUrl = config.getString("service.jolokiaUrl");
            String jolokiaHost;
            try {
                URL jolokia = new URL(jolokiaUrl);
                jolokiaHost = jolokia.getHost();
            } catch (MalformedURLException e) {
                throw new IllegalConfiguration("service.jolokiaUrl must be a valid URL. Error = " + e.getMessage());
            }

            // Setting jolokia url as default
            if (serviceHost == null) {
                serviceHost = jolokiaHost;
            }

        } else if (this.metricClientType == MetricClientType.MBEAN_PLATFORM) {

            // Try to find hostname as default to serviceHost in case it was not provided
            if (serviceHost == null) {
                try {
                    serviceHost = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    throw new IllegalConfiguration("service.host was not defined, and could not determine it from the servers hostname");
                }
            }
        }

        graphiteHostname = config.getString("graphite.hostname");
        graphitePort = config.getInt("graphite.port");
        intervalInSeconds = config.getInt("intervalInSeconds");

        serviceName = config.getString("service.name");

        graphiteConnectTimeout = config.getInt("graphite.connectTimeout");
        graphiteSocketTimeout = config.getInt("graphite.socketTimeout");
        if (config.hasPath("graphite.writeTimeout")) {
            graphiteWriteTimeoutMs = config.getInt("graphite.writeTimeout");
        } else {
            graphiteWriteTimeoutMs = Math.round(0.7f * TimeUnit.SECONDS.toMillis(intervalInSeconds));
        }
    }



    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public void setJolokiaUrl(String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
    }

    public String getGraphiteHostname() {
        return graphiteHostname;
    }

    public void setGraphiteHostname(String graphiteHostname) {
        this.graphiteHostname = graphiteHostname;
    }

    public int getGraphitePort() {
        return graphitePort;
    }

    public void setGraphitePort(int graphitePort) {
        this.graphitePort = graphitePort;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public void setIntervalInSeconds(int intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }

    public void setGraphiteConnectTimeout(int graphiteConnectTimeout) {
        this.graphiteConnectTimeout = graphiteConnectTimeout;
    }

    public void setGraphiteSocketTimeout(int graphiteSocketTimeout) {
        this.graphiteSocketTimeout = graphiteSocketTimeout;
    }

    public void setGraphiteWriteTimeoutMs(int graphiteWriteTimeoutMs) {
        this.graphiteWriteTimeoutMs = graphiteWriteTimeoutMs;
    }

    public MetricClientType getMetricClientType() {
        return metricClientType;
    }

    public void setMetricClientType(MetricClientType metricClientType) {
        this.metricClientType = metricClientType;
    }

    public int getGraphiteConnectTimeout() {
        return graphiteConnectTimeout;
    }

    public int getGraphiteSocketTimeout() {
        return graphiteSocketTimeout;
    }

    public int getGraphiteWriteTimeoutMs() {
        return graphiteWriteTimeoutMs;
    }
}
