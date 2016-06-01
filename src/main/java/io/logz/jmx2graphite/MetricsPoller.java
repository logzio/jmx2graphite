package io.logz.jmx2graphite;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
@DisallowConcurrentExecution
public class MetricsPoller implements Job {
    private static final Logger logger = LoggerFactory.getLogger(MetricsPoller.class);

    private String jolokiaUrl;
    public String jolokiaUserPass;
    private String graphiteHostname;
    private int graphitePort;
    private String serviceName;
    private String serviceHost;
    private int pollingIntervalSeconds;

    private JolokiaClient client;
    private GraphiteClient graphiteClient;

    @Inject
    public MetricsPoller(Jmx2GraphiteConfiguration conf) {
        this(conf.jolokiaUrl, conf.jolokiaUserPass,
                conf.graphiteHostname, conf.graphitePort,
                conf.serviceName, conf.serviceHost,
                conf.getGraphiteConnectTimeout(),
                conf.graphiteSocketTimeout,
                conf.intervalInSeconds,
                conf.getGraphiteWriteTimeoutMs());
    }

    public MetricsPoller(String jolokiaUrl, String jolokiaUserPass,
                         String graphiteHostname, int graphitePort, String serviceName,
                         String serviceHost, int graphiteConnectTimeout, int graphiteSocketTimeout,
                         int pollingIntervalSeconds, int graphiteWriteTimeoutMs) {
        this.jolokiaUrl = jolokiaUrl;
        this.jolokiaUserPass = jolokiaUserPass;
        this.graphiteHostname = graphiteHostname;
        this.graphitePort = graphitePort;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
        client = createJolokiaClient();
        graphiteClient = new GraphiteClient(serviceHost, serviceName, graphiteHostname, graphitePort,
                graphiteConnectTimeout, graphiteSocketTimeout, graphiteWriteTimeoutMs);
    }

    public void poll()  {
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
            metrics = changeTimeTo(pollingWindowStartSeconds, metrics);

            sw.reset().start();
            sendToGraphite(metrics);
            logger.info("metrics sent to Graphite. Time: {} ms, Failed metrics: {}",
                    sw.stop().elapsed(TimeUnit.MILLISECONDS),
                    graphiteClient.getFailedAtLastWrite());
        } catch (GraphiteClient.GraphiteWriteFailed e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed writing to Graphite: "+e.getMessage(), e);
            } else {
                logger.warn("Failed writing to Graphite: "+e.getMessage());
            }
        } catch (JolokiaClient.JolokiaClientPollingFailure e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed polling metrics from Jolokia: " + e.getMessage(), e);
            } else {
                logger.warn("Failed polling metrics from Jolokia: " + e.getMessage());
            }
        }
    }

    private long getPollingWindowStartSeconds() {
        long now = System.currentTimeMillis();
        long pollingIntervalMs = TimeUnit.SECONDS.toMillis(pollingIntervalSeconds);
        return TimeUnit.MILLISECONDS.toSeconds(now - (now % pollingIntervalMs));
    }

    private List<MetricValue> changeTimeTo(long newTime, List<MetricValue> metrics) {
        return metrics.stream()
                .map(m -> new MetricValue(m.getName(), m.getValue(), newTime))
                .collect(Collectors.toList());
    }

    private void printToFile(List<MetricValue> metrics) {
        for (MetricValue v : metrics) {
            logger.trace(v.toString());
        }
    }

    private void sendToGraphite(List<MetricValue> metrics) {
        graphiteClient.sendMetrics(metrics);
    }

    private JolokiaClient createJolokiaClient() {
        return new JolokiaClient(jolokiaUrl, jolokiaUserPass);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        poll();
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
