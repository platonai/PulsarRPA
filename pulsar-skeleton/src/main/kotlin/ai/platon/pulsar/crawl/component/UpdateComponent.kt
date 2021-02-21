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
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.AppMetrics
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.schedule.DefaultFetchSchedule
import ai.platon.pulsar.crawl.schedule.FetchSchedule
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.crawl.signature.SignatureComparator
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.PageCounters.Self
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.CrawlStatusCodes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
@Component
class UpdateComponent(
        val webDb: WebDb,
        val fetchSchedule: FetchSchedule,
        val scoringFilters: ScoringFilters? = null,
        val messageWriter: MiscMessageWriter? = null,
        val conf: ImmutableConfig
) : Parameterized {
    val LOG = LoggerFactory.getLogger(UpdateComponent::class.java)

    companion object {
        enum class Counter { rCreated, rNewDetail, rPassed, rLoaded, rNotExist, rDepthUp, rUpdated, rTotalUpdates, rBadModTime }
        init { AppMetrics.reg.register(Counter::class.java) }
    }

    private val enumCounters = AppMetrics.reg.enumCounterRegistry
    private var fetchRetryMax = conf.getInt(CapabilityTypes.FETCH_MAX_RETRY, 3)
    private var maxFetchInterval: Duration = conf.getDuration(CapabilityTypes.FETCH_MAX_INTERVAL, Duration.ofDays(365))

    constructor(webDb: WebDb, conf: ImmutableConfig): this(webDb, DefaultFetchSchedule(conf), null, null, conf)

    override fun getParams(): Params {
        return Params.of(
                "className", this.javaClass.simpleName,
                "fetchRetryMax", fetchRetryMax,
                "maxFetchInterval", maxFetchInterval,
                "fetchSchedule", fetchSchedule.javaClass.simpleName
        )
    }

    fun updateByOutgoingPage(page: WebPage, outgoingPage: WebPage) {
        val pageCounters = page.pageCounters
        pageCounters.increase(PageCounters.Ref.page)
        page.updateRefContentPublishTime(outgoingPage.contentPublishTime)

        if (outgoingPage.pageCategory.isDetail || CrawlFilter.guessPageCategory(outgoingPage.url).isDetail) {
            pageCounters.increase(PageCounters.Ref.ch, outgoingPage.contentTextLen)
            pageCounters.increase(PageCounters.Ref.item)
        }

        val outgoingPageCounters = outgoingPage.pageCounters
        val missingFields = outgoingPageCounters.get(Self.missingFields)
        val brokenSubEntity = outgoingPageCounters.get(Self.brokenSubEntity)

        pageCounters.increase(PageCounters.Ref.missingFields, missingFields)
        pageCounters.increase(PageCounters.Ref.brokenEntity, if (missingFields > 0) 1 else 0)
        pageCounters.increase(PageCounters.Ref.brokenSubEntity, brokenSubEntity)

        if (outgoingPage.protocolStatus.isFailed) {
            page.deadLinks.add(outgoingPage.url)
            messageWriter?.debugDeadOutgoingPage(outgoingPage.url, page)
        }

        scoringFilters?.updateContentScore(page)
    }

    fun updateByOutgoingPages(page: WebPage, outgoingPages: Collection<WebPage>) {
        val lastPageCounters = page.pageCounters.clone()
        outgoingPages.forEach { updateByOutgoingPage(page, it) }
        updatePageCounters(lastPageCounters, page.pageCounters, page)
    }

    fun updatePageCounters(lastPageCounters: PageCounters, pageCounters: PageCounters, page: WebPage) {
        val lastMissingFields = lastPageCounters.get(PageCounters.Ref.missingFields)
        val lastBrokenEntity = lastPageCounters.get(PageCounters.Ref.brokenEntity)
        val lastBrokenSubEntity = lastPageCounters.get(PageCounters.Ref.brokenSubEntity)
        val missingFieldsLastRound = pageCounters.get(PageCounters.Ref.missingFields) - lastMissingFields
        val brokenEntityLastRound = pageCounters.get(PageCounters.Ref.brokenEntity) - lastBrokenEntity
        val brokenSubEntityLastRound = pageCounters.get(PageCounters.Ref.brokenSubEntity) - lastBrokenSubEntity

        pageCounters.set(PageCounters.Ref.missingFieldsLastRound, missingFieldsLastRound)
        pageCounters.set(PageCounters.Ref.brokenEntityLastRound, brokenEntityLastRound)
        pageCounters.set(PageCounters.Ref.brokenSubEntityLastRound, brokenSubEntityLastRound)

        if (missingFieldsLastRound != 0 || brokenEntityLastRound != 0 || brokenSubEntityLastRound != 0) {
            val message = Params.of(
                    "missingFields", missingFieldsLastRound,
                    "brokenEntity", brokenEntityLastRound,
                    "brokenSubEntity", brokenSubEntityLastRound
            ).formatAsLine()

            messageWriter?.reportBrokenEntity(page.url, message)
            LOG.warn(message)
        }
    }

    /**
     * A simple update procedure
     */
    fun updateByIncomingPages(incomingPages: Collection<WebPage>, page: WebPage) {
        var smallestDepth = page.distance
        var shallowestPage: WebPage? = null

        for (incomingPage in incomingPages) { // log.debug(incomingPage.url() + " -> " + page.url());
            if (incomingPage.distance + 1 < smallestDepth) {
                smallestDepth = incomingPage.distance + 1
                shallowestPage = incomingPage
            }
        }

        if (shallowestPage != null) {
            page.referrer = shallowestPage.url
            // TODO: Not the best options
            page.args = shallowestPage.args
            page.distance = shallowestPage.distance + 1
        }
    }

    fun updateFetchSchedule(page: WebPage) {
        if (page.marks.isInactive) {
            return
        }
        val crawlStatus = page.crawlStatus
        when (crawlStatus.code.toByte()) {
            CrawlStatusCodes.FETCHED,
            CrawlStatusCodes.REDIR_TEMP,
            CrawlStatusCodes.REDIR_PERM,
            CrawlStatusCodes.NOTMODIFIED -> {
                var modified = FetchSchedule.STATUS_UNKNOWN
                if (crawlStatus.code == CrawlStatusCodes.NOTMODIFIED.toInt()) {
                    modified = FetchSchedule.STATUS_NOTMODIFIED
                }

                val prevSig = page.prevSignature
                val signature = page.signature
                if (prevSig != null && signature != null) {
                    modified = if (SignatureComparator.compare(prevSig, signature) != 0) {
                        FetchSchedule.STATUS_MODIFIED
                    } else {
                        FetchSchedule.STATUS_NOTMODIFIED
                    }
                }

                val prevFetchTime = page.prevFetchTime
                val fetchTime = page.fetchTime
                var prevModifiedTime = page.prevModifiedTime
                var modifiedTime = page.modifiedTime
                val newModifiedTime = page.sniffModifiedTime()
                if (newModifiedTime.isAfter(modifiedTime)) {
                    prevModifiedTime = modifiedTime
                    modifiedTime = newModifiedTime
                }

                fetchSchedule.setFetchSchedule(page, prevFetchTime, prevModifiedTime, fetchTime, modifiedTime, modified)
                val fetchInterval = page.fetchInterval
                if (fetchInterval > maxFetchInterval && !page.marks.isInactive) {
                    LOG.info("Force re-fetch page " + page.url + ", fetch interval : " + fetchInterval)
                    fetchSchedule.forceRefetch(page, false)
                }

                if (modifiedTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
                    enumCounters.inc(Counter.rBadModTime)
                    messageWriter?.reportBadModifiedTime(Params.of(
                            "PFT", prevFetchTime, "FT", fetchTime,
                            "PMT", prevModifiedTime, "MT", modifiedTime,
                            "HMT", page.headers.lastModified,
                            "U", page.url
                    ).formatAsLine())
                }
            }
            CrawlStatusCodes.RETRY -> {
                fetchSchedule.setPageRetrySchedule(page, Instant.EPOCH, page.prevModifiedTime, page.fetchTime)
                if (page.fetchRetries < fetchRetryMax) {
                    page.setCrawlStatus(CrawlStatusCodes.UNFETCHED.toInt())
                } else {
                    page.setCrawlStatus(CrawlStatusCodes.GONE.toInt())
                }
            }
            CrawlStatusCodes.GONE -> fetchSchedule.setPageGoneSchedule(page, Instant.EPOCH, page.prevModifiedTime, page.fetchTime)
        }
    }
}
