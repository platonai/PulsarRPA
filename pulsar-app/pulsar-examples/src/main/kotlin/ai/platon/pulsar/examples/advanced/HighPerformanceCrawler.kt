package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink

class HighPerformanceCrawler {
    private val session = PulsarContexts.createSession()

    fun crawl() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        val args = "-i 10s -ignoreFailure"
        // block unnecessary resources, we must be very careful to choose the resource to block
        val blockingUrls = BlockRule().blockingUrls
        // less interaction with the page, faster crawl speed
        val interactSettings = InteractSettings(initScrollPositions = "0.2,0.5", scrollCount = 0)

        val links = LinkExtractors.fromResource(resource).asSequence()
            .map { ListenableHyperlink(it, "", args = args) }
            .onEach {
                it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
                    driver.addBlockedURLs(blockingUrls)
                    page.conf.putBean(interactSettings)
                }
            }.toList()

        session.submitAll(links)
    }
}

fun main(args: Array<String>) {
    BrowserSettings.maxBrowsers(4).maxOpenTabs(12).withSequentialBrowsers()
    HighPerformanceCrawler().crawl()
    PulsarContexts.await()
}
