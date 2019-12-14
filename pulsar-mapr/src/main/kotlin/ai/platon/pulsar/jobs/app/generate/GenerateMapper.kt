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
package ai.platon.pulsar.jobs.app.generate

import ai.platon.pulsar.common.CommonCounter
import ai.platon.pulsar.common.CounterUtils
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.component.GenerateComponent
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.scoring.ScoringFilters
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.jobs.core.Mapper
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.util.*

class GenerateMapper : AppContextAwareGoraMapper<String, GWebPage, SelectorEntry, GWebPage>() {

    private val LOG = LoggerFactory.getLogger(Mapper::class.java)

    private lateinit var generateComponent: GenerateComponent
    private lateinit var scoringFilters: ScoringFilters
    private val unreachableHosts: Set<String> = HashSet()

    public override fun setup(context: Context) {
        generateComponent = applicationContext.getBean(GenerateComponent::class.java)
        scoringFilters = applicationContext.getBean(ScoringFilters::class.java)

        generateComponent.setup(jobConf)

        Params.of(
                "className", this.javaClass.simpleName,
                "ignoreExternalLinks", jobConf.get(CapabilityTypes.PARSE_IGNORE_EXTERNAL_LINKS),
                "maxUrlLength", jobConf.get(CapabilityTypes.PARSE_MAX_URL_LENGTH),
                "defaultAnchorLenMin", jobConf.get(CapabilityTypes.PARSE_MIN_ANCHOR_LENGTH),
                "defaultAnchorLenMax", jobConf.get(CapabilityTypes.PARSE_MAX_ANCHOR_LENGTH),
                "unreachableHosts", unreachableHosts.size
        )
                .merge(generateComponent.params)
                .merge(scoringFilters.params)
                .withLogger(LOG).info()
    }

    override fun map(reversedUrl: String, row: GWebPage, context: Context) {
        metricsCounters.increase(CommonCounter.mRows)
        val page = WebPage.box(reversedUrl, row, true)
        val url = page.url

        if (!generateComponent.shouldFetch(url, reversedUrl, page)) {
            return
        }

        // metricsSystem.report(page);
        val selectorEntry = SelectorEntry(url, scoringFilters.generatorSortValue(page, 1.0f))
        page.sortScore = selectorEntry.sortScore

        output(selectorEntry, page, context)

        updateStatus(page)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun output(entry: SelectorEntry, page: WebPage, context: Context) {
        context.write(entry, page.unbox())
    }

    private fun updateStatus(page: WebPage) {
        if (page.isSeed) {
            metricsCounters.increase(GenerateComponent.Companion.Counter.mSeeds)
        }

        if (page.pageCategory.isDetail || CrawlFilter.guessPageCategory(page.url).isDetail) {
            metricsCounters.increase(CommonCounter.mDetail)
        }

        CounterUtils.increaseMDepth(page.distance, metricsCounters)
        if (!page.isSeed) {
            val pendingDays = Duration.between(page.createTime, startTime).toDays()
            CounterUtils.increaseMDays(pendingDays, metricsCounters)
        }
        metricsCounters.increase(CommonCounter.mPersist)
    }
}
