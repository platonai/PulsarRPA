package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink

class HighPerformanceCrawler {
    private val session = PulsarContexts.getOrCreateSession()

    fun crawl() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        val args = "-refresh"
        // block unnecessary resources, we must be very careful to choose the resource to block
        val blockingUrls = BlockRule().blockingUrls
        // less interaction with the page, faster crawl speed
        val interactSettings = InteractSettings(initScrollPositions = "0.2,0.5", scrollCount = 0)
        session.sessionConfig.putBean(interactSettings)

        val links = LinkExtractors.fromResource(resource).asSequence()
            .map { ListenableHyperlink(it, "", args = args) }
            .onEach {
                it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
                    driver.addBlockedURLs(blockingUrls)
                }
            }.toList().take(2)

        session.submitAll(links)
    }
}

fun main(args: Array<String>) {
    // Highly recommended to enable proxies, or you will be blocked by Amazon
    // System.setProperty("PROXY_HUB_URL", "http://localhost:8192/api/proxies")

    PulsarSettings().maxBrowserContexts(4).maxOpenTabs(12).withSequentialBrowsers()
    HighPerformanceCrawler().crawl()
    PulsarContexts.await()
}
