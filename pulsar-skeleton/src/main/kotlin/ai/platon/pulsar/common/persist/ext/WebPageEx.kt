package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.CrawlEvent
import ai.platon.pulsar.crawl.LoadEvent
import ai.platon.pulsar.crawl.PageEvent
import ai.platon.pulsar.crawl.SimulateEvent
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import java.time.Instant

val WebPage.event: PageEvent?
    get() = this.options.event

val WebPage.crawlEvent: CrawlEvent?
    get() = this.options.event?.crawlEvent

val WebPage.loadEvent: LoadEvent?
    get() = this.options.event?.loadEvent

val WebPage.simulateEvent: SimulateEvent?
    get() = this.options.event?.simulateEvent

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

fun WebPage.updateFetchTime(prevFetchTime: Instant, fetchTime: Instant) {
    this.prevFetchTime = prevFetchTime
    // the next time supposed to fetch
    this.fetchTime = fetchTime

    val pageExt = WebPageExt(this)
    pageExt.updateFetchTimeHistory(fetchTime)
}
