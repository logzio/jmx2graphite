package io.logz.jmx2graphite;

import com.typesafe.config.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author amesika
 */
public class Jmx2GraphiteConfiguration {
    /* Jolokia full URL */
    public String jolokiaUrl;

    public String graphiteHostname;

    public int graphitePort;

    /* Short name of the sampled service (exposed by the Jolokia URL)", required = false */
    public String serviceName = null;

    /* host of the sampled service (exposed by the Jolokia URL) */
    public String serviceHost = null;

    /* Metrics polling interval in seconds */
    public int intervalInSeconds;
    private int graphiteConnectTimeout;
    public int graphiteSocketTimeout;
    private int graphiteWriteTimeoutMs;

    public Jmx2GraphiteConfiguration(Config config) throws IllegalConfiguration {
        jolokiaUrl = config.getString("service.jolokiaUrl");
        String jolokiaHost;
        try {
            URL jolokia = new URL(jolokiaUrl);
            jolokiaHost = jolokia.getHost();
        } catch (MalformedURLException e) {
            throw new IllegalConfiguration("service.jolokiaUrl must be a valid URL. Error = "+e.getMessage());
        }

        graphiteHostname = config.getString("graphite.hostname");
        graphitePort = config.getInt("graphite.port");
        intervalInSeconds = config.getInt("intervalInSeconds");

        if (config.hasPath("service.host")) {
            serviceHost = config.getString("service.host");
        } else {
            serviceHost = jolokiaHost;
        }
        serviceName = config.getString("service.name");

        graphiteConnectTimeout = config.getInt("graphite.connectTimeout");
        graphiteSocketTimeout = config.getInt("graphite.socketTimeout");
        if (config.hasPath("graphite.writeTimeout")) {
            graphiteWriteTimeoutMs = config.getInt("graphite.writeTimeout");
        } else {
            graphiteWriteTimeoutMs = Math.round(0.7f * TimeUnit.SECONDS.toMillis(intervalInSeconds));
        }
    }

    public Jmx2GraphiteConfiguration(String jolokiaUrl, String graphiteHostname, int graphitePort, String serviceName, String serviceHost, int intervalInSeconds, int graphiteConnectTimeout, int graphiteSocketTimeout, int graphiteWriteTimeoutMs) {
        this.jolokiaUrl = jolokiaUrl;
        this.graphiteHostname = graphiteHostname;
        this.graphitePort = graphitePort;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.intervalInSeconds = intervalInSeconds;
        this.graphiteConnectTimeout = graphiteConnectTimeout;
        this.graphiteSocketTimeout = graphiteSocketTimeout;
        this.graphiteWriteTimeoutMs = graphiteWriteTimeoutMs;
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
