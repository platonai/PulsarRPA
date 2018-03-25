package fun.platonic.pulsar.scoring.opic; /**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.crawl.index.IndexDocument;
import fun.platonic.pulsar.crawl.scoring.ScoreVector;
import fun.platonic.pulsar.crawl.scoring.ScoringFilter;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.graph.WebEdge;
import fun.platonic.pulsar.persist.graph.WebGraph;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * This plugin implements a variant of an Online Page Importance Computation
 * (OPIC) score, described in this paper: <a
 * href="http://www2003.org/cdrom/papers/refereed/p007/p7-abiteboul.html"/>
 * Abiteboul, Serge and Preda, Mihai and Cobena, Gregory (2003), Adaptive
 * On-Line Page Importance Computation </a>.
 *
 * @author Andrzej Bialecki
 */
public class OPICScoringFilter implements ScoringFilter {

    private ImmutableConfig conf;
    private float scorePower;
    private float internalScoreFactor;
    private float externalScoreFactor;

    public OPICScoringFilter() {
    }

    public OPICScoringFilter(ImmutableConfig conf) {
        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        scorePower = conf.getFloat("index.score.power", 0.5f);
        internalScoreFactor = conf.getFloat("db.score.link.internal", 1.0f);
        externalScoreFactor = conf.getFloat("db.score.link.external", 1.0f);
    }

    public Params getParams() {
        return Params.of(
                "scorePower", scorePower,
                "internalScoreFactor", internalScoreFactor,
                "externalScoreFactor", externalScoreFactor
        );
    }

    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public void injectedScore(WebPage page) {
        float score = page.getScore();
        page.setCash(score);
    }

    /**
     * Set to 0.0f (unknown value) - inlink contributions will bring it to a
     * correct level. Newly discovered pages have at least one inlink.
     */
    @Override
    public void initialScore(WebPage row) {
        row.setScore(0.0f);
        row.setCash(0.0f);
    }

    /**
     * Use {@link WebPage#getScore()}.
     */
    @Override
    public ScoreVector generatorSortValue(WebPage row, float initSort) {
        return new ScoreVector("1", (int) (initSort * row.getScore()));
    }

    /**
     * Increase the score by a sum of inlinked scores.
     */
    @Override
    public void updateScore(WebPage page, WebGraph graph, Collection<WebEdge> inLinkEdges) {
        float inLinkScore = 0.0f;
        for (WebEdge edge : inLinkEdges) {
            if (!edge.isLoop()) {
                inLinkScore += graph.getEdgeWeight(edge);
            }
        }

        page.setScore(page.getScore() + inLinkScore);
        page.setCash(page.getCash() + inLinkScore);
    }

    /**
     * Get cash on hand, divide it by the number of outlinks and apply.
     */
    @Override
    public void distributeScoreToOutlinks(WebPage page, WebGraph graph, Collection<WebEdge> outgoingEdges, int allCount) {
        float cash = page.getCash();
        if (cash == 0) {
            return;
        }

        // TODO: count filtered vs. all count for outlinks
        float scoreUnit = cash / allCount;
        // internal and external score factor
        float internalScore = scoreUnit * internalScoreFactor;
        float externalScore = scoreUnit * externalScoreFactor;
        for (WebEdge edge : outgoingEdges) {
            if (edge.isLoop()) {
                continue;
            }

            double score = graph.getEdgeWeight(edge);

            try {
                String toHost = new URL(edge.getTargetUrl()).getHost();
                String fromHost = new URL(page.getUrl()).getHost();

                if (toHost.equalsIgnoreCase(fromHost)) {
                    graph.setEdgeWeight(edge, score + internalScore);
                } else {
                    graph.setEdgeWeight(edge, score + externalScore);
                }
            } catch (MalformedURLException e) {
                LOG.error("Failed to distribute score ..." + e.toString());
                graph.setEdgeWeight(edge, score + externalScore);
            }
        }

        page.setCash(0.0f);
    }

    /**
     * Dampen the boost value by scorePower.
     */
    public float indexerScore(String url, IndexDocument doc, WebPage row, float initScore) {
        return (float) Math.pow(row.getScore(), scorePower) * initScore;
    }
}
