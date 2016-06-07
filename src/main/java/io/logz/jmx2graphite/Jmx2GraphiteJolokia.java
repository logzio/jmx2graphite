package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Created by roiravhon on 6/6/16.
 */
public class Jmx2GraphiteJolokia {

    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);
        MBeanClient jolokiaClient = new JolokiaClient(jmx2GraphiteConfiguration.jolokiaUrl);

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration, jolokiaClient);
        main.run();
    }
}