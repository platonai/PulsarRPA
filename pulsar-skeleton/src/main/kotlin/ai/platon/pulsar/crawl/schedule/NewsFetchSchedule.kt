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
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * This class implements an adaptive re-fetch algorithm.
 *
 * NOTE: values of DEC_FACTOR and INC_FACTOR higher than 0.4f may destabilize
 * the algorithm, so that the fetch interval either increases or decreases
 * infinitely, with little relevance to the page changes
 *
 * @author Vincent Zhang
 */
class NewsFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter? = null
) : AdaptiveFetchSchedule(conf, messageWriter) {
    private val LOG = LoggerFactory.getLogger(NewsFetchSchedule::class.java)

    override fun setFetchSchedule(page: WebPage,
                                  newPrevFetchTime: Instant, prevModifiedTime: Instant,
                                  currentFetchTime: Instant, modifiedTime: Instant, state: Int) {
        var time = modifiedTime
        if (time.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            time = currentFetchTime
        }
        var interval = Duration.ofDays(365 * 10.toLong())
        if (page.isSeed) {
            interval = adjustSeedFetchInterval(page, currentFetchTime, time)
        } else {
            page.marks.put(Mark.INACTIVE, AppConstants.YES_STRING)
        }
        updateRefetchTime(page, interval, currentFetchTime, prevModifiedTime, time)
    }

    private fun adjustSeedFetchInterval(page: WebPage, fetchTime: Instant, modifiedTime: Instant): Duration {
        var time: Instant? = modifiedTime
        val publishTime = page.contentPublishTime
        if (publishTime.isAfter(time)) {
            time = publishTime
        }
        val days = ChronoUnit.DAYS.between(time, fetchTime)
        if (days > 7) {
            messageWriter?.reportFetchSchedule(page, false)
            return Duration.ofHours(1)
        }
        return MIN_INTERVAL
    }
}
