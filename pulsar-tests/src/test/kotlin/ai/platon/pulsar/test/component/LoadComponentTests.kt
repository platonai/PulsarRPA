package ai.platon.pulsar.test.component

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.skeleton.crawl.component.LoadComponent
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.test.TestBase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import java.text.MessageFormat
import java.util.concurrent.CompletableFuture
import kotlin.test.*

/**
 * Test event handlers.
 *
 * The test cases are passed when run separately, but are failed when running in batch mode in linux
 * using the following command:
 *
 * ```kotlin
 * mvn -X -pl pulsar-tests
 * ```
 *
 * It seems that await() never returns, and the test cases are blocked.
 * TODO: Investigate the root cause of the issue.
 */
@Tag("BatchTestFailed")
class LoadComponentTests: TestBase() {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")

    @Autowired
    lateinit var loadComponent: LoadComponent

    @BeforeEach
    fun clearResources() {
        session.globalCache.resetCaches()
        session.context.crawlLoops.restart()

        session.delete(url)
        urls.forEach { session.delete(it) }

        assertTrue("Page should not exists | $url") { !session.exists(url) }
        urls.forEach {
            assertTrue("Page should not exists | $it") { !session.exists(it) }
        }
    }

    @Test
    fun testLoadAll() {
        val normUrls = urls.take(5).map { session.normalize(it, "-i 5s -ignoreFailure") }
        val pages = loadComponent.loadAll(normUrls)
        val pages2 = loadComponent.loadAll(normUrls)
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun testLoadAllWithAllCached() {
        val normUrls = urls.take(5).map { session.normalize(it, "-i 5s -ignoreFailure") }
        val pages = loadComponent.loadAll(normUrls)
        val normUrls3 = urls.take(5).map { session.normalize(it) }
        val pages3 = loadComponent.loadAll(normUrls3)
        assertEquals(pages.size, pages3.size)
    }

    @Test
    fun testLoadAsync() {
        val normURL = session.normalize(url, "-i 5s -ignoreFailure")
        val future = loadComponent.loadAsync(normURL)
        assertFalse(future.isCancelled)
        assertFalse(future.isDone)
        future.thenAccept { println(it.url) }
        val page = future.get()
        assertTrue(future.isDone)
        assertEquals(url, page.url)
    }

    @Test
    fun testLoadAllAsync() {
        val normUrls = urls.take(5).map { session.normalize(it, "-i 5s -ignoreFailure") }
        val resultUrls = mutableListOf<String>()
        val futures = loadComponent.loadAllAsync(normUrls)
            .map { it.thenApply { resultUrls.add(it.url) } }

        val future = CompletableFuture.allOf(*futures.toTypedArray())
        future.join()

        assertEquals(normUrls.size, resultUrls.size)
    }

    @Test
    fun testLoadAllAsFlow() = runBlocking {
        val normUrls = urls.take(5).map { session.normalize(it, "-i 5s -ignoreFailure") }

        val resultUrls = mutableListOf<String>()
        normUrls.asFlow()
            .map { loadComponent.loadDeferred(it) }
            .onEach { resultUrls.add(it.url) }
            .onEach { println("Loaded in flow with size ${it.contentLength} | $it") }
            .map { it.contentLength }
            .collect()

        assertEquals(normUrls.size, resultUrls.size)
        assertEquals(resultUrls[0], normUrls[0].spec)
        assertEquals(resultUrls[1], normUrls[1].spec)
    }

    @Test
    fun testLoadWithChannel() = runBlocking {
        val channel = Channel<WebPage>()

        val testUrls = urls.take(5)
        val options = session.options()
        options.eventHandlers.loadEventHandlers.onLoaded.addLast { page ->
            launch {
                channel.send(page)
                MessageFormat.format("SEND ► page {0} | {1}", page.id, page.url).also { println(it) }
            }
        }
        session.submitAll(testUrls, options)

        repeat(testUrls.size) {
            val page = channel.receive()
            MessageFormat.format("RECV ◀ page {0} | {1}", page.id, page.url).also { println(it) }
        }
        println("Done!")
    }
}
