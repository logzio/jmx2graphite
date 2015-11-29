package io.logz.jmx2graphite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.http.client.fluent.Request.Get;
import static org.apache.http.client.fluent.Request.Post;

/**
 * @author amesika
 */
public class JolokiaClient {

    private static final Logger logger = LoggerFactory.getLogger(JolokiaClient.class);
    private String jolokiaFullURL;
    private int connectTimeout = (int) TimeUnit.SECONDS.toMillis(30);
    private int socketTimeout = (int) TimeUnit.SECONDS.toMillis(30);;

    private ObjectMapper objectMapper;
    private Stopwatch stopwatch = Stopwatch.createUnstarted();

    public JolokiaClient(String jolokiaFullURL) {
        this.jolokiaFullURL = jolokiaFullURL;
        if (!jolokiaFullURL.endsWith("/")) {
            this.jolokiaFullURL = jolokiaFullURL +"/";
        }
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<MetricBean> getBeans() throws JolokiaClientPollingFailure {
        try {
            stopwatch.reset().start();
            HttpResponse httpResponse = Get(new URI(jolokiaFullURL + "list"))
                    .connectTimeout(connectTimeout)
                    .socketTimeout(socketTimeout)
                    .execute().returnResponse();
            logger.debug("GET /list from jolokia took {} ms", stopwatch.stop().elapsed(TimeUnit.DAYS.MILLISECONDS));
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed listing beans from jolokia. Response = "+httpResponse.getStatusLine());
            }

            Map<String, Object> listResponse = objectMapper.readValue(httpResponse.getEntity().getContent(), Map.class);
            Map<String, Object> domains = (Map<String, Object>) listResponse.get("value");
            if (domains == null) {
                throw new RuntimeException("Response doesn't have value attribute expected from a list response");
            }
            return extractMetricsBeans(domains);
        } catch (URISyntaxException  | IOException e) {
            throw new JolokiaClientPollingFailure("Failed retrieving list of beans from Jolokia. Error = "+e.getMessage(), e);
        }
    }

    private List<MetricBean> extractMetricsBeans(Map<String, Object> domains) {
        List<MetricBean> result = Lists.newArrayList();
        for (String domainName : domains.keySet()) {
            Map<String, Object> domain = (Map<String, Object>) domains.get(domainName);
            for (String mbeanName : domain.keySet()) {
                Map<String, Object> mbean = (Map<String, Object>) domain.get(mbeanName);
                Map<String, Object> attributes = (Map<String, Object>) mbean.get("attr");
                if (attributes != null) {
                    List<String> attrNames = new ArrayList<String>(attributes.keySet());
                    result.add(new MetricBean(domainName + ":" + mbeanName, attrNames));
                }
            }
        }
        return result;
    }

    public List<MetricValue> getMetrics(List<MetricBean> beans) throws JolokiaClientPollingFailure {
        List<JolokiaReadRequest> readRequests = Lists.newArrayList();
        for (MetricBean bean : beans) {
            readRequests.add(new JolokiaReadRequest(bean.getName(), bean.getAttributes()));
        }

        try {
            String requestBody = objectMapper.writeValueAsString(readRequests);
            HttpResponse httpResponse = Post(jolokiaFullURL+"read?ignoreErrors=true")
                    .connectTimeout(connectTimeout)
                    .socketTimeout(socketTimeout)
                    .bodyString(requestBody, ContentType.APPLICATION_JSON)
                    .execute().returnResponse();

            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed reading beans from jolokia. Response = "+httpResponse.getStatusLine());
            }

            ArrayList<Map<String, Object>> responses = objectMapper.readValue(httpResponse.getEntity().getContent(), ArrayList.class);
            if (logger.isTraceEnabled()) logger.trace("Jolokia getBeans response:\n{}", objectMapper.writeValueAsString(responses));

            List<MetricValue> metricValues = Lists.newArrayList();
            for (Map<String, Object> response : responses) {
                Map<String, Object> request = (Map<String, Object>) response.get("request");
                String mbeanName = (String) request.get("mbean");
                int status = (int) response.get("status");
                if (status != 200) {
                    String errMsg = "Failed reading mbean '" + mbeanName +"': "+status+" - "+response.get("error");
                    if (logger.isDebugEnabled()) {
                        logger.debug(errMsg +". Stacktrace = {}", response.get("stacktrace"));
                    } else {
                        logger.warn(errMsg);
                    }
                    continue;
                }
                long metricTime = (long) ((Integer) response.get("timestamp"));

                Map<String, Object> attrValues = (Map<String, Object>) response.get("value");
                Map<String, Number> metricToValue = flatten(attrValues);
                for (String attrMetricName : metricToValue.keySet()) {
                    try {
                        metricValues.add(new MetricValue(
                                GraphiteClient.sanitizeMetricName(mbeanName, /*keepDot*/ true) + "." + attrMetricName,
                                metricToValue.get(attrMetricName),
                                metricTime));
                    } catch (IllegalArgumentException e) {
                        logger.info("Can't sent Metric since it's invalid: "+e.getMessage());
                    }
                }
            }
            return metricValues;
        } catch (IOException e) {
            throw new JolokiaClientPollingFailure("Failed reading beans from Jolokia. Error = "+e.getMessage(), e);
        }
    }

    private static Map<String, Number> flatten(Map<String, Object> attrValues) {
        Map<String, Number> metricValues = Maps.newHashMap();
        for (String key : attrValues.keySet()) {
            Object value = attrValues.get(key);
            if (value instanceof Map) {
                Map<String, Number> flattenValueTree = flatten((Map) value);

                for (String internalMetricName : flattenValueTree.keySet()) {
                    metricValues.put(
                            GraphiteClient.sanitizeMetricName(key, /*keepDot*/ false) + "."
                            + GraphiteClient.sanitizeMetricName(internalMetricName, /*keepDot*/ false),
                            flattenValueTree.get(internalMetricName));
                }
            } else {
                if (value instanceof Number) {
                    metricValues.put(GraphiteClient.sanitizeMetricName(key, /*keepDot*/ false), (Number) value);
                }
            }
        }
        return metricValues;
    }


    public static class JolokiaClientPollingFailure extends RuntimeException {

        public JolokiaClientPollingFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
