package ar.com.threelegs.newrelic.cassandra.jmx;

import javax.management.MBeanServerConnection;

/**
 * @author germanklf
 */
public interface JMXTemplate<T> {

    T execute(MBeanServerConnection connection) throws Exception;

}
