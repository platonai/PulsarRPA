package org.warps.pulsar.common.proxy;

import org.warps.pulsar.common.NetUtil;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyEntry implements Comparable<ProxyEntry> {

    public static final String IP_PORT_REGEX = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)(:[0-9]{2,5})";
    public static final Pattern IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX);
    // Check if the proxy server is still available if it's not used for 30 seconds
    public static final Duration PROXY_EXPIRED = Duration.ofSeconds(30);
    // if a proxy server can not be connected in a hour, we announce it's dead and remove it from the file
    public static final Duration MISSING_PROXY_DEAD_TIME = Duration.ofHours(1);
    public static final int DEFAULT_PROXY_SERVER_PORT = 19080;

    private String host;
    private int port;
    private String targetHost;
    private Instant lastAvailable = Instant.now();
    private int failedTimes;

    public ProxyEntry(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ProxyEntry(String host, int port, String targetHost) {
        this.host = host;
        this.port = port;
        this.targetHost = targetHost;
    }

    public static ProxyEntry parse(String ipPort) {
        String host = null;
        int port = DEFAULT_PROXY_SERVER_PORT;
        long lastAvailableTime = 0;

        Matcher matcher = IP_PORT_PATTERN.matcher(ipPort);
        if (!matcher.find()) {
            return null;
        }

        String timeString = ipPort.substring(matcher.end()).trim();
        if (timeString.matches("\\d+")) {
            lastAvailableTime = Long.parseLong(timeString);
        }

        ipPort = matcher.group();
        int pos = ipPort.lastIndexOf(':');
        if (pos != -1) {
            host = ipPort.substring(0, pos);
            port = Integer.parseInt(ipPort.substring(pos + 1));
        }

        ProxyEntry proxyEntry = new ProxyEntry(host, port);
        proxyEntry.lastAvailable = Instant.ofEpochMilli(lastAvailableTime);

        return proxyEntry;
    }

    public static boolean validateIpPort(String ipPort) {
        return IP_PORT_PATTERN.matcher(ipPort).matches();
    }

    public static void main(String[] args) {
        String lines[] = {
                "192.144.22.123:89081",
                "192.144.22.123:89082 182342323",
                "192.144.22.123:89082 182341234",
                "192.144.22.123:89082 190341226",
                "192.144.22.123:89083 adasdfadf",
        };

        Set<ProxyEntry> mergedProxyEntries = new TreeSet<ProxyEntry>();
        for (String line : lines) {
            mergedProxyEntries.add(ProxyEntry.parse(line));
        }

        System.out.println(mergedProxyEntries);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Instant getLastAvailable() {
        return lastAvailable;
    }

    public boolean isExpired() {
        return lastAvailable.plus(PROXY_EXPIRED).isBefore(Instant.now());
    }

    public boolean isGone() {
        return failedTimes >= 3;
    }

    public void refresh() {
        this.lastAvailable = Instant.now();
    }

    public String ipPort() {
        return host + ":" + port;
    }

    public boolean testNetwork() {
        if (targetHost != null) {
            // TODO:
        }

        boolean available = NetUtil.testNetwork(getHost(), getPort());
        if (!available) {
            ++failedTimes;
        } else {
            failedTimes = 0;
        }

        return available;
    }

    @Override
    public String toString() {
        return ipPort() + " " + lastAvailable.toEpochMilli();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ProxyEntry) && ipPort().equals(((ProxyEntry) o).ipPort());
    }

    @Override
    public int compareTo(@Nonnull ProxyEntry proxyEntry) {
        return ipPort().compareTo(proxyEntry.ipPort());
    }
}
