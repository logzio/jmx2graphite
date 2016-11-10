package io.logz.jmx2graphite;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestMe {

    private static final Logger logger = LoggerFactory.getLogger(TestMe.class);


    @Test
    public void t() {
        Map<String, Object> p = new HashMap<>();
        p.put("foo2", "yep!");
        Config source = ConfigFactory.parseMap(p);

        Config fileConfig = ConfigFactory.parseString("name: ${?foo}")
                .resolveWith(source)
        ;
        System.out.print("hello "+ fileConfig.getString("name"));
    }
}
