package ai.platon.pulsar.test.session

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.persist.ext.loadEvent
import ai.platon.pulsar.ql.context.SQLContexts
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionTests {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")
    private val args = "-i 5s -ignoreFailure"

    private val context = SQLContexts.create()
    private val session = context.createSession()

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
}
