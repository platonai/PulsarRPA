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
package ai.platon.pulsar.crawl.schedule

import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * This class provides common methods for implementations of
 * [FetchSchedule].
 *
 * @author Andrzej Bialecki
 */
abstract class AbstractFetchSchedule(
        val conf: ImmutableConfig,
        val messageWriter: MiscMessageWriter? = null
) : FetchSchedule {
    private var defaultInterval = conf.getDuration(CapabilityTypes.FETCH_DEFAULT_INTERVAL, Duration.ofDays(30))
    private var maxInterval = conf.getDuration(CapabilityTypes.FETCH_MAX_INTERVAL, Duration.ofDays(90))
    private val impreciseNow = Instant.now()

    override fun getParams(): Params {
        return Params.of(
                "defaultInterval", defaultInterval,
                "maxInterval", maxInterval
        )
    }

    /**
     * Initialize fetch schedule related data. Implementations should at least set
     * the `fetchTime` and `fetchInterval`. The default
     * implementation sets the `fetchTime` to now, using the default
     * `fetchInterval`.
     *
     * @param page
     */
    override fun initializeSchedule(page: WebPage) {
        page.prevFetchTime = page.fetchTime
        page.fetchTime = impreciseNow
        page.fetchInterval = defaultInterval
        // TODO: FetchComponent handles retries
        // page.fetchRetries = 0
        page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
    }

    /**
     * Sets the `fetchInterval` and `fetchTime` on a
     * successfully fetched page. NOTE: this implementation resets the retry
     * counter - extending classes should call super.setFetchSchedule() to
     * preserve this behavior.
     */
    override fun setFetchSchedule(
            page: WebPage, prevFetchTime: Instant,
            prevModifiedTime: Instant, fetchTime: Instant, modifiedTime: Instant, state: Int) {
        // page.fetchRetries = 0
    }

    /**
     * This method specifies how to schedule refetching of pages marked as GONE.
     * Default implementation increases fetchInterval by 50% but the value may
     * never exceed `maxInterval`.
     *
     * @param page
     * @return adjusted page information, including all original information.
     * NOTE: this may be a different instance than
     */
    override fun setPageGoneSchedule(
            page: WebPage, prevFetchTime: Instant, prevModifiedTime: Instant, fetchTime: Instant) {
        val prevInterval = page.getFetchInterval(TimeUnit.SECONDS)
        var newInterval = prevInterval.toFloat()
        // no page is truly GONE ... just increase the interval by 50%
        // and try much later.
        newInterval = if (newInterval < maxInterval.seconds) {
            prevInterval * 1.5f
        } else {
            maxInterval.seconds * 0.9f
        }

        page.setFetchInterval(newInterval)
        page.prevFetchTime = page.fetchTime
        page.fetchTime = fetchTime.plus(page.fetchInterval)
    }

    /**
     * This method adjusts the fetch schedule if fetching needs to be re-tried due
     * to transient errors. The default implementation sets the next fetch time 1
     * day in the future and increases the retry counter.
     *
     * @param page             WebPage to retry
     * @param prevFetchTime    previous fetch time
     * @param prevModifiedTime previous modified time
     * @param fetchTime        current fetch time
     */
    override fun setPageRetrySchedule(page: WebPage, prevFetchTime: Instant, prevModifiedTime: Instant, fetchTime: Instant) {
        page.prevFetchTime = page.fetchTime
        page.fetchTime = fetchTime.plus(1, ChronoUnit.DAYS)
        // TODO: handle retry scope
        page.fetchRetries++
    }

    /**
     * This method return the last fetch time of the WebPage
     *
     * @return the date as a long.
     */
    override fun calculateLastFetchTime(page: WebPage): Instant {
        return page.fetchTime.minus(page.fetchInterval)
    }

    /**
     * This method provides information whether the page is suitable for selection
     * in the current fetchlist. NOTE: a true return value does not guarantee that
     * the page will be fetched, it just allows it to be included in the further
     * selection process based on scores. The default implementation checks
     * `fetchTime`, if it is higher than the
     *
     * @param page    Web page to fetch
     * @param curTime reference time (usually set to the time when the fetchlist
     * generation process was started).
     * @return true, if the page should be considered for inclusion in the current
     * fetchlist, otherwise false.
     */
    override fun shouldFetch(page: WebPage, curTime: Instant): Boolean {
        if (page.hasMark(Mark.INACTIVE)) {
            return false
        }
        // Pages are never truly GONE - we have to check them from time to time.
        // pages with too long fetchInterval are adjusted so that they fit within
        // maximum fetchInterval (batch retention period).
        val fetchTime = page.fetchTime
        if (curTime.plus(maxInterval).isBefore(fetchTime)) {
            if (page.fetchInterval > maxInterval) {
                page.setFetchInterval(maxInterval.seconds * 0.9f)
            }
            page.prevFetchTime = page.fetchTime
            page.fetchTime = curTime
        }
        return fetchTime.isBefore(curTime)
    }

    /**
     * This method resets fetchTime, fetchInterval, modifiedTime,
     * retriesSinceFetch and page text, so that it forces refetching.
     *
     * @param page
     * @param asap if true, force refetch as soon as possible - this sets the
     * fetchTime to now. If false, force refetch whenever the next fetch
     * time is set.
     */
    override fun forceRefetch(page: WebPage, asap: Boolean) {
        if (page.hasMark(Mark.INACTIVE)) {
            return
        }
        // reduce fetchInterval so that it fits within the max value
        if (page.fetchInterval > maxInterval) {
            page.setFetchInterval(maxInterval.seconds * 0.9f)
        }
        page.crawlStatus = CrawlStatus.STATUS_UNFETCHED
        page.fetchRetries = 0
        page.setSignature("".toByteArray())
        page.modifiedTime = Instant.EPOCH
        page.prevFetchTime = page.fetchTime
        if (asap) {
            page.fetchTime = Instant.now()
        }
    }

    protected fun updateRefetchTime(page: WebPage, interval: Duration, fetchTime: Instant, prevModifiedTime: Instant, modifiedTime: Instant) {
        page.fetchInterval = interval
        page.prevFetchTime = page.fetchTime
        page.fetchTime = fetchTime.plus(interval)
        page.prevModifiedTime = prevModifiedTime
        page.modifiedTime = modifiedTime
    }
}
