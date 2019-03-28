/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.scoring

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.scoring.ScoreVector
import ai.platon.pulsar.crawl.scoring.ScoringFilter
import ai.platon.pulsar.persist.PageCounters
import ai.platon.pulsar.persist.WebPage

import java.time.Instant
import java.time.temporal.ChronoUnit

open class ContentAnalysisScoringFilter(conf: ImmutableConfig) : ScoringFilter {

    var config: ImmutableConfig

    init {
        this.config = conf
    }

    override fun getParams(): Params {
        return Params()
    }

    override fun reload(conf: ImmutableConfig) {
        this.config = conf
    }

    override fun getConf(): ImmutableConfig {
        return config
    }

    override fun generatorSortValue(page: WebPage, initSort: Float): ScoreVector {
        return ScoreVector("1", (page.score * initSort).toInt())
    }

    /**
     * Increase the score by a sum of inlinked scores.
     */
    override fun updateContentScore(page: WebPage) {
        page.contentScore = calculateContentScore(page)
    }

    protected fun calculateContentScore(page: WebPage): Float {
        val f1 = 1.2f
        val f2 = 1.0f
        val f3 = 1.2f

        val pageCounters = page.pageCounters
        val re = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.entity)
        val ra = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.article)
        val rc = pageCounters.get<PageCounters.Ref>(PageCounters.Ref.ch)
        var days = ChronoUnit.DAYS.between(page.refContentPublishTime, Instant.now())
        if (days > 10 * 365 || days < 0) {
            // Invalid ref publish time, ignore this parameter
            days = 0
        }

        // LOG.info(Params.of("score", score, "re", re, "ra", ra, "rc", rc, "days", days).formatAsLine())

        return f1 * ra + f2 * (rc / 1000) - f3 * days
    }
}
