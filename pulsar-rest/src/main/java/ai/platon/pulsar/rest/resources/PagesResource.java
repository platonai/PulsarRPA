/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.rest.resources;

import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.common.options.ParseOptions;
import ai.platon.pulsar.common.options.PulsarOptions;
import ai.platon.pulsar.crawl.component.*;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.parse.html.JsoupParser;
import ai.platon.pulsar.crawl.parse.html.JsoupUtils;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.WebPageFormatter;
import ai.platon.pulsar.rest.model.response.LinkDatum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hadoop.classification.InterfaceStability;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Singleton
@Path("/pages")
public class PagesResource {

  public static final Logger LOG = LoggerFactory.getLogger(PagesResource.class);

  private ImmutableConfig conf;
  private WebDb webDb;
  private FetchComponent fetchComponent;
  private ParseComponent parseComponent;
  private IndexComponent indexComponent;
  private UpdateComponent updateComponent;
  private LoadComponent loadComponent;
  private MetricsCounters metricsCounters;
  private ExecutorService executorService;
  private int maxThreads = 50;
  // TODO: Check thread safety
  private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  // @Autowired and @Inject have the same purpose, but @Autowired annotation issues better messages
  // @Inject
  @Autowired
  public PagesResource(
    WebDb webDb,
    FetchComponent fetchComponent,
    ParseComponent parseComponent,
    IndexComponent indexComponent,
    UpdateComponent updateComponent,
    LoadComponent loadComponent,
    MetricsCounters metricsCounters,
    ImmutableConfig conf) {
    this.webDb = webDb;
    this.fetchComponent = fetchComponent;
    this.parseComponent = parseComponent;
    this.indexComponent = indexComponent;
    this.updateComponent = updateComponent;
    this.loadComponent = loadComponent;
    this.metricsCounters = metricsCounters;
    this.conf = conf;
    this.executorService = Executors.newFixedThreadPool(maxThreads);

    // this.conf.setInt(HTTP_FETCH_MAX_RETRY, 1);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String get(
    @QueryParam("url") String url,
    @QueryParam("encoding") @DefaultValue("UTF-8") String encoding) {
    WebPage page = webDb.getOrNil(url);
    return page.isNil() ? "" : page.getContentAsString();
  }

  @GET
  @Path("/get-content")
  @Produces(MediaType.TEXT_HTML)
  public String getHtml(
    @QueryParam("url") String url,
    @QueryParam("args") @DefaultValue("-ps,-prst,--expires,30m") String args,
    @QueryParam("sanitize") @DefaultValue("false") boolean sanitize,
    @QueryParam("pithy") @DefaultValue("false") boolean pithy,
    @QueryParam("encoding") @DefaultValue("UTF-8") String encoding
  ) throws IOException {
    args += "-ps"; // Force parse
    WebPage page = loadComponent.load(url, args);
    String content = page.isNil() ? "" : page.getContentAsString();

    if (sanitize) {
      JsoupParser parser = new JsoupParser(page, conf);
      Document doc = parser.parse();

      content = JsoupUtils.toHtmlPiece(doc, pithy);
    }

    return content;
  }

  @GET
  @Path("/show")
  @Produces(MediaType.TEXT_PLAIN)
  public String show(
    @QueryParam("args") @DefaultValue("-s") String args,
    @QueryParam("url") String url
  ) {
    WebPage page = webDb.getOrNil(url);
    if (page.isNil()) {
      return "";
    }

    String[] options = PulsarOptions.Companion.split(args);
    for (String option : options) {
      if (option.equals("-a") || option.equals("--all")) {
        return new WebPageFormatter(page).withFields().withText().format();
      }
      if (option.equals("-f") || option.equals("--fields")) {
        return new WebPageFormatter(page).withFields().format();
      }
      if (option.equals("-s") || option.equals("--status")) {
        return new WebPageFormatter(page).format();
      }
    }

    return new WebPageFormatter(page).format();
  }

  @GET
  @Path("/get-links")
  @Produces(MediaType.APPLICATION_JSON)
  public String getLinks(
    @QueryParam("args") @DefaultValue("-ps,-rpl,-nlf,-lk,-ll") String args,
    @QueryParam("url") String url
  ) {
    WebPage page = webDb.getOrNil(url);
    if (page.isNil()) {
      return "[]";
    }

    boolean ll = false;
    boolean dl = false;
    boolean lk = false;
    for (String option : PulsarOptions.Companion.split(args)) {
      switch (option) {
        case "-ll":
        case "--live-links":
          ll = true;
          break;
        case "-dl":
        case "--dead-links":
          dl = true;
          break;
        case "-lk":
        case "--links":
          lk = true;
          break;
      }
    }

    ParseOptions parseOptions = ParseOptions.Companion.parse(args);
    if (parseOptions.isParse()) {
      parseComponent.parse(page, parseOptions.isReparseLinks(), parseOptions.isNoLinkFilter());
    }

    Map<String, String> links = new HashMap<>();
    if (lk) {
      page.getLinks().forEach(l -> links.put(l.toString(), l.toString()));
    }
    if (ll) {
      page.getLiveLinks().values().forEach(l -> links.put(l.getUrl().toString(), l.getAnchor().toString()));
    }
    if (dl) {
      page.getDeadLinks().forEach(l -> links.put(l.toString(), l.toString()));
    }

    List<LinkDatum> response = links.entrySet().stream()
      .sorted(Comparator.comparingInt(e -> e.getValue().length()))
      .distinct()
      .map(e -> new LinkDatum(e.getKey(), e.getValue(), 0))
      .collect(Collectors.toList());

    return gson.toJson(response);
  }

  @GET
  @Path("/clear-dead-links")
  @Produces(MediaType.APPLICATION_JSON)
  public String clearDeadLinks(
    @QueryParam("url") String url
  ) {
    WebPage page = webDb.getOrNil(url);
    if (!page.isNil()) {
      page.getDeadLinks().clear();

      webDb.put(page);
      webDb.flush();

      page = webDb.getOrNil(url);
    }

    return page.isNil() ? "{}" : gson.toJson(new WebPageFormatter(page).withLinks(true).toMap());
  }

  /**
   * We load everything from the internet, our storage is just a cache
   * */
  @GET
  @Path("/load")
  @Produces(MediaType.APPLICATION_JSON)
  public String load(
    @QueryParam("url") String url,
    @QueryParam("args") @DefaultValue("--parse,--persist,--reparse-links,--no-link-filter,--expires,30m") String args
  ) {
    LOG.debug("Loading " + url);

    LoadOptions loadOptions = LoadOptions.Companion.parse(args);

    WebPage page = loadComponent.load(url, loadOptions);
    loadComponent.flush();

    Map<String, Object> result = new WebPageFormatter(page)
      .withText(loadOptions.getWithText())
      .withLinks(loadOptions.getWithLinks())
      .withEntities(loadOptions.getWithModel())
      .toMap();
    return gson.toJson(result);
  }

  /**
   * We load everything from the internet, our storage is just a cache
   * */
  @InterfaceStability.Evolving
  @GET
  @Path("/outgoing/load")
  @Produces(MediaType.APPLICATION_JSON)
  public String loadOutPages(
      @QueryParam("url") String url,
      @QueryParam("args") @DefaultValue("-ps,-rpl,-prst,--expires,10m") String loadArgs,
      @QueryParam("lf") @DefaultValue("-amin,5,-amax,50,-umin,23,-umax,150") String linkArgs,
      @QueryParam("start") @DefaultValue("1") int start,
      @QueryParam("limit") @DefaultValue("20") int limit,
      @QueryParam("args2") @DefaultValue("-ps,-prst,--expires,1d") String loadArgs2,
      @QueryParam("log") @DefaultValue("0") int log,
      @Context HttpServletRequest request
  ) {
    linkArgs += ",log," + log;

    Map<String, Object> results = loadComponent.loadOutPages(url,
      LoadOptions.Companion.parse(loadArgs),
      LinkOptions.parse(linkArgs), start, limit,
      LoadOptions.Companion.parse(loadArgs2),
      "",
      log);
    loadComponent.flush();

    // Failed to inject HttpServletRequest using jersey-test-framework-provider-inmemory
    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    return gson.toJson(results);
  }

  @GET
  @Path("/fetch")
  @Produces(MediaType.APPLICATION_JSON)
  public String fetch(
          @QueryParam("args") @DefaultValue("-ps,-prst,-rpl,--retry,--expires,1s") String args,
          @QueryParam("url") String url,
          @QueryParam("log") @DefaultValue("0") int log,
          @Context HttpServletRequest request
  ) {
    LoadOptions loadOptions = LoadOptions.Companion.parse(args);

    WebPage page = loadComponent.load(url, loadOptions);
    loadComponent.flush();

    Map<String, String> header = new LinkedHashMap<>();
    if (request != null) {
      header.put("uri", request.getRequestURI());
      header.put("queryString", request.getQueryString());
    }

    if (log > 0) {
      header.put("loadOptions", loadOptions.toString());
    }

    Map<String, Object> doc = new WebPageFormatter(page)
      .withText(loadOptions.getWithText())
      .withLinks(loadOptions.getWithLinks())
      .withEntities(loadOptions.getWithModel())
      .toMap();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("header", header);
    response.put("body", doc);

    return gson.toJson(response);
  }

  @GET
  @Path(value = "/outgoing/fetch")
  @Produces(MediaType.APPLICATION_JSON)
  public String fetchOutgoingPages(
          @QueryParam("url") String url,
          @QueryParam("args") @DefaultValue("-ps,-rpl,-prst,--expires,10m") String args,
          @QueryParam("lf") @DefaultValue("-amin,5,-amax,50,-umin,23,-umax,150") String linkArgs,
          @QueryParam("start") @DefaultValue("1") int start,
          @QueryParam("limit") @DefaultValue("20") int limit,
          @QueryParam("args2") @DefaultValue("-ps,-rpl,-prst,--expires,1d") String args2,
          @QueryParam("log") @DefaultValue("0") int log,
          @QueryParam("async") @DefaultValue("true") boolean async,
          @Context HttpServletRequest request
  ) {
    // force parse and allow retrying
    args += ",-ps,--retry";
    args2 += ",-ps,--expires,1d";
    linkArgs += ",log," + log;

    Map<String, Object> results = loadComponent.loadOutPages(
      url, LoadOptions.Companion.parse(args),
      LinkOptions.parse(linkArgs), start, limit,
      LoadOptions.Companion.parse(args2),
      "",
      log);
    loadComponent.flush();

    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    return gson.toJson(results);
  }

  @GET
  @Path("/fetch-all")
  @Produces(MediaType.TEXT_HTML)
  public String fetchAll(
          @QueryParam("args") @DefaultValue("-ps,-rpl") String args,
          @QueryParam("urls") String urls,
          @Context HttpServletRequest request
  ) {
    if (urls == null || urls.isEmpty()) {
      return "[]";
    }

    args += ",-ps,--expires,0s";
    LoadOptions loadOptions = LoadOptions.Companion.parse(args);

    List<Map<String, Object>> docs = Stream.of(urls.split("\n"))
      .map(url -> loadComponent.load(url, loadOptions))
      .map(p -> new WebPageFormatter(p).toMap()).collect(Collectors.toList());
    loadComponent.flush();

    Map<String, Object> results = new LinkedHashMap<>();
    results.put("totalCount", docs.size());
    results.put("count", docs.size());

    results.put("docs", docs);

    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    return gson.toJson(results);
  }

  /**
   * We load everything from the internet, our storage is just a cache
   * */
  @PUT
  @Path("/parse")
  @Produces(MediaType.APPLICATION_JSON)
  public String parse(
    String query,
    @QueryParam("url") String url,
    @QueryParam("args") @DefaultValue("--parse,--expires,30m,--with-entities") String args
  ) {
    LoadOptions loadOptions = LoadOptions.Companion.parse(args);

//    LOG.debug("LoadOptions: " + loadOptions.toString());
//    LOG.debug("Query: " + query);
    WebPage page = loadComponent.load(url, loadOptions);
    loadComponent.flush();

    return gson.toJson(new WebPageFormatter(page).withEntities(loadOptions.getWithModel()).toMap());
  }

  @PUT
  @Path(value = "/outgoing/parse")
  @Produces(MediaType.APPLICATION_JSON)
  public String parseOutgoingPages(
    String query,
    @QueryParam("url") String url,
    @QueryParam("args") @DefaultValue("-ps,--expires,1s") String loadArgs,
    @QueryParam("lf") @DefaultValue("-amin,5,-amax,50,-umin,23,-umax,150") String linkArgs,
    @QueryParam("start") @DefaultValue("1") int start,
    @QueryParam("limit") @DefaultValue("20") int limit,
    @QueryParam("args2") @DefaultValue("-ps,--expires,1d,--with-entities") String loadArgs2,
    @QueryParam("log") @DefaultValue("0") int log,
    @Context HttpServletRequest request
  ) {
    loadArgs += ",-ps";
    loadArgs2 += ",-ps";
    linkArgs += ",-log," + log;

    Map<String, Object> results = loadComponent.loadOutPages(url,
      LoadOptions.Companion.parse(loadArgs),
      LinkOptions.parse(linkArgs), start, limit,
      LoadOptions.Companion.parse(loadArgs2),
      query,
      log);
    loadComponent.flush();

    // Failed to inject HttpServletRequest using jersey-test-framework-provider-inmemory
    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    results.put("metricsCounters", metricsCounters.getStatus(true));

    return gson.toJson(results);
  }

  @GET
  @Path("/index")
  @Produces(MediaType.APPLICATION_JSON)
  public String index(@QueryParam("url") String url) {
    WebPage page = loadComponent.load(url);
    IndexDocument doc = indexComponent.index(page);

    return gson.toJson(doc.asMultimap());
  }

  @GET
  @Path(value = "/outgoing/index")
  @Produces(MediaType.APPLICATION_JSON)
  public String indexOutgoingPages(
          @QueryParam("url") String url,
          @QueryParam("start") @DefaultValue("1") int start,
          @QueryParam("limit") @DefaultValue("20") int limit,
          @Context HttpServletRequest request
  ) {
    WebPage page = loadComponent.load(url);

    int totalCount = page.getLiveLinks().size();
    LoadOptions loadOptions = LoadOptions.Companion.create();
    Map<String, Map<String, List<String>>> indexDocuments = page.getSimpleLiveLinks().stream()
        .skip(start > 1 ? start - 1 : 0).limit(limit)
        .map(l -> loadComponent.load(l, loadOptions))
        .map(indexComponent::index)
        .collect(Collectors.toMap(IndexDocument::getUrl, IndexDocument::asMultimap, (i, i2) -> i2));

    Map<String, Object> results = new LinkedHashMap<>();
    results.put("totalCount", totalCount);
    results.put("count", indexDocuments.size());

    results.put("docs", indexDocuments);

    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    return gson.toJson(results);
  }

  @GET
  @Path("/index-all")
  @Produces(MediaType.TEXT_HTML)
  public String indexAll(
          @QueryParam("args") @DefaultValue("") String args,
          @QueryParam("urls") String urls,
          @Context HttpServletRequest request
  ) {
    if (urls.isEmpty()) {
      return "[]";
    }

    String[] urlArray = urls.split("\n");
    int totalCount = urlArray.length;
    LoadOptions loadOptions = LoadOptions.Companion.parse(args);
    Map<String, Map<String, List<String>>> indexDocuments = Stream.of(urlArray)
            .map(l -> loadComponent.load(l, loadOptions))
            .filter(Objects::nonNull)
            .map(p -> indexComponent.index(p))
            .collect(Collectors.toMap(IndexDocument::getUrl, IndexDocument::asMultimap, (i, i2) -> i2));

    Map<String, Object> results = new LinkedHashMap<>();
    results.put("totalCount", totalCount);
    results.put("count", indexDocuments.size());

    results.put("docs", indexDocuments);

    if (request != null) {
      results.put("uri", request.getRequestURI());
      results.put("queryString", request.getQueryString());
    }

    return gson.toJson(results);
  }

  @GET
  @Path(value = "/update")
  @Produces(MediaType.APPLICATION_JSON)
  public String updateByOutgoingPages(
          @QueryParam("url") String url,
          @QueryParam("start") @DefaultValue("1") int start,
          @QueryParam("limit") @DefaultValue("200") int limit,
          @QueryParam("persist") @DefaultValue("false") boolean persist
  ) {
    WebPage page = webDb.getOrNil(url);
    if (page.isNil()) {
      return "{}";
    }

    LoadOptions loadOptions = LoadOptions.Companion.parse("-ps,-prst");
    List<WebPage> outgoingPages = page.getSimpleLiveLinks().stream()
        .skip(start > 1 ? start - 1 : 0).limit(limit)
        .map(l -> loadComponent.load(l, loadOptions))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    updateComponent.updateByOutgoingPages(page, outgoingPages);

    loadComponent.flush();

    return gson.toJson(new WebPageFormatter(page).toMap());
  }

  /**
   * Remove seed
   *
   * @param url url to delete
   * @return boolean
   */
  @DELETE
  @Path("/delete")
  @Produces(MediaType.TEXT_PLAIN)
  public String delete(@QueryParam("url") String url) {
    boolean ret = webDb.delete(url);
    return String.valueOf(ret);
  }
}
