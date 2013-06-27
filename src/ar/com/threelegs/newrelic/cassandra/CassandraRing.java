package ar.com.threelegs.newrelic.cassandra;

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
        try {
            System.out.println("getting ring hosts from discovery_host " + config.getString("discovery_host"));
            List<String> ringHosts = getRingHosts();

            List<Metric> allMetrics = new ArrayList<Metric>();

            System.out.println("getting metrics for hosts [" + ringHosts + "]...");

            for (final String host : ringHosts) {
                System.out.println("getting metrics for host [" + host + "]...");

                try {
                    allMetrics.addAll(JMXHelper.run(host, config.getString("jmx_port"), new JMXTemplate<List<Metric>>() {
                        @Override
                        public List<Metric> execute(MBeanServerConnection connection) throws Exception {

                            ArrayList<Metric> metrics = new ArrayList<Metric>();

                            Double rsl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest", "RangeSlice", "OneMinuteRate");
                            Double rl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest", "Read", "OneMinuteRate");
                            Double wl = JMXHelper.queryAndGetAttribute(connection, "org.apache.cassandra.metrics", "Latency", "ClientRequest", "Write", "OneMinuteRate");

                            Integer cpt = JMXHelper.queryAndGetAttribute(connection, JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=Compaction", "name=PendingTasks"), "Value");
                            Integer mpt = JMXHelper.queryAndGetAttribute(connection, JMXHelper.getObjectNameByKeys("org.apache.cassandra.metrics", "type=ThreadPools", "path=internal", "scope=MemtablePostFlusher", "name=PendingTasks"), "Value");
                            //org.apache.cassandra.metrics:type=,path=,scope=,name=

                            //System.out.println("test: " + JMXHelper.queryAndGetAttribute(connection, JMXHelper.getObjectNameByKeys("org.apache.cassandra.db", "type=ColumnFamilies", "keyspace=system", "columnfamily=peers"), "WriteCount"));

                            metrics.add(new Metric("Cassandra/hosts/" + host + "/Compaction/PendingTasks", "count", cpt));
                            metrics.add(new Metric("Cassandra/hosts/" + host + "/MemtableFlush/PendingTasks", "count", mpt));

                            metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Reads", "mu", rsl));
                            metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Reads", "mu", rl));
                            metrics.add(new Metric("Cassandra/hosts/" + host + "/Latency/Writes", "mu", wl));

                            metrics.add(new Metric("Cassandra/global/Latency/Reads", "mu", rsl));
                            metrics.add(new Metric("Cassandra/global/Latency/Reads", "mu", rl));
                            metrics.add(new Metric("Cassandra/global/Latency/Writes", "mu", wl));

                            return metrics;
                        }
                    }));
                } catch (Exception e) {
                    System.out.println("exception processing host: " + host);
                    e.printStackTrace();
                }

            }

            System.out.println("pushing " + allMetrics.size() + " metrics...");
            for (Metric m : allMetrics) {
                reportMetric(m.name, m.valueType, m.value);
            }
            System.out.println("pushing metrics: done!");
        } catch (Exception e) {
            throw new RuntimeException(e);
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
