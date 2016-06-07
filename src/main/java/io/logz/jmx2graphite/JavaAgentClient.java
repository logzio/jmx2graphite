package io.logz.jmx2graphite;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by roiravhon on 6/6/16.
 */
public class JavaAgentClient extends MBeanClient {

    private static final Logger logger = LoggerFactory.getLogger(JavaAgentClient.class);

    MBeanServer server;
    ObjectMapper objectMapper;

    public JavaAgentClient() {

        server = ManagementFactory.getPlatformMBeanServer();
        objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public List<MetricBean> getBeans() throws MBeanClientPollingFailure {

        try {
            List<MetricBean> metricBeans = Lists.newArrayList();
            Set<ObjectInstance> instances = server.queryMBeans(null, null);

            for (ObjectInstance instance : instances) {
                MBeanInfo mBeanInfo = server.getMBeanInfo(instance.getObjectName());
                List<String> attributes = Lists.newArrayList();

                for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
                    attributes.add(attribute.getName());
                }

                metricBeans.add(new MetricBean(instance.getObjectName().getCanonicalName(), attributes));
            }
            return metricBeans;

        } catch (IntrospectionException | ReflectionException | InstanceNotFoundException e) {
            throw new MBeanClientPollingFailure(e.getMessage(), e);
        }
    }

    @Override
    public List<MetricValue> getMetrics(List<MetricBean> beans) throws MBeanClientPollingFailure{

        List<MetricValue> metricValues = Lists.newArrayList();

        for (MetricBean metricBean : beans) {
            try {
                AttributeList attributeList = server.getAttributes(new ObjectName(metricBean.getName()),
                                                                   metricBean.getAttributes().toArray(new String[0]));

                Map<String, Object> attrValues = new HashMap<>(attributeList.size());
                attributeList.asList().forEach((attr) ->
                        attrValues.put(attr.getName(), attr.getValue()));

                Map<String, Number> metricToValue = flatten(attrValues);

                for (String attrMetricName : metricToValue.keySet()) {
                    try {

                        long metricTime = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toEpochSecond();

                        metricValues.add(new MetricValue(
                                GraphiteClient.sanitizeMetricName(metricBean.getName(), /*keepDot*/ true) + "." + attrMetricName,
                                metricToValue.get(attrMetricName),
                                metricTime));
                    } catch (IllegalArgumentException e) {
                        logger.info("Can't sent Metric since it's invalid: "+e.getMessage());
                    }
                }
            } catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException | IllegalArgumentException e ) {
                throw new MBeanClientPollingFailure(e.getMessage(), e);
            }
        }

        return metricValues;
    }

    private Map<String, Number> flatten(Map<String, Object> attrValues) {

        Map<String, Number> metricValues = Maps.newHashMap();
        for (String key : attrValues.keySet()) {
            Object value = attrValues.get(key);

            if (value instanceof Number) {
                metricValues.put(GraphiteClient.sanitizeMetricName(key, /*keepDot*/ false), (Number) value);
            }
            else if (!(value instanceof String) && !(value instanceof Boolean) && value != null) {
                Map<String, Object> valueMap;
                try {
                    valueMap = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    logger.trace("Can't convert attribute named {} with class type {}", key, value.getClass().getCanonicalName());
                    continue;
                }
                Map<String, Number> flattenValueTree = flatten(valueMap);

                for (String internalMetricName : flattenValueTree.keySet()) {
                    metricValues.put(
                            GraphiteClient.sanitizeMetricName(key, /*keepDot*/ false) + "."
                                    + GraphiteClient.sanitizeMetricName(internalMetricName, /*keepDot*/ false),
                            flattenValueTree.get(internalMetricName));
                }
            }
        }
        return metricValues;
    }
}
