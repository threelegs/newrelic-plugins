package ar.com.threelegs.newrelic.cassandra.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import ar.com.threelegs.newrelic.cassandra.jmx.JMXHelper;
import ar.com.threelegs.newrelic.cassandra.jmx.JMXTemplate;

/**
 * @author juanformoso
 */
public class CassandraHelper {

	@SuppressWarnings("rawtypes")
	public static List<String> getRingHosts(String discoveryHost, String jmxPort) throws Exception {

		return JMXHelper.run(discoveryHost, jmxPort, new JMXTemplate<List<String>>() {
			@Override
			public List<String> execute(MBeanServerConnection connection) throws Exception {
				List<String> ret = new ArrayList<String>();

				Map m = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.db", null, "DynamicEndpointSnitch", null, "Scores");

				if (m != null) {
					for (Object key : m.keySet()) {
                        String val = key.toString();

                        ret.add(val.substring(val.indexOf("/")+1, val.length()));
					}
				}

				return ret;
			}
		});

	}
}
