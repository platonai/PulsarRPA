package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.LoadEventHandler
import ai.platon.pulsar.persist.WebPage
import java.time.Duration
import java.time.Instant

val WebPage.loadEventHandler: LoadEventHandler?
    get() = this.conf.getBean(LoadEventHandler::class.java)

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

fun WebPage.updateFetchTime(newPrevFetchTime: Instant, nextFetchTime: Instant) {
    prevFetchTime = newPrevFetchTime
    fetchTime = nextFetchTime

    updateFetchTimeHistory(prevFetchTime)
}

/**
 * Update the fetch time
 * @param newPrevFetchTime The new prev fetch time, (new prev fetch time) = (page.fetchTime before update)
 * @param currentFetchTime The current fetch time, it's almost now
 * @param fetchInterval0 The interval between now and the next fetch
 * */
fun WebPage.updateFetchTime(newPrevFetchTime: Instant, currentFetchTime: Instant, newFetchInterval: Duration) {
    fetchInterval = newFetchInterval
    prevFetchTime = newPrevFetchTime
    // the next time supposed to fetch
    fetchTime = currentFetchTime + fetchInterval

    updateFetchTimeHistory(prevFetchTime)
}
