package io.logz.jmx2graphite;

import com.google.inject.AbstractModule;

/**
 * @author amesika
 */
public class MyModule extends AbstractModule {
    private Jmx2GraphiteConfiguration conf;

    public MyModule(Jmx2GraphiteConfiguration conf) {
        this.conf = conf;
    }

    @Override
    protected void configure() {
        bind(Jmx2GraphiteConfiguration.class).toInstance(conf);
        bind(MetricsPoller.class).toInstance(new MetricsPoller(conf));
    }
}