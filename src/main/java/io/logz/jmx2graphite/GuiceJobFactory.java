package io.logz.jmx2graphite;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import javax.inject.Inject;

/**
 * Created by timmattison on 8/4/14.
 */
// Some guidance from: http://codesmell.wordpress.com/2009/01/11/quartz-fits/
final class GuiceJobFactory implements JobFactory {
    private final Injector guice;

    @Inject
    public GuiceJobFactory(final Injector guice) {
        this.guice = guice;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        // Get the job detail so we can get the job class
        JobDetail jobDetail = triggerFiredBundle.getJobDetail();
        Class jobClass = jobDetail.getJobClass();

        try {
            // Get a new instance of that class from Guice so we can do dependency injection
            return (Job) guice.getInstance(jobClass);
        } catch (Exception e) {
            // Something went wrong.  Print out the stack trace here so SLF4J doesn't hide it.
            e.printStackTrace();

            // Rethrow the exception as an UnsupportedOperationException
            throw new UnsupportedOperationException(e);
        }
    }
}