package ai.platon.pulsar.test

import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.persist.ext.loadEvent
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

    class MockLoadEvent(hyperlink: MockListenableHyperlink) : DefaultLoadEvent() {
        private val thisHandler = this

        init {
            onWillLoad.addFirst {
                println("............onBeforeLoad")
                it
            }
            onWillParseHTMLDocument.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    println("............onBeforeParse " + page.id)
                    println("$this " + page.loadEvent)
                    assertSame(thisHandler, page.loadEvent)
                    page.variables[VAR_IS_SCRAPE] = true
                }
            })
            onWillParseHTMLDocument.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    assertSame(thisHandler, page.loadEvent)
                    println("............onBeforeHtmlParse " + page.id)
                }
            })
            onHTMLDocumentParsed.addFirst(object: HTMLDocumentHandler() {
                override fun invoke(page: WebPage, document: FeaturedDocument) {
                    println("............onAfterHtmlParse " + page.id)
                    assertSame(thisHandler, page.loadEvent)
                    assertTrue(page.hasVar(VAR_IS_SCRAPE))
                }
            })
            onParsed.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    println("............onAfterParse " + page.id)
                    println("$thisHandler " + page.loadEvent)
                    assertSame(thisHandler, page.loadEvent)
                }
            })
            onLoaded.addFirst(object: WebPageHandler() {
                override fun invoke(page: WebPage) {
                    assertSame(thisHandler, page.loadEvent)
                    hyperlink.page = page
                    hyperlink.isDone.countDown()
                }
            })
        }
    }

    override var args: String? = "-cacheContent true -storeContent false -parse"
    override var event: PageEvent = DefaultPageEvent(
        loadEvent = MockLoadEvent(this)
    )

    init {
        registerEventHandler()
    }

    var page: WebPage? = null

    private val isDone = CountDownLatch(1)
    fun isDone() = isDone.count == 0L
    fun await() = isDone.await()

    private fun registerEventHandler() {
        event.crawlEvent.onLoaded.addFirst { url, page ->
            if (page == null) {
                return@addFirst null
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
