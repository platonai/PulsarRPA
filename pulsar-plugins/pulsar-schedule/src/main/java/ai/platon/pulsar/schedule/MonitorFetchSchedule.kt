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

package ai.platon.pulsar.schedule

import ai.platon.pulsar.common.config.AppConstants.TCP_IP_STANDARDIZED_TIME
import ai.platon.pulsar.common.config.AppConstants.YES_STRING
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.schedule.AdaptiveFetchSchedule
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS

/**
 * This class implements an adaptive re-fetch algorithm.
 *
 *
 * NOTE: values of DEC_FACTOR and INC_FACTOR higher than 0.4f may destabilize
 * the algorithm, so that the fetch interval either increases or decreases
 * infinitely, with little relevance to the page changes.
 *
 *
 * @author Vincent Zhang
 */
class MonitorFetchSchedule(conf: ImmutableConfig) : AdaptiveFetchSchedule() {
    private val DAY: Long = 24
    private val MONTH = (24 * 30).toLong()
    private val YEAR = (24 * 365).toLong()

    private val impreciseNow = Instant.now()
    private val impreciseTomorrow = impreciseNow.plus(1, ChronoUnit.DAYS)
    private val middleNight = LocalDateTime.now().truncatedTo(DAYS)
    private val middleNightInstant = Instant.now().truncatedTo(DAYS)
    // Check semi-inactive pages at 1 o'clock at night
    private val semiInactivePageCheckTime = middleNight.plusHours(25)

    init {
        super.reload(conf)
    }

    override fun setFetchSchedule(page: WebPage,
                                  prevFetchTime: Instant, prevModifiedTime: Instant,
                                  fetchTime: Instant, modifiedTime: Instant, state: Int) {
        var modifiedTime = modifiedTime
        if (modifiedTime.isBefore(TCP_IP_STANDARDIZED_TIME)) {
            modifiedTime = fetchTime
        }

        val distance = page.distance
        val interval: Duration
        if (page.isSeed) {
            interval = adjustSeedFetchInterval(page, fetchTime, modifiedTime, state)
        } else if (distance < maxDistance && veryLikeIndexPage(page)) {
            // TODO : search for new seed pages from navigator(non-leaf) pages.
            // We should monitor seed pages at day, and search for new seed pages at night
            // We should have two mode : monitor mode and explore mode
            // Under monitor mode, we just check seed again and again to detect the changes
            // And under explore mode, indexing pages are detected automatically
            interval = adjustSeedFetchInterval(page, fetchTime, modifiedTime, state)
        } else if (veryLikeDetailPage(page)) {
            // Detail pages are fetched only once, once it's mark
            interval = Duration.ofDays((365 * 10).toLong())
            page.marks.put(Mark.INACTIVE, YES_STRING)
        } else if (modifiedTime.isAfter(middleNightInstant) && modifiedTime.isAfter(prevModifiedTime)) {
            val refArticles = page.pageCounters.get<PageCounters.Ref>(PageCounters.Ref.article)
            val fetchCount = page.fetchCount
            if (refArticles > fetchCount / 10 - 1) {
                // There are still bugs for modify time calculation
                super.setFetchSchedule(page, prevFetchTime, prevModifiedTime, fetchTime, modifiedTime, state)
            }
            return
        } else {
            interval = adjustSemiInactivePageFetchInterval(page, fetchTime, modifiedTime, state)
            page.marks.put(Mark.SEMI_INACTIVE, YES_STRING)
        }

        updateRefetchTime(page, interval, fetchTime, prevModifiedTime, modifiedTime)
    }

    private fun veryLikeIndexPage(page: WebPage): Boolean {
        return page.pageCategory.isIndex || CrawlFilter.sniffPageCategory(page.url).isIndex
    }

    private fun veryLikeDetailPage(page: WebPage): Boolean {
        return page.pageCategory.isDetail || CrawlFilter.sniffPageCategory(page.url).isDetail
    }

    /**
     * Adjust fetch interval for article pages
     */
    private fun adjustSeedFetchInterval(page: WebPage, fetchTime: Instant, modifiedTime: Instant, state: Int): Duration {
        val fetchCount = page.fetchCount
        if (fetchCount <= 1) {
            // Ref-parameters are not initialized yet
            return MIN_INTERVAL
        }

        var interval = page.fetchInterval
        val pageCounters = page.pageCounters
        // int noArticles = pageCounters.get(PageCounters.Self.noArticle);
        val refArticles = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.article)
        if (fetchCount > 5 && refArticles == 0) {
            pageCounters.increase<PageCounters.Self>(PageCounters.Self.noArticle)
            metricsSystem.reportFetchSchedule(page, false)
            // Check it at 1 o'clock next night, decrease fetch frequency if no articles
            interval = Duration.between(LocalDateTime.now(), semiInactivePageCheckTime)
                    .plusDays((fetchCount / 10).toLong()).plusHours(fetchCount.toLong())
            return interval
        }

        val hours = ChronoUnit.HOURS.between(modifiedTime, fetchTime)
        if (hours <= 1 * DAY) {
            // There are updates today, keep re-fetch the page in every crawl loop
            interval = MIN_INTERVAL
        } else if (hours <= 3 * DAY) {
            // If there is not updates in 24 hours but there are updates in 72 hours, re-fetch the page a hour later
            interval = Duration.ofHours(1)
        } else if (hours <= 3 * MONTH) {
            // If there is no any updates in 72 hours but has updates in 3 month,
            // check the page at least 1 hour later and increase fetch interval time by time
            val inc = (interval.seconds * INC_RATE).toLong()

            interval = interval.plusSeconds(inc)
            if (interval.toHours() < 1) {
                interval = Duration.ofHours(1)
            }

            if (hours < 10 * DAY) {
                // No longer than SEED_MAX_INTERVAL
                if (interval.compareTo(SEED_MAX_INTERVAL) > 0) {
                    interval = SEED_MAX_INTERVAL
                }
            } else {
                // The page is
                metricsSystem.reportFetchSchedule(page, false)
            }
        } else if (hours > 10 * YEAR) {
            // Longer than 10 years, it's very likely the publishTime/modifiedTime is wrong
            metricsSystem.reportFetchSchedule(page, false)
            return super.getFetchInterval(page, fetchTime, modifiedTime, state)
        }

        return interval
    }

    private fun adjustSemiInactivePageFetchInterval(page: WebPage, fetchTime: Instant, modifiedTime: Instant, state: Int): Duration {
        val distance = page.distance
        if (distance >= maxDistance) {
            // anything to do
        }

        val fetchCount = page.fetchCount
        var interval = Duration.between(LocalDateTime.now(), semiInactivePageCheckTime).plusHours(fetchCount.toLong())

        val pageCounters = page.pageCounters
        val refArticles = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.article)
        if (fetchCount > 3 && distance < maxDistance && refArticles == 0) {
            interval = interval.plusDays(fetchCount.toLong())
        }

        // it seems that modified time is extracted correctly
        if (modifiedTime.isAfter(TCP_IP_STANDARDIZED_TIME)) {
            val days = ChronoUnit.DAYS.between(modifiedTime, fetchTime)
            if (days > 30) {
                interval = interval.plusDays(days)
            }
        }

        return interval
    }
}
