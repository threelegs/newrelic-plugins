package ar.com.threelegs.newrelic.cassandra.jmx;

/**
 * @author juanformoso
 */
public class ConnectionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4434300827153637920L;
	private String host;
	
	public ConnectionException(String host, Throwable cause) {
		super("unable to connect to " + host, cause);
		this.host = host;
	}

	public String getHost() {
		return host;
	}
}
