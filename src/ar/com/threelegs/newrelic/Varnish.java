package ar.com.threelegs.newrelic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.processors.EpochCounter;
import com.newrelic.metrics.publish.util.Logger;
import com.typesafe.config.Config;

public class Varnish extends Agent {

	private static final Logger LOGGER = Logger.getLogger(Varnish.class);
	private String name;
	private Config config;
	private Map<Pattern, String> units = new HashMap<Pattern, String>();
	private Map<Pattern, String> groups = new HashMap<Pattern, String>();

	public Varnish(Config config) {
		this(config, Defaults.VARNISH_PLUGIN_NAME, Defaults.VERSION);
	}

	public Varnish(Config config, String pluginName, String pluginVersion) {
		super(pluginName, pluginVersion);
		this.name = config.getString("name");
		this.config = config;

		units.put(Pattern.compile("client_.*"), "connections");
		units.put(Pattern.compile("backend_.*"), "connections");
		units.put(Pattern.compile("cache_.*"), "requests");
		units.put(Pattern.compile("fetch_.*"), "fetchs");
		units.put(Pattern.compile("n_wrk.*"), "threads");
		units.put(Pattern.compile("n_object"), "objects");

		groups.put(Pattern.compile("n_wrk.*"), "threads");
	}

	@Override
	public String getComponentHumanLabel() {
		return name;
	}

	@Override
	public void pollCycle() {
		List<Metric> allMetrics = new ArrayList<Metric>();
		try {
			String command = "";

			if (config.hasPath("user")) {
				command += "ssh -t " + config.getString("user") + "@" + config.getString("host");
			}
			
			if (config.hasPath("port")) {
				command += " -p " + config.getString("port");
			}

			command += " varnishstat -1 -x";

			if (config.hasPath("instance")) {
				command += " -n " + config.getString("instance");
			}

			Process p = Runtime.getRuntime().exec(command);

			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String aux = "";

			while ((aux = in.readLine()) != null) {
				builder.append(aux);
			}

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new InputSource(new ByteArrayInputStream(builder.toString().replace("\t", "").getBytes("utf-8"))));

			NodeList nlist = dom.getElementsByTagName("stat");

			for (int i = 0; i < nlist.getLength(); i++) {
				allMetrics.add(getMetric((Element) nlist.item(i)));
			}

			p.waitFor();

		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			LOGGER.debug("pushing " + allMetrics.size() + " metrics...");
			for (Metric m : allMetrics) {
				LOGGER.debug(m.name + "[" + m.valueType + "] --> " + m.value);
				reportMetric(m.name, m.valueType, m.value);
			}
			LOGGER.debug("done!");
		}
	}

	private Map<String, EpochCounter> epochCounters = new HashMap<String, EpochCounter>();

	private Metric getMetric(Element n) throws ParseException {
		String type = n.getElementsByTagName("type").item(0) != null ? n.getElementsByTagName("type").item(0).getTextContent() : null;
		String ident = n.getElementsByTagName("ident").item(0) != null ? n.getElementsByTagName("ident").item(0).getTextContent() : null;
		String name = n.getElementsByTagName("name").item(0) != null ? n.getElementsByTagName("name").item(0).getTextContent() : null;
		String description = n.getElementsByTagName("description").item(0) != null ? n.getElementsByTagName("description").item(0).getTextContent()
				: null;
		String value = n.getElementsByTagName("value").item(0) != null ? n.getElementsByTagName("value").item(0).getTextContent() : null;
		String flag = n.getElementsByTagName("flag").item(0) != null ? n.getElementsByTagName("flag").item(0).getTextContent() : null;

		StringBuilder key = new StringBuilder();
		key.append("Varnish/");

		// does it have a custom group?
		String group = getGroup(name);

		if (group != null) {
			key.append(group + "/");
		} else if (type != null)
			key.append(type + "/");
		else {
			key.append("main/");

			int pos = name.indexOf("_");
			if (pos != -1) {
				key.append(name.substring(0, pos) + "/");
			}
		}

		if (ident != null)
			key.append(ident + "/");

		key.append(description);

		String actualKey = key.toString();
		if ("a".equals(flag)) {
			if (!epochCounters.containsKey(actualKey)) {
				epochCounters.put(actualKey, new EpochCounter());
			}
			return new Metric(actualKey, getUnitName(name) + "/sec", epochCounters.get(actualKey).process(NumberFormat.getInstance().parse(value)));
		} else {
			return new Metric(actualKey, getUnitName(name), NumberFormat.getInstance().parse(value));
		}
	}

	private String getUnitName(String metricName) {
		for (Map.Entry<Pattern, String> e : units.entrySet()) {
			if (e.getKey().matcher(metricName).matches()) {
				return e.getValue();
			}
		}

		return "value";
	}

	private String getGroup(String metricName) {
		for (Map.Entry<Pattern, String> e : groups.entrySet()) {
			if (e.getKey().matcher(metricName).matches()) {
				return e.getValue();
			}
		}

		return null;
	}
}
