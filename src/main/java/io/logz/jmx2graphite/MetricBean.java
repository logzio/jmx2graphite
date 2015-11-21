package io.logz.jmx2graphite;

import java.util.List;

/**
 * @author amesika
 */
public class MetricBean {
    String name;
    List<String> attributes;

    public MetricBean(String name, List<String> attrNames) {
        this.name = name;
        this.attributes = attrNames;
    }

    public String getName() {
        return name;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "MetricBean{" +
                "name='" + name + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
