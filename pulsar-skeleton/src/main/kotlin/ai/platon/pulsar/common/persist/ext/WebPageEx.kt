package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.CrawlEventHandler
import ai.platon.pulsar.persist.WebPage

val WebPage.eventHandler: CrawlEventHandler?
    get() {
        return volatileConfig?.getBean(CrawlEventHandler::class.java)
    }

/**
 *
 * Get the load options
 *
 * @return a [java.lang.String] object.
 */
val WebPage.loadOptions: LoadOptions get() {
    return variables.variables.computeIfAbsent(args) { LoadOptions.parse(args) } as LoadOptions
}

/**
 *
 * Get the label
 *
 * @return a [java.lang.String] object.
 */
val WebPage.label: String get() {
    return loadOptions.label
}
