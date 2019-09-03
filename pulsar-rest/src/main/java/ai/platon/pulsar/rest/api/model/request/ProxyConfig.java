package ai.platon.pulsar.rest.api.model.request;

// gson compatible class
public class ProxyConfig {
  public class Coordinator {
    public int serverPortBase = 19080;
    public int proxyProcessCount = 10;

    Coordinator() {
    }
  }

  public Coordinator coordinator;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(coordinator.serverPortBase);
    sb.append('\t');
    sb.append(coordinator.proxyProcessCount);
    return sb.toString();
  }
}
