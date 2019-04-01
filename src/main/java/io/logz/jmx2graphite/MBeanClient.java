package io.logz.jmx2graphite;

import java.util.List;

/**
 * Created by roiravhon on 6/6/16.
 */
public abstract class MBeanClient {

    public abstract  List<MetricBean> getBeans();
    public abstract List<MetricValue> getMetrics(List<MetricBean> beans);

    public static class MBeanClientPollingFailure extends RuntimeException {

        public MBeanClientPollingFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
