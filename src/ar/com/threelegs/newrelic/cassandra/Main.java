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
   public static void main(String[] args) throws Exception {
      File configDirectory = new File("config");

      // check config file locations
      for (int i = 0; i < args.length; i++) {
         if ("-c".equalsIgnoreCase(args[i])
               && i + 1 < args.length) {
            configDirectory = new File(args[i + 1]);
         }
      }
      System.setProperty("newrelic.platform.config.dir", configDirectory.getAbsolutePath());
      System.out.println("Using config directory " + configDirectory.getAbsolutePath());

      Runner runner = new Runner();
      Config config = ConfigFactory.parseFile(new File(configDirectory, "/application.conf"));
      Config activation = ConfigFactory.parseFile(new File(configDirectory, "/activation.conf"));

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

      try {
         runner.setupAndRun();
      } catch (ConfigurationException e) {
         e.printStackTrace();
         System.err.println("Error configuring");
         System.exit(-1);
      }
   }
}
