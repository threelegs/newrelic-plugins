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
		Config activation = ConfigFactory.parseFile(new File("config/activation.conf"));

		for (Config c : config.getConfigList("cassandra")) {
			if (activation.getBoolean("ring")) {
				runner.register(new CassandraRing(c));
			}

			if (activation.getBoolean("host") || activation.getBoolean("data")) {
				List<String> ringHosts = CassandraHelper.getRingHosts(c.getString("discovery_host"), c.getString("jmx_port"));
				for (final String host : ringHosts) {
					if (activation.getBoolean("host")) {
						runner.register(new CassandraHost(host, c));
					}
					// TODO: Temp test
					if (activation.getBoolean("data")) {
						runner.register(new CassandraData(host, c));
					}
				}
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
