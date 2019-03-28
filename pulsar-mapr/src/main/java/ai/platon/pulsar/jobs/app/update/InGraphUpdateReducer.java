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
import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.component.UpdateComponent;
import ai.platon.pulsar.crawl.filter.CrawlFilter;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import ai.platon.pulsar.persist.CrawlMarks;
import ai.platon.pulsar.persist.PageCounters;
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

import static ai.platon.pulsar.common.CommonCounter.rPersist;
import static ai.platon.pulsar.common.CommonCounter.rRows;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_SOURCE;
import static ai.platon.pulsar.persist.metadata.CrawlVariables.REDIRECT_DISCOVERED;
import static ai.platon.pulsar.persist.metadata.Mark.*;

class InGraphUpdateReducer extends AppContextAwareGoraReducer<GraphGroupKey, WebGraphWritable, String, GWebPage> {

  public static final Logger LOG = LoggerFactory.getLogger(InGraphUpdateReducer.class);

  private WebDb webDb;
  private MetricsSystem metricsSystem;
  private UpdateComponent updateComponent;

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    String crawlId = conf.get(STORAGE_CRAWL_ID);

    metricsSystem = applicationContext.getBean(MetricsSystem.class);
    webDb = applicationContext.getBean(WebDb.class);
    updateComponent = applicationContext.getBean(UpdateComponent.class);

    Params.of(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId
    ).merge(updateComponent.getParams()).withLogger(LOG).info();
  }

  @Override
  protected void reduce(GraphGroupKey key, Iterable<WebGraphWritable> values, Context context) {
    try {
      doReduce(key, values, context);
    } catch (Throwable e) {
      LOG.error(StringUtil.stringifyException(e));
    }
  }

  private void doReduce(GraphGroupKey key, Iterable<WebGraphWritable> subGraphs, Context context)
          throws IOException, InterruptedException {
    metricsCounters.increase(rRows);

    String reversedUrl = key.getReversedUrl();
    String url = UrlUtil.unreverseUrl(reversedUrl);

    WebGraph graph = buildGraph(url, subGraphs);
    WebPage page = graph.getFocus().getWebPage();

    if (page == null) {
      metricsCounters.increase(UpdateComponent.Counter.rNotExist);
      return;
    }

    updateGraph(graph);
    if (page.hasMark(FETCH)) {
      updateComponent.updateFetchSchedule(page);
      CounterUtils.updateStatusCounter(page.getCrawlStatus(), metricsCounters);
    }
    updateMetadata(page);
    updateMarks(page);

    context.write(reversedUrl, page.unbox());

    metricsSystem.report(page);
    metricsCounters.increase(rPersist);
  }

  /**
   * The graph should be like this:
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
  private WebGraph buildGraph(String url, Iterable<WebGraphWritable> subGraphs) {
    WebGraph graph = new WebGraph();

    WebVertex focus = new WebVertex(url);
    for (WebGraphWritable graphWritable : subGraphs) {
      assert (graphWritable.getOptimizeMode().equals(IGNORE_SOURCE));

      WebGraph subGraph = graphWritable.get();
      subGraph.edgeSet().forEach(edge -> {
        if (edge.isLoop()) {
          focus.setWebPage(edge.getTargetWebPage());
        }
        graph.addEdgeLenient(focus, edge.getTarget(), subGraph.getEdgeWeight(edge));
      });
    }

    if (!focus.hasWebPage()) {
      WebPage page = webDb.get(url);

      // Page is always in the db, because it's the page who introduces this page
      if (page != null) {
        focus.setWebPage(page);
        metricsCounters.increase(UpdateComponent.Counter.rLoaded);
      }
    }
    else {
      metricsCounters.increase(UpdateComponent.Counter.rPassed);
    }

    graph.setFocus(focus);

    return graph;
  }

  private boolean updateGraph(WebGraph graph) {
    WebVertex focus = graph.getFocus();
    WebPage page = focus.getWebPage();

    int totalUpdates = 0;
    for (WebEdge outgoingEdge : graph.outgoingEdgesOf(focus)) {
      if (outgoingEdge.isLoop()) {
        continue;
      }

      /* Update outlink page */

      WebPage outgoingPage = outgoingEdge.getTargetWebPage();

      PageCounters lastPageCounters = page.getPageCounters().clone();
      updateComponent.updateByOutgoingPage(page, outgoingPage);
      updateComponent.updatePageCounters(lastPageCounters, page.getPageCounters(), page);

      if (outgoingPage.getPageCategory().isDetail() || CrawlFilter.sniffPageCategory(outgoingPage.getUrl()).isDetail()) {
        ++totalUpdates;
      }
    }

    if (totalUpdates > 0) {
      metricsCounters.increase(UpdateComponent.Counter.rUpdated);
      metricsCounters.increase(UpdateComponent.Counter.rTotalUpdates, totalUpdates);

      return true;
    }

    return false;
  }

  private void updateMetadata(WebPage page) {
    // Clear temporary metadata
    page.getMetadata().remove(REDIRECT_DISCOVERED);
    page.getMetadata().remove(GENERATE_TIME);
  }

  private void updateMarks(WebPage page) {
    CrawlMarks marks = page.getMarks();
    marks.putIfNonNull(UPDATEING, marks.get(UPDATEOUTG));
    marks.remove(INJECT);
    marks.remove(GENERATE);
    marks.remove(FETCH);
    marks.remove(PARSE);
    marks.remove(INDEX);
    marks.remove(UPDATEOUTG);
  }
}
