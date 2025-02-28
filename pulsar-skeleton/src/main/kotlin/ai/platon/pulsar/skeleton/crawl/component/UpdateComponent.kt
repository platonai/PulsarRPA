
package ai.platon.pulsar.skeleton.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import ai.platon.pulsar.persist.metadata.CrawlStatusCodes
import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.skeleton.common.metrics.MetricsSystem
import ai.platon.pulsar.skeleton.crawl.schedule.DefaultFetchSchedule
import ai.platon.pulsar.skeleton.crawl.schedule.FetchSchedule
import ai.platon.pulsar.skeleton.crawl.schedule.ModifyInfo
import ai.platon.pulsar.skeleton.crawl.scoring.ScoringFilters
import ai.platon.pulsar.skeleton.signature.SignatureComparator
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * The update component.
 */
class UpdateComponent(
    val webDb: WebDb,
    val fetchSchedule: FetchSchedule,
    val scoringFilters: ScoringFilters? = null,
    val messageWriter: MiscMessageWriter? = null,
    val conf: ImmutableConfig,
) : Parameterized {
    private val LOG = LoggerFactory.getLogger(UpdateComponent::class.java)

    companion object {
        enum class Counter { rCreated, rNewDetail, rPassed, rLoaded, rNotExist, rDepthUp, rUpdated, rTotalUpdates, rBadModTime }

        init {
            MetricsSystem.reg.register(Counter::class.java)
        }
    }

    private val enumCounters = MetricsSystem.reg.enumCounterRegistry

    constructor(webDb: WebDb, conf: ImmutableConfig) : this(webDb, DefaultFetchSchedule(conf), null, null, conf)

    override fun getParams(): Params {
        return Params.of(
            "className", this.javaClass.simpleName,
            "fetchSchedule", fetchSchedule.javaClass.simpleName
        )
    }

    fun updateFetchSchedule(page: WebPage) {
        if (page.marks.isInactive) {
            return
        }

        val crawlStatus = page.crawlStatus
        val m = handleModifiedTime(page, crawlStatus)

        when (crawlStatus.code.toByte()) {
            CrawlStatusCodes.FETCHED,
            CrawlStatusCodes.REDIR_TEMP,
            CrawlStatusCodes.REDIR_PERM,
            CrawlStatusCodes.NOTMODIFIED,
            -> {
                val now = Instant.now()
                require(Duration.between(m.fetchTime, now).seconds < 1) {
                    "The actual fetch time should be very close to now. Now: $now FetchTime: ${m.fetchTime}"
                }

                fetchSchedule.setFetchSchedule(page, m)

                // do not enable the force fetch feature
//                val enableForceFetch = false
//                val fetchInterval = page.fetchInterval
//                if (enableForceFetch && fetchInterval > fetchSchedule.maxFetchInterval) {
//                    LOG.info("Force re-fetch page with interval {} | {}", fetchInterval, page.url)
//                    fetchSchedule.forceRefetch(page, m.prevFetchTime, false)
//                }
            }
            CrawlStatusCodes.RETRY -> {
                fetchSchedule.setPageRetrySchedule(page, m.prevFetchTime, m.prevModifiedTime, m.fetchTime)
            }
            CrawlStatusCodes.GONE -> fetchSchedule.setPageGoneSchedule(
                page, m.prevFetchTime, m.prevModifiedTime, m.fetchTime)
        }
    }

    private fun handleModifiedTime(page: WebPage, crawlStatus: CrawlStatus): ModifyInfo {
        val pageExt = WebPageExt(page)

        // page.fetchTime is not the actual fetch time!
        val prevFetchTime = page.fetchTime
        val fetchTime = Instant.now()

        var prevModifiedTime = page.prevModifiedTime
        var modifiedTime = page.modifiedTime
        val newModifiedTime = pageExt.sniffModifiedTime()

        var modified = FetchSchedule.STATUS_UNKNOWN
        if (crawlStatus.code == CrawlStatusCodes.NOTMODIFIED.toInt()) {
            modified = FetchSchedule.STATUS_NOTMODIFIED
        }

        val prevSig = page.prevSignature
        val signature = page.signature
        if (prevSig != null && signature != null) {
            modified = if (SignatureComparator.compare(prevSig, signature) != 0) {
                FetchSchedule.STATUS_MODIFIED
            } else {
                FetchSchedule.STATUS_NOTMODIFIED
            }
        }

        if (newModifiedTime.isAfter(modifiedTime)) {
            prevModifiedTime = modifiedTime
            modifiedTime = newModifiedTime
        }

        if (modifiedTime.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            handleBadModified(page)
        }

        return ModifyInfo(fetchTime, prevFetchTime, prevModifiedTime, modifiedTime, modified)
    }

    private fun handleBadModified(page: WebPage) {
        enumCounters.inc(Counter.rBadModTime)
        messageWriter?.reportBadModifiedTime(Params.of(
            "PFT", page.prevFetchTime, "FT", page.fetchTime,
            "PMT", page.prevModifiedTime, "MT", page.modifiedTime,
            "HMT", page.headers.lastModified,
            "U", page.url
        ).formatAsLine())
    }
}
