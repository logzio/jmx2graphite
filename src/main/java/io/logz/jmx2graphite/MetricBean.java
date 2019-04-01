package io.logz.jmx2graphite;

import java.util.List;

public class MetricBean {
    private String name;
    private List<String> attributes;

    public MetricBean() {}

    public MetricBean(String name, List<String> attr) {
        this.name = name;
        this.attributes = attr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
}
