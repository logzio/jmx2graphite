package io.logz.jmx2graphite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author amesika
 */
public class Jmx2Graphite {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2Graphite.class);

    private Jmx2GraphiteConfiguration conf;
    private ScheduledThreadPoolExecutor taskScheduler;
    private MBeanClient client;

    public Jmx2Graphite(Jmx2GraphiteConfiguration conf) {
        this.conf = conf;

        this.taskScheduler = new ScheduledThreadPoolExecutor(1);

        if (conf.getMetricClientType() == Jmx2GraphiteConfiguration.MetricClientType.JOLOKIA) {
            this.client = new JolokiaClient(conf.getJolokiaUrl());

        } else if (conf.getMetricClientType() == Jmx2GraphiteConfiguration.MetricClientType.MBEAN_PLATFORM) {
            this.client = new JavaAgentClient();
        }
    }

    public void run() {

        if (conf.getMetricClientType() == Jmx2GraphiteConfiguration.MetricClientType.JOLOKIA) {
            logger.info("Running with Jolokia URL: {}", conf.getJolokiaUrl());
        }
        else if (conf.getMetricClientType() == Jmx2GraphiteConfiguration.MetricClientType.MBEAN_PLATFORM) {
            logger.info("Running with Mbean client");
        }
        logger.info("Graphite: host = {}, port = {}", conf.getGraphiteHostname(), conf.getGraphitePort());

        enableHangupSupport();

        MetricsPipeline pipeline = new MetricsPipeline(conf, client);
        taskScheduler.scheduleWithFixedDelay(pipeline::pollAndSend, 0, conf.getIntervalInSeconds(), TimeUnit.SECONDS);
    }

    private void shutdown() {
        logger.info("Shutting down...");
        try {
            taskScheduler.shutdown();
            taskScheduler.awaitTermination(20, TimeUnit.SECONDS);
            taskScheduler.shutdownNow();
        } catch (InterruptedException e) {

            Thread.interrupted();
            taskScheduler.shutdownNow();
        }
    }

    /**
     * Enables the hangup support. Gracefully stops by calling shutdown() on a
     * Hangup signal.
     */
    private void enableHangupSupport() {
        HangupInterceptor interceptor = new HangupInterceptor(this);
        Runtime.getRuntime().addShutdownHook(interceptor);
    }

    /**
     * A class for intercepting the hang up signal and do a graceful shutdown of the Camel.
     */
    private static final class HangupInterceptor extends Thread {
        private Logger logger = LoggerFactory.getLogger(HangupInterceptor.class);
        private Jmx2Graphite main;

        public HangupInterceptor(Jmx2Graphite main) {
            this.main = main;
        }

        @Override
        public void run() {
            logger.info("Received hang up - stopping...");
            try {
                main.shutdown();
            } catch (Exception ex) {
                logger.warn("Error during stopping main", ex);
            }
        }
    }
}
