package io.logz.jmx2graphite;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author amesika
 */
public class Jmx2Graphite {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2Graphite.class);

    private Jmx2GraphiteConfiguration conf;
    private Injector injector;
    private Scheduler scheduler;

    public static void main(String[] args) {
       Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);
        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration);
        main.run();
    }

    public Jmx2Graphite(Jmx2GraphiteConfiguration conf) {
        this.conf = conf;
    }

    public void run() {
        try {
            logger.info("Running with Jolokia URL: "+conf.jolokiaUrl);
            logger.info("Graphite: host = "+conf.graphiteHostname +", port = "+conf.graphitePort);

            injector = Guice.createInjector(new MyModule(conf));

            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.setJobFactory(injector.getInstance(GuiceJobFactory.class));
            submitPollerJob();
            enableHangupSupport();

            scheduler.start();
        } catch (SchedulerException e) {
            throw Throwables.propagate(e);
        }
    }

    private void submitPollerJob() throws SchedulerException {
        JobDetail job = newJob(MetricsPoller.class)
                .withIdentity("MetricsPoller", "Pollers")
                .build();

        // Trigger the job to run now, and then repeat every 40 seconds
        Trigger trigger = newTrigger()
                .withIdentity("ScheduledTrigger", "PollerTriggers")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(conf.intervalInSeconds)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    public void shutdown() {
        logger.info("Shutting down...");
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                logger.info("Failed closing Quartz scheduler. Error = " + e.getMessage(), e);
            }
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
