package io.logz.jmx2graphite;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author amesika
 */

public class MetricsPipeline {
    private static final Logger logger = LoggerFactory.getLogger(MetricsPipeline.class);

    private int pollingIntervalSeconds;

    private GraphiteClient graphiteClient;
    private MBeanClient client;

    public MetricsPipeline(Jmx2GraphiteConfiguration conf, MBeanClient client) {

        this.graphiteClient = new GraphiteClient(conf.serviceHost, conf.serviceName, conf.graphiteHostname,
                                                 conf.graphitePort, conf.getGraphiteConnectTimeout(),
                                                 conf.graphiteSocketTimeout, conf.getGraphiteWriteTimeoutMs());
        this.client = client;
        this.pollingIntervalSeconds = conf.intervalInSeconds;
    }

    private List<MetricValue> poll() {
        try {
            long pollingWindowStartSeconds = getPollingWindowStartSeconds();
            Stopwatch sw = Stopwatch.createStarted();
            List<MetricBean> beans = client.getBeans();
            logger.info("Found {} metric beans. Time = {}ms, for {}", beans.size(),
                    sw.stop().elapsed(TimeUnit.MILLISECONDS),
                    new Date(TimeUnit.SECONDS.toMillis(pollingWindowStartSeconds)));

            sw.reset().start();
            List<MetricValue> metrics = client.getMetrics(beans);
            logger.info("metrics fetched. Time: {} ms; Metrics: {}", sw.stop().elapsed(TimeUnit.MILLISECONDS), metrics.size());
            if (logger.isTraceEnabled()) printToFile(metrics);
            return changeTimeTo(pollingWindowStartSeconds, metrics);

        } catch (JolokiaClient.JolokiaClientPollingFailure e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed polling metrics from Jolokia: " + e.getMessage(), e);
            } else {
                logger.warn("Failed polling metrics from Jolokia: " + e.getMessage());
            }

            return null;
        }
    }

    public void pollAndSend()  {

        List<MetricValue> metrics = poll();

        try {
            Stopwatch sw = Stopwatch.createStarted();
            sw.reset().start();
            sendToGraphite(metrics);
            logger.info("metrics sent to Graphite. Time: {} ms, Failed metrics: {}",
                    sw.stop().elapsed(TimeUnit.MILLISECONDS),
                    graphiteClient.getFailedAtLastWrite());

        } catch (GraphiteClient.GraphiteWriteFailed e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed writing to Graphite: " + e.getMessage(), e);
            } else {
                logger.warn("Failed writing to Graphite: " + e.getMessage());
            }
        }
    }

    protected long getPollingWindowStartSeconds() {
        long now = System.currentTimeMillis();
        long pollingIntervalMs = TimeUnit.SECONDS.toMillis(pollingIntervalSeconds);
        return TimeUnit.MILLISECONDS.toSeconds(now - (now % pollingIntervalMs));
    }

    protected void printToFile(List<MetricValue> metrics) {
        for (MetricValue v : metrics) {
            logger.trace(v.toString());
        }
    }

    protected List<MetricValue> changeTimeTo(long newTime, List<MetricValue> metrics) {
        return metrics.stream()
                .map(m -> new MetricValue(m.getName(), m.getValue(), newTime))
                .collect(Collectors.toList());
    }

    private void sendToGraphite(List<MetricValue> metrics) {
        graphiteClient.sendMetrics(metrics);
    }

    //FIXME not called for now
    public void close() {
        try {
            graphiteClient.close();
        } catch (IOException e) {
            logger.info("Failed closing graphite client. Error = "+e.getMessage(), e);
        }
    }
}
