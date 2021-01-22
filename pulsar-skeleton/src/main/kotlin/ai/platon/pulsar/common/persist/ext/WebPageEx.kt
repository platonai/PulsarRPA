package ai.platon.pulsar.common.persist.ext

import ai.platon.pulsar.crawl.CrawlEventHandler
import ai.platon.pulsar.persist.WebPage

val WebPage.eventHandler: CrawlEventHandler?
    get() {
        return volatileConfig?.getBean(CrawlEventHandler::class.java)
    }
