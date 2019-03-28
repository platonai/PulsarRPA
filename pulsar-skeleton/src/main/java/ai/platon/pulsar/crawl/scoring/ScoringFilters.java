/**
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

package ai.platon.pulsar.crawl.scoring;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.graph.WebEdge;
import ai.platon.pulsar.persist.graph.WebGraph;
import ai.platon.pulsar.crawl.index.IndexDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScoringFilters implements ScoringFilter {

    public static final Logger LOG = LoggerFactory.getLogger(ScoringFilters.class);

    private ImmutableConfig conf;
    private ArrayList<ScoringFilter> scoringFilters = new ArrayList<>();

    public ScoringFilters() {
        this(Collections.emptyList(), null);
    }

    public ScoringFilters(ImmutableConfig conf) {
        this(Collections.emptyList(), conf);
    }

    public ScoringFilters(List<ScoringFilter> scoringFilters, ImmutableConfig conf) {
        this.scoringFilters.addAll(scoringFilters);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
    }

    @Override
    public Params getParams() {
        return new Params().merge(scoringFilters.stream().map(ScoringFilter::getParams).collect(Collectors.toList()));
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    /**
     * Calculate a sort value for Generate.
     */
    @Override
    public ScoreVector generatorSortValue(WebPage row, float initSort) {
        ScoreVector score = new ScoreVector(0);
        for (ScoringFilter filter : scoringFilters) {
            score = filter.generatorSortValue(row, initSort);
        }
        return score;
    }

    /**
     * Calculate a new initial score, used when adding newly discovered pages.
     */
    @Override
    public void initialScore(WebPage page) {
        for (ScoringFilter filter : scoringFilters) {
            filter.initialScore(page);
        }
    }

    /**
     * Calculate a new initial score, used when injecting new pages.
     */
    @Override
    public void injectedScore(WebPage page) {
        for (ScoringFilter filter : scoringFilters) {
            filter.injectedScore(page);
        }
    }

    @Override
    public void distributeScoreToOutlinks(WebPage page, WebGraph graph, Collection<WebEdge> outLinkEdges, int allCount) {
        for (ScoringFilter filter : scoringFilters) {
            filter.distributeScoreToOutlinks(page, graph, outLinkEdges, allCount);
        }
    }

    @Override
    public void updateScore(WebPage page, WebGraph graph, Collection<WebEdge> inLinkEdges) {
        for (ScoringFilter filter : scoringFilters) {
            filter.updateScore(page, graph, inLinkEdges);
        }
    }

    @Override
    public void updateContentScore(WebPage page) {
        for (ScoringFilter filter : scoringFilters) {
            filter.updateContentScore(page);
        }
    }

    @Override
    public float indexerScore(String url, IndexDocument doc, WebPage page, float initScore) {
        for (ScoringFilter filter : scoringFilters) {
            initScore = filter.indexerScore(url, doc, page, initScore);
        }
        return initScore;
    }

    @Override
    public String toString() {
        return scoringFilters.stream().map(n -> n.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }
}
