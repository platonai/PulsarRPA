package ai.platon.pulsar.examples.advanced

import ai.platon.pulsar.browser.common.BlockRule
import ai.platon.pulsar.browser.common.InteractSettings
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_ROTATION_URL
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink

class HighPerformanceCrawler {
    private val session = SQLContexts.getOrCreateSession()

    fun crawl() {
        val resource = "seeds/amazon/best-sellers/leaf-categories.txt"
        val args = "-refresh -dropContent"

        // Block unnecessary resources to speed up loading.
        // ⚠️ Be cautious with what you block — some resources may be essential for rendering.
        val blockingUrls = BlockRule().blockingUrls

        // Reduce interaction to increase crawling speed.
        val interactSettings = InteractSettings(
            initScrollPositions = "0.2,0.5",  // Initial scroll positions to simulate user behavior
            scrollCount = 0                   // No additional scrolling
        )
        session.sessionConfig.putBean(interactSettings)

        val links =
            LinkExtractors.fromResource(resource).asSequence().map { ListenableHyperlink(it, "", args = args) }.onEach {
                    it.eventHandlers.browseEventHandlers.onWillNavigate.addLast { page, driver ->
                        driver.addBlockedURLs(blockingUrls)
                    }
                }.toList()

        session.submitAll(links)
    }
}

fun main(args: Array<String>) {
    // Highly recommended to enable proxies, or you will be blocked by Amazon
    val proxyHubURL = "http://localhost:8192/api/proxies"
    if (NetUtil.testHttpNetwork(proxyHubURL)) {
        System.setProperty(PROXY_ROTATION_URL, proxyHubURL)
    }

    PulsarSettings().maxBrowserContexts(2).maxOpenTabs(8).withSequentialBrowsers()
    HighPerformanceCrawler().crawl()
    PulsarContexts.await()
}
