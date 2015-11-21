package io.logz.jmx2graphite;

import com.codahale.metrics.graphite.GraphiteSender;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GraphiteClient implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(GraphiteClient.class);
    private GraphiteSender graphite;
    private String metricsPrefix;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int failuresAtLastWrite = 0;

    public GraphiteClient(String serviceHost, String serviceName, String graphiteHostname, int graphitePort,
                          int connectTimeout, int socketTimeout, int writeTimeoutMs) {
        List<String> prefixElements = Lists.newArrayList();
        if (serviceName != null && !serviceName.isEmpty()) {
            prefixElements.add(sanitizeMetricName(serviceName));
        }
        if (serviceHost != null && !serviceHost.isEmpty()) {
            prefixElements.add(sanitizeMetricName(serviceHost));
        }
        if (!prefixElements.isEmpty()) {
            metricsPrefix = Joiner.on('.').join(prefixElements).concat(".");
        } else {
            metricsPrefix = "";
        }

        logger.info("Graphite metrics prefix: "+metricsPrefix);

        SocketFactory socketFactory = new SocketFactoryWithTimeouts(connectTimeout, socketTimeout);
        graphite = new PickledGraphite(new InetSocketAddress(graphiteHostname, graphitePort),
                socketFactory, /*batchSize */ 400, writeTimeoutMs);

        logger.info("Graphite Client: writeTimeoutMs = {} [ms]" ,writeTimeoutMs);
    }

    public static String sanitizeMetricName(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                sb.append('_');
            } else if (c == ':') {
                sb.append('.');
            } else if (c == ',') {
                sb.append('.');
            } else if (c == '.') {
                sb.append('_');
            } else if (c == '"') {
                // Removing it
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void sendMetrics(List<MetricValue> metrics) throws GraphiteWriteFailed {
        try {
            if (!graphite.isConnected()) {
                graphite.connect();
            }
        } catch (Exception e) {
            throw new GraphiteWriteFailed("Failed connecting to Graphite. Error = "+e.getMessage(), e);
        }

        int failuresBefore = graphite.getFailures();
        for (MetricValue mv : metrics) {
            try {
                graphite.send(metricsPrefix + mv.getName(), mv.getValue().toString(), mv.getTimestampSeconds());
            } catch (Exception e) {
                throw new GraphiteWriteFailed("Failed writing metric to graphite. Error = "+e.getMessage(), e);
            }
        }
        try {
            graphite.flush();
        } catch (Exception e) {
            logger.error("Failed writing metrics to graphite - flush: ", e);
            throw new GraphiteWriteFailed("Failed writing metrics to graphite (flush). Error = "+e.getMessage(), e);
        }
        failuresAtLastWrite = graphite.getFailures() - failuresBefore;
    }

    public int getFailedAtLastWrite() {
        return failuresAtLastWrite;
    }

    @Override
    public void close() throws IOException {
        graphite.close();
    }

    public static class GraphiteWriteFailed extends RuntimeException {
        public GraphiteWriteFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class SocketFactoryWithTimeouts extends SocketFactory {
        private SocketFactory socketFactory = SocketFactory.getDefault();

        // FIXME make it use this connect timeout (I can't find a way to do it
        private int connectTimeout;
        private int socketTimeout;

        public SocketFactoryWithTimeouts(int connectTimeout, int socketTimeout) {
            this.connectTimeout = connectTimeout;
            this.socketTimeout = socketTimeout;
        }

        public static SocketFactory getDefault() {
            return new SocketFactoryWithTimeouts(5, 10);
        }

        @Override
        public Socket createSocket() throws IOException {
            return configureTimeouts(socketFactory.createSocket());
        }

        @Override
        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
            return configureTimeouts(socketFactory.createSocket(s, i));
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
            return configureTimeouts(socketFactory.createSocket(s, i, inetAddress, i1));
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return configureTimeouts(socketFactory.createSocket(inetAddress, i));
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return configureTimeouts(socketFactory.createSocket(inetAddress, i, inetAddress1, i1));
        }

        private Socket configureTimeouts(Socket socket) throws SocketException {
            socket.setSoTimeout(socketTimeout);
            return socket;
        }
    }
}
