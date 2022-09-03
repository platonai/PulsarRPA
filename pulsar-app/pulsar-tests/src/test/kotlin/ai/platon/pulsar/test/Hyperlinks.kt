package ai.platon.pulsar.test

import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.crawl.common.url.StatefulListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import java.util.concurrent.CountDownLatch
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

open class MockListenableHyperlink(url: String) : StatefulListenableHyperlink(url) {

    class MockLoadEventHandler(hyperlink: MockListenableHyperlink) : DefaultLoadEventHandler() {
        private val thisHandler = this

        init {
            onBeforeLoad.addFirst {
                println("............onBeforeLoad")
            }
            onBeforeParse.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    println("............onBeforeParse " + page.id)
                    println("$this " + page.loadEventHandler)
                    assertSame(thisHandler, page.loadEventHandler)
                    page.variables[VAR_IS_SCRAPE] = true
                }
            })
            onBeforeHtmlParse.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    assertSame(thisHandler, page.loadEventHandler)
                    println("............onBeforeHtmlParse " + page.id)
                }
            })
            onAfterHtmlParse.addFirst(object: HtmlDocumentHandler() {
                override fun invoke(page: WebPage, document: FeaturedDocument) {
                    println("............onAfterHtmlParse " + page.id)
                    assertSame(thisHandler, page.loadEventHandler)
                    assertTrue(page.hasVar(VAR_IS_SCRAPE))
                }
            })
            onAfterParse.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    println("............onAfterParse " + page.id)
                    println("$thisHandler " + page.loadEventHandler)
                    assertSame(thisHandler, page.loadEventHandler)
                }
            })
            onAfterLoad.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    assertSame(thisHandler, page.loadEventHandler)
                    hyperlink.page = page
                    hyperlink.isDone.countDown()
                }
            })
        }
    }

    override var args: String? = "-cacheContent true -storeContent false -parse"
    override var eventHandler: PulsarEventHandler = DefaultPulsarEventHandler(
        loadEventHandler = MockLoadEventHandler(this)
    )

    init {
        registerEventHandler()
    }

    var page: WebPage? = null

    private val isDone = CountDownLatch(1)
    fun isDone() = isDone.count == 0L
    fun await() = isDone.await()

    private fun registerEventHandler() {
        eventHandler.crawlEventHandler.onLoaded.addFirst { url, page ->
            if (page == null) {
                return@addFirst
            }

            println("............SinkAwareCrawlEventHandler onAfterLoad " + page.id)
            page.variables.variables.forEach { (t, u) -> println("$t $u") }

            if (page.protocolStatus.isSuccess) {
                assertTrue(page.isLoaded || page.isContentUpdated)
                assertNull(page.persistContent)
                if (page.isContentUpdated) {
                    assertNotNull(page.tmpContent) { "if the page is fetched, the content must be cached" }
                }
            }
            assertTrue(page.hasVar(VAR_IS_SCRAPE))
        }
    }
}
