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
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper;
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

import static ai.platon.pulsar.common.CommonCounter.mPersist;
import static ai.platon.pulsar.common.CommonCounter.mRows;
import static ai.platon.pulsar.common.config.CapabilityTypes.STORAGE_CRAWL_ID;
import static ai.platon.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_SOURCE;
import static ai.platon.pulsar.persist.metadata.Mark.UPDATEOUTG;

class InGraphUpdateMapper extends AppContextAwareGoraMapper<String, GWebPage, GraphGroupKey, WebGraphWritable> {

  public static final Logger LOG = LoggerFactory.getLogger(InGraphUpdateMapper.class);

  public enum Counter {mMapped, mNewMapped, mNoInLinks, mNotUpdated, mUrlFiltered, mTooDeep }
  static { MetricsCounters.register(Counter.class); }

  private GraphGroupKey graphGroupKey;
  private WebGraphWritable webGraphWritable;

  @Override
  public void setup(Context context) throws IOException, InterruptedException {
    String crawlId = conf.get(STORAGE_CRAWL_ID);

    graphGroupKey = new GraphGroupKey();
    webGraphWritable = new WebGraphWritable(null, IGNORE_SOURCE, conf.unbox());

    LOG.info(Params.format(
      "className", this.getClass().getSimpleName(),
      "crawlId", crawlId
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
    WebVertex v2 = new WebVertex(page);

    /* A loop in the graph */
    graph.addEdgeLenient(v2, v2, page.getScore());

    page.getInlinks().forEach((key, value) -> graph.addEdgeLenient(new WebVertex(key), v2));
    graph.incomingEdgesOf(v2).forEach(edge -> writeAsSubGraph(edge, graph));

    metricsCounters.increase(mPersist);
    metricsCounters.increase(Counter.mNewMapped, graph.inDegreeOf(v2));
  }

  private boolean shouldProcess(WebPage page) {
    if (!page.hasMark(UPDATEOUTG)) {
      metricsCounters.increase(Counter.mNotUpdated);
      // return false;
    }

    if (page.getInlinks().isEmpty()) {
      metricsCounters.increase(Counter.mNoInLinks);
    }

    return true;
  }

  /**
   * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
   * The center vertex has a web page inside, and the edges has in-link information.
   * <p>
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
  private void writeAsSubGraph(WebEdge edge, WebGraph graph) {
    try {
      WebGraph subGraph = WebGraph.of(edge, graph);

      // int score = graph.getEdgeWeight(edge) - edge.getTargetWebPage().getFetchPriority();
      graphGroupKey.reset(Urls.reverseUrl(edge.getSourceUrl()), graph.getEdgeWeight(edge));
      webGraphWritable.reset(subGraph);

      // noinspection unchecked
      context.write(graphGroupKey, webGraphWritable);
    } catch (IOException | InterruptedException e) {
      LOG.error("Failed to write to hdfs. " + StringUtil.stringifyException(e));
    }
  }
}
