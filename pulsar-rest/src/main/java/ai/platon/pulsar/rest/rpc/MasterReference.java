package ai.platon.pulsar.rest.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ai.platon.pulsar.rest.model.response.LinkDatum;
import org.apache.hadoop.conf.Configuration;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  private final Client client;

  public MasterReference(Configuration conf) {
    this(conf.get(PULSAR_MASTER_HOST, DEFAULT_PULSAR_MASTER_HOST),
      conf.getInt(PULSAR_MASTER_PORT, DEFAULT_PULSAR_MASTER_PORT));
  }

  public MasterReference(String baseUri) {
    this.baseUri = URI.create(baseUri);
    client = ClientBuilder.newClient(new ClientConfig());
    LOG.info("MasterReference created, baseUri: " + baseUri);
  }

  public MasterReference(String host, int port) {
    this.baseUri = URI.create(String.format("http://%s:%d/%s", host, port, ROOT_PATH));

    client = ClientBuilder.newClient(new ClientConfig());
    LOG.info("MasterReference created, baseUri: " + baseUri);
  }

  public MasterReference(URI baseUri, Client client) {
    this.baseUri = baseUri;
    this.client = client;
    LOG.info("MasterReference created, baseUri: " + baseUri);
  }

  public boolean test() {
    try {
      return testHttpNetwork(baseUri.toURL());
    } catch (MalformedURLException ignored) {
    }
    return false;
  }

  public List<LinkDatum> list() {
    String result = target("seeds")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .get(String.class);

    Gson gson = new GsonBuilder().create();
    Type listType = new TypeToken<ArrayList<LinkDatum>>() {
    }.getType();
    ArrayList<LinkDatum> links = gson.fromJson(result, listType);

    LOG.debug(links.toString());

    return links;
  }

  public List<LinkDatum> home() {
    return target("seeds")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .get(new GenericType<List<LinkDatum>>() {
      });
  }

  public Map<String, String> inject(String url) {
    return inject(url, "");
  }

  /**
   * Register this fetch server instance
   */
  public Map<String, String> inject(String url, String args) {
    String result = target("seeds")
      .path("inject")
      .queryParam("url", url)
      .queryParam("args", args)
      .request()
      .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>(){}.getType());
  }

  public Map<String, String> unInject(String url) {
    String result = target("seeds")
        .path("uninject")
        .queryParam("url", url)
        .request()
        .get(String.class);

    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>(){}.getType());
  }

  public final WebTarget target() {
    return this.client.target(this.baseUri);
  }

  public final WebTarget target(String path) {
    return this.target().path(path);
  }
}
