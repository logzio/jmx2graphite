package io.logz.jmx2graphite;

import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amesika
 *
 */
public class TestGraphiteClient {

    private final static Logger logger = LoggerFactory.getLogger(TestGraphiteClient.class);

    private int port = new Random().nextInt(65000 - 10000) + 10000;
    private DummyGraphiteServer server;

    public void startMockGraphiteServer() {
        // Generate random port between [10000, 65000)
        server = new DummyGraphiteServer(port);
        logger.info("Starting dummy graphite server on port {}", port);
        server.start();

    }

    public void stopMockGraphiteServer() {
        logger.info("Shutting down mock graphite server");
        server.stop();
    }

    @Test(timeout = 60000)
    public void testOnServerShutdown() throws Exception {
        int connectTimeout = 1000;
        int socketTimeout = 1000;
        GraphiteClient client = new GraphiteClient("bla-host.com", "bla-service", "localhost",
                                                   port, connectTimeout, socketTimeout, 2000, null);

        ArrayList<MetricValue> dummyMetrics = Lists.newArrayList(new MetricValue("dice", 4, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));

        startMockGraphiteServer();
        client.sendMetrics(dummyMetrics);
        stopMockGraphiteServer();

        try {
            for (int i = 0; i < 1000; i++) {
                client.sendMetrics(dummyMetrics);
            }
        } catch (GraphiteClient.GraphiteWriteFailed e) {
            // Great
            return;
        }

        fail("Send metrics succeeded but server is down");
    }

    @Test
    public void testOnServerRestart() throws InterruptedException {
        int connectTimeout = 1000;
        int socketTimeout = 1000;
        GraphiteClient client = new GraphiteClient("bla-host.com", "bla-service", "localhost",
                                                   port, connectTimeout, socketTimeout, 20000,
                                                   null);

        ArrayList<MetricValue> dummyMetrics = Lists.newArrayList(new MetricValue("dice", 4, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));

        startMockGraphiteServer();

        client.sendMetrics(dummyMetrics);

        stopMockGraphiteServer();

        Thread.sleep(3000);

        startMockGraphiteServer();

        try {
            for (int i = 0; i < 1000; i++) {
                client.sendMetrics(dummyMetrics);
            }
        } catch (GraphiteClient.GraphiteWriteFailed e) {
            logger.error("failed: "+e.getMessage(), e);
            fail("Send metrics failed, this shouldn't happen");
        }

    }
}
