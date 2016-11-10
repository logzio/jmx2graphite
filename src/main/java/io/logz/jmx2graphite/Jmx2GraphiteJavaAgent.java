package io.logz.jmx2graphite;

import com.google.common.base.Splitter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by roiravhon on 6/6/16.
 */
public class Jmx2GraphiteJavaAgent {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2GraphiteJavaAgent.class);

    public static void premain(String agentArgument, Instrumentation instrumentation) {

        logger.info("Loading with agentArgument: {}", agentArgument);

        Map<String, String> configurationMap = parseArgumentsString(agentArgument);

        if (configurationMap.get(getArgumentConfigurationRepresentation("GRAPHITE_HOSTNAME")) == null) {
            throw new IllegalConfiguration("GRAPHITE_HOSTNAME must be one of the arguments");
        }
        if (configurationMap.get(getArgumentConfigurationRepresentation("SERVICE_NAME")) == null) {
            throw new IllegalConfiguration("SERVICE_NAME must be one of the arguments");
        }

        Config userConfig = ConfigFactory.parseMap(configurationMap);
        Config fileConfig = ConfigFactory.load("javaagent.conf");

        // Merge the two configurations
        Config finalConfig = userConfig.withFallback(fileConfig);

        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(finalConfig);

        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration);
        logger.info("Initiated new java agent based Jmx2Graphite instance");

        try {
            main.run();

          // Catching anything, because if we throw exception here, it will stop the main thread as well.
        } catch (Throwable e) {
            logger.error("Stopping jmx2graphite Java Agent due to unexpected exception: "+e.getMessage(), e);
        }
    }

    private static Map<String, String> parseArgumentsString(String arguments) throws IllegalConfiguration {
        try {
            Map<String, String> argumentsMap = new HashMap<>();
            Map<String, String> keyValues = Splitter.on(';').omitEmptyStrings().withKeyValueSeparator('=').split(arguments);

            keyValues.forEach((k,v) -> argumentsMap.put(getArgumentConfigurationRepresentation(k),v));

            return argumentsMap;

        } catch (IllegalArgumentException e) {
            throw new IllegalConfiguration("Java agent arguments must be in form of: key=value;key=value");
        }
    }

    private static String getArgumentConfigurationRepresentation(String key) throws IllegalConfiguration {

        switch (key) {
            case "GRAPHITE_HOSTNAME":
                return "graphite.hostname";
            case "GRAPHITE_PORT":
                return "graphite.port";
            case "SERVICE_NAME":
                return "service.name";
            case "SERVICE_HOST":
                return "service.host";
            case "INTERVAL_IN_SEC":
                return "metricsPollingIntervalInSeconds";
            case "GRAPHITE_CONNECT_TIMEOUT":
                return "graphite.connectTimeout";
            case "GRAPHITE_SOCKET_TIMEOUT":
                return "graphite.socketTimeout";
            case "GRAPHITE_WRITE_TIMEOUT_MS":
                return "graphite.writeTimeout";
            case "GRAPHITE_PROTOCOL":
                return "graphite.protocol";
            default:
                throw new IllegalConfiguration("Unknown configuration option: " + key);
        }
    }
}
