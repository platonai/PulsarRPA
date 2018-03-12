package org.warps.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.warps.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_HOST;
import static org.warps.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_PORT;

public class NetUtil {

    protected static final Logger LOG = LoggerFactory.getLogger(NetUtil.class);

    public static int ProxyConnectionTimeout = 5 * 1000;

    public static boolean testNetwork(String ip, int port) {
        return testTcpNetwork(ip, port);
    }

    public static boolean testHttpNetwork(URL url) {
        boolean reachable = false;

        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(ProxyConnectionTimeout);
            con.connect();
            // logger.info("available proxy server {} : {}", ip, port);
            reachable = true;
            con.disconnect();
        } catch (Exception ignored) {
        }

        return reachable;
    }

    public static boolean testHttpNetwork(String host, int port) {
        try {
            URL url = new URL("http", host, port, "/");
            return testHttpNetwork(url);
        } catch (MalformedURLException ignored) {
        }

        return false;
    }

    public static boolean testTcpNetwork(String ip, int port) {
        boolean reachable = false;
        Socket con = new Socket();

        try {
            con.connect(new InetSocketAddress(ip, port), ProxyConnectionTimeout);
            // logger.info("available proxy server : " + ip + ":" + port);
            reachable = true;
            con.close();
        } catch (Exception e) {
            // logger.warn("can not connect to " + ip + ":" + port);
        }

        return reachable;
    }

    /**
     * TODO : use package org.warps.pulsar.crawl.net.domain
     */
    public static String getTopLevelDomain(String baseUri) {
        if (baseUri == null || baseUri.startsWith("/") || baseUri.startsWith("file://")) {
            return null;
        }

        int pos = -1;
        String domain = null;

        if (StringUtils.startsWith(baseUri, "http")) {
            final int fromIndex = "https://".length();
            pos = baseUri.indexOf('/', fromIndex);

            if (pos != -1)
                baseUri = baseUri.substring(0, pos);
        }

        pos = baseUri.indexOf(".") + 1;

        if (pos != 0) {
            domain = baseUri.substring(pos);
        }

        // for example : http://dangdang.com
        if (domain != null && !domain.contains(".")) {
            domain = baseUri.replaceAll("(http://|https://)", "");
        }

        return domain;
    }

    public static String getAgentString(String agentName) {
        return agentName;
    }

    public static String getAgentString(String agentName, String agentVersion,
                                        String agentDesc, String agentURL, String agentEmail) {

        if ((agentName == null) || (agentName.trim().length() == 0)) {
            LOG.error("No User-Agent string set (http.agent.name)!");
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

    public static String getHostname() {
        try {
            return Files.readAllLines(Paths.get("/etc/hostname")).get(0);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * TODO : We may need a better solution to indicate whether it's a master
     */
    public static boolean isMaster(Configuration conf) {
        String masterHostname = conf.get(PULSAR_MASTER_HOST, "localhost");
        return masterHostname.equals("localhost") || masterHostname.equals(NetUtil.getHostname());

    }

    public static URL getUrl(Configuration conf, String path) throws MalformedURLException {
        String host = conf.get(PULSAR_MASTER_HOST, "localhost");
        int port = conf.getInt(PULSAR_MASTER_PORT, 8182);

        return new URL("http", host, port, path);
    }

    public static String getBaseUrl(Configuration conf) {
        String host = conf.get(PULSAR_MASTER_HOST);
        int port = conf.getInt(PULSAR_MASTER_PORT, 8182);

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
