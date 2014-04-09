package ar.com.threelegs.newrelic;

public class Metric {
    public final String name;
    public final String valueType;
    public final Number value;

    public Metric(String name, String valueType, Number value) {
        this.name = name;
        this.valueType = valueType;
        this.value = value;
    }

}
