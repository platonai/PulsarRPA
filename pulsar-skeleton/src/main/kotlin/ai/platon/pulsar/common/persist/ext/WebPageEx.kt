package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.CrawlEvent
import ai.platon.pulsar.crawl.LoadEvent
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.BrowseEvent
import ai.platon.pulsar.persist.MutableWebPage
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import java.time.Instant

val WebPage.event: PageEvent?
    get() = this.options.rawEvent

val WebPage.crawlEvent: CrawlEvent?
    get() = this.options.rawEvent?.crawlEvent

val WebPage.loadEvent: LoadEvent?
    get() = this.options.rawEvent?.loadEvent

val WebPage.browseEvent: BrowseEvent?
    get() = this.options.rawEvent?.browseEvent

/**
 * Get or create a LoadOptions from the args
 *
 * @return a LoadOptions object.
 */
val WebPage.options: LoadOptions
    get() {
        return variables.variables.computeIfAbsent(VAR_LOAD_OPTIONS) {
            LoadOptions.parse(args, conf)
        } as LoadOptions
    }

/**
 * Get the page label
 */
val WebPage.label: String get() = options.label

fun MutableWebPage.updateFetchTime(prevFetchTime: Instant, fetchTime: Instant) {
    this.prevFetchTime = prevFetchTime
    // the next time supposed to fetch
    this.fetchTime = fetchTime

    val pageExt = WebPageExt(this)
    pageExt.updateFetchTimeHistory(fetchTime)
}
