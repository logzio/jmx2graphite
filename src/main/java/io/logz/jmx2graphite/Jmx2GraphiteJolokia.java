package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by roiravhon on 6/6/16.
 */
public class Jmx2GraphiteJolokia {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2GraphiteJolokia.class);

    public static void main(String[] args) {

        Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config, Jmx2GraphiteConfiguration.MetricClientType.JOLOKIA);
        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration);

        logger.info("Initiated new jolokia based Jmx2Graphite instance");
        main.run();
    }
}
