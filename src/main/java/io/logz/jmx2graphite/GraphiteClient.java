package io.logz.jmx2graphite;

import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.logz.jmx2graphite.GraphiteProtocol.TCP;
import static io.logz.jmx2graphite.GraphiteProtocol.UDP;


public class GraphiteClient implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(GraphiteClient.class);
    private static final String ENCODING = StandardCharsets.UTF_8.name();
    private GraphiteSender graphite;
    private String metricsPrefix;
    private int failuresAtLastWrite = 0;

    public GraphiteClient(String serviceHost, String serviceName, String graphiteHostname, int graphitePort,
            boolean ssl, int connectTimeout, int socketTimeout, int writeTimeoutMs, GraphiteProtocol protocol) {
        List<String> prefixElements = Lists.newArrayList();
        if (serviceName != null && !serviceName.isEmpty()) {
            prefixElements.add(sanitizeMetricName(serviceName));
        }
        if (serviceHost != null && !serviceHost.isEmpty()) {
            prefixElements.add(sanitizeMetricName(serviceHost, /* keepDot */ false));
        }
        if (!prefixElements.isEmpty()) {
            metricsPrefix = Joiner.on('.').join(prefixElements).concat(".");
        } else {
            metricsPrefix = "";
        }

        logger.info("Graphite metrics prefix: {}", metricsPrefix);
        logger.info("Graphite Client: using writeTimeoutMs of {} [ms]. Establishing connection...", writeTimeoutMs);

        SocketFactory socketFactory = new SocketFactoryWithTimeouts(connectTimeout, socketTimeout, ssl);
        if (protocol == UDP) {
            graphite = new GraphiteUDP(new InetSocketAddress(graphiteHostname, graphitePort));
        } else if (protocol == TCP) {
            graphite = new Graphite(new InetSocketAddress(graphiteHostname, graphitePort), socketFactory);
        } else {
            graphite = new PickledGraphite(new InetSocketAddress(graphiteHostname, graphitePort), socketFactory,
                    /* batchSize */ 400, writeTimeoutMs);
        }
    }

    public static String sanitizeMetricName(String s) {
        return sanitizeMetricName(s, /* keepDot */ true);
    }

    public static String sanitizeMetricName(String s, boolean keepDot) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                sb.append('_');
            } else if (c == ':') {
                sb.append('.');
            } else if (c == ',') {
                sb.append('.');
            } else if (c == '.' && !keepDot) {
                sb.append('_');
            } else if (c == '"') {
                // Removing it
            } else if (c == ' ') {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }

        try {
            return URLEncoder.encode(sb.toString(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported encoding {} for metric name {}.", ENCODING, s);
            return sb.toString();
        }
    }

    /**
     * send a list of metrics to graphite
     * 
     * @param metrics
     * @throws GraphiteWriteFailed
     */
    public void sendMetrics(List<MetricValue> metrics) throws GraphiteWriteFailed {
        RetryPolicy<Object> policy = new RetryPolicy<Object>().handleIf(GraphiteClient::isBrokenPipeException)
                .onFailedAttempt(e -> {
                    logger.info("Broken pipe error detected in connection to Graphite. Closing connection to allow retry. Error = {}",e.getLastFailure().toString());
                    graphite.close();
                });
        Failsafe.with(policy).run(() -> sendMetricsInternal(metrics));
    }

    private void sendMetricsInternal(List<MetricValue> metrics) throws GraphiteWriteFailed {

        try {
            if (!graphite.isConnected()) {
                graphite.connect();
            }
        } catch (Exception e) {
            throw new GraphiteWriteFailed(
                    "Failed connecting to Graphite. Error = " + e.getClass() + ": " + e.getMessage(), e);
        }

        int failuresBefore = graphite.getFailures();
        for (MetricValue mv : metrics) {
            try {
                graphite.send(metricsPrefix + mv.getName(), mv.getValue().toString(), mv.getTimestampSeconds());
            } catch (Exception e) {
                throw new GraphiteWriteFailed("Failed writing metric to graphite. Error = " + e.getMessage(), e);
            }
        }
        try {
            graphite.flush();
        } catch (Exception e) {
            throw new GraphiteWriteFailed("Failed writing metrics to graphite (flush). Error = " + e.getMessage(), e);
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
        private final SocketFactory socketFactory;

        // FIXME make it use this connect timeout (I can't find a way to do it
        private int connectTimeout;
        private int socketTimeout;

        public SocketFactoryWithTimeouts(int connectTimeout, int socketTimeout, boolean ssl) {
            this.connectTimeout = connectTimeout;
            this.socketTimeout = socketTimeout;
            this.socketFactory = ssl ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
        }

        public static SocketFactory getDefault() {
            return new SocketFactoryWithTimeouts(5, 10, false);
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
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1)
                throws IOException, UnknownHostException {
            return configureTimeouts(socketFactory.createSocket(s, i, inetAddress, i1));
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return configureTimeouts(socketFactory.createSocket(inetAddress, i));
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1)
                throws IOException {
            return configureTimeouts(socketFactory.createSocket(inetAddress, i, inetAddress1, i1));
        }

        private Socket configureTimeouts(Socket socket) throws SocketException {
            socket.setSoTimeout(socketTimeout);
            return socket;
        }
    }

    private static boolean isBrokenPipeException(Throwable t) {
        if (t instanceof GraphiteWriteFailed) {
            Throwable cause = ExceptionUtils.getCause(t);
            String message = cause == null ? t.getMessage() : cause.getMessage();
            return StringUtils.containsIgnoreCase(message, "broken pipe");
        }
        return false;
    }
}
