package ar.com.threelegs.newrelic;

import java.io.File;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.metrics.publish.util.Logger;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		Runner runner = new Runner();

		Config config = ConfigFactory.parseFile(new File("config/plugin.json"));

		if (config.hasPath("cassandra")) {
			LOGGER.info("instantiating the cassandra agent(s)...");
			for (Config c : config.getConfigList("cassandra")) {
				String pluginName = c.hasPath("plugin_name") ? c.getString("plugin_name") : Defaults.CASSANDRA_PLUGIN_NAME;
				String pluginVersion = c.hasPath("plugin_version") ? c.getString("plugin_version") : Defaults.VERSION;

				runner.register(new CassandraRing(c, pluginName, pluginVersion));
			}
			LOGGER.info("done!");
		}

		if (config.hasPath("varnish")) {
			LOGGER.info("instantiating the varnish agent(s)...");
			for (Config c : config.getConfigList("varnish")) {
				String pluginName = c.hasPath("plugin_name") ? c.getString("plugin_name") : Defaults.VARNISH_PLUGIN_NAME;
				String pluginVersion = c.hasPath("plugin_version") ? c.getString("plugin_version") : Defaults.VERSION;

				runner.register(new Varnish(c, pluginName, pluginVersion));
			}
			LOGGER.info("done!");
		}

		if (config.hasPath("jmxremote")) {
			LOGGER.info("instantiating the jmxremote agent(s)...");
			for (Config c : config.getConfigList("jmxremote")) {
				for (Config i : c.getConfigList("instances")) {
					String pluginName = i.hasPath("plugin_name") ? i.getString("plugin_name") : Defaults.JMXREMOTE_PLUGIN_NAME;
					String pluginVersion = i.hasPath("plugin_version") ? i.getString("plugin_version") : Defaults.VERSION;

					runner.register(new JMXRemote(i, pluginName, pluginVersion));
				}
			}
			LOGGER.info("done!");
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
