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
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.PulsarConstants
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.common.URLPartitioner.SelectorEntryPartitioner
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import ai.platon.pulsar.jobs.core.Mapper
import ai.platon.pulsar.jobs.core.PulsarJob
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.system.exitProcess

class SeedHomeUpdateJob : HomePageUpdateJob() {
    override fun setIndexHomeUrl() {
        jobConf[CapabilityTypes.STAT_INDEX_HOME_URL] = PulsarConstants.SEED_HOME_URL
    }

    @Throws(Exception::class)
    public override fun initJob() {
        PulsarJob.initMapper(currentJob, FIELDS, SelectorEntry::class.java,
                GWebPage::class.java, SeedIndexMapper::class.java, SelectorEntryPartitioner::class.java,
                queryFilter, false)
        PulsarJob.initReducer<SelectorEntry, GWebPage>(currentJob, HomePageUpdateReducer::class.java)
    }

    class SeedIndexMapper : AppContextAwareGoraMapper<String, GWebPage, SelectorEntry, GWebPage>() {
        private var scoringFilters: ScoringFilters? = null
        @Throws(IOException::class, InterruptedException::class)
        public override fun setup(context: Context) {
            scoringFilters = applicationContext.getBean(ScoringFilters::class.java)
            Params.of(
                    "className", this.javaClass.simpleName,
                    "scoringFilters", scoringFilters)
                    .merge(scoringFilters!!.params)
                    .withLogger(LOG).info()
        }

        override fun map(reversedUrl: String, row: GWebPage, context: Context) {
            metricsCounters.increase(CommonCounter.mRows)
            val page = WebPage.box(reversedUrl, row, true)
            val url = page.url
            if (!page.isSeed) {
                return
            }
            val sortScore = scoringFilters!!.generatorSortValue(page, 1.0f)
            context.write(SelectorEntry(url, sortScore), page.unbox())
            metricsCounters.increase(CommonCounter.mPersist)
        }
    }
}

fun main(args: Array<String>) {
    val configLocation = System.getProperty(CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION, PulsarConstants.JOB_CONTEXT_CONFIG_LOCATION)
    val res = AppContextAwareJob.run(configLocation, SeedHomeUpdateJob(), args)
    exitProcess(res)
}
