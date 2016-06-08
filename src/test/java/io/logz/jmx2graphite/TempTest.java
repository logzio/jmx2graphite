package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by roiravhon on 6/6/16.
 */
public class TempTest {

    public static void main(String[] args) {

        Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);

        jmx2GraphiteConfiguration.graphiteHostname = "graphite.staging.us-east-1.internal.logz.io";
        jmx2GraphiteConfiguration.graphitePort = 2004;
        jmx2GraphiteConfiguration.jolokiaUrl = "http://172.31.0.35:11001";
        jmx2GraphiteConfiguration.serviceName = "RoiTestJavaAgent";
        jmx2GraphiteConfiguration.intervalInSeconds = 10;

        //MBeanClient jolokiaClient = new JolokiaClient(jmx2GraphiteConfiguration.jolokiaUrl);
        MBeanClient jolokiaClient = new JavaAgentClient();

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration, jolokiaClient);
        List<String> s = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            s.add("ejrwkejrwlekrj elkrjw elrkj welkrj wlekjr wlekjr wlkejr lwkejr w"+i);
        }
        s.clear();
        System.gc();
        System.gc();
        System.gc();
        main.run();


    }

}
