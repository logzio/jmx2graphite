package io.logz.jmx2graphite;


import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * It's not really a test yet
 * @author amesika
 */
public class TestJmx2Graphite {

    private static final Logger logger = LoggerFactory.getLogger(TestJmx2Graphite.class);


    @Ignore
    @Test
    public void sanity() throws InterruptedException {
        Map params = Maps.newHashMap();
        params.put("service.jolokiaUrl",  "http://172.31.63.154:11001/jolokia/");
        params.put("service.host",  "172_31_63_154");
        params.put("service.name", "Test");
        params.put("graphite.hostname",  "graphite-staging.internal.logz.io");
        params.put("graphite.port", 2004);
        params.put("intervalInSeconds", 10);

        Jmx2GraphiteConfiguration conf = new Jmx2GraphiteConfiguration(ConfigFactory.load(ConfigFactory.parseMap(params)));

        Jmx2Graphite jmx2Graphite = new Jmx2Graphite(conf);
        jmx2Graphite.run();
        Thread.sleep(1200000L);

    }

    @Ignore
    @Test
    public void sanityMain() throws InterruptedException {
        Jmx2Graphite.main(new String[]{});
        Thread.sleep(120000L);
    }
}
