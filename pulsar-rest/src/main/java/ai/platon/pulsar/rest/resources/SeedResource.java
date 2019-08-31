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

import ai.platon.pulsar.common.config.PulsarConstants;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.common.options.LoadOptions;
import ai.platon.pulsar.crawl.component.InjectComponent;
import ai.platon.pulsar.crawl.component.LoadComponent;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.WebPageFormatter;
import ai.platon.pulsar.rest.model.response.LinkDatum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/seeds")
public class SeedResource {
  public static final Logger LOG = LoggerFactory.getLogger(SeedResource.class);

  public static String URL_SEPARATOR = "^^";

  private ApplicationContext applicationContext;
  private final WebDb webDb;

  private final InjectComponent injectComponent;
  private final LoadComponent loadComponent;
  private final FetchSchedule fetchSchedule;

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Autowired
  public SeedResource(WebDb webDb,
                      InjectComponent injectComponent,
                      LoadComponent loadComponent,
                      FetchSchedule fetchSchedule) {
    this.webDb = webDb;
    this.injectComponent = injectComponent;
    this.loadComponent = loadComponent;
    this.fetchSchedule = fetchSchedule;
  }

  @GetMapping
  public String list(
      @RequestParam(value = "start", defaultValue = "1") int start,
      @RequestParam(value = "limit", defaultValue = "40") int limit,
      @RequestParam(value = "log", defaultValue = "1") int logLevel,
      HttpServletRequest request
  ) {
    Map<String, Object> results = loadComponent.loadOutPages(PulsarConstants.SEED_PAGE_1_URL,
      LoadOptions.Companion.parse("-s,-nlf,--expires=36500d"),
      LinkOptions.parse("-amin=0,-umin=1,-log," + logLevel), start, limit,
      LoadOptions.Companion.parse("-s,--expires=36500d"),"",
      logLevel);

    results.put("uri", request.getRequestURI());
    results.put("queryString", request.getQueryString());

    return gson.toJson(results);
  }

  @GetMapping("/get-live-links")
  public String getLiveLinks(
      @RequestParam("start") int start,
      @RequestParam("limit") int limit) {
    WebPage page = webDb.getOrNil(PulsarConstants.SEED_PAGE_1_URL);
    if (page.isNil() || page.getLiveLinks().isEmpty()) {
      return "[]";
    }

    start = start > 1 ? start : 1;
    return "[\n" +
        page.getLiveLinks().values().stream()
            .skip(start - 1)
            .limit(limit)
            .map(Object::toString)
            .collect(Collectors.joining(",\n")) +
        "\n]";
  }

  @GetMapping("/home")
  public List<LinkDatum> home(
      @RequestParam("pageNo") int pageNo,
      @RequestParam("limit") int limit) {
    WebPage page = webDb.getOrNil(PulsarConstants.SEED_PAGE_1_URL);
    List<LinkDatum> links = new ArrayList<>();
    if (page.isNil() || page.getLiveLinks().isEmpty()) {
      return links;
    }

    return page.getLiveLinks().values().stream().limit(limit)
        .map(l -> new LinkDatum(l.getUrl().toString(), l.getAnchor().toString(), l.getOrder()))
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/inject")
  public String inject(
    @RequestParam("url") String url,
    @RequestParam("args") String args
  ) {
    if (url.isEmpty()) {
      return "{}";
    }

    WebPage page = injectComponent.inject(url, args);
    if (page.isSeed()) {
      injectComponent.commit();
      return gson.toJson(new WebPageFormatter(page).toMap());
    }

    return "{}";
  }

  @GetMapping(value = "/uninject")
  public String unInject(@RequestParam("url") String url) {
    if (url.isEmpty()) {
      return "{}";
    }

    WebPage page = injectComponent.unInject(url);
    if (page.isNil()) {
      return "{}";
    }

    injectComponent.commit();
    return gson.toJson(new WebPageFormatter(page).toMap());
  }

  /**
   * Configured url separated by URL_SEPARATOR
   * */
  @GetMapping(value = "/inject-all")
  public String injectAll(@RequestParam("configuredUrls") String configuredUrls) {
    if (configuredUrls.isEmpty()) {
      return "{}";
    }

    List<WebPage> pages = injectComponent.injectAll(configuredUrls.split(URL_SEPARATOR));
    injectComponent.commit();

    return pages.stream().map(p -> gson.toJson(new WebPageFormatter(p).toMap()))
        .collect(Collectors.joining(", ", "[", "]"));
  }

  /**
   * Configured url separated by "__`N`__"
   * */
  @GetMapping(value = "/uninject-all")
  public String unInjectAll(@RequestParam("urls") String urls) {
    List<WebPage> pages = new ArrayList<>(injectComponent.injectAll(urls.split(URL_SEPARATOR)));
    injectComponent.commit();

    return pages.stream().map(p -> gson.toJson(new WebPageFormatter(p).toMap()))
        .collect(Collectors.joining(", ", "[", "]"));
  }

  @GetMapping(value = "/force-refetch")
  public String forceRefetch(@RequestParam("url") String url) {
    WebPage page = webDb.getOrNil(url);
    if (page.isNil()) {
      return "{}";
    }

    fetchSchedule.forceRefetch(page, true);
    return gson.toJson(new WebPageFormatter(page).toMap());
  }

}
