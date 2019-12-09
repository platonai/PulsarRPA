/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.jobs.app.update

import ai.platon.pulsar.common.CommonCounter
import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls.reverseUrl
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.io.WebGraphWritable
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.function.Consumer

internal class OutGraphUpdateMapper : AppContextAwareGoraMapper<String, GWebPage, GraphGroupKey, WebGraphWritable>() {
    val LOG = LoggerFactory.getLogger(OutGraphUpdateMapper::class.java)

    companion object {
        enum class Counter {mMapped, mNewMapped, mNotFetched, mNotParsed, mNoLinks, mTooDeep}
        init { MetricsCounters.register(Counter::class.java) }
    }

    private var maxDistance = PulsarConstants.DISTANCE_INFINITE
    private val maxLiveLinks = 10000
    private var limit = -1
    private var count = 0
    private lateinit var scoringFilters: ScoringFilters

    // Reuse local variables for optimization
    private lateinit var graphGroupKey: GraphGroupKey
    private lateinit var webGraphWritable: WebGraphWritable

    public override fun setup(context: Context) {
        val crawlId = jobConf[CapabilityTypes.STORAGE_CRAWL_ID]
        limit = jobConf.getInt(CapabilityTypes.LIMIT, -1)
        maxDistance = jobConf.getUint(CapabilityTypes.CRAWL_MAX_DISTANCE, PulsarConstants.DISTANCE_INFINITE)
        scoringFilters = getBean(ScoringFilters::class.java)
        graphGroupKey = GraphGroupKey()
        webGraphWritable = WebGraphWritable(null, WebGraphWritable.OptimizeMode.IGNORE_TARGET, jobConf.unbox())

        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "maxDistance", maxDistance,
                "maxLiveLinks", maxLiveLinks,
                "ioSerialization", jobConf["io.serializations"],
                "limit", limit
        ))
    }

    override fun map(reversedUrl: String, row: GWebPage, context: Context) {
        metricsCounters.increase(CommonCounter.mRows)
        val page = WebPage.box(reversedUrl, row, true)
        val url = page.url
        if (!shouldProcess(page)) {
            return
        }

        val graph = WebGraph()
        val v1 = WebVertex(page)
        /* A loop in the graph */
        graph.addEdgeLenient(v1, v1, page.score.toDouble())
        // Never create rows deeper then max distance, they can never be fetched
        if (page.distance < maxDistance) {
            page.liveLinks.values.sortedBy { it.order }.take(maxLiveLinks).forEach {
                addEdge(v1, HypeLink.box(it), graph)
            }
            scoringFilters.distributeScoreToOutlinks(page, graph, graph.outgoingEdgesOf(v1), graph.outDegreeOf(v1))
            metricsCounters.increase(Counter.mNewMapped, graph.outDegreeOf(v1) - 1)
        }

        graph.outgoingEdgesOf(v1).forEach { writeAsSubGraph(it, graph) }
        metricsCounters.increase(CommonCounter.mPersist)
    }

    /**
     * Add a new WebEdge and set all inherited parameters from referrer page.
     */
    private fun addEdge(v1: WebVertex, link: HypeLink, graph: WebGraph): WebEdge {
        val edge = graph.addEdgeLenient(v1, WebVertex(link.url))
        val page = v1.webPage
        // All inherited parameters from referrer page are set here
        // edge.putMetadata(SEED_OPTS, page.getOptions());
        // edge.putMetadata(ANCHOR_ORDER, l.getOrderAsString());
        edge.options = page.options
        edge.anchor = link.anchor
        edge.order = link.order
        return edge
    }

    private fun shouldProcess(page: WebPage): Boolean {
        if (!page.hasMark(Mark.FETCH)) {
            metricsCounters.increase(Counter.mNotFetched)
            return false
        }
        if (!page.hasMark(Mark.PARSE)) {
            metricsCounters.increase(Counter.mNotParsed)
            return false
        }
        if (page.distance > maxDistance) {
            metricsCounters.increase(Counter.mTooDeep)
            return false
        }
        if (limit > 0 && count++ > limit) {
            stop("Hit limit, stop")
            return false
        }
        return true
    }

    /**
     * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
     * The center vertex has a web page inside, and the edges has in-link information.
     *
     * v1
     * |
     * v
     * v2 -> vc <- v3
     * ^ ^
     * /  \
     * v4  v5
     */
    private fun writeAsSubGraph(edge: WebEdge, graph: WebGraph) {
        try {
            val subGraph = WebGraph.of(edge, graph)
            graphGroupKey.reset(reverseUrl(edge.targetUrl), graph.getEdgeWeight(edge))
            webGraphWritable.reset(subGraph)
            // noinspection unchecked
            context.write(graphGroupKey, webGraphWritable)
        } catch (e: IOException) {
            LOG.error("Failed to write to hdfs. " + StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            LOG.error("Failed to write to hdfs. " + StringUtil.stringifyException(e))
        }
    }
}
