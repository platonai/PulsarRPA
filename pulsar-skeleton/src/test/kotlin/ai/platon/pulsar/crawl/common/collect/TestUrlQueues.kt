package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.collect.ConcurrentNonReentrantQueue
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlAware
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestUrlQueues {
    val urls = IntRange(1, 100)
            .map { "https://www.amazon.com/s?k=insomnia&i=aps&page=$it" }
            .map { Hyperlink(it) }

    @Test
    fun testConcurrentNEntrantUrlQueue() {
        val queue = ConcurrentNEntrantQueue<UrlAware>(3)
        queue.addAll(urls)

        assertTrue { queue.size == urls.size }
        var i = 0
        while (i++ < urls.size) {
            val v = queue.poll()
            assertNotNull(v)
            assertTrue { v is Hyperlink }
        }
        assertTrue { queue.isEmpty() }
    }

    @Test
    fun testConcurrentNonEntrantUrlQueue() {
        val queue = ConcurrentNonReentrantQueue<UrlAware>()
        queue.addAll(urls)

        assertTrue { queue.size == urls.size }
        var i = 0
        while (i++ < urls.size) {
            val v = queue.poll()
            assertNotNull(v)
            assertTrue { v is Hyperlink }
        }
        assertTrue { queue.isEmpty() }
    }
}
