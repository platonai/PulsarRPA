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

package ai.platon.pulsar.scoring

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants.FETCH_PRIORITY_DEFAULT
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.scoring.NamedScoreVector
import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * This plugin implements a variant of an Online Page Importance Computation
 * (OPIC) score, described in this paper: [](http://www2003.org/cdrom/papers/refereed/p007/p7-abiteboul.html)
 * Abiteboul, Serge and Preda, Mihai and Cobena, Gregory (2003), Adaptive
 * On-Line Page Importance Computation .
 */
class MonitorScoringFilter(conf: ImmutableConfig) : ContentAnalysisScoringFilter(conf) {
    private val LOG = LoggerFactory.getLogger(MonitorScoringFilter::class.java)

    private var topN: Int = 0
    private var priorPageRate: Float = 0f
    private var maxPriorPages: Int = 0
    private var priorPages = 0

    private var scorePower: Float = 0f
    private var internalScoreFactor: Float = 0f
    private var externalScoreFactor: Float = 0f

    private var errorCounterDivisor: Int = 0
    private var webGraphScoreDivisor: Int = 0
    private var contentScoreDivisor: Int = 0

    private val impreciseNow = Instant.now()
    private val impreciseTomorrow = impreciseNow.plus(1, ChronoUnit.DAYS)
    private val impreciseLocalNow = LocalDateTime.now()

    init {
        reload(conf)
    }

    override fun reload(conf: ImmutableConfig) {
        super.reload(conf)

        scorePower = conf.getFloat("index.score.power", 0.5f)
        internalScoreFactor = conf.getFloat("db.score.link.internal", 1.0f)
        externalScoreFactor = conf.getFloat("db.score.link.external", 1.0f)
        errorCounterDivisor = conf.getInt(SCORE_SORT_ERROR_COUNTER_DIVISOR, 20)
        webGraphScoreDivisor = conf.getInt(SCORE_SORT_WEB_GRAPH_SCORE_DIVISOR, 20)
        contentScoreDivisor = conf.getInt(SCORE_SORT_CONTENT_SCORE_DIVISOR, 20)

        topN = conf.getInt(GENERATE_TOP_N, Integer.MAX_VALUE)
        priorPageRate = conf.getFloat(GENERATE_DETAIL_PAGE_RATE, 0.80f)
        maxPriorPages = Math.round(topN * priorPageRate)
    }

    override fun getParams(): Params {
        return Params.of(
                "topN", topN,
                "priorPageRate", priorPageRate,
                "maxPriorPages", maxPriorPages
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
    override fun initialScore(row: WebPage) {
        row.score = 0.0f
        row.cash = 0.0f
    }

    override fun generatorSortValue(page: WebPage, initSort: Float): ScoreVector {
        val priority = calculatePriority(page)
        val distance = page.distance
        val createTime = page.createTime
        val modifiedTime = calculateModifiedTime(page)
        val createDuration = Duration.between(createTime, impreciseNow)
        val createdDays = createDuration.toDays()

        val inlinkOrder = page.anchorOrder

        // Lower the score if there are too many errors
        val pageCounters = page.pageCounters
        val fetchCount = page.fetchCount.toFloat()
        val refFetchErr = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.fetchErr).toFloat()
        val refParseErr = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.parseErr).toFloat()
        val refExtractErr = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.extractErr).toFloat()
        val refIndexErr = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.indexErr).toFloat()

        val refFetchErrDensity = if (refFetchErr == 0f) 0f else fetchCount / refFetchErr / errorCounterDivisor.toFloat()
        val refParseErrDensity = if (refParseErr == 0f) 0f else fetchCount / refParseErr / errorCounterDivisor.toFloat()
        val refExtractErrDensity = if (refExtractErr == 0f) 0f else fetchCount / refExtractErr / errorCounterDivisor.toFloat()
        val refIndexErrDensity = if (refIndexErr == 0f) 0f else fetchCount / refIndexErr / errorCounterDivisor.toFloat()

        val score = NamedScoreVector()

        score.setValue(NamedScoreVector.Name.priority, priority)
        score.setValue(NamedScoreVector.Name.distance, -distance)

        // Do not care about create time if it's created longer than 3 days
        if (createdDays <= 3) {
            // yyyyMMddHH format is OK to convert to a int
            score.setValue(NamedScoreVector.Name.createTime,
                    NumberUtils.toInt(DateTimeUtil.format(createTime, "yyyyMMddHH"), 0))
        }

        score.setValue(NamedScoreVector.Name.contentScore, initSort * page.contentScore / contentScoreDivisor)
        score.setValue(NamedScoreVector.Name.webGraphScore, initSort * page.score / webGraphScoreDivisor)

        score.setValue(NamedScoreVector.Name.refFetchErrDensity, refFetchErrDensity)
        score.setValue(NamedScoreVector.Name.refParseErrDensity, refParseErrDensity)
        score.setValue(NamedScoreVector.Name.refExtractErrDensity, refExtractErrDensity)
        score.setValue(NamedScoreVector.Name.refIndexErrDensity, refIndexErrDensity)

        // yyyyMMddHH format is OK to convert to a int
        score.setValue(NamedScoreVector.Name.modifyTime,
                NumberUtils.toInt(DateTimeUtil.format(modifiedTime, "yyyyMMddHH"), 0))

        score.setValue(NamedScoreVector.Name.inlinkOrder, -inlinkOrder)

        return score
    }

    private fun calculateModifiedTime(page: WebPage): Instant {
        var modifiedTime = page.sniffModifiedTime()

        if (modifiedTime.isAfter(impreciseTomorrow)) {
            // Bad modified time, decrease the score
            modifiedTime = Instant.EPOCH
        }
        if (page.fetchCount == 0) {
            modifiedTime = page.createTime
        }

        return modifiedTime
    }

    private fun calculatePriority(page: WebPage): Int {
        val createTime = page.createTime
        val createDuration = Duration.between(createTime, impreciseNow)
        val createdDays = createDuration.toDays()
        val createdHours = createDuration.toHours()
        var priority = FETCH_PRIORITY_DEFAULT

        // raise priority for detail pages
        if (!page.isSeed) {
            if (page.fetchCount == 0 && priorPages < maxPriorPages) {
                val isDetail = page.pageCategory.isDetail || CrawlFilter.sniffPageCategory(page.url).isDetail
                if (createdDays == 0L) {
                    // Pages created today has the highest priority
                    priority += if (isDetail) 300 else 200
                } else if (isDetail && createdDays <= 3) {
                    // Cares only about detail pages created in 3 days
                    priority += 100
                } else {
                    // We should upgrade our hardware to fetch pages faster
                }

                //        if (isDetail) {
                //          priority += 300;
                //        }
                //        priority -= createdDays;
            }

            // reserve resource for seeds
            if (priority > FETCH_PRIORITY_DEFAULT) {
                ++priorPages
            }
        }

        if (page.isSeed) {
            // Raise up newly added seeds
            if (createdDays < 7) {
                priority += (1000 + (7 - createdDays)).toInt()
            }

            // Lower down seeds priority at night
            val hour = impreciseLocalNow.hour.toLong()
            if (hour in 2..6) {
                priority -= 100
            }
        }

        return priority
    }

    /** Increase the score by a sum of inlinked scores.  */
    override fun updateScore(page: WebPage, graph: WebGraph, inLinkEdges: Collection<WebEdge>) {
        var inLinkScore = 0.0f
        for (edge in inLinkEdges) {
            if (!edge.isLoop) {
                inLinkScore += graph.getEdgeWeight(edge).toFloat()
            }
        }

        page.score = page.score + inLinkScore
        page.cash = page.cash + inLinkScore
    }

    /** Get cash on hand, divide it by the number of outlinks and apply.  */
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
                val toHost = URL(edge.target.url).host
                val fromHost = URL(page.url).host

                if (toHost.equals(fromHost, ignoreCase = true)) {
                    graph.setEdgeWeight(edge, score + internalScore)
                } else {
                    graph.setEdgeWeight(edge, score + externalScore)
                }
            } catch (e: MalformedURLException) {
                LOG.error("Failed with the following MalformedURLException: ", e)
                graph.setEdgeWeight(edge, score + externalScore)
            }

        }

        page.cash = 0.0f
    }

    /** Dampen the boost value by scorePower.  */
    override fun indexerScore(url: String, doc: IndexDocument, row: WebPage, initScore: Float): Float {
        return Math.pow(row.score.toDouble(), scorePower.toDouble()).toFloat() * initScore
    }
}
