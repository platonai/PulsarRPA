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
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

/**
 * This class implements an adaptive re-fetch algorithm. This works as follows:
 *
 *  * for pages that has changed since the last fetchTime, decrease their
 * fetchInterval by a factor of DEC_FACTOR (default value is 0.2f).
 *  * for pages that haven't changed since the last fetchTime, increase their
 * fetchInterval by a factor of INC_FACTOR (default value is 0.2f).<br></br>
 * If SYNC_DELTA property is true, then:
 *
 *  * calculate a `delta = fetchTime - modifiedTime`
 *  * try to synchronize with the time of change, by shifting the next
 * fetchTime by a fraction of the difference between the last modification time
 * and the last fetch time. I.e. the next fetch time will be set to
 * `fetchTime + fetchInterval - delta * SYNC_DELTA_RATE`
 *  * if the adjusted fetch interval is bigger than the delta, then
 * `fetchInterval = delta`.
 *
 *
 *  * the minimum value of fetchInterval may not be smaller than MIN_INTERVAL
 * (default is 1 minute).
 *  * the maximum value of fetchInterval may not be bigger than MAX_INTERVAL
 * (default is 365 days).
 *
 *
 *
 * NOTE: values of DEC_FACTOR and INC_FACTOR higher than 0.4f may destabilize
 * the algorithm, so that the fetch interval either increases or decreases
 * infinitely, with little relevance to the page changes. Please use
 * [.] method to test the values before applying them in a
 * production system.
 *
 *
 * @author Andrzej Bialecki
 */
open class AdaptiveFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter? = null
) : AbstractFetchSchedule(conf, messageWriter) {

    protected var INC_RATE = conf.getFloat(CapabilityTypes.SCHEDULE_INC_RATE, 0.2f)
    protected var DEC_RATE = conf.getFloat(CapabilityTypes.SCHEDULE_DEC_RATE, 0.2f)
    protected var MIN_INTERVAL = conf.getDuration(CapabilityTypes.SCHEDULE_MIN_INTERVAL, Duration.ofMinutes(10))
    protected var MAX_INTERVAL = conf.getDuration(CapabilityTypes.SCHEDULE_MAX_INTERVAL, Duration.ofDays(365))
    protected var SEED_MAX_INTERVAL = conf.getDuration(CapabilityTypes.SCHEDULE_SEED_MAX_INTERVAL, Duration.ofDays(1))
    protected var SYNC_DELTA = conf.getBoolean(CapabilityTypes.SCHEDULE_SYNC_DELTA, true)
    protected var SYNC_DELTA_RATE = conf.getFloat(CapabilityTypes.SCHEDULE_SYNC_DELTA_RATE, 0.2f).toDouble()
    protected var maxDistance = conf.getInt(CapabilityTypes.CRAWL_MAX_DISTANCE, AppConstants.DISTANCE_INFINITE)

    override fun getParams(): Params {
        return Params.of(
                "className", javaClass.simpleName,
                "MIN_INTERVAL", MIN_INTERVAL,
                "MAX_INTERVAL", MAX_INTERVAL,
                "SEED_MAX_INTERVAL", SEED_MAX_INTERVAL
        ).merge(super.getParams())
    }

    override fun setFetchSchedule(
        page: WebPage, newPrevFetchTime: Instant,
        prevModifiedTime: Instant, currentFetchTime: Instant, modifiedTime: Instant, state: Int) {
        var newModifiedTime = modifiedTime
        super.setFetchSchedule(page, newPrevFetchTime, prevModifiedTime, currentFetchTime, newModifiedTime, state)
        if (newModifiedTime < AppConstants.TCP_IP_STANDARDIZED_TIME) {
            newModifiedTime = currentFetchTime
        }

        val newInterval = getFetchInterval(page, currentFetchTime, newModifiedTime, state)
        updateRefetchTime(page, newInterval, currentFetchTime, prevModifiedTime, newModifiedTime)
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
        page: WebPage, newPrevFetchTime: Instant, prevModifiedTime: Instant, currentFetchTime: Instant) {
        val prevInterval = page.fetchInterval.seconds.toFloat()
        var newInterval = prevInterval
        // no page is truly GONE ... just increase the interval by 50%
        // and try much later.
        newInterval = if (newInterval < maxFetchInterval.seconds) {
            prevInterval * 1.5f
        } else {
            maxFetchInterval.seconds * 0.9f
        }

        page.setFetchInterval(newInterval)
        page.fetchTime = currentFetchTime.plus(page.fetchInterval)
    }

    protected fun getFetchInterval(page: WebPage, fetchTime_: Instant, modifiedTime: Instant, state: Int): Duration {
        var fetchTime = fetchTime_
        var interval = page.fetchInterval.seconds
        when (state) {
            FetchSchedule.STATUS_MODIFIED -> interval *= (1.0f - DEC_RATE).toLong()
            FetchSchedule.STATUS_NOTMODIFIED -> interval *= (1.0f + INC_RATE).toLong()
            FetchSchedule.STATUS_UNKNOWN -> {
            }
        }

        if (SYNC_DELTA) {
            val gap = fetchTime.epochSecond - modifiedTime.epochSecond
            if (gap > interval) {
                interval = gap
            }
            // TODO : check fetch time
            fetchTime = fetchTime.minusSeconds(Math.round(gap * SYNC_DELTA_RATE))
        }

        var newInterval = Duration.ofSeconds(interval)
        if (newInterval < MIN_INTERVAL) newInterval = MIN_INTERVAL
        if (newInterval > MAX_INTERVAL) newInterval = MAX_INTERVAL

        return newInterval
    }
}
