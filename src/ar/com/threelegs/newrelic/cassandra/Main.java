package ar.com.threelegs.newrelic.cassandra;

import java.io.File;

import ar.com.threelegs.newrelic.cassandra.CassandraRing;
import ar.com.threelegs.newrelic.cassandra.Varnish;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author juanformoso
 * @author sschwartzman
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Runner runner = new Runner();

		Config config = ConfigFactory.parseFile(new File("config/application.conf"));
		Config activation = ConfigFactory.parseFile(new File("config/activation.conf"));

		if (activation.getBoolean("cassandra")) {
			for (Config c : config.getConfigList("cassandra")) {
				runner.register(new CassandraRing(c));
			}
		}

		if (activation.getBoolean("varnish")) {
			for (Config c : config.getConfigList("varnish")) {
				runner.register(new Varnish(c));
			}
		}
		
		if (activation.getBoolean("jmxremote")) {
			for (Config c : config.getConfigList("jmxremote")) {
				String pluginname = c.getString("pluginname");
				String pluginversion = c.getString("pluginversion");
				for (Config i : c.getConfigList("instances"))
					runner.register(new JMXRemote(i, pluginname, pluginversion));
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
