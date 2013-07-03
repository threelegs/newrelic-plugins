package ar.com.threelegs.newrelic.cassandra;

import java.io.File;
import java.util.List;

import ar.com.threelegs.newrelic.cassandra.util.CassandraHelper;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author juanformoso
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Runner runner = new Runner();

		Config config = ConfigFactory.parseFile(new File("config/application.conf"));

		for (Config c : config.getConfigList("cassandra")) {
			runner.register(new CassandraRing(c));

			List<String> ringHosts = CassandraHelper.getRingHosts(c.getString("discovery_host"), c.getString("jmx_port"));
			for (final String host : ringHosts) {
				runner.register(new CassandraHost(host, c));
				// TODO: Temp test
				runner.register(new CassandraData(host, c));
			}
		}

		try {
			runner.setupAndRun();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.err.println("Error configuring");
			System.exit(-1);
		}
	}
}
