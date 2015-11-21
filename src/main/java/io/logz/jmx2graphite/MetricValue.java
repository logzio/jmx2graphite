package io.logz.jmx2graphite;

/**
 * @author amesika
 */
public class MetricValue {
    private final String name;
    private final Number value;
    private final long timestampSeconds;

    public MetricValue(String name, Number value, long timestampSeconds) {
        final long max = 4294967295L;

        if (timestampSeconds < 0) {
            throw new IllegalArgumentException("timestampSeconds is negative: "+timestampSeconds+ " for metric named '"+name+"'");
        }
        // Verifies this in seconds
        if (timestampSeconds > max) {
            throw new IllegalArgumentException("timestampSeconds ("+timestampSeconds+") is above allowed max value: "+timestampSeconds+ " for metric named '"+name+"'");
        }
        this.name = name;
        this.value = value;
        this.timestampSeconds = timestampSeconds;
    }

    public String getName() {
        return name;
    }

    public Number getValue() {
        return value;
    }

    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    @Override
    public String toString() {
        return "MetricValue{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", timestampSeconds=" + timestampSeconds +
                '}';
    }
}
