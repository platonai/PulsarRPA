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
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.AppPaths.PATH_BANNED_URLS
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.metrics.EnumCounterRegistry
import ai.platon.pulsar.crawl.common.JobInitialized
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.common.URLUtil.GroupMode
import ai.platon.pulsar.crawl.filter.CrawlFilter
import ai.platon.pulsar.crawl.filter.CrawlFilters
import ai.platon.pulsar.crawl.filter.CrawlUrlFilters
import ai.platon.pulsar.crawl.filter.CrawlUrlNormalizers
import ai.platon.pulsar.crawl.schedule.FetchSchedule
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
class GenerateComponent(
    val crawlFilters: CrawlFilters,
    val webDb: WebDb,
    val urlFilters: CrawlUrlFilters,
    val urlNormalizers: CrawlUrlNormalizers,
    val fetchSchedule: FetchSchedule,
    val messageWriter: MiscMessageWriter,
    val conf: ImmutableConfig
) : Parameterized, JobInitialized {

    companion object {
        enum class Counter {
            mSeeds, mBanned, mHostGone, lastGenerated, mBeforeStart, mNotInRange, mUrlMalformed, mNotNormal,
            mUrlFiltered, mUrlOldDate, tieba, bbs, news, blog, mGenerated, mTooDeep,
            mLater, mLater0, mLater1, mLater2, mLater3, mLater4, mLater5, mLater6, mLater7, mLaterN,
            mAhead, mSeedAhead, mSeedLater, mInactive
        }
        init { AppMetrics.reg.register(Counter::class.java) }
    }

    val LOG = LoggerFactory.getLogger(GenerateComponent::class.java)

    private val enumCounters = AppMetrics.reg.enumCounterRegistry
    private val startTime = Instant.now()

    private val unreachableHosts: MutableSet<String> = HashSet()
    private val bannedUrls: MutableSet<String> = HashSet()
    private val keyRange = crawlFilters.maxReversedKeyRange

    private lateinit var crawlId: String
    private lateinit var batchId: String
    private lateinit var fetchMode: FetchMode
    private lateinit var groupMode: GroupMode
    private var reGenerate: Boolean = false
    private var reGenerateSeeds: Boolean = false
    private var filter: Boolean = true
    private var normalise: Boolean = true
    private var maxDistance: Int = -1
    private lateinit var pseudoCurrTime: Instant
    private var topN: Int = -1
    private var lowGeneratedRowsRate = 0.8f
    private var lastGeneratedRows: Int = -1
    private var lowGeneratedRows: Int = -1

    init {
        arrayOf(PATH_BANNED_URLS, PATH_UNREACHABLE_HOSTS).forEach {
            it.takeIf { !Files.exists(it) }?.let { Files.createFile(it) }
        }
        // TODO : move to a filter
        bannedUrls.addAll(FSUtils.readAllLinesSilent(PATH_BANNED_URLS, conf))
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS))
    }

    override fun setup(jobConf: ImmutableConfig) {
        crawlId = jobConf.get(CapabilityTypes.STORAGE_CRAWL_ID, "")
        batchId = jobConf.get(CapabilityTypes.BATCH_ID, AppConstants.ALL_BATCHES)
        fetchMode = jobConf.getEnum(CapabilityTypes.FETCH_MODE, FetchMode.BROWSER)

        groupMode = jobConf.getEnum(CapabilityTypes.FETCH_QUEUE_MODE, GroupMode.BY_HOST)
        reGenerate = jobConf.getBoolean(CapabilityTypes.GENERATE_REGENERATE, false)
        reGenerateSeeds = jobConf.getBoolean(CapabilityTypes.GENERATE_REGENERATE_SEEDS, false)
        filter = jobConf.getBoolean(CapabilityTypes.GENERATE_FILTER, true)
        normalise = jobConf.getBoolean(CapabilityTypes.GENERATE_NORMALISE, true)
        maxDistance = jobConf.getUint(CapabilityTypes.CRAWL_MAX_DISTANCE, AppConstants.DISTANCE_INFINITE)
        pseudoCurrTime = jobConf.getInstant(CapabilityTypes.GENERATE_CUR_TIME, startTime)
        topN = jobConf.getInt(CapabilityTypes.GENERATE_TOP_N, -1)
        lastGeneratedRows = jobConf.getInt(CapabilityTypes.GENERATE_LAST_GENERATED_ROWS, -1)
        lowGeneratedRowsRate = 0.8f
        lowGeneratedRows = (lowGeneratedRowsRate * topN).toInt()
    }

    override fun getParams(): Params {
        return Params.of("className", this.javaClass.simpleName)
                .merge(Params.of(
                        "crawlId", crawlId,
                        "fetchMode", fetchMode,
                        "batchId", batchId,
                        "groupMode", groupMode,
                        "filter", filter,
                        "normalise", normalise,
                        "maxDistance", maxDistance,
                        "reGenerate", reGenerate,
                        "reGenerateSeeds", reGenerateSeeds,
                        "pseudoCurrTime", DateTimes.format(pseudoCurrTime.truncatedTo(ChronoUnit.SECONDS)),
                        "topN", topN,
                        "lastGeneratedRows", lastGeneratedRows,
                        "lowGeneratedRowsRate", lowGeneratedRowsRate,
                        "lowGeneratedRows", lowGeneratedRows,
                        "fetchSchedule", fetchSchedule.javaClass.name,
                        "urlNormalizers", urlNormalizers,
                        "urlFilters", urlFilters,
                        "crawlFilters", crawlFilters,
                        "keyRange", "" + keyRange[0] + " - " + keyRange[1],
                        "unreachableHostsPath", PATH_UNREACHABLE_HOSTS,
                        "unreachableHosts", unreachableHosts.size
                ))
    }

    /**
     * TODO : We may move some filters to hbase query filters directly
     * TODO : Move to CrawlFilter
     */
    fun shouldFetch(url: String, reversedUrl: String, page: WebPage): Pair<Boolean, String> {
        val u: String = url

        if (reGenerateSeeds && page.isSeed) {
            return true to "+reGenerateSeeds"
        }

        if (!checkFetchSchedule(page)) {
            return false to "-FetchSchedule"
        }

        if (!checkHost(u)) {
            return false to "-Host"
        }

        if (bannedUrls.contains(u)) {
            enumCounters.inc(Counter.mBanned)
            return false to "-Banned"
        }

        if (unreachableHosts.contains(URLUtil.getHost(page.url, groupMode))) {
            enumCounters.inc(Counter.mHostGone)
            return false to "-unreachableHost"
        }

        if (page.hasMark(Mark.GENERATE)) {
            enumCounters.inc(Counter.mGenerated)
            /*
             * Fetch entries are generated, empty webpage entries are created in the database(HBase)
             * case 1. another fetcher job is fetching the generated batch. In this case, we should not generate it.
             * case 2. another fetcher job handled the generated batch, but failed, which means the pages are not fetched.
             *
             * There are three ways to fetch pages that are generated but not fetched nor fetching.
             * 1. Restart a text with ignoreGenerated set to be false
             * 2. Resume a FetchJob with resume set to be true
             * */
            if (!reGenerate) {
                val days = Duration.between(page.generateTime, startTime).toDays()
                when {
                    days == 1L -> {
                        // may be used by other jobs, or not fetched correctly
                        return false to "-1days"
                    }
                    days <= 3 -> {
                        // force re-generate
                    }
                    else -> {
                        // ignore pages too old
                        return false to "-TooOld"
                    }
                }
            } else {
                // re-generate
            }
        } // if

        val distanceBias = 0
        // Filter on distance
        if (page.distance > maxDistance + distanceBias) {
            enumCounters.inc(Counter.mTooDeep)
            return false to "-Distance"
        }

        // TODO : Url range filtering should be applied to (HBase) native query filter
        // key before start key
        if (!CrawlFilter.keyGreaterEqual(reversedUrl, keyRange[0])) {
            enumCounters.inc(Counter.mBeforeStart)
            return false to "-BeforeStart"
        }

        // key after end key, finish the mapper
        if (!CrawlFilter.keyLessEqual(reversedUrl, keyRange[1])) {
            //      stop("Complete mapper, reason : hit end key " + reversedUrl
//          + ", upper bound : " + keyRange[1]
//          + ", diff : " + reversedUrl.compareTo(keyRange[1]));
            return false to "-AfterEnd"
        }

        // key not fall in key ranges
        if (!crawlFilters.testKeyRangeSatisfied(reversedUrl)) {
            enumCounters.inc(Counter.mNotInRange)
            return false to "-KeyOutOfRange"
        }

        var u2: String? = u
        // If filtering is on don't generate URLs that don't pass UrlFilters
        if (normalise) {
            u2 = urlNormalizers.normalize(u, CrawlUrlNormalizers.SCOPE_GENERATE_HOST_COUNT)
        }

        if (u2 == null) {
            enumCounters.inc(Counter.mNotNormal)
            return false to "-notNormal"
        }

        if (filter && urlFilters.filter(u2) == null) {
            enumCounters.inc(Counter.mUrlFiltered)
            return false to "-UrlFiltered"
        }

        return true to "+Pass"
    }

    /**
     * Fetch schedule, timing filter
     * TODO: all the logic can be write in the [FetchSchedule]
     */
    private fun checkFetchSchedule(page: WebPage): Boolean {
        if (fetchSchedule.shouldFetch(page, pseudoCurrTime)) {
            return true
        }

        // INACTIVE mark is already filtered in HBase query phrase, double check here for diagnoses
        if (page.hasMark(Mark.INACTIVE)) {
            enumCounters.inc(Counter.mInactive)
        }

        val fetchTime = page.fetchTime
        val hours = ChronoUnit.HOURS.between(pseudoCurrTime, fetchTime)
        if (hours <= 6 && 0 < lastGeneratedRows && lastGeneratedRows < lowGeneratedRows) {
            // TODO : we can expend maxDistance to gain a bigger web graph if the machines are idle
            val fetchInterval = ChronoUnit.HOURS.between(page.prevFetchTime, pseudoCurrTime)
            if (fetchInterval > 6) {
                enumCounters.inc(Counter.mAhead)
                if (page.isSeed) {
                    enumCounters.inc(Counter.mSeedAhead)
                }

                // There are plenty resource to do tasks ahead of time
                return true
            }
        }

        if (hours <= 30 * 24) {
            increaseMDaysLater(hours.toInt() / 24, enumCounters)
            if (page.isSeed) {
                enumCounters.inc(Counter.mSeedLater)
                messageWriter.debugFetchLaterSeeds(page)
            }
        }

        return false
    }

    // Check Host
    private fun checkHost(url: String): Boolean {
        val host = URLUtil.getHost(url, groupMode)
        if (host == null || host.isEmpty()) {
            enumCounters.inc(Counter.mUrlMalformed)
            return false
        }
        if (unreachableHosts.contains(host)) {
            enumCounters.inc(Counter.mHostGone)
            return false
        }
        return true
    }

    private fun increaseMDaysLater(days: Int, enumCounterRegistry: EnumCounterRegistry) {
        val counter: Counter = when (days) {
            0 -> Counter.mLater0
            1 -> Counter.mLater1
            2 -> Counter.mLater2
            3 -> Counter.mLater3
            4 -> Counter.mLater4
            5 -> Counter.mLater5
            6 -> Counter.mLater6
            7 -> Counter.mLater7
            else -> Counter.mLaterN
        }

        enumCounterRegistry.inc(counter)
        enumCounterRegistry.inc(Counter.mLater)
    }
}
