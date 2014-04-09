package ar.com.threelegs.newrelic.jmx;

import javax.management.MBeanServerConnection;

public interface JMXTemplate<T> {
    T execute(MBeanServerConnection connection) throws Exception;
}
