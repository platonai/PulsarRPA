package ai.platon.pulsar.common;

import ai.platon.pulsar.common.urls.URLUtils;
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


    public static Duration CONNECTION_TIMEOUT = Duration.ofSeconds(3);

    public static Duration PROXY_CONNECTION_TIMEOUT = Duration.ofSeconds(3);

    // Pattern for matching ip[:port]

    public static final Pattern IP_PORT_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?");

    public static boolean testNetwork(String host, int port) {
        return testTcpNetwork(host, port);
    }

    public static boolean testHttpNetwork(String url) {
        var u = URLUtils.getURLOrNull(url);
        if (u == null) {
            return false;
        }
        return testHttpNetwork(u.getHost(), u.getPort());
    }

    public static boolean testHttpNetwork(URL url) {
        return testHttpNetwork(url, null);
    }

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

    public static String getHostname() {
        try {return "" + InetAddress.getLocalHost();}
        catch(UnknownHostException uhe) {return "" + uhe;}
    }
}
