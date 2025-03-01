package ai.platon.pulsar.test.session

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.common.persist.ext.loadEventHandlers
import ai.platon.pulsar.skeleton.session.BasicPulsarSession
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionLoadTests {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")

    private val context = SQLContexts.create()
    private val session = context.createSession()

    @BeforeEach
    fun clearResources() {
        session.delete(url)
        urls.forEach { session.delete(it) }

        assertTrue("Page should not exists | $url") { !session.exists(url) }
        urls.forEach {
            assertTrue("Page should not exists | $it") { !session.exists(it) }
        }
    }

    @Test
    fun ensureSessionCreatedBySQLContextIsNotSQLSession() {
        assertFalse { session is SQLSession }
        assertTrue { session is BasicPulsarSession }
    }

    @Test
    fun whenLoadAllAsyncTwiceWithRefresh_thenPagesAreFetchedInBothTime() {
        val normUrls = urls.take(5).map { session.normalize(it, "-refresh") }
        val futures = session.loadAllAsync(normUrls)

        val future1 = CompletableFuture.allOf(*futures.toTypedArray())
        future1.join()

        println("The first round is finished, all ${futures.size} futures are promised, they should be fetched from the internet")

        val normUrls2 = urls.take(5).map { session.normalize(it, "-refresh") }
        val futures2 = session.loadAllAsync(normUrls2)
        val future2 = CompletableFuture.allOf(*futures2.toTypedArray())
        future2.join()

        println("The second round is finished, all ${futures2.size} futures are promised, they should be loaded from the internet")

        assertEquals(futures.size, futures2.size)

        val pages = futures.map { it.get() }
        val pages2 = futures2.map { it.get() }

        println("All pages are loaded")

        pages.forEach { assertTrue { it.isFetched } }
        pages2.forEach { assertTrue { it.isFetched } }
        pages2.forEach { assertTrue { it.loadEventHandlers != null } }
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun whenLoadAllAsyncSecondlyWithoutExpiry_thenPagesAreLoadedFromCache() {
        val normUrls = urls.take(5).map { session.normalize(it, "-i 0s -ignoreFailure") }
        val futures = session.loadAllAsync(normUrls)

        val future1 = CompletableFuture.allOf(*futures.toTypedArray())
        future1.join()

        println("The first round is finished, all ${futures.size} futures are promised, they should be fetched from the internet")

        val normUrls2 = urls.take(5).map { session.normalize(it) }
        val futures2 = session.loadAllAsync(normUrls2)
        val future2 = CompletableFuture.allOf(*futures2.toTypedArray())
        future2.join()

        println("The second round is finished, all ${futures2.size} futures are promised, they should be loaded from PDCache")

        assertEquals(futures.size, futures2.size)

        val pages = futures.map { it.get() }
        val pages2 = futures2.map { it.get() }

        pages.forEach { assertTrue("Should be fetched from internet | $it") { it.isFetched } }
        pages2.forEach { assertTrue("Should be loaded from PDCache | $it") { it.isCached } }
        pages2.forEach { assertTrue { it.loadEventHandlers != null } }
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun `When loaded a HTML page then the navigate state are correct`() {
        val options = session.options("-refresh")
        options.eventHandlers.browseEventHandlers.onDidScroll.addLast { page, driver ->
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
