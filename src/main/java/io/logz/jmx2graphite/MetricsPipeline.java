package io.logz.jmx2graphite;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author amesika
 */

public class MetricsPipeline {
    private static final Logger logger = LoggerFactory.getLogger(MetricsPipeline.class);

    private GraphiteClient graphiteClient;
    private MetricsPoller poller;

    public MetricsPipeline(Jmx2GraphiteConfiguration conf, MetricsPoller poller) {

        this.poller = poller;
        this.graphiteClient = new GraphiteClient(conf.serviceHost, conf.serviceName, conf.graphiteHostname,
                                                 conf.graphitePort, conf.getGraphiteConnectTimeout(),
                                                 conf.graphiteSocketTimeout, conf.getGraphiteWriteTimeoutMs());
    }

    public void pollAndSend()  {

        List<MetricValue> metrics = poller.poll();

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
