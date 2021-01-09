package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.ConcurrentNEntrantLoadingQueue
import ai.platon.pulsar.common.url.Hyperlink
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLoadingQueues: TestBase() {

    @Test
    fun testConcurrentNEntrantLoadingQueue() {
        val queue = ConcurrentNEntrantLoadingQueue(urlLoader)
        queue.load()

        assertTrue { queue.isNotEmpty() }
        var i = 0
        while (i++ < queueSize) {
            val v = queue.poll()
            assertNotNull(v)
            assertTrue { v is Hyperlink }
        }
        assertTrue { queue.isEmpty() }
    }
}
