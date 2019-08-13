package ai.platon.pulsar.rest.service;

import ai.platon.pulsar.common.config.Params;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@XmlRootElement
public class PortManager {
  public static final Logger LOG = LoggerFactory.getLogger(PortManager.class);

  public static final int BASE_PORT = 21000;
  public static final int MAX_PORT = BASE_PORT + 1000;

  private String type;
  private int basePort;
  private int maxPort;

  private AtomicInteger nextPort = new AtomicInteger(0);
  private Set<Integer> activePorts = Sets.newHashSet();
  private Queue<Integer> freePorts = Queues.newPriorityQueue();

  public PortManager() {
    this.type = "";
    this.basePort = BASE_PORT;
    this.maxPort = MAX_PORT;
    this.nextPort.set(basePort);
  }

  public PortManager(String type) {
    this(type, BASE_PORT, MAX_PORT);
  }

  public PortManager(String type, int basePort, int maxPort) {
    this.type = type;
    this.basePort = basePort;
    this.maxPort = maxPort;
    this.nextPort.set(basePort);
  }

  public String getType() { return type; }

  public synchronized List<Integer> getActivePorts() { return Lists.newArrayList(activePorts); }

  public synchronized List<Integer> getFreePorts() { return Lists.newArrayList(freePorts); }

  public synchronized Integer acquire() {
    return getNextPort();
  }

  public synchronized void recycle(int port) {
    if (port >= basePort && port <= maxPort) {
      activePorts.remove(port);
      freePorts.add(port);
    }
  }

  private int getNextPort() {
    int port;

    if (!freePorts.isEmpty()) {
      port = freePorts.poll();
    }
    else {
      port = nextPort.incrementAndGet();
    }

    if (port >= basePort && port <= maxPort) {
      activePorts.add(port);
    }
    else {
      port = -1;
      LOG.warn("Port out of range [{}, {}]", basePort, maxPort);
    }

    return port;
  }

  @Override
  public synchronized String toString() {
    return Params.format(
        "Next port", nextPort,
        "Active ports", StringUtils.join(activePorts, ", "),
        "Free ports", StringUtils.join(freePorts, ", ")
    );
  }
}
