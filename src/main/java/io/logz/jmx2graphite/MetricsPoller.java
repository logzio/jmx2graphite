package io.logz.jmx2graphite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by roiravhon on 6/6/16.
 */
public abstract class MetricsPoller {

    private static final Logger logger = LoggerFactory.getLogger(MetricsPoller.class);

    private int pollingIntervalSeconds;

    public MetricsPoller(Jmx2GraphiteConfiguration conf) {
        pollingIntervalSeconds = conf.intervalInSeconds;
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

    abstract List<MetricValue> poll();
}
