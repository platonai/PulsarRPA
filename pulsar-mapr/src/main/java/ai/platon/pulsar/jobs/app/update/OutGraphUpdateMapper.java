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

import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper;
import ai.platon.pulsar.persist.HypeLink;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.graph.GraphGroupKey;
import ai.platon.pulsar.persist.graph.WebEdge;
import ai.platon.pulsar.persist.graph.WebGraph;
import ai.platon.pulsar.persist.graph.WebVertex;
import ai.platon.pulsar.persist.io.WebGraphWritable;
import ai.platon.pulsar.persist.metadata.Mark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;

import static ai.platon.pulsar.common.CommonCounter.mPersist;
import static ai.platon.pulsar.common.CommonCounter.mRows;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.DISTANCE_INFINITE;
import static ai.platon.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_TARGET;

@Component
class OutGraphUpdateMapper extends AppContextAwareGoraMapper<String, GWebPage, GraphGroupKey, WebGraphWritable> {

  public static final Logger LOG = LoggerFactory.getLogger(OutGraphUpdateMapper.class);

  public enum Counter {mMapped, mNewMapped, mNotFetched, mNotParsed, mNoLinks, mTooDeep }
  static { MetricsCounters.register(Counter.class); }

  private ScoringFilters scoringFilters;

  private int maxDistance = DISTANCE_INFINITE;
  private int maxLiveLinks = 10000;
  private int limit = -1;
  private int count = 0;

  // Resue local variables for optimization
  private GraphGroupKey graphGroupKey;
  private WebGraphWritable webGraphWritable;

  @Override
  public void setup(Context context) throws IOException, InterruptedException {
    String crawlId = conf.get(STORAGE_CRAWL_ID);
    limit = conf.getInt(LIMIT, -1);

    maxDistance = conf.getUint(CRAWL_MAX_DISTANCE, DISTANCE_INFINITE);
    scoringFilters = getBean(ScoringFilters.class);

    graphGroupKey = new GraphGroupKey();
    webGraphWritable = new WebGraphWritable(null, IGNORE_TARGET, conf.unbox());

    LOG.info(Params.format(
        "className", this.getClass().getSimpleName(),
        "crawlId", crawlId,
        "maxDistance", maxDistance,
        "maxLiveLinks", maxLiveLinks,
        "ioSerialization", conf.get("io.serializations"),
        "limit", limit
    ));
  }

  @Override
  public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
    metricsCounters.increase(mRows);

    WebPage page = WebPage.box(reversedUrl, row, true);
    String url = page.getUrl();

    if (!shouldProcess(page)) {
      return;
    }

    WebGraph graph = new WebGraph();
    WebVertex v1 = new WebVertex(page);

    /* A loop in the graph */
    graph.addEdgeLenient(v1, v1, page.getScore());

    // Never create rows deeper then max distance, they can never be fetched
    if (page.getDistance() < maxDistance) {
      page.getLiveLinks().values().stream()
          .sorted(Comparator.comparingInt(GHypeLink::getOrder))
          .limit(maxLiveLinks)
          .forEach(l -> addEdge(v1, HypeLink.box(l), graph));
      scoringFilters.distributeScoreToOutlinks(page, graph, graph.outgoingEdgesOf(v1), graph.outDegreeOf(v1));
      metricsCounters.increase(Counter.mNewMapped, graph.outDegreeOf(v1) - 1);
    }

    graph.outgoingEdgesOf(v1).forEach(edge -> writeAsSubGraph(edge, graph));

    metricsCounters.increase(mPersist);
  }

  /**
   * Add a new WebEdge and set all inherited parameters from referrer page.
   * */
  private WebEdge addEdge(WebVertex v1, HypeLink link, WebGraph graph) {
    WebEdge edge = graph.addEdgeLenient(v1, new WebVertex(link.getUrl()));
    WebPage page = v1.getWebPage();

    // All inherited parameters from referrer page are set here
//    edge.putMetadata(SEED_OPTS, page.getOptions());
//    edge.putMetadata(ANCHOR_ORDER, l.getOrderAsString());
    edge.setOptions(page.getOptions());
    edge.setAnchor(link.getAnchor());
    edge.setOrder(link.getOrder());

    return edge;
  }

  private boolean shouldProcess(WebPage page) {
    if (!page.hasMark(Mark.FETCH)) {
      metricsCounters.increase(Counter.mNotFetched);
      return false;
    }

    if (!page.hasMark(Mark.PARSE)) {
      metricsCounters.increase(Counter.mNotParsed);
      return false;
    }

    if (page.getDistance() > maxDistance) {
      metricsCounters.increase(Counter.mTooDeep);
      return false;
    }

    if (limit > 0 && count++ > limit) {
      stop("Hit limit, stop");
      return false;
    }

    return true;
  }

  /**
   * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
   * The center vertex has a web page inside, and the edges has in-link information.
   * <p>
   * v1
   * |
   * v
   * v2 -> vc <- v3
   * ^ ^
   * /  \
   * v4  v5
   */
  private void writeAsSubGraph(WebEdge edge, WebGraph graph) {
    try {
      WebGraph subGraph = WebGraph.of(edge, graph);

      graphGroupKey.reset(Urls.reverseUrl(edge.getTargetUrl()), graph.getEdgeWeight(edge));
      webGraphWritable.reset(subGraph);
      // noinspection unchecked
      context.write(graphGroupKey, webGraphWritable);
    } catch (IOException | InterruptedException e) {
      LOG.error("Failed to write to hdfs. " + StringUtil.stringifyException(e));
    }
  }
}
