package io.logz.jmx2graphite;

import com.typesafe.config.Config;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * @author amesika
 */
public class Jmx2GraphiteConfiguration {

    private static final String ACCEPT_ALL_REGEX = ".*";
    private String jolokiaFullUrl;

    private Graphite graphite;
    private Pattern whiteListPattern;
    /* Short name of the sampled service, required = false */
    private String serviceName = null;

    /* host of the sampled service */
    private String serviceHost = null;

    /* Metrics polling interval in seconds */
    private int metricsPollingIntervalInSeconds;
    private int graphiteConnectTimeout;
    private int graphiteSocketTimeout;
    private int graphiteWriteTimeoutMs;

    // Which client should we use
    private MetricClientType metricClientType;
    private GraphiteProtocol graphiteProtocol;

    private String logLevel;

    public GraphiteProtocol getGraphiteProtocol() {
        return graphiteProtocol;
    }

    public Pattern getWhiteListPattern() {
        return whiteListPattern;
    }

    public enum MetricClientType {
        JOLOKIA,
        MBEAN_PLATFORM
    }

    private class Graphite {
        public String hostname;
        public int port;
    }

    public Jmx2GraphiteConfiguration(Config config) throws IllegalConfiguration {
        if (config.hasPath("service.host")) {
            serviceHost = config.getString("service.host");
        }

        if (config.hasPath("service.poller.jolokia")) {
            metricClientType = MetricClientType.JOLOKIA;
        }
        else if (config.hasPath("service.poller.mbean-direct")) {
            metricClientType = MetricClientType.MBEAN_PLATFORM;
        }

        if (this.metricClientType == MetricClientType.JOLOKIA) {
            jolokiaFullUrl = config.getString("service.poller.jolokia.jolokiaFullUrl");
            String jolokiaHost;
            try {
                URL jolokia = new URL(jolokiaFullUrl);
                jolokiaHost = jolokia.getHost();
            } catch (MalformedURLException e) {
                throw new IllegalConfiguration("service.jolokiaFullUrl must be a valid URL. Error = " + e.getMessage());
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

        graphite = new Graphite();
        graphite.hostname = config.getString("graphite.hostname");
        graphite.port = config.getInt("graphite.port");
        metricsPollingIntervalInSeconds = config.getInt("metricsPollingIntervalInSeconds");

        serviceName = config.getString("service.name");

        graphiteConnectTimeout = config.getInt("graphite.connectTimeout");
        graphiteSocketTimeout = config.getInt("graphite.socketTimeout");
        graphiteProtocol = getGraphiteProtocol(config);
        if (config.hasPath("graphite.writeTimeout")) {
            graphiteWriteTimeoutMs = config.getInt("graphite.writeTimeout");
        } else {
            graphiteWriteTimeoutMs = Math.round(0.7f * TimeUnit.SECONDS.toMillis(metricsPollingIntervalInSeconds));
        }
        setFilterPatterns(config);

        if(config.hasPath("log.level")) {
            logLevel = config.getString("log.level");
        } else {
            logLevel = Level.INFO.getName();
        }
    }

    private GraphiteProtocol getGraphiteProtocol(Config config) {
        String GRAPHITE_PROTOCOL = "graphite.protocol";
        String protocol = config.hasPath(GRAPHITE_PROTOCOL) ? config.getString(GRAPHITE_PROTOCOL) : null;
        if (protocol != null) {
            return GraphiteProtocol.valueOf(protocol.toUpperCase());
        }
        return null;
    }

    private void setFilterPatterns(Config config) {
        try {
            whiteListPattern = Pattern.compile(config.hasPath(Jmx2GraphiteJavaAgent.WHITE_LIST_REGEX) ?
                    config.getString(Jmx2GraphiteJavaAgent.WHITE_LIST_REGEX) : ACCEPT_ALL_REGEX);
        } catch (Exception e) {
            whiteListPattern = Pattern.compile(ACCEPT_ALL_REGEX);
        }

    }

    public String getJolokiaFullUrl() {
        return jolokiaFullUrl;
    }

    public void setJolokiaFullUrl(String jolokiaFullUrl) {
        this.jolokiaFullUrl = jolokiaFullUrl;
    }

    public String getGraphiteHostname() {
        return graphite.hostname;
    }

    public void setGraphiteHostname(String graphiteHostname) {
        this.graphite.hostname = graphiteHostname;
    }

    public int getGraphitePort() {
        return graphite.port;
    }

    public void setGraphitePort(int graphitePort) {
        this.graphite.port = graphitePort;
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

    public int getMetricsPollingIntervalInSeconds() {
        return metricsPollingIntervalInSeconds;
    }

    public void setMetricsPollingIntervalInSeconds(int metricsPollingIntervalInSeconds) {
        this.metricsPollingIntervalInSeconds = metricsPollingIntervalInSeconds;
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

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}
