package ar.com.threelegs.newrelic.cassandra;

import ar.com.threelegs.newrelic.cassandra.Metric;
import ar.com.threelegs.newrelic.cassandra.jmx.ConnectionException;
import ar.com.threelegs.newrelic.cassandra.jmx.JMXHelper;
import ar.com.threelegs.newrelic.cassandra.jmx.JMXTemplate;

import com.newrelic.metrics.publish.Agent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import java.util.ArrayList;
import java.util.List;
// import java.util.concurrent.TimeUnit;

/**
 * @author juanformoso
 * @author germanklf
 * @author sschwartzman
 */
public class JMXRemote extends Agent {

	private String name, host, port, metricPrefix;
	private List<? extends Config> JMXList;
	// private Config config;
	
	public JMXRemote(Config config, String pluginname, String pluginversion) {
		// this.pluginName = "ar.com.3legs.newrelic.jmxremote"
		super(pluginname, pluginversion);
		this.name = config.getString("name");
		this.host = config.getString("host");
		this.port = config.getString("port");
		this.JMXList = config.getConfigList("metrics");
		this.metricPrefix = "JMX/hosts/" + host +":" + port;
		// this.config = config;
	}

	@Override
	public String getComponentHumanLabel() {
		return name;
	}

	@Override
	public void pollCycle() {
		System.out.println("starting poll cycle");
		List<Metric> allMetrics = new ArrayList<Metric>();
		try {
			System.out.println("Connecting to host [" + this.host + ":" + this.port + "]...");
				try {
					List<Metric> metrics = JMXHelper.run(this.host, this.port, new JMXTemplate<List<Metric>>() {
						@Override
						public List<Metric> execute(MBeanServerConnection connection) throws Exception {
							List<? extends Config> myList = JMXList;
							ArrayList<Metric> metrics = new ArrayList<Metric>();
							for (Config thisMetric : myList) {
								ObjectName thisObjectName = ObjectName.getInstance(thisMetric.getString("objectname"));
								String thisMetricType;
								try {
									thisMetricType = thisMetric.getString("type");
								} catch (ConfigException e) {
									thisMetricType = "value";
								}
								
								for(String thisAttribute : thisMetric.getStringList("attributes")) {
									try {
										Object resultValue = JMXHelper.queryAndGetAttribute(connection, thisObjectName, thisAttribute);
										String metricName = metricPrefix + "/" + thisMetric.getString("objectname").replaceAll(":", "/").replaceAll(",", "/") + "/" + thisAttribute;
										metrics.add(new Metric(metricName, thisMetricType, (Number) resultValue));								
									} catch (Exception e) {
										System.out.println("exception processing metric: " + thisMetric);									
										e.printStackTrace();
										System.out.println("failed object: " + thisMetric.getString("objectname"));
										System.out.println("failed attribute: " + thisAttribute);
									}
								}
							}	
							return metrics;
						}
					});
					if (metrics != null)
						allMetrics.addAll(metrics);
				} catch (ConnectionException e) {
					allMetrics.add(new Metric(metricPrefix + "/" + "status", "value", 3));
					e.printStackTrace();
				} catch (Exception e) {
					System.out.println("exception processing host: " + host + ":" + port);
					e.printStackTrace();
				} finally {
					allMetrics.add(new Metric(metricPrefix + "/" + "status", "value", 1));
				}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("pushing " + allMetrics.size() + " metrics...");
			int dropped = 0;
			for (Metric m : allMetrics) {
				if (m.value != null && !m.value.toString().equals("NaN"))
					reportMetric(m.name, m.valueType, m.value);
				else
					dropped++;
			}
			System.out.println("pushing metrics: done! dropped metrics: " + dropped);
		}
	}
}
