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

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.AppConstants.FETCH_PRIORITY_DEFAULT
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.scoring.Name
import ai.platon.pulsar.crawl.scoring.NamedScoreVector
import ai.platon.pulsar.crawl.scoring.ScoringFilter
import ai.platon.pulsar.persist.WebPage

class ProductMonitorScoringFilter(conf: ImmutableConfig) : ScoringFilter {

    override fun getParams(): Params {
        return Params.of(
                "className", this::class.java.name
        )
    }

    /**
     *
     * */
    override fun injectedScore(page: WebPage) {
        page.cash = page.score
    }

    /**
     * Set to 0.0f (unknown value) - inlink contributions will bring it to a
     * correct level. Newly discovered pages have at least one inlink.
     *
     * Called in update phase
     */
    override fun initialScore(page: WebPage) {
        page.score = 0.0f
        page.cash = 0.0f
    }

    /**
     * Called in generate phase
     * */
    override fun generatorSortValue(page: WebPage, initSort: ScoreVector): ScoreVector {
        val score = NamedScoreVector()

        score.setValue(Name.priority, calculatePriority(page))
        if (isDetail(page)) {
            score.setValue(Name.anchorOrder, -page.anchorOrder)
        }

        return score
    }

    private fun calculatePriority(page: WebPage): Int {
        val priority = when {
            page.isSeed -> {
                SEED_PRIORITY
            }
            isIndex(page) -> {
                INDEX_PAGE_PRIORITY
            }
            isDetail(page) -> {
                DETAIL_PAGE_PRIORITY
            }
            else -> 0
        }

        return priority
    }

    private fun isIndex(page: WebPage): Boolean {
        return page.pageCategory.isIndex || CrawlFilter.getPageCategory(page.url).isIndex
    }

    private fun isDetail(page: WebPage): Boolean {
        return page.pageCategory.isDetail || CrawlFilter.getPageCategory(page.url).isDetail
    }

    companion object {
        private const val INDEX_PAGE_PRIORITY = FETCH_PRIORITY_DEFAULT
        private const val DETAIL_PAGE_PRIORITY = FETCH_PRIORITY_DEFAULT + 200
        private const val SEED_PRIORITY = FETCH_PRIORITY_DEFAULT + 300
    }
}
