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
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.io.WebGraphWritable
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.io.IOException

internal class In2OutGraphUpdateMapper : AppContextAwareGoraMapper<String, GWebPage, GraphGroupKey, WebGraphWritable>() {
    val LOG = LoggerFactory.getLogger(In2OutGraphUpdateMapper::class.java)

    companion object {
        enum class Counter { mMapped, mNewMapped, mNoInLinks, mNotUpdated, mUrlFiltered, mTooDeep }
        init { MetricsCounters.register(Counter::class.java) }
    }

    private lateinit var graphGroupKey: GraphGroupKey
    private lateinit var webGraphWritable: WebGraphWritable

    public override fun setup(context: Context) {
        val crawlId = jobConf[CapabilityTypes.STORAGE_CRAWL_ID]
        graphGroupKey = GraphGroupKey()
        webGraphWritable = WebGraphWritable(null, WebGraphWritable.OptimizeMode.IGNORE_SOURCE, jobConf.unbox())
        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId
        ))
    }

    override fun map(reversedUrl: String, row: GWebPage, context: Context) {
        metricsCounters.increase(CommonCounter.mRows)

        val page = WebPage.box(reversedUrl, row, true)
        if (!shouldProcess(page)) {
            return
        }

        val graph = WebGraph()
        val v2 = WebVertex(page)
        /* A loop in the graph */
        graph.addEdgeLenient(v2, v2, page.score.toDouble())
        page.inlinks.forEach { (key, _) -> graph.addEdgeLenient(WebVertex(key), v2) }
        graph.incomingEdgesOf(v2).forEach { writeAsSubGraph(it, graph) }

        metricsCounters.increase(CommonCounter.mPersist)
        metricsCounters.increase(Counter.mNewMapped, graph.inDegreeOf(v2))
    }

    private fun shouldProcess(page: WebPage): Boolean {
        if (!page.hasMark(Mark.UPDATEOUTG)) {
            metricsCounters.increase(Counter.mNotUpdated)
            // return false;
        }
        if (page.inlinks.isEmpty()) {
            metricsCounters.increase(Counter.mNoInLinks)
        }
        return true
    }

    /**
     * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
     * The center vertex has a web page inside, and the edges has in-link information.
     *
     *
     * <pre>
     * v1
     * ^
     * |
     * v2 <- vc -> v3
     * / \
     * v  v
     * v4  v5
    </pre> *
     */
    private fun writeAsSubGraph(edge: WebEdge, graph: WebGraph) {
        try {
            val subGraph = WebGraph.of(edge, graph)
            // int score = graph.getEdgeWeight(edge) - edge.getTargetWebPage().getFetchPriority();
            graphGroupKey.reset(reverseUrl(edge.sourceUrl), graph.getEdgeWeight(edge))
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
