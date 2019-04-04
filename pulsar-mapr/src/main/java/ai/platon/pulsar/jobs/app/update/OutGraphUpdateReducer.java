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
package ai.platon.pulsar.jobs.app.update;

import ai.platon.pulsar.common.CounterUtils;
import ai.platon.pulsar.common.MetricsSystem;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.component.UpdateComponent;
import ai.platon.pulsar.crawl.filter.CrawlFilter;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import ai.platon.pulsar.persist.CrawlMarks;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.graph.GraphGroupKey;
import ai.platon.pulsar.persist.graph.WebEdge;
import ai.platon.pulsar.persist.graph.WebGraph;
import ai.platon.pulsar.persist.graph.WebVertex;
import ai.platon.pulsar.persist.io.WebGraphWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.CommonCounter.rPersist;
import static ai.platon.pulsar.common.CommonCounter.rRows;
import static ai.platon.pulsar.common.PulsarParams.VAR_PAGE_EXISTENCE;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.DISTANCE_INFINITE;
import static ai.platon.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_TARGET;
import static ai.platon.pulsar.persist.metadata.Mark.PARSE;
import static ai.platon.pulsar.persist.metadata.Mark.UPDATEOUTG;

public class OutGraphUpdateReducer extends AppContextAwareGoraReducer<GraphGroupKey, WebGraphWritable, String, GWebPage> {

  public static final Logger LOG = LoggerFactory.getLogger(OutGraphUpdateReducer.class);

  private enum PageExistence { PASSED, LOADED, CREATED }

  private FetchSchedule fetchSchedule;
  private ScoringFilters scoringFilters;
  private UpdateComponent updateComponent;
  private WebDb webDb;
  private MetricsSystem metricsSystem;

  private Instant impreciseNow = Instant.now();
  private int maxInLinks;

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    fetchSchedule = applicationContext.getBean("fetchSchedule", FetchSchedule.class);
    scoringFilters = applicationContext.getBean(ScoringFilters.class);
    metricsSystem = applicationContext.getBean(MetricsSystem.class);
    webDb = applicationContext.getBean(WebDb.class);
    // Active counter registration
    updateComponent = applicationContext.getBean(UpdateComponent.class);

    // getPulsarReporter().setLog(LOG_ADDITIVITY);

    String crawlId = conf.get(STORAGE_CRAWL_ID);
    maxInLinks = conf.getInt(UPDATE_MAX_INLINKS, 1000);

    Params.of(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "maxLinks", maxInLinks,
        "fetchSchedule", fetchSchedule.getClass().getSimpleName(),
        "scoringFilters", scoringFilters.toString()
    ).withLogger(LOG).info();
  }

  @Override
  protected void reduce(GraphGroupKey key, Iterable<WebGraphWritable> values, Context context) {
    try {
      doReduce(key, values, context);
    } catch (Throwable e) {
      LOG.error(StringUtil.stringifyException(e));
    }
  }

  /**
   * We get a list of score datum who are inlinks to a webpage after partition,
   * so the dbupdate phrase calculates the scores of all pages
   * <p>
   * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
   * The center vertex has a web page inside, and the edges has in-link information.
   * <pre>
   *        v1
   *        |
   *        v
   * v2 -> vc <- v3
   *       ^ ^
   *      /  \
   *     v4  v5
   * </pre>
   * And this is a out-link graph:
   * <pre>
   *       v1
   *       ^
   *       |
   * v2 <- vc -> v3
   *      / \
   *     v  v
   *    v4  v5
   * </pre>
   */
  private void doReduce(GraphGroupKey key, Iterable<WebGraphWritable> subGraphs, Context context)
      throws IOException, InterruptedException {
    metricsCounters.increase(rRows);

    String reversedUrl = key.getReversedUrl();
    String url = Urls.unreverseUrl(reversedUrl);

    WebGraph graph = buildGraph(url, subGraphs);
    WebPage page = graph.getFocus().getWebPage();

    if (page == null) {
      return;
    }

    if (!page.getUrl().equals(url) || !page.getKey().equals(reversedUrl)) {
      LOG.error("Inconsistent url : " + url);
      return;
    }

    // 1. update depth, 2. update in-links, 3. update score from in-coming pages
    updateGraph(graph);
    updateMarks(page);

    context.write(reversedUrl, page.unbox());
    metricsCounters.increase(rPersist);
  }

  /**
   * The graph should be like this:
   * <pre>
   *            v1
   *            |
   *            v
   * v2 -> TargetVertex <- v3
   *       ^          ^
   *      /           \
   *     v4           v5
   * </pre>
   */
  private WebGraph buildGraph(String url, Iterable<WebGraphWritable> subGraphs) {
    WebGraph graph = new WebGraph();

    WebVertex focus = new WebVertex(url);
    for (WebGraphWritable graphWritable : subGraphs) {
      assert (graphWritable.getOptimizeMode().equals(IGNORE_TARGET));

      WebGraph subGraph = graphWritable.get();
      subGraph.edgeSet().forEach(edge -> {
        if (edge.isLoop()) {
          focus.setWebPage(edge.getSourceWebPage());
        }

        // LOG.info("MultiMetadata " + url + "\t<-\t" + edge.getMetadata());
        graph.addEdgeLenient(edge.getSource(), focus, subGraph.getEdgeWeight(edge)).setMetadata(edge.getMetadata());
      });
    }

    if (focus.hasWebPage()) {
      focus.getWebPage().getVariables().set(VAR_PAGE_EXISTENCE, PageExistence.PASSED);
      metricsCounters.increase(UpdateComponent.Counter.rPassed);
    } else {
      WebPage page = loadOrCreateWebPage(url);
      focus.setWebPage(page);
    }
    graph.setFocus(focus);

    return graph;
  }

  /**
   * <pre>
   *            v1
   *            |
   *            v
   * v2 -> TargetVertex <- v3
   *       ^          ^
   *      /           \
   *     v4           v5
   * </pre>
   */
  private void updateGraph(WebGraph graph) {
    WebVertex focus = graph.getFocus();
    WebPage focusedPage = focus.getWebPage();
    Set<WebEdge> incomingEdges = graph.incomingEdgesOf(focus);

    focusedPage.getInlinks().clear();
    int smallestDepth = focusedPage.getDistance();
    WebEdge shallowestEdge = null;
    Set<CharSequence> anchors = new HashSet<>();

    for (WebEdge incomingEdge : incomingEdges) {
      if (incomingEdge.isLoop()) {
        continue;
      }

      // LOG.info(incomingEdge.toString());

      /* Update in-links */
      if (focusedPage.getInlinks().size() <= maxInLinks) {
        focusedPage.getInlinks().put(incomingEdge.getSourceUrl(), incomingEdge.getAnchor());
      }

      if (incomingEdge.getAnchor().length() != 0 && anchors.size() < 10) {
        anchors.add(incomingEdge.getAnchor());
      }

      WebPage incomingPage = incomingEdge.getSourceWebPage();
      if (incomingPage.getDistance() + 1 < smallestDepth) {
        smallestDepth = incomingPage.getDistance() + 1;
        shallowestEdge = incomingEdge;
      }
    }

    if (focusedPage.getDistance() != DISTANCE_INFINITE && smallestDepth < focusedPage.getDistance()) {
      metricsSystem.debugDepthUpdated(focusedPage.getDistance() + " -> " + smallestDepth + ", " + focus.getUrl());
    }

    if (shallowestEdge != null) {
      WebPage incomingPage = shallowestEdge.getSourceWebPage();

      focusedPage.setReferrer(incomingPage.getUrl());
      focusedPage.setDistance(smallestDepth);

      // Do we have this field?
      // focusedPage.setOptions(incomingPage.getOptions());

      focusedPage.setOptions(shallowestEdge.getOptions());
      focusedPage.setAnchor(shallowestEdge.getAnchor());
      focusedPage.setAnchorOrder(shallowestEdge.getOrder());

      updateDepthCounter(smallestDepth, focusedPage);
    }

    // Anchor can be used to determine the article title
    if (!anchors.isEmpty()) {
      focusedPage.setInlinkAnchors(anchors);
    }

    /* Update score */
    scoringFilters.updateScore(focusedPage, graph, incomingEdges);
  }

  /**
   * There are three cases of a page's state of existence
   * 1. the page have been fetched in this batch and so it's passed from the mapper
   * 2. the page have been fetched in other batch, so it's in the data store
   * 3. the page have not be fetched yet
   */
  private WebPage loadOrCreateWebPage(String url) {
    // TODO: Is datastore.get a distributed operation? And is there a local cache?
    // TODO: distinct url and baseUrl, the url passed by OutGraphUpdateMapper might be the original baseUrl
    WebPage loadedPage = webDb.get(url);
    WebPage page;

    // Page is already in the db
    if (loadedPage != null) {
      page = loadedPage;
      page.getVariables().set(VAR_PAGE_EXISTENCE, PageExistence.LOADED);
      metricsCounters.increase(UpdateComponent.Counter.rLoaded);
    } else {
      // Here we create a new web page from outlink
      page = createNewRow(url);
      page.getVariables().set(VAR_PAGE_EXISTENCE, PageExistence.CREATED);

      metricsCounters.increase(UpdateComponent.Counter.rCreated);
      if (CrawlFilter.sniffPageCategory(url).isDetail()) {
        metricsCounters.increase(UpdateComponent.Counter.rNewDetail);
      }
    }

    return page;
  }

  private WebPage createNewRow(String url) {
    WebPage page = WebPage.newWebPage(url);

    scoringFilters.initialScore(page);
    fetchSchedule.initializeSchedule(page);

    return page;
  }

  private void updateMarks(WebPage page) {
    CrawlMarks marks = page.getMarks();
    marks.putIfNonNull(UPDATEOUTG, marks.get(PARSE));
  }

  private void updateDepthCounter(int depth, WebPage page) {
    PageExistence pageExistence = page.getVariables().get(VAR_PAGE_EXISTENCE, PageExistence.PASSED);
    if (pageExistence != PageExistence.CREATED) {
      metricsCounters.increase(UpdateComponent.Counter.rDepthUp);
    }

    CounterUtils.increaseRDepth(depth, metricsCounters);
  }
}
