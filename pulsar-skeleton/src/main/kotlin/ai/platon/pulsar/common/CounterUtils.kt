package ai.platon.pulsar.common

import ai.platon.pulsar.persist.CrawlStatus

/**
 * Created by vincent on 17-4-5.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object CounterUtils {
    fun increaseMDays(days: Long, metricsCounters: MetricsCounters) {
        val counter = when (days) {
            0L -> {
                CommonCounter.mDay0
            }
            1L -> {
                CommonCounter.mDay1
            }
            2L -> {
                CommonCounter.mDay2
            }
            3L -> {
                CommonCounter.mDay3
            }
            4L -> {
                CommonCounter.mDay4
            }
            5L -> {
                CommonCounter.mDay5
            }
            6L -> {
                CommonCounter.mDay6
            }
            7L -> {
                CommonCounter.mDay7
            }
            else -> {
                CommonCounter.mDayN
            }
        }
        metricsCounters.inc(counter)
    }

    fun increaseRDays(days: Long, metricsCounters: MetricsCounters) {
        val counter = when (days) {
            0L -> {
                CommonCounter.rDay0
            }
            1L -> {
                CommonCounter.rDay1
            }
            2L -> {
                CommonCounter.rDay2
            }
            3L -> {
                CommonCounter.rDay3
            }
            4L -> {
                CommonCounter.rDay4
            }
            5L -> {
                CommonCounter.rDay5
            }
            6L -> {
                CommonCounter.rDay6
            }
            7L -> {
                CommonCounter.rDay7
            }
            else -> {
                CommonCounter.rDayN
            }
        }
        metricsCounters.inc(counter)
    }

    fun increaseMDepth(depth: Int, metricsCounters: MetricsCounters) {
        val counter = when (depth) {
            0 -> {
                CommonCounter.mDepth0
            }
            1 -> {
                CommonCounter.mDepth1
            }
            2 -> {
                CommonCounter.mDepth2
            }
            3 -> {
                CommonCounter.mDepth3
            }
            else -> {
                CommonCounter.mDepthN
            }
        }
        metricsCounters.inc(counter)
    }

    fun increaseRDepth(depth: Int, counter: MetricsCounters) {
        when (depth) {
            0 -> {
                counter.inc(CommonCounter.rDepth0)
            }
            1 -> {
                counter.inc(CommonCounter.rDepth1)
            }
            2 -> {
                counter.inc(CommonCounter.rDepth2)
            }
            3 -> {
                counter.inc(CommonCounter.rDepth3)
            }
            else -> {
                counter.inc(CommonCounter.rDepthN)
            }
        }
    }

    fun updateStatusCounter(crawlStatus: CrawlStatus, counter: MetricsCounters) {
        when (crawlStatus.code.toByte()) {
            CrawlStatus.FETCHED -> counter.inc(CommonCounter.stFetched)
            CrawlStatus.REDIR_TEMP -> counter.inc(CommonCounter.stRedirTemp)
            CrawlStatus.REDIR_PERM -> counter.inc(CommonCounter.stRedirPerm)
            CrawlStatus.NOTMODIFIED -> counter.inc(CommonCounter.stNotModified)
            CrawlStatus.RETRY -> counter.inc(CommonCounter.stRetry)
            CrawlStatus.UNFETCHED -> counter.inc(CommonCounter.stUnfetched)
            CrawlStatus.GONE -> counter.inc(CommonCounter.stGone)
            else -> {
            }
        }
    }
}
