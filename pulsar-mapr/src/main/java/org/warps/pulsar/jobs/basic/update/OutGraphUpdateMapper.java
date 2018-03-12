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
package org.warps.pulsar.jobs.basic.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.StringUtil;
import org.warps.pulsar.common.UrlUtil;
import org.warps.pulsar.crawl.scoring.ScoringFilters;
import org.warps.pulsar.jobs.core.GoraMapper;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.gora.generated.GWebPage;
import org.warps.pulsar.persist.graph.GraphGroupKey;
import org.warps.pulsar.persist.graph.WebEdge;
import org.warps.pulsar.persist.graph.WebGraph;
import org.warps.pulsar.persist.graph.WebVertex;
import org.warps.pulsar.persist.io.WebGraphWritable;

import java.io.IOException;

import static org.warps.pulsar.common.CommonCounter.mPersist;
import static org.warps.pulsar.common.CommonCounter.mRows;
import static org.warps.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_TARGET;

class OutGraphUpdateMapper extends GoraMapper<String, GWebPage, GraphGroupKey, WebGraphWritable> {

    public static final Logger LOG = LoggerFactory.getLogger(OutGraphUpdateMapper.class);

    private ScoringFilters scoringFilters = new ScoringFilters();

    // Resue local variables for optimization
    private GraphGroupKey graphGroupKey;
    private WebGraphWritable webGraphWritable;

    @Override
    public void setup(Context context) throws IOException, InterruptedException {
        graphGroupKey = new GraphGroupKey();
        webGraphWritable = new WebGraphWritable(null, IGNORE_TARGET, conf.unbox());
//    scoringFilters = applicationContext.getBean(ScoringFilters.class);
    }

    @Override
    public void map(String reversedUrl, GWebPage row, Context context) throws IOException, InterruptedException {
        metricsCounters.increase(mRows);

        WebPage page = WebPage.box(reversedUrl, row, true);
        String url = page.getUrl();

        WebGraph graph = new WebGraph();
        WebVertex v1 = new WebVertex(page);

        /* A loop edge in the graph */
        graph.addEdgeLenient(v1, v1, page.getScore());

        page.getLiveLinks().forEach((k, v) -> graph.addEdgeLenient(v1, new WebVertex(k))
                .setAnchor(v.getAnchor()));

        scoringFilters.distributeScoreToOutlinks(page, graph, graph.outgoingEdgesOf(v1), graph.outDegreeOf(v1));

        graph.outgoingEdgesOf(v1).forEach(edge -> writeAsSubGraph(edge, graph));

        metricsCounters.increase(mPersist);
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

            graphGroupKey.reset(UrlUtil.reverseUrl(edge.getTargetUrl()), graph.getEdgeWeight(edge));
            webGraphWritable.reset(subGraph);
            // noinspection unchecked
            context.write(graphGroupKey, webGraphWritable);
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to write to hdfs. " + StringUtil.stringifyException(e));
        }
    }
}
