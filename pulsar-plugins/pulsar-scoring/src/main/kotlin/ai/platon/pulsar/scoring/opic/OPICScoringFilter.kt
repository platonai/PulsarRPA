package ai.platon.pulsar.scoring.opic

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.skeleton.crawl.scoring.ScoringFilter
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.pow

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

/**
 * This plugin implements a variant of an Online Page Importance Computation
 * (OPIC) score, described in this paper: [](http://www2003.org/cdrom/papers/refereed/p007/p7-abiteboul.html)
 * Abiteboul, Serge and Preda, Mihai and Cobena, Gregory (2003), Adaptive
 * On-Line Page Importance Computation .
 *
 * @author Andrzej Bialecki
 */
class OPICScoringFilter(val conf: ImmutableConfig) : ScoringFilter {
    private val scorePower = conf.getFloat("index.score.power", 0.5f)
    private val internalScoreFactor = conf.getFloat("db.score.link.internal", 1.0f)
    private val externalScoreFactor = conf.getFloat("db.score.link.external", 1.0f)

    override fun getParams(): Params {
        return Params.of(
                "scorePower", scorePower,
                "internalScoreFactor", internalScoreFactor,
                "externalScoreFactor", externalScoreFactor
        )
    }

    override fun injectedScore(page: WebPage) {
        val score = page.score
        page.cash = score
    }

    /**
     * Set to 0.0f (unknown value) - inlink contributions will bring it to a
     * correct level. Newly discovered pages have at least one inlink.
     */
    override fun initialScore(page: WebPage) {
        page.score = 0.0f
        page.cash = 0.0f
    }

    /**
     * Increase the score by a sum of inlinked scores.
     */
    override fun updateScore(page: WebPage, graph: WebGraph, incomingEdges: Collection<WebEdge>) {
        val score = incomingEdges.sumOf { if (it.isLoop) 0.0 else graph.getEdgeWeight(it) }.toFloat()
        page.score = page.score + score
        page.cash = page.cash + score
    }

    /**
     * Get cash on hand, divide it by the number of outlinks and apply.
     */
    override fun distributeScoreToOutlinks(page: WebPage, graph: WebGraph, outgoingEdges: Collection<WebEdge>, allCount: Int) {
        val cash = page.cash
        if (cash == 0f) {
            return
        }
        // TODO: count filtered vs. all count for outlinks
        val scoreUnit = cash / allCount
        // internal and external score factor
        val internalScore = scoreUnit * internalScoreFactor
        val externalScore = scoreUnit * externalScoreFactor
        for (edge in outgoingEdges) {
            if (edge.isLoop) {
                continue
            }
            val score = graph.getEdgeWeight(edge)
            try {
                val toHost = URL(edge.targetUrl).host
                val fromHost = URL(page.url).host
                if (toHost.equals(fromHost, ignoreCase = true)) {
                    graph.setEdgeWeight(edge, score + internalScore)
                } else {
                    graph.setEdgeWeight(edge, score + externalScore)
                }
            } catch (e: MalformedURLException) {
                ScoringFilter.LOG.error("Failed to distribute score ...$e")
                graph.setEdgeWeight(edge, score + externalScore)
            }
        }
        page.cash = 0.0f
    }

    /**
     * Dampen the boost value by scorePower.
     */
    override fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        return page.score.toDouble().pow(scorePower.toDouble()).toFloat() * initScore
    }

}
