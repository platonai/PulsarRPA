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

import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebPage
import java.time.Instant

/**
 * This interface defines the contract for implementations that manipulate fetch
 * times and re-fetch intervals.
 */
interface FetchSchedule : Parameterized {

    companion object {
        /**
         * It is unknown whether page was changed since our last visit.
         */
        const val STATUS_UNKNOWN = 0
        /**
         * Page is known to have been modified since our last visit.
         */
        const val STATUS_MODIFIED = 1
        /**
         * Page is known to remain unmodified since our last visit.
         */
        const val STATUS_NOTMODIFIED = 2
    }

    /**
     * Initialize fetch schedule related data. Implementations should at least set
     * the `fetchTime` and `fetchInterval`. The default
     * implementation set the `fetchTime` to now, using the default
     * `fetchInterval`.
     *
     * @param page
     */
    fun initializeSchedule(page: WebPage)

    /**
     * Sets the `fetchInterval` and `fetchTime` on a
     * successfully fetched page. Implementations may use supplied arguments to
     * support different re-fetching schedules.
     *
     * @param page             The Web page
     * @param prevFetchTime    previous value of fetch time, or -1 if not available
     * @param prevModifiedTime previous value of modifiedTime, or -1 if not available
     * @param fetchTime        the latest time, when the page was recently re-fetched. Most
     * FetchSchedule implementations should update the value in
     * @param modifiedTime     last time the content was modified. This information comes from
     * the protocol implementations, or is set to < 0 if not available.
     * Most FetchSchedule implementations should update the value in
     * @param state            if [STATUS_MODIFIED], then the content is considered to be
     * "changed" before the `fetchTime`, if
     * [STATUS_NOTMODIFIED] then the content is known to be
     * unchanged. This information may be obtained by comparing page
     * signatures before and after fetching. If this is set to
     * [STATUS_UNKNOWN], then it is unknown whether the page was
     * changed; implementations are free to follow a sensible default
     * behavior.
     */
    fun setFetchSchedule(page: WebPage, prevFetchTime: Instant,
                         prevModifiedTime: Instant, fetchTime: Instant, modifiedTime: Instant, state: Int)

    /**
     * This method specifies how to schedule refetching of pages marked as GONE.
     * Default implementation increases fetchInterval by 50%, and if it exceeds
     * the `maxInterval` it calls
     * [.forceRefetch].
     *
     * @param page The page
     */
    fun setPageGoneSchedule(page: WebPage, prevFetchTime: Instant, prevModifiedTime: Instant, fetchTime: Instant)

    /**
     * This method adjusts the fetch schedule if fetching needs to be re-tried due
     * to transient errors. The default implementation sets the next fetch time 1
     * day in the future and increases the retry counter.Set
     *
     * @param page             The page
     * @param prevFetchTime    previous fetch time
     * @param prevModifiedTime previous modified time
     * @param fetchTime        current fetch time
     */
    fun setPageRetrySchedule(page: WebPage, prevFetchTime: Instant, prevModifiedTime: Instant, fetchTime: Instant)

    /**
     * Calculates last fetch time of the given CrawlDatum.
     *
     * @return the date as a long.
     */
    fun calculateLastFetchTime(page: WebPage): Instant

    /**
     * This method provides information whether the page is suitable for selection
     * in the current fetchlist. NOTE: a true return value does not guarantee that
     * the page will be fetched, it just allows it to be included in the further
     * selection process based on scores. The default implementation checks
     * `fetchTime`, if it is higher than the
     *
     * @param curTime it returns false, and true otherwise. It will also check that
     * fetchTime is not too remote (more than `maxInterval),
     * in which case it lowers the interval and returns true.
     * @param page The Web page
     * @param curTime reference time(usually set to the time when the fetch list generation process was started).
     * @return true, if the page should be considered for inclusion in the current fetch list, otherwise false.
     * */
    fun shouldFetch(page: WebPage, curTime: Instant): Boolean

    /**
     * This method resets fetchTime, fetchInterval, modifiedTime and page
     * text, so that it forces refetching.
     *
     * @param page The Web page
     * @param asap if true, force refetch as soon as possible - this sets the
     * fetchTime to now. If false, force refetch whenever the next fetch
     * time is set.
     */
    fun forceRefetch(page: WebPage, asap: Boolean)

}
