package ai.platon.pulsar.rest.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Check PMaster is available.
 * NOTICE : MasterReference is compatible with PMaster in class api level, but is compatible in REST api level
 * */
public class PageResourceReference {

  public static final Logger LOG = LoggerFactory.getLogger(PageResourceReference.class);
  public static final String ROOT_PATH = "api";

  private final String baseUri;
  private RestTemplate restTemplate = new RestTemplate();

  public PageResourceReference(String host, int port) {
    this.baseUri = String.format("http://%s:%d/%s", host, port, ROOT_PATH);

    LOG.info("PageResourceReference is created, baseUri: " + baseUri);
  }

  public String get(String url) {
    return restTemplate.getForObject(baseUri + "/pages", String.class, "url", url);
  }

  public String getHtml(String url) {
    return get(url);
  }

  public Map<String, String> getLiveLinks(String url) {
    String result = restTemplate.getForObject(baseUri + "/pages/get-outlinks", String.class, "url", url);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>() {}.getType());
  }

  public Map<String, Object> load(String url) {
    String result = restTemplate.getForObject(baseUri + "/pages/load", String.class, "url", url, "args", "-s");
    // ParameterizedTypeReference<HashMap<String, String>>
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>() {}.getType());
  }

  // Not OK
  public int injectAll(String... configuredUrls) {
    String data = StringUtils.join(configuredUrls, "__||__");
    return restTemplate.postForObject(baseUri + "/seeds/inject", data, Integer.class);
  }

  public Map<String, Object> fetch(String url) {
    String result = restTemplate.getForObject(baseUri + "/pages/fetch", String.class, "url", url);
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>() {}.getType());
  }

  public Map<String, Object> parse(String url) {
    String result = restTemplate.getForObject(baseUri + "/pages/parse", String.class, "url", url);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>(){}.getType());
  }

  public Map<String, List<String>> index(String url) {
    String result = restTemplate.getForObject(baseUri + "/pages/index", String.class, "url", url);
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, List<String>>>(){}.getType());
  }

  public void delete(String url) {
    restTemplate.delete(baseUri + "/pages/index", String.class, "url", url);
  }

  @Nonnull
  public Map<String, Object> loadOutPages(String url, String args, String args2, int start, int limit, int log) {
    String result = restTemplate.getForObject(baseUri + "/pages/load-out-pages", String.class,
            "url", url,
            "args", args,
            "args2", args2,
            "start", start,
            "limit", limit,
            "log", log
    );

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.fromJson(result, new TypeToken<Map<String, Object>>(){}.getType());
  }

  @Nonnull
  public Map<String, Map<String, String>> parseOutgoingPages(String url, int limit) {
    String result = restTemplate.getForObject(baseUri + "/pages/parse", String.class,
            "url", url,
            "limit", limit
    );

    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Map<String, String>>>(){}.getType());
  }

  public Map<String, Map<String, List<String>>> indexOutgoingPages(String url, int limit) {
    String result = restTemplate.getForObject(baseUri + "/pages/outgoing/index", String.class,
            "url", url,
            "limit", limit
    );

    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, Map<String, List<String>>>>(){}.getType());
  }

  public Map<String, String> updateOutgoingPages(String url, int limit) {
    String result = restTemplate.getForObject(baseUri + "/pages/update-by-out-pages", String.class,
            "url", url,
            "fmt", "json",
            "limit", limit
    );

    Gson gson = new GsonBuilder().create();
    return gson.fromJson(result, new TypeToken<Map<String, String>>(){}.getType());
  }
}
