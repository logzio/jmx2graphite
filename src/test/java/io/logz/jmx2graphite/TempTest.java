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
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config, Jmx2GraphiteConfiguration.MetricClientType.MBEAN_PLATFORM);

        jmx2GraphiteConfiguration.setGraphiteHostname("graphite.staging.us-east-1.internal.logz.io");
        jmx2GraphiteConfiguration.setGraphitePort(2004);
        jmx2GraphiteConfiguration.setJolokiaUrl("http://172.31.0.35:11001");
        jmx2GraphiteConfiguration.setServiceName("RoiTestJavaAgent");
        jmx2GraphiteConfiguration.setIntervalInSeconds(10);

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration);

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
