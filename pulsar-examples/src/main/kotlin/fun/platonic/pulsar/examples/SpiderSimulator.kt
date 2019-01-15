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

import `fun`.platonic.pulsar.common.MetricsSystem
import `fun`.platonic.pulsar.common.config.PulsarConstants.DISTANCE_INFINITE
import `fun`.platonic.pulsar.common.URLUtil
import `fun`.platonic.pulsar.common.config.ImmutableConfig
import `fun`.platonic.pulsar.common.config.Params
import `fun`.platonic.pulsar.crawl.component.FetchComponent
import `fun`.platonic.pulsar.crawl.component.IndexComponent
import `fun`.platonic.pulsar.crawl.component.ParseComponent
import `fun`.platonic.pulsar.crawl.inject.SeedBuilder
import `fun`.platonic.pulsar.crawl.schedule.FetchSchedule
import `fun`.platonic.pulsar.crawl.scoring.ScoringFilters
import `fun`.platonic.pulsar.persist.HypeLink
import `fun`.platonic.pulsar.persist.PageCounters
import `fun`.platonic.pulsar.persist.WebPage
import `fun`.platonic.pulsar.persist.graph.WebGraph
import `fun`.platonic.pulsar.persist.graph.WebVertex
import `fun`.platonic.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.util.*
import java.util.stream.Collectors.joining

class SpiderSimulator(
        val conf: ImmutableConfig,
        var scoringFilters: ScoringFilters,
        var fetchSchedule: FetchSchedule,
        var seedBuilder: SeedBuilder,
        var fetchComponent: FetchComponent,
        var parseComponent: ParseComponent,
        var indexComponent: IndexComponent) {

    private lateinit var store: MutableMap<String, WebPage>
    private val graphs = HashMap<String, WebGraph>()
    private lateinit var metricsSystem: MetricsSystem

    // Inject simulation
    fun inject(seedUrls: List<String>) {
        store = seedUrls.map { seedBuilder.create(it, "") }.map { it.url to it }.toMap().toMutableMap()
    }

    /**
     * Assertion that the accepted and and actual resultant scores are the same.
     */
    fun crawl(seedUrls: List<String>, round: Int) {
        inject(seedUrls)
        for (i in 1..round) {
            crawlOneLoop(i)
        }
    }

    private fun crawlOneLoop(round: Int) {
        val batchId = round.toString()
        val limit = 100

        graphs.put(batchId, WebGraph())

        val pages = store.values
                .map { generate(batchId, it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.GENERATE) }
                .subList(0, limit)
                .map { fetchComponent.fetchContent(it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.FETCH) }
                .onEach { parseComponent.parse(it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.PARSE) }
                // .map(indexComponent.dryRunIndex(it))
                .map { WebVertex(it) }
                .map { WebGraph(it, it) }
                .reduce { g, g2 -> g.combine(g2) }
                .vertexSet()
                .map { it.webPage }
                .map { updateOutGraphMapper(it) }
                .map { updateOutGraphReducerBuildGraph(it) }
                .onEach { report(it) }
                .map { updateOutGraphReducer(it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.UPDATEOUTG) }
                .map { updateInGraphMapper(it) }
                .map { updateInGraphReducer(it) }

        println(pages.size)
    }

    private fun updateOutGraphMapper(page: WebPage): WebPage {
        val graph = graphs[page.batchId] ?: return page

        val v1 = graph.find(page.url)

        graph.addEdgeLenient(v1, v1)

        // Create outlinks and set metadata
        page.liveLinks.map { HypeLink.box(it.value) }.forEach {
            val edge = graph.addEdgeLenient(v1, WebVertex(it.url))
            edge.anchor = it.anchor
            edge.options = page.options
            edge.order = page.anchorOrder
        }

        scoringFilters.distributeScoreToOutlinks(v1.webPage, graph, graph.outgoingEdgesOf(v1), graph.outDegreeOf(v1))

        return page
    }

    private fun updateOutGraphReducerBuildGraph(page: WebPage): WebPage {
        val batchId = page.batchId ?: return page
        val graph = graphs[batchId] ?: return page
        val focus = graph.find(page.url)

        // Create new rows from outlinks
        graph.outgoingEdgesOf(focus)
                .filter { !it.isLoop }
                .map { it.target }
                .filter { !it.hasWebPage() }
                .onEach { it.webPage = createOrLoad(batchId, it.url) }
                .forEach { store.put(it.webPage.url, it.webPage) }

        return page
    }

    private fun updateOutGraphReducer(page: WebPage): WebPage {
        val batchId = page.batchId ?: return page
        val graph = graphs[batchId] ?: return page

        val focus = graph.find(page.url)

        // Update distance
        val distance = graph.incomingEdgesOf(focus).stream()
                .filter { !it.isLoop }
                .map { it.source }
                .map { it.webPage }
                .filter { Objects.nonNull(it) }
                .mapToInt { it.distance }
                .min().orElse(DISTANCE_INFINITE)
        page.distance = distance + 1

        // Update anchor
        val edge = graph.incomingEdgesOf(focus)
                .filter { !it.isLoop }
                .sortedBy { it.sourceWebPage.distance }
                .firstOrNull() ?: return page

        page.anchor = edge.anchor
        page.options = edge.options
        page.anchorOrder = edge.order

        // Update score
        scoringFilters.updateScore(focus.webPage, graph, graph.incomingEdgesOf(focus))

        page.marks.put(Mark.UPDATEOUTG, batchId)

        return page
    }

    private fun updateInGraphMapper(page: WebPage): WebPage {
        val graph = graphs[page.batchId] ?: return page
        val focus = graph.find(page.url)

        page.inlinks.keys.forEach { graph.addEdgeLenient(WebVertex(it), focus) }

        return page
    }

    /* InGraphUpdateJob simulation */
    // Update by in-links
    private fun updateInGraphReducer(page: WebPage): WebPage {
        val graph = graphs[page.batchId] ?: return page
        val focus = graph.find(page.url) ?: return page

        // Update by in-links
        graph.outgoingEdgesOf(focus)
                .filter { !it.isLoop }
                .filter { it.hasSourceWebPage() }
                .filter { it.hasTargetWebPage() }
                .forEach {
                    val p1 = it.sourceWebPage
                    val p2 = it.targetWebPage

                    // Update by out-links
                    p1.updateRefContentPublishTime(p2.contentPublishTime)
                    p1.pageCounters.increase(PageCounters.Ref.ch, p2.contentTextLen)
                    p1.pageCounters.increase(PageCounters.Ref.article)
                }

        scoringFilters.updateContentScore(focus.webPage)

        fetchSchedule.setFetchSchedule(page,
                page.prevFetchTime, page.prevModifiedTime,
                page.fetchTime, page.modifiedTime, page.crawlStatus.code)

        page.marks.put(Mark.UPDATEING, page.batchId)

        return page
    }

    // Generate simulation
    private fun generate(batchId: String, page: WebPage): WebPage {
        page.batchId = batchId

        scoringFilters.generatorSortValue(page, 1.0f)
        page.marks.put(Mark.GENERATE, page.batchId)

        return page
    }

    private fun createOrLoad(batchId: String, url: String): WebPage {
        val graph = graphs[batchId]
        val vertex = graph!!.find(url)
        if (vertex.hasWebPage()) {
            return vertex.webPage
        }

        val page = WebPage.newWebPage(url)

        fetchSchedule.initializeSchedule(page)
        scoringFilters.initialScore(page)

        return page
    }

    fun report(graph: WebGraph) {
        Params.of(
                "vertices", graph.vertexSet().size,
                "vertices(hasWebPage)", graph.vertexSet().stream().filter { it.hasWebPage() }.count(),
                "edges", graph.edgeSet().size.toString() + " : " + graph.edgeSet().stream().map { it.toString() }
                .collect(joining(", "))
        ).withLogger(LOG).info(true)
    }

    fun report(vertex: WebVertex) {
        if (vertex.hasWebPage()) {
            report(vertex.webPage)
        } else {
            Params.of("url", vertex.url).withLogger(LOG).info(true)
        }
    }

    fun report(page: WebPage) {
        Params.of(
                "batchId", page.batchId,
                "fetchCount", page.fetchCount,
                "distance", page.distance,
                "anchor", page.anchorOrder,
                "refCounters", page.pageCounters,
                "liveLinks", page.liveLinks.size,
                "inlinks", page.inlinks.size,
                "marks", page.marks.unbox().keys.map { it.toString() }.joinToString(),
                "url", page.url
        ).withLogger(LOG).info(true)
    }

    fun report() {
        store.values.forEach { page -> LOG.info(metricsSystem.getPageReport(page)) }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(SpiderSimulator::class.java)
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage : SpiderSimulator [-depth <depth>] [-round <round>] url")
        return
    }

    var url: String? = null
    var maxDepth = DISTANCE_INFINITE
    var maxRound = 4

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-depth" -> maxDepth = Integer.parseInt(args[++i])
            "-round" -> maxRound = Integer.parseInt(args[++i])
            else -> url = URLUtil.toASCII(args[i])
        }
        i++
    }

    if (url == null) {
        println("Usage : SpiderSimulator [-depth <depth>] [-round <round>] url")
        return
    }

    val applicationContext = ClassPathXmlApplicationContext("classpath:/context/tools-context.xml")
    val spiderSimulator = applicationContext.getBean(SpiderSimulator::class.java)
    spiderSimulator.crawl(listOf(url), maxRound)
    spiderSimulator.report()
}
