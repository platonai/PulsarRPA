package ai.platon.pulsar.jobs.fetch.data;

import java.net.URL;
import java.util.Objects;

/**
 * Created by vincent on 17-3-8.
 */
public class PoolId implements Comparable<PoolId> {
  private final int priority;
  private final String protocol;
  private final String host;

  // private final String browser;

  public PoolId(int priority, URL url) {
    this.priority = priority;
    this.protocol = url.getProtocol();
    this.host = url.getHost();
  }

  public PoolId(int priority, String protocol, String host) {
    this.priority = priority;
    this.protocol = protocol;
    this.host = host;
  }

  public int getPriority() {
    return priority;
  }

  public String getProtocal() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public String toUrl() {
    return protocol + "://" + host;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof PoolId)) return false;

    PoolId other = (PoolId) obj;
    return priority == other.priority
        && Objects.equals(protocol, other.protocol)
        && Objects.equals(host, other.host);
  }

  @Override
  public int compareTo(PoolId other) {
    int c = priority - other.priority;
    if (c == 0) {
      c = protocol.compareTo(other.protocol);
      if (c == 0) {
        c = host.compareTo(other.host);
      }
    }

    return c;
  }

  @Override
  public int hashCode() {
    return priority * 31^2 + protocol.hashCode() * 31 + host.hashCode();
  }

  @Override
  public String toString() { return "<" + priority + ", " + toUrl() + ">"; }
}
