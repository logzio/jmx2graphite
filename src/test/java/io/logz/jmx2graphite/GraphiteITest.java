package io.logz.jmx2graphite;

import static com.jayway.awaitility.Awaitility.await;
import static io.restassured.RestAssured.given;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

@SuppressWarnings({ "SpellCheckingInspection", "rawtypes" })
public class GraphiteITest {

    private final static Logger logger = LoggerFactory.getLogger(GraphiteITest.class);

    private static GenericContainer graphiteContainer;
    private static Stopwatch beforeClassStopwatch;
    public static final String TEST_SERVICE_HOST = "testhost";
    public static final String TEST_SERVICE_NAME = "testservice";

    @SuppressWarnings({"resource", "unchecked"})
    @BeforeClass
    public static void beforeClass() throws IOException {
        beforeClassStopwatch = Stopwatch.createStarted();
        File tempDir = createTempDir();

        graphiteContainer = new GenericContainer<>("sitespeedio/graphite:1.0.2-2").withExposedPorts(2003, 80)
                .withFileSystemBind(tempDir.getAbsolutePath(), "/var/log/carbon", BindMode.READ_WRITE)
                .withClasspathResourceMapping("carbon.conf", "/opt/graphite/conf/carbon.conf", BindMode.READ_ONLY)
                .withClasspathResourceMapping("blacklist.conf", "/opt/graphite/conf/blacklist.conf", BindMode.READ_ONLY)
                .withClasspathResourceMapping("storage-schemas.conf", "/opt/graphite/conf/storage-schemas.conf",
                        BindMode.READ_ONLY)
                .waitingFor(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(30)))
                .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("graphite"));

        graphiteContainer.setPortBindings(Lists.newArrayList("2003:2003", "1180:80"));
        graphiteContainer.start();
        beforeClassStopwatch.stop();
    }

    @AfterClass
    public static void afterClass() {
        if (graphiteContainer != null)
            graphiteContainer.stop();
        logger.info("Setup time took {} [sec]", beforeClassStopwatch.elapsed(TimeUnit.SECONDS));
    }

    @Test
    public void testResilienceToGraphiteRestart() throws IOException {
        try (GraphiteClient graphite = createGraphiteClient()) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(MINUTES);
            String metricName = "numOfLogz";
            float value = 100.0f;
            graphite.sendMetrics(metricsListOf(metricName, value, now.toEpochSecond()));
            assertThat(graphite.getFailedAtLastWrite()).isEqualTo(0);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fullMetricPath = String.format("%s.%s.%s", TEST_SERVICE_NAME, TEST_SERVICE_HOST, metricName);

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                String result = queryGraphiteAsCsv(fullMetricPath);
                assertThat(result).contains(fullMetricPath + "," + dateTimeFormatter.format(now) + "," + value);
            });

            graphiteContainer.stop();

            await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> assertThatThrownBy(
                            () -> graphite.sendMetrics(metricsListOf(metricName, value, now.toEpochSecond())))
                            .isInstanceOf(GraphiteClient.GraphiteWriteFailed.class)
                            .hasMessageContaining("Connection refused"));

            graphiteContainer.start();

            await().atMost(10, TimeUnit.SECONDS).ignoreExceptionsInstanceOf(ConnectException.class)
                    .until(() -> queryGraphiteAsCsv(fullMetricPath) != null);

            String metricName2 = "numOfLogz2";

            graphite.sendMetrics(metricsListOf(metricName2, value, now.toEpochSecond()));
            assertThat(graphite.getFailedAtLastWrite()).isEqualTo(0);

            String fullMetricPath2 = String.format("%s.%s.%s", TEST_SERVICE_NAME, TEST_SERVICE_HOST, metricName2);
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                String result2 = queryGraphiteAsCsv(fullMetricPath2);
                assertThat(result2).contains(fullMetricPath2 + "," + dateTimeFormatter.format(now) + "," + value);
            });
        }
    }

    private String queryGraphiteAsCsv(String fullMetricPath) {
        return given().port(1180).auth().basic("guest", "guest").param("target", fullMetricPath).param("format", "csv")
                .param("from", "now-2min").when().get("/render").asString();
    }

    private GraphiteClient createGraphiteClient() {
        int connectTimeout = (int) Duration.ofSeconds(5).toMillis();
        int socketTimeout = (int) Duration.ofSeconds(5).toMillis();
        int writeTimeoutMs = (int) Duration.ofSeconds(5).toMillis();

        return new GraphiteClient(TEST_SERVICE_HOST, TEST_SERVICE_NAME, "localhost", 2003, 
                connectTimeout, socketTimeout, writeTimeoutMs, GraphiteProtocol.TCP);
    }

    private static List<MetricValue> metricsListOf(String metricName, Number value, long epochSeconds) {
        List<MetricValue> metricValueList = new ArrayList<>();
        metricValueList.add(new MetricValue(metricName, value, epochSeconds));
        return metricValueList;
    }

	private static File createTempDir() throws IOException {
		final File tempFile = Files.createTempDirectory("mytest").toFile();
	    logger.info("Logging to {}", tempFile.getAbsolutePath());
	    return tempFile;
	}
}
