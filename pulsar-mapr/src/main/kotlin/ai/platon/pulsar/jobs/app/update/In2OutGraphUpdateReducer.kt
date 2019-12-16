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
import ai.platon.pulsar.common.CounterUtils
import ai.platon.pulsar.common.MetricsSystem
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.Urls.unreverseUrl
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.io.WebGraphWritable
import ai.platon.pulsar.persist.metadata.CrawlVariables
import ai.platon.pulsar.persist.metadata.Mark

internal class In2OutGraphUpdateReducer : AppContextAwareGoraReducer<GraphGroupKey, WebGraphWritable, String, GWebPage>() {
    private lateinit var webDb: WebDb
    private lateinit var metricsSystem: MetricsSystem
    private lateinit var updateComponent: UpdateComponent

    override fun setup(context: Context) {
        val crawlId = jobConf[CapabilityTypes.STORAGE_CRAWL_ID]
        metricsSystem = applicationContext.getBean(MetricsSystem::class.java)
        webDb = applicationContext.getBean(WebDb::class.java)
        updateComponent = applicationContext.getBean(UpdateComponent::class.java)
        Params.of(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId
        ).merge(updateComponent.params).withLogger(log).info()
    }

    override fun reduce(key: GraphGroupKey, values: Iterable<WebGraphWritable>, context: Context) {
        try {
            doReduce(key, values, context)
        } catch (e: Throwable) {
            log.error(StringUtil.stringifyException(e))
        }
    }

    private fun doReduce(key: GraphGroupKey, subGraphs: Iterable<WebGraphWritable>, context: Context) {
        metricsCounters.increase(CommonCounter.rRows)
        val reversedUrl = key.reversedUrl.toString()
        val url = unreverseUrl(reversedUrl)
        val graph = buildGraph(url, subGraphs)
        val page = graph.focus?.page
        if (page == null) {
            metricsCounters.increase(UpdateComponent.Companion.Counter.rNotExist)
            return
        }

        updateGraph(graph)
        if (page.hasMark(Mark.FETCH)) {
            updateComponent.updateFetchSchedule(page)
            CounterUtils.updateStatusCounter(page.crawlStatus, metricsCounters)
        }

        updateMetadata(page)
        updateMarks(page)

        context.write(reversedUrl, page.unbox())
        metricsSystem.report(page)
        metricsCounters.increase(CommonCounter.rPersist)
    }

    /**
     * The graph should be like this:
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
    private fun buildGraph(url: String, subGraphs: Iterable<WebGraphWritable>): WebGraph {
        val graph = WebGraph()
        val focus = WebVertex(url)

        for (graphWritable in subGraphs) {
            assert(graphWritable.optimizeMode == WebGraphWritable.OptimizeMode.IGNORE_SOURCE)
            val subGraph = graphWritable.graph
            subGraph.edgeSet().forEach {
                if (it.isLoop) {
                    focus.page = it.targetWebPage
                }
                graph.addEdgeLenient(focus, it.target, subGraph.getEdgeWeight(it))
            }
        }

        if (!focus.hasWebPage()) {
            // TODO: whether to ignore url query
            val page = webDb.get(url)
            // Page is always in the db, because it's the page who introduces this page
            if (page != null) {
                focus.page = page
                metricsCounters.increase(UpdateComponent.Companion.Counter.rLoaded)
            }
        } else {
            metricsCounters.increase(UpdateComponent.Companion.Counter.rPassed)
        }

        graph.focus = focus
        return graph
    }

    /**
     * Update vertices by outgoing edges
     *
     */
    private fun updateGraph(graph: WebGraph): Boolean {
        val focus = graph.focus
        val page = focus?.page?:return false
        var totalUpdates = 0
        for (outgoingEdge in graph.outgoingEdgesOf(focus)) {
            if (outgoingEdge.isLoop) {
                // a loop edge is a edge which has only one vertex
                continue
            }

            /* Update outlink page */
            val outgoingPage = outgoingEdge.targetWebPage?:return false
            val lastPageCounters = page.pageCounters.clone()

            updateComponent.updateByOutgoingPage(page, outgoingPage)
            updateComponent.updatePageCounters(lastPageCounters, page.pageCounters, page)

            if (outgoingPage.pageCategory.isDetail) {
                ++totalUpdates
            } else if (outgoingPage.pageCategory.isUnknown) {
                if (CrawlFilter.guessPageCategory(outgoingPage.url).isDetail) {
                    // TODO: make sure page category is calculated
                    ++totalUpdates
                }
            }
        }

        if (totalUpdates > 0) {
            metricsCounters.increase(UpdateComponent.Companion.Counter.rUpdated)
            metricsCounters.increase(UpdateComponent.Companion.Counter.rTotalUpdates, totalUpdates)

            return true
        }

        return false
    }

    private fun updateMetadata(page: WebPage) {
        // Clear temporary metadata
        page.metadata.remove(CrawlVariables.REDIRECT_DISCOVERED)
        page.metadata.remove(CapabilityTypes.GENERATE_TIME)
    }

    private fun updateMarks(page: WebPage) {
        val marks = page.marks
        marks.putIfNotNull(Mark.UPDATEING, marks[Mark.UPDATEOUTG])

        val retiredMarks = listOf(
                Mark.INJECT,
                Mark.GENERATE,
                Mark.FETCH,
                Mark.PARSE,
                Mark.INDEX,
                Mark.UPDATEOUTG
        )
        marks.removeAll(retiredMarks)
    }
}
