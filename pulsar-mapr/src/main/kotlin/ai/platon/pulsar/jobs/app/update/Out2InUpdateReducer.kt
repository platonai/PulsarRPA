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

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.Urls.unreverseUrl
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.UpdateComponent
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.schedule.FetchSchedule
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.graph.GraphGroupKey
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.io.WebGraphWritable
import ai.platon.pulsar.persist.metadata.CrawlVariables
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.util.*

class Out2InUpdateReducer : AppContextAwareGoraReducer<GraphGroupKey, WebGraphWritable, String, GWebPage>() {
    private val LOG = LoggerFactory.getLogger(Out2InUpdateReducer::class.java)

    private enum class PageExistence {
        PASSED, LOADED, CREATED
    }

    private lateinit var fetchSchedule: FetchSchedule
    private lateinit var scoringFilters: ScoringFilters
    private lateinit var updateComponent: UpdateComponent
    private lateinit var webDb: WebDb
    private lateinit var metricsSystem: MetricsSystem
    private var maxInLinks = 0
    private var ignoreInGraph = false

    override fun setup(context: Context) {
        fetchSchedule = applicationContext.getBean("fetchSchedule", FetchSchedule::class.java)
        scoringFilters = applicationContext.getBean(ScoringFilters::class.java)
        metricsSystem = applicationContext.getBean(MetricsSystem::class.java)
        webDb = applicationContext.getBean(WebDb::class.java)
        // Active counter registration
        updateComponent = applicationContext.getBean(UpdateComponent::class.java)

        // getPulsarReporter().setLog(LOG_ADDITIVITY);
        val crawlId = jobConf.get(CapabilityTypes.STORAGE_CRAWL_ID)
        maxInLinks = jobConf.getInt(CapabilityTypes.UPDATE_MAX_INLINKS, 1000)
        ignoreInGraph = jobConf.getBoolean(CapabilityTypes.UPDATE_IGNORE_IN_GRAPH, false)

        Params.of(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "maxLinks", maxInLinks,
                "fetchSchedule", fetchSchedule.javaClass.simpleName,
                "scoringFilters", scoringFilters.toString()
        ).withLogger(LOG).info()
    }

    override fun reduce(key: GraphGroupKey, values: Iterable<WebGraphWritable>, context: Context) {
        try {
            doReduce(key, values, context)
        } catch (e: Throwable) {
            LOG.error(StringUtil.stringifyException(e))
        }
    }

    /**
     * We get a list of score datum who are inlinks to a webpage after partition,
     * so the db-update phrase calculates the scores of all pages
     *
     *
     * The following graph shows the in-link graph. Every reduce group contains a center vertex and a batch of edges.
     * The center vertex has a web page inside, and the edges has in-link information.
     * <pre>
     * v1
     * |
     * v
     * v2 -> vc <- v3
     * ^ ^
     * /  \
     * v4  v5
    </pre> *
     * And this is a out-link graph:
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
    private fun doReduce(key: GraphGroupKey, subGraphs: Iterable<WebGraphWritable>, context: Context) {
        metricsCounters.increase(CommonCounter.rRows)
        val reversedUrl = key.reversedUrl
        val url = unreverseUrl(reversedUrl)
        val graph = buildGraph(url, subGraphs)
        val page = graph.focus.webPage ?: return
        if (page.url != url || page.key != reversedUrl) {
            LOG.error("Inconsistent url : $url")
            return
        }
        // 1. update depth, 2. update in-links, 3. update score from in-coming pages
        updateGraph(graph)

        updateMetadata(page)
        updateMarks(page)

        context.write(reversedUrl, page.unbox())
        metricsCounters.increase(CommonCounter.rPersist)
    }

    /**
     * The graph should be like this:
     * <pre>
     * v1
     * |
     * v
     * v2 -> TargetVertex <- v3
     * ^          ^
     * /           \
     * v4           v5
    </pre> *
     */
    private fun buildGraph(url: String, subGraphs: Iterable<WebGraphWritable>): WebGraph {
        val graph = WebGraph()
        val focus = WebVertex(url)
        for (graphWritable in subGraphs) {
            assert(graphWritable.optimizeMode == WebGraphWritable.OptimizeMode.IGNORE_TARGET)
            val subGraph = graphWritable.get()
            subGraph.edgeSet().forEach { edge: WebEdge ->
                if (edge.isLoop) {
                    focus.webPage = edge.sourceWebPage
                }
                // log.info("MultiMetadata " + url + "\t<-\t" + edge.getMetadata());
                graph.addEdgeLenient(edge.source, focus, subGraph.getEdgeWeight(edge)).metadata = edge.metadata
            }
        }

        if (focus.hasWebPage()) {
            focus.webPage.variables[PulsarParams.VAR_PAGE_EXISTENCE] = PageExistence.PASSED
            metricsCounters.increase(UpdateComponent.Companion.Counter.rPassed)
        } else {
            val page = loadOrCreateWebPage(url)
            focus.webPage = page
        }

        graph.focus = focus
        return graph
    }

    /**
     * Update vertices by incoming edges
     *
     * <pre>
     * v1
     * |
     * v
     * v2 -> TargetVertex <- v3
     * ^          ^
     * /           \
     * v4           v5
    </pre>
     *
     */
    private fun updateGraph(graph: WebGraph) {
        val focus = graph.focus
        val focusedPage = focus.webPage
        val incomingEdges = graph.incomingEdgesOf(focus)
        focusedPage.inlinks.clear()
        var smallestDepth = focusedPage.distance
        var shallowestEdge: WebEdge? = null
        val anchors: MutableSet<CharSequence> = HashSet()

        for (incomingEdge in incomingEdges) {
            if (incomingEdge.isLoop) {
                continue
            }

            // log.info(incomingEdge.toString());
            /* Update in-links */
            if (focusedPage.inlinks.size <= maxInLinks) {
                focusedPage.inlinks[incomingEdge.sourceUrl] = incomingEdge.anchor
            }

            if (incomingEdge.anchor.isNotEmpty() && anchors.size < 10) {
                anchors.add(incomingEdge.anchor)
            }

            val incomingPage = incomingEdge.sourceWebPage
            if (incomingPage.distance + 1 < smallestDepth) {
                smallestDepth = incomingPage.distance + 1
                shallowestEdge = incomingEdge
            }
        }

        if (focusedPage.distance != AppConstants.DISTANCE_INFINITE && smallestDepth < focusedPage.distance) {
            metricsSystem.debugDepthUpdated(focusedPage.distance.toString() + " -> " + smallestDepth + ", " + focus.url)
        }

        if (shallowestEdge != null) {
            val incomingPage = shallowestEdge.sourceWebPage
            focusedPage.referrer = incomingPage.url
            focusedPage.distance = smallestDepth
            // Do we have this field?
            // focusedPage.setOptions(incomingPage.getOptions());
            focusedPage.options = shallowestEdge.options
            focusedPage.anchor = shallowestEdge.anchor
            focusedPage.anchorOrder = shallowestEdge.order
            updateDepthCounter(smallestDepth, focusedPage)
        }

        // Anchor can be used to determine the article title
        if (anchors.isNotEmpty()) {
            focusedPage.setInlinkAnchors(anchors)
        }

        /* Update score */
        scoringFilters.updateScore(focusedPage, graph, incomingEdges)
    }

    /**
     * There are three cases of a page's state of existence
     * 1. the page have been fetched in this batch and so it's passed from the mapper
     * 2. the page have been fetched in other batch, so it's in the data store
     * 3. the page have not be fetched yet
     */
    private fun loadOrCreateWebPage(url: String): WebPage {
        // TODO: Is datastore.get a distributed operation? And is there a local cache?
        // TODO: distinct url and baseUrl, the url passed by OutGraphUpdateMapper might be the original baseUrl
        val loadedPage = webDb.get(url)
        val page: WebPage = loadedPage?:createNewRow(url)

        // Page is already in the db
        if (loadedPage != null) {
            page.variables[PulsarParams.VAR_PAGE_EXISTENCE] = PageExistence.LOADED
            metricsCounters.increase(UpdateComponent.Companion.Counter.rLoaded)
        } else { // Here we create a new web page from outlink
            page.variables[PulsarParams.VAR_PAGE_EXISTENCE] = PageExistence.CREATED
            metricsCounters.increase(UpdateComponent.Companion.Counter.rCreated)
            if (CrawlFilter.sniffPageCategory(url).isDetail) {
                metricsCounters.increase(UpdateComponent.Companion.Counter.rNewDetail)
            }
        }

        return page
    }

    private fun createNewRow(url: String): WebPage {
        val page = WebPage.newWebPage(url)
        scoringFilters.initialScore(page)
        fetchSchedule.initializeSchedule(page)
        return page
    }

    private fun updateMarks(page: WebPage) {
        val marks = page.marks
        marks.putIfNotNull(Mark.UPDATEOUTG, marks[Mark.PARSE])

        if (ignoreInGraph) {
            val retiredMarks = listOf(
                    Mark.INJECT,
                    Mark.GENERATE,
                    Mark.FETCH,
                    Mark.PARSE,
                    Mark.INDEX
            )
            marks.removeAll(retiredMarks)
        }
    }

    private fun updateMetadata(page: WebPage) {
        if (ignoreInGraph) {
            // Clear temporary metadata
            page.metadata.remove(CrawlVariables.REDIRECT_DISCOVERED)
            page.metadata.remove(CapabilityTypes.GENERATE_TIME)
        }
    }

    private fun updateDepthCounter(depth: Int, page: WebPage) {
        val pageExistence = page.variables.get(PulsarParams.VAR_PAGE_EXISTENCE, PageExistence.PASSED)
        if (pageExistence != PageExistence.CREATED) {
            metricsCounters.increase(UpdateComponent.Companion.Counter.rDepthUp)
        }
        CounterUtils.increaseRDepth(depth, metricsCounters)
    }
}
