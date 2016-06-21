package io.logz.jmx2graphite;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

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
