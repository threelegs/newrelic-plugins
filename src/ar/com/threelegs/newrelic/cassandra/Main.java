package ar.com.threelegs.newrelic.cassandra;

import java.io.File;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/***
 * @author juanformoso
 */
public class Main {
	public static void main(String[] args) {
		Runner runner = new Runner();

		Config config = ConfigFactory.parseFile(new File("config/application.conf"));

		for (Config c : config.getConfigList("cassandra")) {
			runner.register(new CassandraRing(c));
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
