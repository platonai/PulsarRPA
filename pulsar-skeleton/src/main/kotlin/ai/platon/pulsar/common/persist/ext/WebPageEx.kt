package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.LoadEventHandler
import ai.platon.pulsar.persist.WebPage

val WebPage.loadEventHandler: LoadEventHandler?
    get() = this.conf.getBean(LoadEventHandler::class.java)

/**
 * Get or create a LoadOptions from the args
 *
 * @return a LoadOptions object.
 */
val WebPage.options: LoadOptions get() =
    variables.variables.computeIfAbsent(args) { LoadOptions.parse(args, conf) } as LoadOptions

/**
 * Get the page label
 */
val WebPage.label: String get() = options.label
