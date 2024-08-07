
package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.AppPaths.PATH_BANNED_URLS
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.FSUtils
import ai.platon.pulsar.common.LocalFSUtils
import ai.platon.pulsar.common.config.*
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.common.metrics.EnumCounterRegistry
import ai.platon.pulsar.skeleton.common.metrics.MetricsSystem
import ai.platon.pulsar.skeleton.crawl.common.JobInitialized
import ai.platon.pulsar.skeleton.crawl.common.URLUtil
import ai.platon.pulsar.skeleton.crawl.common.URLUtil.GroupMode
import ai.platon.pulsar.skeleton.crawl.filter.ChainedUrlNormalizer
import ai.platon.pulsar.skeleton.crawl.filter.CrawlFilters
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilters
import ai.platon.pulsar.skeleton.crawl.schedule.FetchSchedule
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The generate component.
 */
class GenerateComponent(
    val crawlFilters: CrawlFilters,
    val webDb: WebDb,
    val urlFilters: CrawlUrlFilters,
    val urlNormalizers: ChainedUrlNormalizer,
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
        init { MetricsSystem.reg.register(Counter::class.java) }
    }

    val LOG = LoggerFactory.getLogger(GenerateComponent::class.java)

    private val enumCounters = MetricsSystem.reg.enumCounterRegistry
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
     * Fetch schedule, timing filter
     * TODO: all the logic can be write in the [FetchSchedule]
     */
    private fun checkFetchSchedule(page: WebPage): Boolean {
        if (fetchSchedule.shouldFetch(page, pseudoCurrTime)) {
            return true
        }

        // INACTIVE mark is already filtered in HBase query phase, double check here for diagnoses
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
