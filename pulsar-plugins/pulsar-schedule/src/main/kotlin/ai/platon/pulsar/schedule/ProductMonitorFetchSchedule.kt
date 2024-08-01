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

import ai.platon.pulsar.common.config.AppConstants.YES_STRING
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.crawl.filter.CrawlFilter
import ai.platon.pulsar.skeleton.crawl.schedule.AdaptiveFetchSchedule
import ai.platon.pulsar.skeleton.crawl.schedule.ModifyInfo
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import java.time.Duration
import java.time.Instant

/**
 * This class implements an re-fetch algorithm for index-item pattern web sites.
 *
 * @author Vincent Zhang
 */
class ProductMonitorFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter
): AdaptiveFetchSchedule(conf, messageWriter) {

    override fun setFetchSchedule(page: WebPage, m: ModifyInfo) {
        // 1. for every seed, re-fetch it every day
        // 2. for every index page, re-fetch it every day
        // 3. for every detail page, we will re-fetch it 90 days later if there are enough resource
        // 4. for unknown pages, never fetch
        // the page must be parsed

        val fetchInterval: Duration = when {
            page.isSeed -> Duration.ofDays(1L)
            isIndexPage(page) -> Duration.ofDays(1L)
            isDetailPage(page) -> Duration.ofDays(90L)
            else -> Duration.ofDays(365 * 100L)
        }

        if (fetchInterval.toDays() > 365) {
            // Set a mark to accelerate record filtering
            page.marks.put(Mark.INACTIVE, YES_STRING)
        }

        updateRefetchTime(page, fetchInterval, m)
    }

    private fun isIndexPage(page: WebPage): Boolean {
        return page.pageCategory.isIndex || CrawlFilter.guessPageCategory(page.url).isIndex
    }

    private fun isDetailPage(page: WebPage): Boolean {
        return page.pageCategory.isDetail || CrawlFilter.guessPageCategory(page.url).isDetail
    }
}
