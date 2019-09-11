package io.logz.jmx2graphite;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author amesika
 */

public class MetricsPipeline {
    private static final Logger logger = LoggerFactory.getLogger(MetricsPipeline.class);

    private final Pattern beansWhiteListPattern;
    private final Pattern beansBlackListPattern;

    private int pollingIntervalSeconds;

    private GraphiteClient graphiteClient;
    private MBeanClient client;

    public MetricsPipeline(Jmx2GraphiteConfiguration conf, MBeanClient client) {

        this.graphiteClient = new GraphiteClient(conf.getServiceHost(), conf.getServiceName(), conf.getGraphiteHostname(),
                                                 conf.getGraphitePort(), conf.getGraphiteConnectTimeout(),
                                                 conf.getGraphiteSocketTimeout(), conf.getGraphiteWriteTimeoutMs(),
                                                 conf.getGraphiteProtocol());
        this.client = client;
        this.pollingIntervalSeconds = conf.getMetricsPollingIntervalInSeconds();
        this.beansWhiteListPattern = conf.getWhiteListPattern();
        this.beansBlackListPattern = conf.getBlackListPattern();

    }

    public List<MetricBean> getFilteredBeans(List<MetricBean> beans) {
        List<MetricBean> filteredBeans = beans.stream()
                .filter(bean -> beansWhiteListPattern.matcher(bean.getName()).find())
                .collect(Collectors.toList());

        filteredBeans.removeAll(beans.stream()
                .filter((bean -> beansBlackListPattern.matcher(bean.getName()).find()))
                .collect(Collectors.toList()));
        return filteredBeans;
    }

    private List<MetricValue> poll() {
        try {
            long pollingWindowStartSeconds = getPollingWindowStartSeconds();
            Stopwatch sw = Stopwatch.createStarted();
            List<MetricBean> beans = client.getBeans();

            logger.info("Found {} metric beans. Time = {}ms, for {}", beans.size(),
                    sw.stop().elapsed(TimeUnit.MILLISECONDS),
                    new Date(TimeUnit.SECONDS.toMillis(pollingWindowStartSeconds)));
            List<MetricBean> filteredBeans = getFilteredBeans(beans);
            logger.info("Filtered out {} metrics out of {} after white/blacklisting", beans.size() - filteredBeans.size(), beans.size());

            sw.reset().start();
            List<MetricValue> metrics = client.getMetrics(filteredBeans);
            logger.info("metrics fetched. Time: {} ms; Metrics: {}", sw.stop().elapsed(TimeUnit.MILLISECONDS), metrics.size());
            if (logger.isTraceEnabled()) printToFile(metrics);
            return changeTimeTo(pollingWindowStartSeconds, metrics);

        } catch (MBeanClient.MBeanClientPollingFailure e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed polling metrics from client ({}): {}", client.getClass().toString(), e.getMessage(), e);
            } else {
                logger.warn("Failed polling metrics from client ({}): {}", client.getClass().toString(), e.getMessage());
            }

            return Collections.emptyList();
        }
    }

    public void pollAndSend()  {

        try {
            List<MetricValue> metrics = poll();
            Stopwatch sw = Stopwatch.createStarted();
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
        } catch (Throwable t) {
            logger.error("Unexpected error occured while polling and sending. Error = {}", t.getMessage(), t);
            // not throwing out since the scheduler will stop in any exception
        }
    }

    private long getPollingWindowStartSeconds() {
        long now = System.currentTimeMillis();
        long pollingIntervalMs = TimeUnit.SECONDS.toMillis(pollingIntervalSeconds);
        return TimeUnit.MILLISECONDS.toSeconds(now - (now % pollingIntervalMs));
    }

    private void printToFile(List<MetricValue> metrics) {
        for (MetricValue v : metrics) {
            logger.trace(v.toString());
        }
    }

    private List<MetricValue> changeTimeTo(long newTime, List<MetricValue> metrics) {
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
