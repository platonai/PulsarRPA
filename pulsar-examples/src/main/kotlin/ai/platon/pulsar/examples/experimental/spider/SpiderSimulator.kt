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

package ai.platon.pulsar.examples.experimental.spider

import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.common.config.AppConstants.DISTANCE_INFINITE
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.crawl.component.ParseComponent
import ai.platon.pulsar.crawl.inject.SeedBuilder
import ai.platon.pulsar.crawl.schedule.FetchSchedule
import ai.platon.pulsar.crawl.scoring.NamedScoreVector
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.persist.HyperLink
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import java.time.Instant

class SpiderSimulator(
        val conf: ImmutableConfig,
        val scoringFilters: ScoringFilters,
        val fetchSchedule: FetchSchedule,
        val seedBuilder: SeedBuilder,
        val fetchComponent: FetchComponent,
        val loadComponent: LoadComponent,
        val parseComponent: ParseComponent,
        val messageWriter: MiscMessageWriter
) {
    private val log = LoggerFactory.getLogger(SpiderSimulator::class.java)

    private val store = mutableMapOf<String, WebPage>()
    private val graphs = mutableMapOf<String, WebGraph>()

    // Inject simulation
    fun inject(seedUrls: List<String>) {
        seedUrls.map { seedBuilder.create(it, "") }.associateByTo(store) { it.url }
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
        val limit = 5

        println("Batch #$batchId ===============================================")
        val pages = store.values
                .map { generate(batchId, it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.GENERATE) }
                .take(limit)
                .map { fetch(it.batchId, it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.FETCH) }
                .onEach { parse(it.batchId, it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.PARSE) }
                // .map(indexComponent.dryRunIndex(it))
                .map { WebVertex(it) }
                .map { WebGraph(it, it) }
                .reduce { g, g2 -> g.combine(g2) }
                .also { graphs[batchId] = it }
                .vertexSet()
                .mapNotNull { it.page }
                .map { out2InGraphUpdateMapper(it) }
                .onEach { out2InGraphUpdateMapper2(it) }
                .onEach { report(it) }
                .map { out2InGraphUpdateReducer(batchId, it) }
                .filter { it.batchId == batchId }
                .filter { it.marks.contains(Mark.UPDATEOUTG) }
                .map { in2OutGraphUpdateMapper(it) }
                .map { in2OutGraphUpdateReducer(it) }
                .toList()

        log.info("Batch " + batchId + " finished, total " + pages.size + " pages")
    }

    // Generate simulation
    private fun generate(batchId: String, page: WebPage): WebPage {
        if (!fetchSchedule.shouldFetch(page, Instant.now())) {
            return page
        }

        scoringFilters.generatorSortValue(page, NamedScoreVector.ONE)
        page.marks.put(Mark.GENERATE, batchId)
        require(page.marks[Mark.GENERATE].toString() == batchId)
        // page.fetchTime = Instant.now()

        page.batchId = batchId

        return page
    }

    private fun fetch(batchId: String, page: WebPage): WebPage {
        // val p = loadComponent.load(page.url)
        fetchComponent.fetchContent(page)
        require(page.marks[Mark.FETCH]?.toString() == batchId)

        return page
    }

    private fun parse(batchId: String, page: WebPage) {
        parseComponent.parse(page)
        page.marks.putIfNotNull(Mark.PARSE, page.marks[Mark.FETCH])
    }

    private fun out2InGraphUpdateMapper(page: WebPage): WebPage {
        val graph = graphs[page.batchId]
        require(graph != null)

        val v1 = graph.find(page.url)
        require(v1 != null)

        graph.addEdgeLenient(v1, v1)

        // Create outlinks and set metadata
        page.liveLinks.filter { it.toString() !in store.keys }.map { HyperLink.box(it.value) }.forEach {
            val edge = graph.addEdgeLenient(v1, WebVertex(it.url))
            edge.anchor = it.anchor
            edge.options = page.options
            edge.order = page.anchorOrder
        }

        scoringFilters.distributeScoreToOutlinks(v1.page!!, graph, graph.outgoingEdgesOf(v1), graph.outDegreeOf(v1))

        return page
    }

    private fun out2InGraphUpdateMapper2(page: WebPage): List<WebPage> {
        val batchId = page.batchId
        require(batchId.isNotBlank())
        val graph = graphs[batchId]
        require(graph != null)
        val focus = graph.find(page.url)
        require(focus != null)

        if (page.liveLinks.isNotEmpty()) {
            require(graph.outgoingEdgesOf(focus).isNotEmpty())
        }

        // Create new rows from outlinks
        val pages = graph.outgoingEdgesOf(focus)
                .filter { !it.isLoop }
                .map { it.target }
                .filter { !it.hasWebPage() }
                .onEach { it.page = loadOrCreate(batchId, it.url) }
                .mapNotNull { it.page }

        pages.forEach { store.put(it.url, it) }

        val numNewPages = pages.count { it.crawlStatus.isUnFetched }

        log.info("Created {} new pages", numNewPages)

        return pages
    }

    private fun out2InGraphUpdateReducer(batchId: String, page: WebPage): WebPage {
        val graph = graphs[batchId]
        require(graph != null)
        val focus = graph.find(page.url)
        require(focus != null)

        // Update distance
        val distance = graph.incomingEdgesOf(focus)
                .asSequence()
                .filter { !it.isLoop }
                .mapNotNull { it.source.page?.distance }
                .min()?:-1
        page.distance = distance + 1

        // Update anchor
        val edge = graph.incomingEdgesOf(focus)
                .filter { !it.isLoop }.minBy { it.sourceWebPage!!.distance } ?: return page

        page.anchor = edge.anchor
        page.options = edge.options
        page.anchorOrder = edge.order

        // Update score
        scoringFilters.updateScore(focus.page!!, graph, graph.incomingEdgesOf(focus))

        page.marks.put(Mark.UPDATEOUTG, batchId)

        return page
    }

    private fun in2OutGraphUpdateMapper(page: WebPage): WebPage {
        val graph = graphs[page.batchId]
        require(graph != null)
        val focus = graph.find(page.url)
        require(focus != null)

        page.inlinks.keys.forEach { graph.addEdgeLenient(WebVertex(it.toString()), focus) }

        return page
    }

    /* InGraphUpdateJob simulation */
    // Update by in-links
    private fun in2OutGraphUpdateReducer(page: WebPage): WebPage {
        val graph = graphs[page.batchId]
        require(graph != null)
        val focus = graph.find(page.url)
        require(focus != null)

        if (page.liveLinks.isNotEmpty()) {
            require(graph.outgoingEdgesOf(focus).isNotEmpty())
        }

        // Update by in-links
        graph.outgoingEdgesOf(focus)
                .filter { !it.isLoop && it.hasTargetWebPage() }
                .forEach {
                    val p1 = it.sourceWebPage!!
                    val p2 = it.targetWebPage!!

                    // Update by out-links
                    p1.updateRefContentPublishTime(p2.contentPublishTime)
                    p1.pageCounters.increase(PageCounters.Ref.ch, p2.contentTextLen)
                    p1.pageCounters.increase(PageCounters.Ref.item)
                }

        scoringFilters.updateContentScore(focus.page!!)

        fetchSchedule.setFetchSchedule(page,
                page.prevFetchTime, page.prevModifiedTime,
                page.fetchTime, page.modifiedTime, page.crawlStatus.code)
        require(!fetchSchedule.shouldFetch(page, Instant.now()))

        val mark = page.marks[Mark.UPDATEOUTG]
        require(mark.toString() == page.batchId)
        page.marks.clear()
        page.marks.put(Mark.UPDATEING, mark)

        return page
    }

    private fun loadOrCreate(batchId: String, url: String): WebPage? {
        var page = store.get(url)
        if (page != null) {
            return page
        }

        page = WebPage.newWebPage(url, false)

        fetchSchedule.initializeSchedule(page)
        scoringFilters.initialScore(page)

        return page
    }

    fun report(graph: WebGraph) {
        Params.of(
                "vertices", graph.vertexSet().size,
                "vertices(hasWebPage)", graph.vertexSet().count { it.hasWebPage() },
                "edges", graph.edgeSet().size.toString() + " : " + graph.edgeSet().joinToString { it.toString() }
        ).withLogger(log).info(true)
    }

    fun report(vertex: WebVertex) {
        if (vertex.hasWebPage()) {
            report(vertex.page!!)
        } else {
            Params.of("url", vertex.url).withLogger(log).info(true)
        }
    }

    private fun report(page: WebPage) {
        Params.of(
                "batchId", page.batchId,
                "fetchCount", page.fetchCount,
                "distance", page.distance,
                "anchorOrder", page.anchorOrder,
                "refCounters", page.pageCounters,
                "liveLinks", page.liveLinks.size,
                "inlinks", page.inlinks.size,
                "marks", page.marks.unbox().keys.joinToString { it.toString() },
                "url", page.url
        ).withLogger(log).info(true)
    }

    fun report() {
        store.values.forEach { messageWriter.reportFetchSchedule(it, true) }
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
            "-depth" -> maxDepth = args[++i].toInt()
            "-round" -> maxRound = args[++i].toInt()
            else -> url = URLUtil.toASCII(args[i])
        }
        i++
    }

    if (url == null) {
        println("Usage : SpiderSimulator [-depth <depth>] [-round <round>] url")
        return
    }

    val applicationContext = ClassPathXmlApplicationContext("classpath:/pulsar-beans/examples-context.xml")
    val spiderSimulator = applicationContext.getBean(SpiderSimulator::class.java)
    spiderSimulator.crawl(listOf(url), maxRound)
    spiderSimulator.report()
}
