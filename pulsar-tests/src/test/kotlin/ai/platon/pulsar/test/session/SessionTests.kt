package ai.platon.pulsar.test.session

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.skeleton.common.persist.ext.loadEvent
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTests {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")
    private val args = "-i 5s -ignoreFailure"

    private val context = SQLContexts.create()
    private val session = context.createSession()

    @Test
    fun ensureSessionCreatedBySQLContextIsNotSQLSession() {
        assertFalse { session is SQLSession }
        assertTrue { session is BasicPulsarSession }
    }

    @Test
    fun testLoadAll() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }
        val futures = session.loadAllAsync(normUrls)

        val future1 = CompletableFuture.allOf(*futures.toTypedArray())
        future1.join()

        println("The first round is finished")

        val normUrls2 = urls.take(5).map { session.normalize(it, args) }
        val futures2 = session.loadAllAsync(normUrls2)
        val future2 = CompletableFuture.allOf(*futures2.toTypedArray())
        future2.join()

        println("The second round is finished")

        assertEquals(futures.size, futures2.size)

        val pages = futures.map { it.get() }
        val pages2 = futures2.map { it.get() }

        println("All pages are loaded")

        pages.forEach { assertTrue { it.isFetched } }
        pages2.forEach { assertTrue { it.loadEvent != null } }
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun testLoadAllCached() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }
        val futures = session.loadAllAsync(normUrls)

        val future1 = CompletableFuture.allOf(*futures.toTypedArray())
        future1.join()

        println("The first round is finished")

        val normUrls2 = urls.take(5).map { session.normalize(it) }
        val futures2 = session.loadAllAsync(normUrls2)
        val future2 = CompletableFuture.allOf(*futures2.toTypedArray())
        future2.join()

        println("The second round is finished")

        assertEquals(futures.size, futures2.size)

        val pages = futures.map { it.get() }
        val pages2 = futures2.map { it.get() }

        println("All pages are loaded")

        pages.forEach { assertTrue { it.isFetched } }
        pages2.forEach { assertTrue { it.isCached } }
        pages2.forEach { assertTrue { it.loadEvent != null } }
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun `When loaded a HTML page then the navigate state are correct`() {
        val options = session.options("-refresh")
        options.event.browseEventHandlers.onDidScroll.addLast { page, driver ->
            require(driver is ChromeDevtoolsDriver)
            val navigateEntry = driver.navigateEntry
            assertTrue { navigateEntry.documentTransferred }
            assertTrue { navigateEntry.networkRequestCount.get() > 0 }
            assertTrue { navigateEntry.networkResponseCount.get() > 0 }
            
            assertEquals(200, driver.mainResponseStatus)
            assertTrue { driver.mainResponseStatus == 200 }
            assertTrue { driver.mainResponseHeaders.isNotEmpty() }
            assertEquals(200, navigateEntry.mainResponseStatus)
            assertTrue { navigateEntry.mainResponseStatus == 200 }
            assertTrue { navigateEntry.mainResponseHeaders.isNotEmpty() }
        }
        session.load(url, options)
    }
}
