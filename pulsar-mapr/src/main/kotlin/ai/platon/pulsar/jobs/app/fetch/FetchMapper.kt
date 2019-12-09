package ai.platon.pulsar.jobs.app.fetch

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.CounterUtils
import ai.platon.pulsar.common.LocalFSUtils
import ai.platon.pulsar.common.MetricsCounters
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.jobs.common.FetchEntryWritable
import ai.platon.pulsar.jobs.core.AppContextAwareGoraMapper
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.apache.hadoop.io.IntWritable
import java.util.*

/**
 *
 *
 * Mapper class for Fetcher.
 *
 *
 *
 * This class reads the random integer written by [GenerateJob] as its
 * key while outputting the actual key and value arguments through a
 * [FetchEntryWritable] instance.
 *
 *
 *
 * This approach (combined with the use of PartitionUrlByHost makes
 * sure that Fetcher is still polite while also randomizing the key order. If
 * one host has a huge number of URLs in your table while other hosts have
 * not, [FetchReducer] will not be stuck on one host but process URLs
 * from other hosts as well.
 *
 */
class FetchMapper : AppContextAwareGoraMapper<String, GWebPage, IntWritable, FetchEntryWritable>() {

    companion object {
        enum class Counter { mNotGenerated, mFetched, mHostGone, mSeeds }
        init { MetricsCounters.register(Counter::class.java) }
    }

    private var resume = false
    private var limit = 1000000
    private var count = 0
    private val random = Random()
    private val unreachableHosts: MutableSet<String> = HashSet()

    public override fun setup(context: Context) {
        val numTasks = jobConf.getInt(CapabilityTypes.MAPREDUCE_JOB_REDUCES, 2)
        limit = jobConf.getUint(CapabilityTypes.MAPPER_LIMIT, 1000000)
        limit = if (limit < 2 * numTasks) limit else limit / numTasks
        resume = jobConf.getBoolean(CapabilityTypes.RESUME, false)
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(AppPaths.PATH_UNREACHABLE_HOSTS))
        val crawlId = jobConf[CapabilityTypes.STORAGE_CRAWL_ID]
        val batchId = jobConf[CapabilityTypes.BATCH_ID]
        val fetchMode = jobConf[CapabilityTypes.FETCH_MODE]

        LOG.info(Params.format(
                "className", this.javaClass.simpleName,
                "crawlId", crawlId,
                "batchId", batchId,
                "fetchMode", fetchMode,
                "resume", resume,
                "numTasks", numTasks,
                "limit", limit,
                "unreachableHostsPath", AppPaths.PATH_UNREACHABLE_HOSTS,
                "unreachableHosts", unreachableHosts.size
        ))
    }

    /**
     * Rows are filtered by batchId first in FetchJob setup, which can be a range search, the time complex is O(ln(N))
     * and then filtered by mapper, which is a scan, the time complex is O(N)
     */
    override fun map(reversedUrl: String, row: GWebPage, context: Context) {
        val page = WebPage.box(reversedUrl, row, true)
        if (!page.hasMark(Mark.GENERATE)) {
            metricsCounters.increase(Counter.mNotGenerated)
            return
        }

        /*
         * Resume the batch, but ignore rows that are already fetched.
         * If FetchJob runs again and has -resume flag, the pages already fetched in the batch should be fetched again.
         * */
        if (page.hasMark(Mark.FETCH)) {
            // The page has both GENERATE mark and FETCH mark
            metricsCounters.increase(Counter.mFetched)
            if (!resume) return
        }
        // Higher priority comes first
        val shuffleOrder = random.nextInt(10000) - 10000 * page.fetchPriority
        context.write(IntWritable(shuffleOrder), FetchEntryWritable(jobConf.unbox(), page.key, page))
        updateStatus(page)
        if (++count > limit) {
            stop("Hit limit $limit, finish the mapper.")
        }
    }

    private fun updateStatus(page: WebPage) {
        CounterUtils.increaseMDepth(page.distance, metricsCounters)
        if (page.isSeed) {
            metricsCounters.increase(Counter.mSeeds)
        }
    }
}
