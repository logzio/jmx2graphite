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

    /* Basic auth for Jolokia in Base64 */
    public String jolokiaUserPass;

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
        String jolokiaHost = null;
        try {
            URL jolokia = new URL(jolokiaUrl);
            jolokiaHost = jolokia.getHost();
        } catch (MalformedURLException e) {
            throw new IllegalConfiguration("service.jolokiaUrl must be a valid URL. Error = "+e.getMessage());
        }
        jolokiaUserPass = config.getString("service.jolokiaUserPass");

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
