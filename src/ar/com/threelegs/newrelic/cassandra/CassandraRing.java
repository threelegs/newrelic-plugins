package ar.com.threelegs.newrelic.cassandra;

import ar.com.threelegs.newrelic.cassandra.jmx.ConnectionException;
import ar.com.threelegs.newrelic.cassandra.jmx.JMXHelper;
import ar.com.threelegs.newrelic.cassandra.jmx.JMXTemplate;
import com.newrelic.metrics.publish.Agent;
import com.typesafe.config.Config;

import javax.management.MBeanServerConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author juanformoso
 * @author germanklf
 */
public class CassandraRing extends Agent {

	private String name = "Default";
	private Config config;

	public CassandraRing(Config config) {
		super("ar.com.3legs.newrelic.cassandra", "0.0.1");
		this.name = config.getString("name");
		this.config = config;
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
			System.out.println("getting ring hosts from discovery_host " + config.getString("discovery_host"));
			List<String> ringHosts = getRingHosts();

			System.out.println("getting metrics for hosts [" + ringHosts + "]...");

			allMetrics.add(new Metric("Cassandra/global/totalHosts", "count", ringHosts.size()));
			int downCount = 0;

			for (final String host : ringHosts) {
				System.out.println("getting metrics for host [" + host + "]...");

				try {
					List<Metric> metrics = JMXHelper.run(host, config.getString("jmx_port"), new JMXTemplate<List<Metric>>() {
						@Override
						public List<Metric> execute(MBeanServerConnection connection) throws Exception {

							ArrayList<Metric> metrics = new ArrayList<Metric>();

							// Latency
							Double rsl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest",
									"RangeSlice", "OneMinuteRate");
							Double rl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest",
									"Read", "OneMinuteRate");
							Double wl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest",
									"Write", "OneMinuteRate");

							metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Reads", "micros", rsl));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Reads", "micros", rl));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Writes", "micros", wl));
							metrics.add(new Metric("Cassandra/global/Latency/Reads", "micros", rsl));
							metrics.add(new Metric("Cassandra/global/Latency/Reads", "micros", rl));
							metrics.add(new Metric("Cassandra/global/Latency/Writes", "micros", wl));

							// System
							Integer cpt = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Compaction", "name=PendingTasks"), "Value");
							Long mpt = JMXHelper.queryAndGetAttribute(connection, JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics",
									"type=ThreadPools", "path=internal", "scope=MemtablePostFlusher", "name=PendingTasks"), "Value");

							metrics.add(new Metric("Cassandra/hosts/" + host + "/Compaction/PendingTasks", "count", cpt));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/MemtableFlush/PendingTasks", "count", mpt));

							// Cache
							Double kchr = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=KeyCache", "name=HitRate"),
									"Value");
							Long kcs = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=KeyCache", "name=Size"),
									"Value");
							Integer kce = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=KeyCache", "name=Entries"),
									"Value");
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/KeyCache/HitRate", "rate", kchr));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/KeyCache/Size", "bytes", kcs));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/KeyCache/Entries", "count", kce));
							metrics.add(new Metric("Cassandra/global/Cache/KeyCache/HitRate", "rate", kchr));
							metrics.add(new Metric("Cassandra/global/Cache/KeyCache/Size", "bytes", kcs));
							metrics.add(new Metric("Cassandra/global/Cache/KeyCache/Entries", "count", kce));

							Double rchr = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=RowCache", "name=HitRate"),
									"Value");
							Long rcs = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=RowCache", "name=Size"),
									"Value");
							Integer rce = JMXHelper.queryAndGetAttribute(connection,
									JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Cache", "scope=RowCache", "name=Entries"),
									"Value");
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/RowCache/HitRate", "rate", rchr));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/RowCache/Size", "bytes", rcs));
							metrics.add(new Metric("Cassandra/hosts/" + host + "/Cache/RowCache/Entries", "count", rce));
							metrics.add(new Metric("Cassandra/global/Cache/RowCache/HitRate", "rate", rchr));
							metrics.add(new Metric("Cassandra/global/Cache/RowCache/Size", "bytes", rcs));
							metrics.add(new Metric("Cassandra/global/Cache/RowCache/Entries", "count", rce));

							return metrics;
						}
					});

					if (metrics != null)
						allMetrics.addAll(metrics);
				} catch (ConnectionException e) {
					allMetrics.add(new Metric("Cassandra/downtime/hosts/" + e.getHost(), "value", 1));
					downCount++;
					allMetrics.add(new Metric("Cassandra/downtime/global", "count", downCount));
					e.printStackTrace();
				} catch (Exception e) {
					System.out.println("exception processing host: " + host);
					e.printStackTrace();
				}
			}

		} catch (ConnectionException e) {
			allMetrics.add(new Metric("Cassandra/downtime/hosts/" + e.getHost(), "value", 1));
			// TODO: change to correct value (qty of failed connections) when we make discoveryHosts a list.
			allMetrics.add(new Metric("Cassandra/downtime/global", "count", 1));
			e.printStackTrace();
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

	@SuppressWarnings("rawtypes")
	private List<String> getRingHosts() throws Exception {

		return JMXHelper.run(config.getString("discovery_host"), config.getString("jmx_port"), new JMXTemplate<List<String>>() {
			@Override
			public List<String> execute(MBeanServerConnection connection) throws Exception {
				List<String> ret = new ArrayList<String>();

				Map m = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.db", null, "DynamicEndpointSnitch", null, "Scores");

				if (m != null) {
					for (Object key : m.keySet()) {
						ret.add(key.toString().replaceFirst("/", ""));
					}
				}

				return ret;
			}
		});

	}

}
