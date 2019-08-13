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
import ai.platon.pulsar.crawl.component.*;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.WebPageFormatter;
import ai.platon.pulsar.rest.model.response.LinkDatum;
import ai.platon.pulsar.rest.service.JobConfigurations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Singleton
@Path("/seeds")
public class SeedResource {
  public static final Logger LOG = LoggerFactory.getLogger(SeedResource.class);

  public static String URL_SEPARATOR = "^^";

  private final JobConfigurations jobConfigurations;
  private final WebDb webDb;

  private final InjectComponent injectComponent;
  private final FetchComponent fetchComponent;
  private final ParseComponent parseComponent;
  private final UpdateComponent updateComponent;
  private final LoadComponent loadComponent;
  private final FetchSchedule fetchSchedule;

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Inject
  public SeedResource(WebDb webDb,
                      InjectComponent injectComponent,
                      FetchComponent fetchComponent,
                      ParseComponent parseComponent,
                      UpdateComponent updateComponent,
                      LoadComponent loadComponent,
                      FetchSchedule fetchSchedule,
                      JobConfigurations jobConfigurations) {
    this.webDb = webDb;
    this.injectComponent = injectComponent;
    this.fetchComponent = fetchComponent;
    this.parseComponent = parseComponent;
    this.updateComponent = updateComponent;
    this.loadComponent = loadComponent;
    this.fetchSchedule = fetchSchedule;
    this.jobConfigurations = jobConfigurations;
  }

  @GET
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  public String list(
      @QueryParam("start") @DefaultValue("1") int start,
      @QueryParam("limit") @DefaultValue("1000") int limit,
      @QueryParam("log") @DefaultValue("0") int logLevel,
      @Context HttpServletRequest request
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

  @GET
  @Path("/get-live-links")
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  public String getLiveLinks(
      @QueryParam("start") @DefaultValue("1") int start,
      @QueryParam("limit") @DefaultValue("1000") int limit) {
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

  @GET
  @Path("/home")
  @Produces(MediaType.APPLICATION_JSON)
  public List<LinkDatum> home(
      @QueryParam("pageNo") @DefaultValue("1") int pageNo,
      @QueryParam("limit") @DefaultValue("1000") int limit) {
    WebPage page = webDb.getOrNil(PulsarConstants.SEED_PAGE_1_URL);
    List<LinkDatum> links = new ArrayList<>();
    if (page.isNil() || page.getLiveLinks().isEmpty()) {
      return links;
    }

    return page.getLiveLinks().values().stream().limit(limit)
        .map(l -> new LinkDatum(l.getUrl().toString(), l.getAnchor().toString(), l.getOrder()))
        .collect(Collectors.toList());
  }

  @GET
  @Path(value = "/inject")
  public String inject(
    @QueryParam("url") String url,
    @QueryParam("args") String args
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

  @GET
  @Path(value = "/uninject")
  public String unInject(@QueryParam("url") String url) {
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
  @GET
  @Path(value = "/inject-all")
  public String injectAll(@QueryParam("configuredUrls") String configuredUrls) {
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
  @GET
  @Path(value = "/uninject-all")
  public String unInjectAll(@QueryParam("urls") String urls) {
    List<WebPage> pages = new ArrayList<>(injectComponent.injectAll(urls.split(URL_SEPARATOR)));
    injectComponent.commit();

    return pages.stream().map(p -> gson.toJson(new WebPageFormatter(p).toMap()))
        .collect(Collectors.joining(", ", "[", "]"));
  }

  @GET
  @Path(value = "/force-refetch")
  public String forceRefetch(@QueryParam("url") String url) {
    WebPage page = webDb.getOrNil(url);
    if (page.isNil()) {
      return "{}";
    }

    fetchSchedule.forceRefetch(page, true);
    return gson.toJson(new WebPageFormatter(page).toMap());
  }
}
