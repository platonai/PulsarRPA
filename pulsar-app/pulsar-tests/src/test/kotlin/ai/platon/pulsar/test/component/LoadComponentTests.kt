package ai.platon.pulsar.test.component

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.test.TestBase
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture
import kotlin.test.*

class LoadComponentTests: TestBase() {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")
    private val args = "-i 5s -ignoreFailure"

    @Autowired
    lateinit var loadComponent: LoadComponent

    @BeforeTest
    fun setup() {
        crawlLoops.start()
    }

    @Test
    fun testLoadAll() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }
        val pages = loadComponent.loadAll(normUrls)
        val pages2 = loadComponent.loadAll(normUrls)
        assertEquals(pages.size, pages2.size)
    }

    @Test
    fun testLoadAllWithAllCached() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }
        val pages = loadComponent.loadAll(normUrls)
        val normUrls3 = urls.take(5).map { session.normalize(it) }
        val pages3 = loadComponent.loadAll(normUrls3)
        assertEquals(pages.size, pages3.size)
    }

    @Test
    fun testLoadAsync() {
        val normUrl = session.normalize(url, args)
        val future = loadComponent.loadAsync(normUrl)
        assertFalse(future.isCancelled)
        assertFalse(future.isDone)
        future.thenAccept { println(it.url) }
        val page = future.get()
        assertTrue(future.isDone)
        assertEquals(url, page.url)
    }

    @Test
    fun testLoadAllAsync() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }
        val resultUrls = mutableListOf<String>()
        val futures = loadComponent.loadAllAsync(normUrls)
            .map { it.thenApply { resultUrls.add(it.url) } }

        val future = CompletableFuture.allOf(*futures.toTypedArray())
        future.join()

        assertEquals(normUrls.size, resultUrls.size)
    }

    @Test
    fun testLoadAllAsFlow() {
        val normUrls = urls.take(5).map { session.normalize(it, args) }

        val resultUrls = mutableListOf<String>()
        runBlocking {
            normUrls.asFlow()
                .map { loadComponent.loadDeferred(it) }
                .onEach { resultUrls.add(it.url) }
                .map { it.contentLength }
                .collect()
        }

        assertEquals(normUrls.size, resultUrls.size)
        assertEquals(resultUrls[0], normUrls[0].spec)
        assertEquals(resultUrls[1], normUrls[1].spec)
    }
}
