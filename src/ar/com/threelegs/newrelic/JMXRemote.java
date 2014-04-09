package ar.com.threelegs.newrelic;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import ar.com.threelegs.newrelic.jmx.ConnectionException;
import ar.com.threelegs.newrelic.jmx.JMXHelper;
import ar.com.threelegs.newrelic.jmx.JMXTemplate;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.util.Logger;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class JMXRemote extends Agent {
	private static final Logger LOGGER = Logger.getLogger(JMXRemote.class);
	private String name, host, port, metricPrefix;
	private List<? extends Config> JMXList;
	
	public JMXRemote(Config config) {
		this(config, Defaults.JMXREMOTE_PLUGIN_NAME, Defaults.VERSION);
	}
	
	public JMXRemote(Config config, String pluginName, String pluginVersion) {
		super(pluginName, pluginVersion);
		this.name = config.getString("name");
		this.host = config.getString("host");
		this.port = config.getString("port");
		this.JMXList = config.getConfigList("metrics");
		this.metricPrefix = "JMX/hosts/" + host +":" + port;
	}

	@Override
	public String getComponentHumanLabel() {
		return name;
	}

	@Override
	public void pollCycle() {
		LOGGER.debug("starting poll cycle");
		List<Metric> allMetrics = new ArrayList<Metric>();
		try {
			LOGGER.debug("Connecting to host [" + this.host + ":" + this.port + "]...");
				try {
					List<Metric> metrics = JMXHelper.run(this.host, this.port, new JMXTemplate<List<Metric>>() {
						@Override
						public List<Metric> execute(MBeanServerConnection connection) throws Exception {
							List<? extends Config> myList = JMXList;
							ArrayList<Metric> metrics = new ArrayList<Metric>();
							for (Config thisMetric : myList) {
								ObjectName thisObjectName = ObjectName.getInstance(thisMetric.getString("objectname"));
								String thisMetricType;
								try { thisMetricType = thisMetric.getString("type"); } catch (ConfigException e) { thisMetricType = "value"; }
								try { 
									// Using <Metric> with different assignments for the 2 strings in it.
									List<Metric> resultValues = JMXHelper.queryAndGetAttributes(connection, thisObjectName, thisMetric.getStringList("attributes"));
									for (Metric thisValue : resultValues) {
										String metricName = metricPrefix + "/" + thisValue.name.replaceAll(":", "/").replaceAll(",", "/") + "/" + thisValue.valueType;
										// Adding actual metric in proper form to metric list.
										metrics.add(new Metric(metricName, thisMetricType, thisValue.value));
									}
								} catch (Exception e) {
									LOGGER.error(e, "failed object: " + thisMetric.getString("objectname"));
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
					LOGGER.debug("exception processing host: " + host + ":" + port);
					e.printStackTrace();
				} finally {
					allMetrics.add(new Metric(metricPrefix + "/" + "status", "value", 1));
				}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			LOGGER.debug("pushing " + allMetrics.size() + " metrics...");
			int dropped = 0;
			for (Metric m : allMetrics) {
				if (m.value != null && !m.value.toString().equals("NaN"))
					reportMetric(m.name, m.valueType, m.value);
				else
					dropped++;
			}
			LOGGER.debug("pushing metrics: done! dropped metrics: " + dropped);
		}
	}
}
