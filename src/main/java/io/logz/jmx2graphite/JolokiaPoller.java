package io.logz.jmx2graphite;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by roiravhon on 6/6/16.
 */
public class JolokiaPoller extends MetricsPoller {

    private static final Logger logger = LoggerFactory.getLogger(JolokiaPoller.class);

    JolokiaClient client;

    public JolokiaPoller(Jmx2GraphiteConfiguration conf) {

        super(conf);
        this.client = new JolokiaClient(conf.jolokiaUrl);
    }

    @Override
    public List<MetricValue> poll() {
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
}
