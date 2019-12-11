/**
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
package jobs

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.FetchComponent.Companion.updateContent
import ai.platon.pulsar.crawl.component.FetchComponent.Companion.updateFetchTime
import ai.platon.pulsar.crawl.component.FetchComponent.Companion.updateMarks
import ai.platon.pulsar.crawl.component.FetchComponent.Companion.updateStatus
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.schedule.DefaultFetchSchedule
import ai.platon.pulsar.crawl.schedule.FetchSchedule
import ai.platon.pulsar.persist.*
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import ai.platon.pulsar.persist.graph.WebVertex
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata
import ai.platon.pulsar.scoring.MonitorScoringFilter
import com.google.common.collect.Lists
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * JUnit test for `MonitorScoringFilter`. For an example set of URLs, we
 * simulate inlinks and liveLinks of the available graph. By manual calculation,
 * we determined the correct score points of URLs for each depth. For
 * convenience, a Map (dbWebPages) is used to store the calculated scores
 * instead of a persistent data store. At the end of the test, calculated scores
 * in the map are compared to our correct scores and a boolean result is
 * returned.
 */
class TestMonitorScoringFilter {
    companion object {
        val LOG = LoggerFactory.getLogger(TestMonitorScoringFilter::class.java)
        private val df = DecimalFormat("#.###")
        private val seedUrls = arrayOf("http://a.com", "http://b.com", "http://c.com")
        // An example web graph; shows websites as connected nodes
        private val linkGraph: MutableMap<String, List<String>> = LinkedHashMap()
        // Previously calculated values for each three depths. We will compare these
// to the results this test generates
        private val acceptedScores = HashMap<Int, HashMap<String, Float>>()

        init {
            linkGraph["http://a.com"] = Lists.newArrayList("http://b.com")
            linkGraph["http://b.com"] = Lists.newArrayList("http://a.com", "http://c.com", "http://e.com")
            linkGraph["http://c.com"] = Lists.newArrayList("http://a.com", "http://b.com", "http://d.com", "http://f.com")
            linkGraph["http://d.com"] = Lists.newArrayList()
            linkGraph["http://e.com"] = Lists.newArrayList()
            linkGraph["http://f.com"] = Lists.newArrayList()
        }

        init {
            acceptedScores[1] = hashMapOf(
                    "http://a.com" to 2.666f,
                    "http://b.com" to 3.333f,
                    "http://c.com" to 2.166f,
                    "http://d.com" to 0.278f
            )

            acceptedScores[2] = hashMapOf(
                    "http://a.com" to 2.666f,
                    "http://b.com" to 3.333f,
                    "http://c.com" to 2.166f,
                    "http://d.com" to 0.278f
            )

            acceptedScores[3] = hashMapOf(
                "http://a.com" to 3.388f,
                "http://b.com" to 4.388f,
                "http://c.com" to 2.666f,
                "http://d.com" to 0.5f
            )
        }
    }

    private val conf = ImmutableConfig()
    private val ROUND = 10
    private lateinit var scoringFilter: MonitorScoringFilter
    private lateinit var fetchSchedule: FetchSchedule

    private var rows: MutableMap<String, WebPage> = mutableMapOf()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        scoringFilter = MonitorScoringFilter(conf)
        fetchSchedule = DefaultFetchSchedule(conf)
        val scoreInjected = 1.0f
        LOG.info("scoreInjected : $scoreInjected")

        scoringFilter.reload(conf)
        // Inject simulation

        seedUrls.map { WebPage.newWebPage(it) }
                .onEach { it.score = scoreInjected }
                .onEach { scoringFilter.injectedScore(it) }
                .associateByTo(rows) { it.url }
    }

    /**
     * Assertion that the accepted and and actual resultant scores are the same.
     */
    @Test
    fun testCrawlAndEvaluateScore() { // Crawl loop simulation
        val now = Instant.now()
        for (i in 0 until ROUND) {
            val round = 1 + i
            val batchId = round.toString()
            rows = rows.values.onEach { page: WebPage ->
                // Generate simulation
                page.batchId = batchId
                // assertEquals(1.0f, page.getScore(), 1.0e-10);
                val initSortScore = calculateInitSortScore(page)
                val sortScore = scoringFilter.generatorSortValue(page, initSortScore)
                page.marks.put(Mark.GENERATE, batchId)
                // assertEquals(10001.0f, sortScore, 1.0e-10);
                Params.of("url", page.url, "sortScore", sortScore).withLogger(LOG).info(true)
            }.filter { page: WebPage -> page.hasMark(Mark.GENERATE) }.onEach { page: WebPage ->
                // Fetch simulation
                updateStatus(page, CrawlStatus.STATUS_FETCHED, ProtocolStatus.STATUS_SUCCESS)
                updateContent(page, Content(page.url, page.url, "".toByteArray(), "text/html",
                        SpellCheckedMultiMetadata(), conf))
                updateFetchTime(page, now.minus(60 - round * 2.toLong(), ChronoUnit.MINUTES))
                updateMarks(page)
                // Re-publish the article
                val publishTime = now.minus(30 - round * 2.toLong(), ChronoUnit.HOURS)
                page.modifiedTime = publishTime
                page.updateContentPublishTime(publishTime)

                page.setLiveLinks(linkGraph[page.url]!!.map { HypeLink(it) }.toList())
                Assert.assertEquals(publishTime, page.contentPublishTime)
            }.associateByTo(mutableMapOf()) { it.url }

            //      assertEquals(3, rows.size());
            /* Build the web graph */
            // 1. Add all vertices
            val graph = WebGraph()
            rows.values.forEach(Consumer { page: WebPage? -> graph.addVertex(WebVertex(page)) })
            // 2. Build all links as edges
            val vertices: Collection<WebVertex> = graph.vertexSet().stream().filter { obj: WebVertex -> obj.hasWebPage() }.collect(Collectors.toList())
            vertices.forEach(Consumer { v1: WebVertex ->
                v1.webPage.liveLinks.values
                        .forEach(Consumer { l: GHypeLink -> graph.addEdgeLenient(v1, WebVertex(l.url)).anchor = l.anchor.toString() })
            }
            )

            //      for (WebVertex v1 : graph.vertexSet()) {
            //        for (WebVertex v2 : graph.vertexSet()) {
            //          if (v1.getWebPage().getLiveLinks().keySet().contains(v2.getUrl())) {
            //            graph.addEdge(v1, v2);
            //          }
            //        }
            //      }

            // 3. report and assertions
            Params.of(
                    "round", round,
                    "rows", rows.size,
                    "vertices", graph.vertexSet().size,
                    "vertices(hasWebPage)", graph.vertexSet().stream().filter { obj: WebVertex -> obj.hasWebPage() }.count(),
                    "edges", graph.edgeSet().size.toString() + " : "
                    + graph.edgeSet().stream().map { obj: WebEdge -> obj.toString() }.collect(Collectors.joining(", "))
            ).withLogger(LOG).info(true)
            /* OutGraphUpdateJob simulation */ // 1. distribute score to outLinks
            graph.vertexSet().filter { it.hasWebPage() }
                    .forEach { scoringFilter.distributeScoreToOutlinks(it.webPage, graph, graph.outgoingEdgesOf(it), graph.outDegreeOf(it)) }
            // 2. update score for all rows
            graph.vertexSet().filter { it.hasWebPage() }.forEach {
                scoringFilter.updateScore(it.webPage, graph, graph.incomingEdgesOf(it))
            }
            // 3. update marks
            graph.vertexSet().filter { it.hasWebPage() }.forEach { it.webPage.marks.put(Mark.UPDATEOUTG, batchId) }
            // 4. generate new rows
            val newRows: Map<String, WebPage> = graph.vertexSet()
                    .filter { !it.hasWebPage() }
                    .map { v: WebVertex -> createNewRow(v.url) }
                    .associateBy { it.url }

            rows.putAll(newRows)
            /* InGraphUpdateJob simulation */ // Update by in-links
            graph.edgeSet()
                    .filter { it.hasSourceWebPage() }
                    .filter { it.hasTargetWebPage() }
                    .forEach { edge: WebEdge ->
                        val p1 = edge.sourceWebPage
                        val p2 = edge.targetWebPage
                        // Update by out-links
                        p1.updateRefContentPublishTime(p2.contentPublishTime)
                        p1.pageCounters.increase(PageCounters.Ref.ch, 1000 * round)
                        p1.pageCounters.increase(PageCounters.Ref.article)
                    }

            // Report the result
            graph.vertexSet()
                    .filter { it.hasWebPage() }
                    // .filter(v -> v.getUrl().contains("a.com"))
                    .map { it.webPage }
                    .forEach { LOG.info(it.toString()) }
        }
    }

    private fun createNewRow(url: String): WebPage {
        val page = WebPage.newWebPage(url)
        fetchSchedule.initializeSchedule(page)
        page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
        scoringFilter.initialScore(page)
        return page
    }

    private fun calculateInitSortScore(page: WebPage): Float {
        val raise = false
        val factor = if (raise) 1.0f else 0.0f
        val depth = page.distance
        return 10000.0f - 100 * depth + factor * 100000.0f
    }
}
