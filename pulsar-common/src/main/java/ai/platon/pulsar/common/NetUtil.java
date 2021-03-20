package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * <p>NetUtil class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class NetUtil {

    private static final Logger log = LoggerFactory.getLogger(NetUtil.class);

    /** Constant <code>CONNECTION_TIMEOUT</code> */
    public static Duration CONNECTION_TIMEOUT = Duration.ofSeconds(3);
    /** Constant <code>PROXY_CONNECTION_TIMEOUT</code> */
    public static Duration PROXY_CONNECTION_TIMEOUT = Duration.ofSeconds(3);

    // Pattern for matching ip[:port]
    /** Constant <code>IP_PORT_PATTERN</code> */
    public static final Pattern IP_PORT_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?");

    /**
     * <p>testNetwork.</p>
     *
     * @param host a {@link java.lang.String} object.
     * @param port a int.
     * @return a boolean.
     */
    public static boolean testNetwork(String host, int port) {
        return testTcpNetwork(host, port);
    }

    /**
     * <p>testHttpNetwork.</p>
     *
     * @param url a {@link java.net.URL} object.
     * @return a boolean.
     */
    public static boolean testHttpNetwork(URL url) {
        return testHttpNetwork(url, null);
    }

    /**
     * <p>testHttpNetwork.</p>
     *
     * @param url a {@link java.net.URL} object.
     * @param proxy a {@link java.net.Proxy} object.
     * @return a boolean.
     */
    public static boolean testHttpNetwork(URL url, Proxy proxy) {
        boolean reachable = false;

        try {
            HttpURLConnection con;
            if (proxy != null) {
                con = (HttpURLConnection) url.openConnection(proxy);
            } else {
                con = (HttpURLConnection) url.openConnection();
            }
            con.setConnectTimeout((int) PROXY_CONNECTION_TIMEOUT.toMillis());
            con.connect();

            if (proxy != null) {
                log.debug("Proxy is available {} for {}", proxy, url);
            }

            reachable = true;
            con.disconnect();
        } catch (Exception ignored) {
        }

        return reachable;
    }
    
    public static boolean testHttpNetwork(String host, int port) {
        if (host.isBlank()) {
            return false;
        }

        try {
            URL url = new URL("http", host, port, "/");
            return testHttpNetwork(url);
        } catch (MalformedURLException ignored) {
        }

        return false;
    }

    public static boolean testTcpNetwork(String host, int port) {
        return testTcpNetwork(host, port, CONNECTION_TIMEOUT);
    }

    public static boolean testTcpNetwork(String host, int port, Duration timeout) {
        if (host.isBlank()) {
            return false;
        }

        boolean reachable = false;
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), (int)timeout.toMillis());
            reachable = socket.isConnected();
            socket.close();
        } catch (Exception ignored) {
            // logger.warn("can not connect to " + ip + ":" + port);
        }

        return reachable;
    }
    
    public static String getAgentString(String agentName) {
        return agentName;
    }
    
    public static String getAgentString(String agentName, String agentVersion,
                                        String agentDesc, String agentURL, String agentEmail) {

        if ((agentName == null) || (agentName.trim().length() == 0)) {
            log.error("No User-Agent string set (http.agent.name)!");
        }

        StringBuilder buf = new StringBuilder();

        buf.append(agentName);
        if (agentVersion != null) {
            buf.append("/");
            buf.append(agentVersion);
        }
        if (((agentDesc != null) && (agentDesc.length() != 0))
                || ((agentEmail != null) && (agentEmail.length() != 0))
                || ((agentURL != null) && (agentURL.length() != 0))) {
            buf.append(" (");

            if ((agentDesc != null) && (agentDesc.length() != 0)) {
                buf.append(agentDesc);
                if ((agentURL != null) || (agentEmail != null))
                    buf.append("; ");
            }

            if ((agentURL != null) && (agentURL.length() != 0)) {
                buf.append(agentURL);
                if (agentEmail != null)
                    buf.append("; ");
            }

            if ((agentEmail != null) && (agentEmail.length() != 0))
                buf.append(agentEmail);

            buf.append(")");
        }
        return buf.toString();
    }
    
    public static String getChromeUserAgent(String mozilla, String appleWebKit, String chrome, String safari) {
        return String.format("Mozilla/%s (X11; Linux x86_64) AppleWebKit/%s (KHTML, like Gecko) Chrome/%s Safari/%s",
                mozilla, appleWebKit, chrome, safari);
    }
    
    public static String gethost() {
        try {return "" + InetAddress.getLocalHost();}
        catch(UnknownHostException uhe) {return "" + uhe;}
    }
    
    public static String gethostOfIP(String ipPort) {
        if (null == ipPort || !IP_PORT_PATTERN.matcher(ipPort).matches()) {
            return null;
        }

        try {
            int colonIdx = ipPort.indexOf(':');
            String ip = (-1 == colonIdx) ? ipPort
                    : ipPort.substring(0, ipPort.indexOf(':'));
            return InetAddress.getByName(ip).getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * TODO : We may need a better solution to indicate whether it's a master
     *
     * @return a boolean.
     */
    public static boolean isMaster(ImmutableConfig conf) {
        String masterhost = conf.get(CapabilityTypes.PULSAR_MASTER_HOST, "localhost");
        return masterhost.equals("localhost") || masterhost.equals(gethost());
    }

    public static URL getMasterURL(ImmutableConfig conf, String path) throws MalformedURLException {
        String host = conf.get(CapabilityTypes.PULSAR_MASTER_HOST, "localhost");
        int port = conf.getInt(CapabilityTypes.PULSAR_MASTER_PORT, 8182);

        return new URL("http", host, port, path);
    }

    public static String getMasterUrl(ImmutableConfig conf) {
        String host = conf.get(CapabilityTypes.PULSAR_MASTER_HOST);
        int port = conf.getInt(CapabilityTypes.PULSAR_MASTER_PORT, 8182);
        return "http://" + host + ":" + port;
    }

    public static boolean isExternalLink(String sourceUrl, String destUrl) {
        try {
            String toHost = new URL(destUrl).getHost().toLowerCase();
            String fromHost = new URL(sourceUrl).getHost().toLowerCase();
            return !toHost.equals(fromHost);
        } catch (MalformedURLException ignored) {
        }

        return true;
    }
}
