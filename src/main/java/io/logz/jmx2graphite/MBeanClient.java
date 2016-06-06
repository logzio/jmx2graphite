package io.logz.jmx2graphite;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by roiravhon on 6/6/16.
 */
public abstract class MBeanClient {

    protected List<MetricBean> extractMetricsBeans(Map<String, Object> domains) {
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

    protected static Map<String, Number> flatten(Map<String, Object> attrValues) {
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

    public abstract  List<MetricBean> getBeans();
    public abstract List<MetricValue> getMetrics(List<MetricBean> beans);
}
