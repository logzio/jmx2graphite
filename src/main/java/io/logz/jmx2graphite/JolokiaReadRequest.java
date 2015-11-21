package io.logz.jmx2graphite;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author amesika
 */
public class JolokiaReadRequest {

    @JsonProperty("type")
    private final String type = "read";

    @JsonProperty("mbean")
    private String mbeanName;

    @JsonProperty("attribute")
    private String[] attributes;

    public JolokiaReadRequest(String mbeanName, List<String> attributes) {
        this.mbeanName = mbeanName;
        this.attributes = attributes.toArray(new String[]{});
    }

    public String[] getAttributes() {
        return attributes;
    }

    public String getMbeanName() {
        return mbeanName;
    }

    public String getType() {
        return type;
    }
}
