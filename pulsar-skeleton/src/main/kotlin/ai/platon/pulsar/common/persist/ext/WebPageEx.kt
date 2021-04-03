package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.LoadEventHandler
import ai.platon.pulsar.crawl.common.FetchReason
import ai.platon.pulsar.persist.WebPage

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

val WebPage.isExpired: Boolean get() = options.isExpired(lastFetchTime)

/**
 * Get the page label
 */
val WebPage.label: String get() = options.label
