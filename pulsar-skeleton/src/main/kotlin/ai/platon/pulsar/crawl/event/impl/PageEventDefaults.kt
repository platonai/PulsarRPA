package ai.platon.pulsar.crawl.event.impl

import ai.platon.pulsar.crawl.CrawlEvent
import ai.platon.pulsar.crawl.LoadEvent
import ai.platon.pulsar.crawl.BrowseEvent
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.crawl.fetch.driver.rpa.BrowseRPA
import ai.platon.pulsar.crawl.fetch.driver.rpa.DefaultBrowseRPA

open class DefaultLoadEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractLoadEvent()

open class DefaultCrawlEvent: AbstractCrawlEvent()

class PerfectTraceBrowseEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractBrowseEvent() {

    override val onBrowserLaunched = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver -> rpa.warnUpBrowser(page, driver) }
    }

    override val onWillFetch = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver ->
            rpa.waitForReferrer(page, driver)
            rpa.waitForPreviousPage(page, driver)
        }
    }
}

class EmptyBrowseEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractBrowseEvent() {

}

typealias DefaultBrowseEvent = EmptyBrowseEvent

/**
 * TODO: use a page event factory
 * */
open class DefaultPageEvent(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    browseEvent: BrowseEvent = DefaultBrowseEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
): AbstractPageEvent(loadEvent, browseEvent, crawlEvent)
