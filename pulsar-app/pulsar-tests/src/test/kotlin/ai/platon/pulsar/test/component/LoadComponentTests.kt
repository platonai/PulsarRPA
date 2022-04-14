package ai.platon.pulsar.test.component

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.crawl.CrawlLoops
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.test.TestBase
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class LoadComponentTests: TestBase() {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")
    private val args = "-i 5s"

    @Autowired
    lateinit var crawlLoops: CrawlLoops

    @Autowired
    lateinit var loadComponent: LoadComponent

    @Before
    fun setup() {
        crawlLoops.start()
    }

    @Test
    fun testLoadAsync() {
        val options = session.options(args)
        val normUrl = NormUrl(url, options)
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
        val options = session.options(args)
        val normUrls = urls.take(5).map { NormUrl(it, options) }
        val resultUrls = mutableListOf<String>()
        val futures = loadComponent.loadAllAsync(normUrls)
        futures.map { it.thenApply { resultUrls.add(it.url) } }

        val future = CompletableFuture.allOf(*futures.toTypedArray())
        future.join()

        assertEquals(normUrls.size, resultUrls.size)
    }

    @Test
    fun testLoadAllFlow() {
        val options = session.options(args)
        val normUrls = urls.take(5).map { NormUrl(it, options) }

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
