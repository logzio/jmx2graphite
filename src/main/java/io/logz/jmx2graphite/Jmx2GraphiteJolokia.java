package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by roiravhon on 6/6/16.
 */
public class Jmx2GraphiteJolokia {

    private static final Logger logger = LoggerFactory.getLogger(Jmx2GraphiteJolokia.class);
    private static final int CONFIG_FILE_INDEX = 0;


    public static void main(String[] args) {

        Config config;
        if (args.length > 0) {
            String configFilePath = args[CONFIG_FILE_INDEX];
            if ((new File(configFilePath)).exists()) {
                logger.info("Loading from config file: {}", configFilePath);
                config = ConfigFactory.parseFile(new File(configFilePath)).resolve();
            } else {
                logger.error("config filename {} supplied but couldn't be found.", configFilePath);
                return;
            }
        } else {
            config = ConfigFactory.load();
        }
        Jmx2GraphiteConfiguration jmx2GraphiteConfiguration = new Jmx2GraphiteConfiguration(config);
        Jmx2Graphite main = new Jmx2Graphite(jmx2GraphiteConfiguration);

        logger.info("Starting jmx2graphite using Jolokia-based poller");
        main.run();
    }
}
