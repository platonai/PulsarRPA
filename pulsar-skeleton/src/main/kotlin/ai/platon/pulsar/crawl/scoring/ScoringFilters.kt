/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class ScoringFilters(scoringFilters: List<ScoringFilter> = emptyList(), val conf: ImmutableConfig) : ScoringFilter {
    private val scoringFilters = ArrayList<ScoringFilter>()

    init {
        this.scoringFilters.addAll(scoringFilters)
    }

    constructor(conf: ImmutableConfig) : this(emptyList(), conf)

    override fun getParams(): Params {
        return Params().merge(scoringFilters.map { it.params })
    }

    /**
     * Calculate a sort value for Generate.
     */
    override fun generatorSortValue(page: WebPage, initSort: Float): ScoreVector {
        var score = ScoreVector(0)
        for (filter in scoringFilters) {
            score = filter.generatorSortValue(page, initSort)
        }
        return score
    }

    /**
     * Calculate a new initial score, used when adding newly discovered pages.
     */
    override fun initialScore(page: WebPage) {
        for (filter in scoringFilters) {
            filter.initialScore(page)
        }
    }

    /**
     * Calculate a new initial score, used when injecting new pages.
     */
    override fun injectedScore(page: WebPage) {
        for (filter in scoringFilters) {
            filter.injectedScore(page)
        }
    }

    override fun distributeScoreToOutlinks(page: WebPage, graph: WebGraph, outLinkEdges: Collection<WebEdge>, allCount: Int) {
        for (filter in scoringFilters) {
            filter.distributeScoreToOutlinks(page, graph, outLinkEdges, allCount)
        }
    }

    override fun updateScore(page: WebPage, graph: WebGraph, inLinkEdges: Collection<WebEdge>) {
        for (filter in scoringFilters) {
            filter.updateScore(page, graph, inLinkEdges)
        }
    }

    override fun updateContentScore(page: WebPage) {
        for (filter in scoringFilters) {
            filter.updateContentScore(page)
        }
    }

    override fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        var score = initScore
        for (filter in scoringFilters) {
            score = filter.indexerScore(url, doc, page, score)
        }
        return score
    }

    override fun toString(): String {
        return scoringFilters.joinToString { it.javaClass.simpleName }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(ScoringFilters::class.java)
    }
}