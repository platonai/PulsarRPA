/*******************************************************************************
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
package ai.platon.pulsar.jobs.app.homepage

import ai.platon.pulsar.common.CommonCounter
import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.common.URLPartitioner.SelectorEntryPartitioner
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.jobs.core.Mapper
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.scoring.ContentAnalysisScoringFilter
import java.io.IOException

class TopPageHomeUpdateJob : HomePageUpdateJob() {
    override fun setIndexHomeUrl() {
        jobConf[CapabilityTypes.STAT_INDEX_HOME_URL] = PulsarConstants.TOP_PAGE_HOME_URL
    }

    public override fun initJob() {
        initMapper(currentJob, FIELDS, SelectorEntry::class.java,
                GWebPage::class.java, TopPageHomeIndexMapper::class.java, SelectorEntryPartitioner::class.java,
                queryFilter, false)
        initReducer<SelectorEntry, GWebPage>(currentJob, HomePageUpdateReducer::class.java)
    }

    class TopPageHomeIndexMapper : AppContextAwareGoraMapper<String, GWebPage, SelectorEntry, GWebPage>() {
        private lateinit var scoringFilter: ContentAnalysisScoringFilter

        public override fun setup(context: Context) {
            scoringFilter = ContentAnalysisScoringFilter(conf)
            Params.of(
                    "className", this.javaClass.simpleName,
                    "scoringFilter", scoringFilter)
                    .merge(scoringFilter.params)
                    .withLogger(LOG).info()
        }

        override fun map(reversedUrl: String, row: GWebPage, context: Context) {
            metricsCounters.increase(CommonCounter.mRows)
            val page = WebPage.box(reversedUrl, row, true)
            val url = page.url
            if (page.pageCategory.isDetail) {
                return
            }
            scoringFilter.updateContentScore(page)
            if (page.contentScore < 10) {
                return
            }
            val sortScore = ScoreVector("1", page.contentScore.toInt())
            context.write(SelectorEntry(url, sortScore), page.unbox())
            metricsCounters.increase(CommonCounter.mPersist)
        }
    }
}