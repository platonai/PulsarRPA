package ai.platon.pulsar.rest.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Check PMaster is available.
 * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
 * */
public class PageResourceReference {

  public static final Logger LOG = LoggerFactory.getLogger(PageResourceReference.class);
  public static final String ROOT_PATH = "api";

  private final URI baseUri;
  private final Client client;

  public PageResourceReference(String host, int port) {
    this.baseUri = URI.create(String.format("http://%s:%d/%s", host, port, ROOT_PATH));

    client = ClientBuilder.newClient(new ClientConfig());
    LOG.info("PageResourceReference is created, baseUri: " + baseUri);
  }

  public PageResourceReference(URI baseUri, Client client) {
    this.baseUri = baseUri;
    this.client = client;
    LOG.info("PageResourceReference is created, baseUri: " + baseUri);
  }

  public String get(String url) {
    return target("/").queryParam("url", url).request().get(String.class);
  }

  public String getHtml(String url) {
    return get(url);
  }

  public Map<String, String> getLiveLinks(String url) {
    String result = target("get-outlinks")
        .queryParam("url", url)
        .request()
        .get(String.class);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>() {}.getType());
  }

  public Map<String, Object> load(String url) {
    String result = target("load")
            .queryParam("args", "-s")
            .queryParam("url", url).request().get(String.class);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>() {}.getType());
  }

  // Not OK
  public int injectAll(String... configuredUrls) {
    String result = client.target(baseUri).path("seeds").path("inject")
            .request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .put(Entity.json(StringUtils.join(configuredUrls, "__||__")), String.class);
    return NumberUtils.toInt(result, -1);
  }

  public Map<String, Object> fetch(String url) {
    String result = target("fetch")
            .queryParam("url", url)
            .request()
            .get(String.class);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>() {}.getType());
  }

  public Map<String, Object> parse(String url) {
    String result = target("parse")
            .queryParam("url", url)
            .request()
            .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>(){}.getType());
  }

  public Map<String, List<String>> index(String url) {
    String result = target("index")
            .queryParam("url", url)
            .request()
            .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, List<String>>>(){}.getType());
  }

  public boolean delete(String url) {
    return target("delete").queryParam("url", url).request().delete(Boolean.class);
  }

  @Nonnull
  public Map<String, Object> loadOutPages(String url, String args, String args2, int start, int limit, int log) {
    String result = target("outgoing/load")
      .queryParam("url", url)
      .queryParam("args", args)
      .queryParam("args2", args2)
      .queryParam("start", start)
      .queryParam("limit", limit)
      .queryParam("log", log)
      .request()
      .get(String.class);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>(){}.getType());
  }

  @Nonnull
  public Map<String, Map<String, String>> parseOutgoingPages(String url, int limit) {
    String result = target("outgoing/parse")
        .queryParam("url", url)
        .queryParam("limit", limit)
        .request()
        .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Map<String, String>>>(){}.getType());
  }

  public Map<String, Map<String, List<String>>> indexOutgoingPages(String url, int limit) {
    String result = target("outgoing/index")
        .queryParam("url", url)
        .queryParam("limit", limit)
        .request()
        .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Map<String, List<String>>>>(){}.getType());
  }

  public Map<String, String> updateOutgoingPages(String url, int limit) {
    String result = target("update-by-out-pages")
        .queryParam("url", url)
        .queryParam("fmt", "json")
        .queryParam("limit", limit)
        .request()
        .get(String.class);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>(){}.getType());
  }

  public final WebTarget target(String path) {
    return client.target(baseUri).path("pages").path(path);
  }
}
