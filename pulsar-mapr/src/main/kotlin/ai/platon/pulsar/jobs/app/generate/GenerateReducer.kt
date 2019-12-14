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

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppFiles.writeBatchId
import ai.platon.pulsar.common.URLUtil.GroupMode
import ai.platon.pulsar.common.Urls.reverseUrl
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.jobs.common.SelectorEntry
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.Mark
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import java.time.Duration

/**
 * Reduce class for generate
 *
 * The #reduce() method write a random integer to all generated URLs. This
 * random number is then used by [FetchMapper].
 */
class GenerateReducer : AppContextAwareGoraReducer<SelectorEntry, GWebPage, String, GWebPage>() {

    companion object {
        enum class Counter { rHosts, rMalformedUrl, rSeeds, rFromSeed; }
        init { MetricsCounters.register(Counter::class.java) }
    }

    private var limit = Int.MAX_VALUE
    private var maxCountPerHost = 100000
    private val hostNames: Multiset<String> = LinkedHashMultiset.create()
    private lateinit var batchId: String
    private lateinit var groupMode: GroupMode
    private lateinit var metricsSystem: MetricsSystem
    private var count = 0

    override fun setup(context: Context) {
        val crawlId = jobConf.get(CapabilityTypes.STORAGE_CRAWL_ID)
        batchId = jobConf.get(CapabilityTypes.BATCH_ID, AppConstants.ALL_BATCHES)
        // Generate top N links only
        limit = jobConf.getUint(CapabilityTypes.GENERATE_TOP_N, Int.MAX_VALUE)
        limit /= context.numReduceTasks
        maxCountPerHost = jobConf.getUint(CapabilityTypes.GENERATE_MAX_TASKS_PER_HOST, 100000)
        groupMode = jobConf.getEnum(CapabilityTypes.FETCH_QUEUE_MODE, GroupMode.BY_HOST)
        metricsSystem = applicationContext.getBean(MetricsSystem::class.java)

        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "batchId", batchId,
                "limit", limit,
                "maxCountPerHost", maxCountPerHost,
                "groupMode", groupMode
        ))
    }

    override fun reduce(key: SelectorEntry, rows: Iterable<GWebPage>, context: Context) {
        metricsCounters.increase(CommonCounter.rRows)
        val url = key.url
        val host = URLUtil.getHost(url, groupMode)
        if (host.isEmpty()) {
            metricsCounters.increase(Counter.rMalformedUrl)
            return
        }

        for (row in rows) {
            val page = WebPage.box(url, row)
            try {
                if (count > limit) {
                    stop("Generated enough pages, quit generator")
                    break
                }
                
                addGeneratedHosts(host)
                
                if (hostNames.count(host) > maxCountPerHost) {
                    LOG.warn("Too many urls in host {}, ignore ...", host)
                    break
                }
                
                updatePage(page)
                context.write(reverseUrl(url), page.unbox())
                ++count
                updateStatus(page, context)
                
                // Write the generated batch id to last-batch-id file
                writeBatchId(batchId)
                metricsSystem.debugSortScore(page)
            } catch (e: Throwable) {
                LOG.error(StringUtil.stringifyException(e))
            }
        } // for
    }

    private fun updatePage(page: WebPage) {
        page.batchId = batchId
        page.generateTime = startTime
        page.marks.remove(Mark.INJECT)
        page.marks.put(Mark.GENERATE, batchId)
    }

    private fun updateStatus(page: WebPage, context: Context) {
        CounterUtils.increaseRDepth(page.distance, metricsCounters)
        if (!page.isSeed) {
            val createTime = page.createTime
            val createdDays = Duration.between(createTime, startTime).toDays()
            CounterUtils.increaseRDays(createdDays, metricsCounters)
        }
        // double check (depth == 0 or has IS-SEED metadata) , can be removed later
        if (page.isSeed) {
            metricsCounters.increase(Counter.rSeeds)
        }
        if (page.pageCategory.isDetail || CrawlFilter.guessPageCategory(page.url).isDetail) {
            metricsCounters.increase(CommonCounter.rDetail)
        }
        metricsCounters.increase(CommonCounter.rPersist)
    }

    override fun cleanup(context: Context) {
        LOG.info("Generated total " + hostNames.elementSet().size + " hosts/domains")
        metricsSystem.reportGeneratedHosts(hostNames.elementSet())
    }

    private fun addGeneratedHosts(host: String) {
        hostNames.add(host)
        pulsarCounters.setValue(Counter.rHosts, hostNames.entrySet().size)
    }
}