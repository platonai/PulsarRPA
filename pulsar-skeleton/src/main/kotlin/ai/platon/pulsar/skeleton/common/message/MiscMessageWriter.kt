package ai.platon.pulsar.skeleton.common.message

import ai.platon.pulsar.common.MultiSinkWriter
import ai.platon.pulsar.persist.WebPage
import java.util.*

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * Write misc messages into misc sinks
 */
class MiscMessageWriter: MultiSinkWriter() {

    fun debugIllegalLastFetchTime(page: WebPage) {
        val report = String.format(
            "ft: {} lft: {}, fc: {} fh: {} status: {}",
            page.fetchTime,
            page.prevFetchTime,
            page.fetchCount,
            "",
            page.protocolStatus,
        )

        write(report, "illegal-last-fetch-time.txt")
    }

    fun debugLongUrls(report: String) {
        write(report, "urls-long.txt")
    }

    fun reportFetchSchedule(page: WebPage, verbose: Boolean) {
        var report = FetchStatusFormatter(page).toString()
        if (verbose) {
            report = page.referrer + " -> " + page.url + "\n" + report
            report += "\n" + page + "\n\n"
        }

        val prefix = if (verbose) "verbose-" else ""
        val category = page.pageCategory.name.lowercase(Locale.getDefault())
        write(report, prefix + "fetch-schedule-$category.txt")
    }

    fun reportBadModifiedTime(report: String) {
        write(report, "bad-modified-urls.txt")
    }
}
