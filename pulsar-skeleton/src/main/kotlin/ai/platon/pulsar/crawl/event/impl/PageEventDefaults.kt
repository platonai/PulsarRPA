package ai.platon.pulsar.crawl.event.impl

import ai.platon.pulsar.crawl.CrawlEvent
import ai.platon.pulsar.crawl.LoadEvent
import ai.platon.pulsar.crawl.SimulateEvent
import ai.platon.pulsar.crawl.event.*
import ai.platon.pulsar.crawl.fetch.driver.rpa.BrowseRPA
import ai.platon.pulsar.crawl.fetch.driver.rpa.DefaultBrowseRPA

open class DefaultLoadEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractLoadEvent() {
    override val onBrowserLaunched = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver -> rpa.warnUpBrowser(page, driver) }
    }
}

class DefaultCrawlEvent: AbstractCrawlEvent()

class DefaultEmulateEvent: AbstractEmulateEvent() {
    override val onSniffPageCategory: PageDatumEventHandler = PageDatumEventHandler()
    override val onCheckHtmlIntegrity: PageDatumEventHandler = PageDatumEventHandler()
}

class DefaultSimulateEvent(
    val rpa: BrowseRPA = DefaultBrowseRPA()
): AbstractSimulateEvent() {

    override val onWillFetch = WebPageWebDriverEventHandler().also {
        it.addLast { page, driver ->
            rpa.waitForReferrer(page, driver)
            rpa.waitForPreviousPage(page, driver)
        }
    }
}

open class DefaultPageEvent(
    loadEvent: LoadEvent = DefaultLoadEvent(),
    simulateEvent: SimulateEvent = DefaultSimulateEvent(),
    crawlEvent: CrawlEvent = DefaultCrawlEvent()
): AbstractPageEvent(loadEvent, simulateEvent, crawlEvent)
