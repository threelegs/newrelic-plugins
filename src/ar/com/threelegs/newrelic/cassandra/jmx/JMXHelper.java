package ar.com.threelegs.newrelic.cassandra.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import ar.com.threelegs.newrelic.cassandra.Metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * @author germanklf
 */
public class JMXHelper {
	
	public static <T> T run(String host, String port, JMXTemplate<T> template) throws ConnectionException {
		JMXServiceURL address;
		JMXConnector connector = null;
		T value = null;

		try {
			address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
			try {
				connector = JMXConnectorFactory.connect(address);
			} catch (IOException ex) {
				throw new ConnectionException(host, ex);
			}
			MBeanServerConnection mbs = connector.getMBeanServerConnection();

			value = template.execute(mbs);
		} catch (ConnectionException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(connector);
		}

		return value;
	}

	public static Set<ObjectInstance> queryConnectionBy(MBeanServerConnection connection, ObjectName objectName) throws Exception {
		return connection.queryMBeans(objectName, null);
	}

	public static Set<ObjectInstance> queryConnectionBy(MBeanServerConnection connection, String domain, String name, String type, String scope)
			throws Exception {
		return queryConnectionBy(connection, getObjectName(domain, name, type, scope));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(MBeanServerConnection connection, ObjectName objectName, String attribute) throws Exception {
		return (T) connection.getAttribute(objectName, attribute);
	}

	public static <T> T queryAndGetAttribute(MBeanServerConnection connection, String domain, String name, String type, String scope, String attribute)
			throws Exception {
		return queryAndGetAttribute(connection, getObjectName(domain, name, type, scope), attribute);
	}

	public static <T> T queryAndGetAttribute(MBeanServerConnection connection, ObjectName objectName, String attribute) throws Exception {
		Set<ObjectInstance> instances = queryConnectionBy(connection, objectName);

		if (instances != null && instances.size() == 1) {
			return getAttribute(connection, objectName, attribute);
		} else {
			return null;
		}
	}
	
	public static List<Metric> queryAndGetAttributes(MBeanServerConnection connection, ObjectName objectName, List<String> attributes) throws Exception {
		List<Metric> returnList = new ArrayList<Metric>();
		Set<ObjectInstance> instances = queryConnectionBy(connection, objectName);
		if (instances != null && instances.size() > 0) {
			for (ObjectInstance thisInstance : instances) {
				for (String thisAttribute : attributes) {
					try {
						returnList.add(new Metric(thisInstance.getObjectName().toString(), thisAttribute, (Number)getAttribute(connection, thisInstance.getObjectName(), thisAttribute)));
					} catch (Exception e) {
						// Not a number, won't add metric, but won't crash the dang thing
						System.out.println("Exception processing query");								
						System.out.println("failed object: " + thisInstance.getObjectName().toString());
						System.out.println("failed attribute: " + thisAttribute);
						e.printStackTrace();
					}
				}
			}
		}
		if (!returnList.isEmpty()) {
			return returnList;
		} else {
			System.out.println("No results found.");
			return null;
		}
	}

	public static ObjectName getObjectName(String domain, String name, String type, String scope) throws Exception {
		Hashtable<String, String> map = new Hashtable<String, String>();
		if (name != null)
			map.put("name", name);
		if (type != null)
			map.put("type", type);
		if (scope != null)
			map.put("scope", scope);
		return ObjectName.getInstance(domain, map);
	}

	public static ObjectName getObjectNameByKeys(String domain, String... values) throws Exception {
		return ObjectName.getInstance(domain, hashtableOf(values));
	}

	public static Hashtable<String, String> hashtableOf(String... values) {
		Hashtable<String, String> hashtable = new Hashtable<String, String>();

		for (String s : values) {
			String[] v = s.split("=");
			hashtable.put(v[0], v[1]);
		}

		return hashtable;
	}

	private static void close(JMXConnector connector) {
		if (connector != null) {
			try {
				connector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
