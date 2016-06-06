package io.logz.jmx2graphite;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Created by roiravhon on 6/6/16.
 */
public class JavaAgentClient extends MBeanClient {

    MBeanServer server;
    public JavaAgentClient() {

        server = ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public List<MetricBean> getBeans() {
        return null;
    }

    @Override
    public List<MetricValue> getMetrics(List<MetricBean> beans) {
        return null;
    }
}
