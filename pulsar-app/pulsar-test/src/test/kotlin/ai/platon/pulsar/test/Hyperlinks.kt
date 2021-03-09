package ai.platon.pulsar.test

import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.url.UrlAware
import ai.platon.pulsar.crawl.AbstractCrawlEventHandler
import ai.platon.pulsar.crawl.AbstractLoadEventHandler
import ai.platon.pulsar.crawl.CrawlEventHandler
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.CountDownLatch
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

open class MockListenableHyperlink(url: String) : ListenableHyperlink(url) {

    class MockLoadEventHandler(hyperlink: MockListenableHyperlink) : AbstractLoadEventHandler() {
        override var onBeforeLoad: (String) -> Unit = {
            println("............onBeforeLoad")
        }
        override var onBeforeParse: (WebPage) -> Unit = { page ->
            println("............onBeforeParse " + page.id)
            println("$this " + page.loadEventHandler)
            assertSame(this, page.loadEventHandler)
            assertTrue(page.isCachedContentEnabled)
            page.variables["VAR_IS_SCRAPE"] = true
        }
        override var onBeforeHtmlParse: (WebPage) -> Unit = { page ->
            assertSame(this, page.loadEventHandler)
            println("............onBeforeHtmlParse " + page.id)
        }
        override var onAfterHtmlParse: (WebPage, FeaturedDocument) -> Unit = { page, document ->
            println("............onAfterHtmlParse " + page.id)
            assertSame(this, page.loadEventHandler)
            assertTrue(page.hasVar("VAR_IS_SCRAPE"))
        }
        override var onAfterParse: (WebPage) -> Unit = { page ->
            println("............onAfterParse " + page.id)
            println("$this " + page.loadEventHandler)
            assertSame(this, page.loadEventHandler)
        }
        override var onAfterLoad: (WebPage) -> Unit = { page ->
            assertSame(this, page.loadEventHandler)
            hyperlink.page = page
            hyperlink.isDone.countDown()
        }
    }

    class MockCrawlEventHandler(hyperlink: MockListenableHyperlink) : AbstractCrawlEventHandler() {
        override var onAfterLoad: (UrlAware, WebPage) -> Unit = { url, page ->
            println("............SinkAwareCrawlEventHandler onAfterLoad " + page.id)
            page.variables.variables.forEach { (t, u) -> println("$t $u") }

            if (page.protocolStatus.isSuccess) {
                assertTrue(page.isLoaded || page.isContentUpdated)
                assertTrue(page.isCachedContentEnabled)
                assertNull(page.persistContent)
                if (page.isContentUpdated) {
                    assertNotNull(page.cachedContent) { "if the page is fetched, the content must be cached" }
                }
            }
            assertTrue(page.hasVar("VAR_IS_SCRAPE"))
        }
    }

    override var args: String? = "-cacheContent true -storeContent false -parse"
    override var loadEventHandler: ai.platon.pulsar.crawl.LoadEventHandler? = MockLoadEventHandler(this)
    override var crawlEventHandler: CrawlEventHandler? = MockCrawlEventHandler(this)

    var page: WebPage? = null

    private val isDone = CountDownLatch(1)
    fun isDone() = isDone.count == 0L
    fun await() = isDone.await()
}
