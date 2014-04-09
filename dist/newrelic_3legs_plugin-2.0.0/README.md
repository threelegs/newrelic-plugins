# Plugins for New Relic

## Prerequisites
All plugins for New Relic requires the following:
- A New Relic account. Signup for a free account at http://newrelic.com
- A configured Java Runtime (JRE) environment Version 1.6 or better
- Network access to New Relic (authenticated proxies are not currently supported, but see workaround below)

The Cassandra plugin for New Relic requires the following:

- A cassandra cluster version 1.2.X+

The Varnish plugin for New Relic requires the following:

- One or more varnish instances

The JMX Remote plugin for New Relic requires the following:
- One or more Java processes that expose JMX MBeans remotely
  - http://stackoverflow.com/questions/856881/how-to-activate-jmx-on-my-jvm-for-access-with-jconsole

## Download
Download and unpack the New Relic plugin you want from Plugin Central: https://rpm.newrelic.com/plugins/ or from https://github.com/threelegs/newrelic-plugins/tree/master/dist (click on the file name and select Raw from the gray menu bar)  

Linux example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    $ tar xfz newrelic_3legs_plugin-[version].tar.gz
    
## Configuring your agent environment
The New Relic plugin runs an agent process to collect and report metrics to New Relic. Configure your New Relic license and plugin information.

### Configure your New Relic license
Specify your license key in the appropriate file.
Your license key can be found under Account Settings at https://rpm.newrelic.com see https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

    $ cp config/newrelic.template.json config/newrelic.json
    # vim config/newrelic.json (and paste in your license key)

Additionally, in this file you can configure the logging properties as specified here: https://github.com/newrelic-platform/metrics_publish_java/tree/serened/beta_branch_v2

### Configure your Plugin properties
Linux example:

    $ cp config/plugin.template.json config/plugin.json
    # vim config/plugin.json

The unpacked plugin contains all supported plugins, all you need to activate them is configure their corresponding node in the configuration json you just renamed. If you don't want the Varnish plugin to run for example, simply remove it from your configuration.

All plugins support the `plugin_name` and `plugin_version` property, which you can use if you wish to create your own dashboards instead of using the plugin's default, just change the `plugin_name` parameter to anything you would like that is unique. The plugin will then report with the classname you define here.

#### To use the Remote JMX plugin, follow these guidelines when configuring plugin.json:
* The `host`, `port` and `name` (instance name to appear in New Relic UI) are required for each instance.
* Wildcards ARE permissable in an Object Name, for example: `java.lang:type=GarbageCollector,name=*`
* Multiple Attributes ARE permissable under an Object Name, for example: `["CollectionCount", "CollectionTime"]`
* If polling a single Attribute in an Object Name, you will still need to put it inside of '[' and ']', like so: `["CollectionCount"]`
* `type` is optional. If used, all of the attributes in that ObjectName definition will be typed with what you define here.
* If `type` is not used, the default "value" will be used for the attribute values in that Object Name.

## Running the agent
To run the plugin in from the command line: 
`$ java -jar plugin.jar`

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 
`$ java -Dhttps.proxyHost=proxyhost -Dhttps.proxyPort=8080 -jar plugin.jar`

To run the plugin in from the command line and detach the process so it will run in the background:
`$ nohup java -jar plugin.jar &`

*Note: we currently only support Cassandra version 1.2.X or above*

*Another note: You may use a [init.d](http://en.wikipedia.org/wiki/Init) script to start the New Relic plugin at system startup. Read more below.*

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

## Create a init.d file

The plugin comes with a init.d file to start and stop the plugin form the command line. The first thing you need to do to install the script is to copy it to **/etc/init.d/** and make it executable.

``` bash
cp /path/to/repo/resources/varnish-new-relic.init-file /etc/init.d/varnish-new-relic
chmod 744 /etc/init.d/varnish-new-relic
```
Look at the beginning of the **/etc/init.d/varnish-new-relic** file. There is three parameters that you may have to change. They are
* PLUGIN_PATH - This is the path to where you unzipped the plugin
* FILE_NAME - This is the name of the .jar
* USER - This is the user that will execute the .jar. Make sure he has permission to read and execute the PLUGIN_PATH and the FILE_NAME.

Now you need to tell the system that the file exists and that you want to use it.
``` bash
insserv /etc/init.d/varnish-new-relic
```
You are all set. The plugin will automatically start with you system. You may also run these commands as root:
``` bash
/etc/init.d/varnish-new-relic start
/etc/init.d/varnish-new-relic stop
/etc/init.d/varnish-new-relic status
/etc/init.d/varnish-new-relic restart
```

## For support
Plugin support for troubleshooting assistance can be obtained by visiting the official [New Relic Community Forum](https://discuss.newrelic.com/) or the [plugin issues page](https://github.com/threelegs/newrelic-plugins/issues).  
You can ping me directly in the forums by mentioning my username `@juanformoso` in any post, but any member of the community will be able to help you.
