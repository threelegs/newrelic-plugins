# Cassandra plugin for New Relic

## Prerequisites
The Cassandra plugin for New Relic requires the following:

- A New Relic account. Signup for a free account at http://newrelic.com
- A cassandra cluster version 1.2.X+
- A configured Java Runtime (JRE) environment Version 1.6 or better
- Network access to New Relic (authenticated proxies are not currently supported, but see workaround below)

## Download
Download and unpack the New Relic plugin for Cassandra from Plugin Central: https://rpm.newrelic.com/plugins/ or the mirror in our
homepage: http://3legs.com.ar/downloads/newrelic/cassandra/

Linux example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    $ tar xfz cassandra_cassandra_plugin*.tar.gz
    
## Configuring your agent environment
The New Relic plugin for Cassandra runs an agent process to collect and report Cassandra metrics to New Relic. Configure your New Relic license and Cassandra information.

### Configure your New Relic license
Specify your license key in the necessary properties file.
Your license key can be found under Account Settings at https://rpm.newrelic.com see https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

    $ cp config/template_newrelic.properties config/newrelic.properties
    # Edit config/newrelic.properties and paste in your license key

### Configure your Cassandra properties

Linux example:

    $ cp config/template_application.conf config/application.conf
    # Edit config/application.conf

## Running the agent
To run the plugin in from the command line: 
`$ java -jar newrelic_cassandra_plugin*.jar`

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 
`$ java -Dhttps.proxyHost=proxyhost -Dhttps.proxyPort=8080 -jar newrelic_cassandra_plugin*.jar`

To run the plugin in from the command line and detach the process so it will run in the background:
`$ nohup java -jar newrelic_cassandra_plugin*.jar &`

*Note: we currently only support Cassandra version 1.2.X or above, we could add support for older versions if there is enough demand.*

*Another note: At present there are no [init.d](http://en.wikipedia.org/wiki/Init) scripts to start the New Relic Cassandra plugin at system startup.*

Keep in mind that the plugin connects to your cassandra nodes using RMI, so check if you need to edit *cassandra-env.sh* and alter the rmi hostname configuration (may not be needed, it depends on your network)  
Look for this section:  
    # jmx: metrics and administration interface
    #
    # add this if you're having trouble connecting:
    # JVM_OPTS="$JVM_OPTS -Djava.rmi.server.hostname=

## Keep this process running
You can use services like these to manage this process.

- [Upstart](http://upstart.ubuntu.com/)
- [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
- [Runit](http://smarden.org/runit/)
- [Monit](http://mmonit.com/monit/)  


## For support
Plugin support for troubleshooting assistance can be obtained by visiting [the 3legs homepage](http://3legs.com.ar) or the [plugin issues page](https://github.com/threelegs/newrelic-cassandra/issues)