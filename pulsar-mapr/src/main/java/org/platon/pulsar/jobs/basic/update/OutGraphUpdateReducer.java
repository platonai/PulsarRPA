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
package org.platon.pulsar.jobs.basic.update;

import ai.platon.pulsar.common.CounterUtils;
import ai.platon.pulsar.common.MetricsCounters;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.UrlUtil;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.graph.GraphGroupKey;
import ai.platon.pulsar.persist.graph.WebEdge;
import ai.platon.pulsar.persist.graph.WebGraph;
import ai.platon.pulsar.persist.graph.WebVertex;
import ai.platon.pulsar.persist.io.WebGraphWritable;
import ai.platon.pulsar.crawl.schedule.DefaultFetchSchedule;
import ai.platon.pulsar.crawl.schedule.FetchSchedule;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import org.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static ai.platon.pulsar.common.CommonCounter.rPersist;
import static ai.platon.pulsar.common.CommonCounter.rRows;
import static ai.platon.pulsar.persist.io.WebGraphWritable.OptimizeMode.IGNORE_TARGET;
import static ai.platon.pulsar.persist.metadata.Mark.PARSE;
import static ai.platon.pulsar.persist.metadata.Mark.UPDATEOUTG;

public class OutGraphUpdateReducer extends AppContextAwareGoraReducer<GraphGroupKey, WebGraphWritable, String, GWebPage> {

    public static final Logger LOG = LoggerFactory.getLogger(OutGraphUpdateReducer.class);

    static {
        MetricsCounters.register(Counter.class);
    }

    private FetchSchedule fetchSchedule = new DefaultFetchSchedule();
    private ScoringFilters scoringFilters = new ScoringFilters();
    private WebDb webDb;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        webDb = applicationContext.getBean(WebDb.class);

        Params.of(
                "className", this.getClass().getSimpleName(),
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
        String url = UrlUtil.unreverseUrl(reversedUrl);

        WebGraph graph = buildGraph(url, reversedUrl, subGraphs);
        WebPage page = graph.getFocus().getWebPage();

        if (page == null) {
            return;
        }

        updateGraph(graph);
        page.getMarks().putIfNonNull(UPDATEOUTG, page.getMarks().get(PARSE));

        context.write(reversedUrl, page.unbox());
        metricsCounters.increase(rPersist);
        CounterUtils.increaseRDepth(page.getDistance(), metricsCounters);
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
    private WebGraph buildGraph(String url, String reversedUrl, Iterable<WebGraphWritable> subGraphs) {
        WebGraph graph = new WebGraph();

        WebVertex focus = new WebVertex(url);
        for (WebGraphWritable graphWritable : subGraphs) {
            assert (graphWritable.getOptimizeMode().equals(IGNORE_TARGET));

            WebGraph subGraph = graphWritable.get();
            subGraph.edgeSet().forEach(edge -> {
                if (edge.isLoop()) {
                    focus.setWebPage(edge.getSourceWebPage());
                }

                // log.info("Metadata " + url + "\t<-\t" + edge.getMetadata());
                graph.addEdgeLenient(edge.getSource(), focus, subGraph.getEdgeWeight(edge));
            });
        }

        if (!focus.hasWebPage()) {
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

        for (WebEdge incomingEdge : incomingEdges) {
            if (incomingEdge.isLoop()) {
                continue;
            }

            /* Update in-links */
            focusedPage.getInlinks().put(incomingEdge.getSourceUrl(), incomingEdge.getAnchor());

            WebPage incomingPage = incomingEdge.getSourceWebPage();
            if (incomingPage.getDistance() + 1 < smallestDepth) {
                smallestDepth = incomingPage.getDistance() + 1;
                shallowestEdge = incomingEdge;
            }
        }

        if (shallowestEdge != null) {
            WebPage incomingPage = shallowestEdge.getSourceWebPage();

            focusedPage.setReferrer(incomingPage.getUrl());
            focusedPage.setDistance(incomingPage.getDistance() + 1);

            focusedPage.setAnchor(shallowestEdge.getAnchor());
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
        WebPage loadedPage = webDb.getOrNil(url);
        WebPage page;

        // Page is already in the db
        if (!loadedPage.isNil()) {
            page = loadedPage;
        } else {
            // We have never seen this url, create a new web page
            page = createNewRow(url);
            metricsCounters.increase(Counter.rCreated);
        }

        return page;
    }

    private WebPage createNewRow(String url) {
        WebPage page = WebPage.newWebPage(url);
        fetchSchedule.initializeSchedule(page);
        scoringFilters.initialScore(page);
        return page;
    }

    public enum Counter {rCreated}
}
