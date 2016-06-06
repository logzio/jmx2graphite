package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.lang.instrument.Instrumentation;

/**
 * Created by roiravhon on 6/6/16.
 */
public class Jmx2GraphiteJavaAgent {

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        Config config = ConfigFactory.load();
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);
        MBeanClient javaAgentClient = new JavaAgentClient();

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration, javaAgentClient);
        main.run();
    }
}
