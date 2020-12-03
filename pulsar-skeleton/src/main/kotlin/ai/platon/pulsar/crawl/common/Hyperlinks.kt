package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.url.StatefulHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

open class ListenableHyperlink(
        val label: String,
        val uuid: String,
        url: String
): StatefulHyperlink(url) {

    var authToken: String? = null
    var remoteAddr: String? = null
    val idleTime get() = Duration.between(modifiedAt, Instant.now())

    var onBeforeLoad: () -> Unit = {}
    var onBeforeFetch: (WebPage) -> Unit = { _: WebPage -> }
    var onAfterFetch: (WebPage) -> Unit = { _: WebPage -> }
    var onBeforeParse: (WebPage) -> Unit = { _: WebPage -> }
    var onBeforeExtract: (WebPage) -> Unit = { _: WebPage -> }
    var onAfterExtract: (WebPage, FeaturedDocument) -> Unit = { _: WebPage, _: FeaturedDocument -> }
    var onAfterParse: (WebPage, FeaturedDocument) -> Unit = { _: WebPage, _: FeaturedDocument -> }
    var onAfterLoad: (WebPage) -> Unit = { _: WebPage -> }

    override fun toString() = "$label $uuid ${super.toString()}"
}
