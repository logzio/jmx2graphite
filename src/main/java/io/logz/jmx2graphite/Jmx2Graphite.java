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

    public Jmx2Graphite(Jmx2GraphiteConfiguration conf, MBeanClient client) {
        this.conf = conf;
        this.taskScheduler = new ScheduledThreadPoolExecutor(1);
        this.client = client;
    }

    public void run() {

        logger.info("Running with Jolokia URL: "+conf.jolokiaUrl);
        logger.info("Graphite: host = "+conf.graphiteHostname +", port = "+conf.graphitePort);

        enableHangupSupport();

        MetricsPipeline pipeline = new MetricsPipeline(conf, client);
        taskScheduler.scheduleWithFixedDelay(pipeline::pollAndSend, 0, conf.intervalInSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
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
    public void enableHangupSupport() {
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
