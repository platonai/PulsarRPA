package ai.platon.pulsar.common

import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.metadata.CrawlStatusCodes

/**
 * Created by vincent on 17-4-5.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object CounterUtils {
    fun increaseMDays(days: Long, metricsCounters: MetricsCounters) {
        val counter = when (days.toInt()) {
            0 -> CommonCounter.mDay0
            1 -> CommonCounter.mDay1
            2 -> CommonCounter.mDay2
            3 -> CommonCounter.mDay3
            4 -> CommonCounter.mDay4
            5 -> CommonCounter.mDay5
            6 -> CommonCounter.mDay6
            7 -> CommonCounter.mDay7
            else -> CommonCounter.mDayN
        }
        metricsCounters.inc(counter)
    }

    fun increaseRDays(days: Long, metricsCounters: MetricsCounters) {
        val counter = when (days.toInt()) {
            0 -> CommonCounter.rDay0
            1 -> CommonCounter.rDay1
            2 -> CommonCounter.rDay2
            3 -> CommonCounter.rDay3
            4 -> CommonCounter.rDay4
            5 -> CommonCounter.rDay5
            6 -> CommonCounter.rDay6
            7 -> CommonCounter.rDay7
            else -> CommonCounter.rDayN
        }
        metricsCounters.inc(counter)
    }

    fun increaseMDepth(depth: Int, metricsCounters: MetricsCounters) {
        val counter = when (depth) {
            0 -> CommonCounter.mDepth0
            1 -> CommonCounter.mDepth1
            2 -> CommonCounter.mDepth2
            3 -> CommonCounter.mDepth3
            else -> CommonCounter.mDepthN
        }
        metricsCounters.inc(counter)
    }

    fun increaseRDepth(depth: Int, counter: MetricsCounters) {
        when (depth) {
            0 -> counter.inc(CommonCounter.rDepth0)
            1 -> counter.inc(CommonCounter.rDepth1)
            2 -> counter.inc(CommonCounter.rDepth2)
            3 -> counter.inc(CommonCounter.rDepth3)
            else -> counter.inc(CommonCounter.rDepthN)
        }
    }

    fun updateStatusCounter(crawlStatus: CrawlStatus, counter: MetricsCounters) {
        when (crawlStatus.code.toByte()) {
            CrawlStatusCodes.FETCHED -> counter.inc(CommonCounter.stFetched)
            CrawlStatusCodes.REDIR_TEMP -> counter.inc(CommonCounter.stRedirTemp)
            CrawlStatusCodes.REDIR_PERM -> counter.inc(CommonCounter.stRedirPerm)
            CrawlStatusCodes.NOTMODIFIED -> counter.inc(CommonCounter.stNotModified)
            CrawlStatusCodes.RETRY -> counter.inc(CommonCounter.stRetry)
            CrawlStatusCodes.UNFETCHED -> counter.inc(CommonCounter.stUnfetched)
            CrawlStatusCodes.GONE -> counter.inc(CommonCounter.stGone)
            else -> {
            }
        }
    }
}
