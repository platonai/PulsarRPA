
package ai.platon.pulsar.skeleton.crawl.schedule

import ai.platon.pulsar.skeleton.common.message.MiscMessageWriter
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * This class implements an adaptive re-fetch algorithm.
 *
 * NOTE: values of DEC_FACTOR and INC_FACTOR higher than 0.4f may destabilize
 * the algorithm, so that the fetch interval either increases or decreases
 * infinitely, with little relevance to the page changes
 *
 * @author Vincent Zhang
 */
class NewsFetchSchedule(
        conf: ImmutableConfig,
        messageWriter: MiscMessageWriter? = null
) : AdaptiveFetchSchedule(conf, messageWriter) {
    private val LOG = LoggerFactory.getLogger(NewsFetchSchedule::class.java)

    override fun setFetchSchedule(page: WebPage, m: ModifyInfo) {
        var time = m.modifiedTime
        if (time.isBefore(AppConstants.TCP_IP_STANDARDIZED_TIME)) {
            time = m.fetchTime
        }

        var interval = Duration.ofDays(365 * 10.toLong())
        if (page.isSeed) {
            interval = adjustSeedFetchInterval(page, m.fetchTime, time)
        } else {
            page.marks.put(Mark.INACTIVE, AppConstants.YES_STRING)
        }

        m.fetchTime = time
        updateRefetchTime(page, interval, m)
    }

    private fun adjustSeedFetchInterval(page: WebPage, fetchTime: Instant, modifiedTime: Instant): Duration {
        var time: Instant? = modifiedTime
        val publishTime = page.contentPublishTime
        if (publishTime.isAfter(time)) {
            time = publishTime
        }
        val days = ChronoUnit.DAYS.between(time, fetchTime)
        if (days > 7) {
            messageWriter?.reportFetchSchedule(page, false)
            return Duration.ofHours(1)
        }
        return MIN_INTERVAL
    }
}
