package io.logz.jmx2graphite;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static io.logz.jmx2graphite.Jmx2GraphiteConfiguration.MetricClientType.JOLOKIA;
import static io.logz.jmx2graphite.Jmx2GraphiteConfiguration.MetricClientType.MBEAN_PLATFORM;

/**
 * @author amesika
 */
public class Jmx2Graphite {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2Graphite.class);

    private final Jmx2GraphiteConfiguration conf;
    private final ScheduledThreadPoolExecutor taskScheduler;
    private final MBeanClient client;

    public Jmx2Graphite(Jmx2GraphiteConfiguration conf) {
        this.conf = conf;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Jmx2GraphiteSender-%d")
                .build();
        this.taskScheduler = new ScheduledThreadPoolExecutor(1, threadFactory);

        if (conf.getMetricClientType() == JOLOKIA) {
            this.client = new JolokiaClient(conf.getJolokiaFullUrl());
            logger.info("Running with Jolokia URL: {}", conf.getJolokiaFullUrl());

        } else if (conf.getMetricClientType() == MBEAN_PLATFORM) {
            this.client = new JavaAgentClient();
            logger.info("Running with Mbean client");
        }
        else {
            throw new IllegalConfiguration("Unsupported client type: " + conf.getMetricClientType());
        }

        Configurator.setRootLevel(Level.valueOf(conf.getLogLevel()));
    }

    public void run() {
        logger.info("Graphite: host = {}, port = {}", conf.getGraphiteHostname(), conf.getGraphitePort());
        enableHangupSupport();
        MetricsPipeline pipeline = new MetricsPipeline(conf, client);
        taskScheduler.scheduleWithFixedDelay(pipeline::pollAndSend, 0, conf.getMetricsPollingIntervalInSeconds(), TimeUnit.SECONDS);
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
