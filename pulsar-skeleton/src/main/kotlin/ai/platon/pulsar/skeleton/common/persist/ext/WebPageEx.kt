package ai.platon.pulsar.skeleton.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageExt
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.BrowseEventHandlers
import ai.platon.pulsar.skeleton.crawl.LoadEventHandlers
import ai.platon.pulsar.skeleton.crawl.PageEventHandlers
import java.time.Instant

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

@Deprecated("Use eventHandlers instead", ReplaceWith("eventHandlers"))
val WebPage.event: PageEventHandlers?
    get() = eventHandlers

val WebPage.eventHandlers: PageEventHandlers?
    get() = this.options.rawEvent

val WebPage.loadEventHandlers: LoadEventHandlers?
    get() = this.options.rawEvent?.loadEventHandlers

val WebPage.browseEventHandlers: BrowseEventHandlers?
    get() = this.options.rawEvent?.browseEventHandlers

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
