package ai.platon.pulsar.jobs.fetch.service.jersey1;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.rdb.model.ServerInstance;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static ai.platon.pulsar.common.NetUtil.testHttpNetwork;
import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_HOST;
import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_PORT;
import static ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_HOST;
import static ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_PORT;

/**
 * Check PMaster is available.
 * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
 * */
public class MasterReference {

  public static final Logger LOG = LoggerFactory.getLogger(MasterReference.class);
  public static final String ROOT_PATH = "api";

  private final URI baseUri;
  private final Client client = Client.create();
  private final WebResource target;

  public MasterReference(ImmutableConfig conf) {
    this(conf.get(PULSAR_MASTER_HOST, DEFAULT_PULSAR_MASTER_HOST),
        conf.getInt(PULSAR_MASTER_PORT, DEFAULT_PULSAR_MASTER_PORT));
  }

  public MasterReference(String host, int port) {
    this.baseUri = URI.create(String.format("http://%s:%d/%s", host, port, ROOT_PATH));
    target = client.resource(baseUri);
    LOG.info("MasterReference created, baseUri : " + baseUri);
  }

  public void addFilter(ClientFilter filter) {
    client.addFilter(filter);
  }

  public boolean test() {
    try {
      return testHttpNetwork(baseUri.toURL());
    } catch (MalformedURLException ignored) {
    }
    return false;
  }

  public ServerInstance echo(ServerInstance serverInstance) {
    return target("service").path("echo")
        .type(MediaType.APPLICATION_JSON)
//        .accept(MediaType.APPLICATION_JSON)
        .post(ServerInstance.class, serverInstance);
  }

  /**
   * Register this fetch server instance
   */
  public ServerInstance register(ServerInstance serverInstance) {
    return target("service").path("register")
        .type(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .post(ServerInstance.class, serverInstance);
  }

  /**
   * Unregister this fetch server instance
   */
  public ServerInstance unregister(long serverId) {
    return target("service").path("unregister").path(String.valueOf(serverId))
        .type(MediaType.APPLICATION_JSON)
        .delete(ServerInstance.class);
  }

  /**
   * Acquire a available fetch server port
   */
  public int acquirePort(ServerInstance.Type type) {
    String response = target("port").path("legacy").path("acquire")
        .queryParam("type", type.name())
        .get(String.class);
    return Integer.parseInt(response);
  }

  /**
   * Recycle a available fetch server port
   * */
  public void recyclePort(ServerInstance.Type type, int port) {
    target("port")
        .path("recycle")
        .queryParam("type", type.name())
        .queryParam("port", String.valueOf(port))
        .put();
  }

  /**
   * Get all active ports
   * */
  public List<Integer> getFreePorts(ServerInstance.Type type) {
    String response = target("port")
        .path("legacy")
        .path("free")
        .queryParam("type", type.name())
        .get(String.class);
    Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
    return new GsonBuilder().create().fromJson(response, listType);
  }

  private WebResource target(String path) {
    return target.path(path);
  }
}
