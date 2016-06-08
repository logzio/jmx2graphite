package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

/**
 * Created by roiravhon on 6/6/16.
 */
public class TempTest {

    @Test
    public void tempTest() {

        Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);

        jmx2GraphiteConfiguration.graphiteHostname = "graphite.staging.us-east-1.internal.logz.io";
        jmx2GraphiteConfiguration.graphitePort = 2004;
        jmx2GraphiteConfiguration.jolokiaUrl = "http://172.31.0.35:11001";
        jmx2GraphiteConfiguration.serviceName = "RoiTestJavaAgent";

        //MBeanClient jolokiaClient = new JolokiaClient(jmx2GraphiteConfiguration.jolokiaUrl);
        MBeanClient jolokiaClient = new JavaAgentClient();

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration, jolokiaClient);
        main.run();
    }
}
