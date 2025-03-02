package ai.platon.pulsar.test.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.skeleton.common.collect.HyperlinkCollector
import ai.platon.pulsar.skeleton.common.collect.PeriodicalLocalFileHyperlinkCollector
import ai.platon.pulsar.test.TestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
 *
 * Environment:
 * Ubuntu 13.3.0-6ubuntu2~24.04
 * openjdk version "21.0.6" 2025-01-21
 * */
@Tag("LinuxBatchTestFailed")
class HyperlinkCollectorTests: TestBase() {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
    private val urls = LinkExtractors.fromResource("categories.txt")

    @BeforeEach
    fun clearResources() {
        session.globalCache.resetCaches()

        session.delete(url)
        urls.forEach { session.delete(it) }

        assertTrue("Page should not exists | $url") { !session.exists(url) }
        urls.forEach {
            assertTrue("Page should not exists | $it") { !session.exists(it) }
        }
    }

    @Test
    fun testHyperlinkCollector() {
        val options = session.options("-i 1000d -ol a[href~=/dp/] -ignoreFailure -storeContent true")
        val seeds = urls.take(5).mapTo(LinkedList()) { session.normalize(it, options) }

        val collector = HyperlinkCollector(session, seeds)
        val sink = mutableListOf<UrlAware>()
        assertTrue { collector.hasMore() }
        while (collector.hasMore()) {
            collector.collectTo(sink)
        }
        assertTrue { collector.seeds.isEmpty() }
        assertTrue { sink.size > 5 * collector.seeds.size }
    }

    @Test
    fun testPeriodicalHyperlinkCollector() {
        val resource = "categories.txt"

        val path = ResourceLoader.getPath(resource)
        val options = session.options("-i 1s")
        val collector = PeriodicalLocalFileHyperlinkCollector(path, options)

        val sink = mutableListOf<UrlAware>()
        val sourceSize = collector.hyperlinks.size
        var i = 0
        while (i++ < 500) {
            collector.collectTo(sink)

            if (i % 100 == 0) {
                println(
                    "$i.\tRound ${collector.round} " +
                            "expected collected: ${collector.counters.collected}, actual collected: ${sink.size}, " +
                            "total hyperlinks: ${collector.hyperlinks.size}"
                )
            }

            assertEquals(sink.size, collector.counters.collected)
            if (!collector.hasMore()) {
                assertEquals(collector.round * sourceSize, sink.size)
                assertTrue { collector.startTime < Instant.now() }
                assertTrue { collector.finishTime > collector.startTime }
            }

            sleep(Duration.ofMillis(10))
        }
        assertTrue { sink.isNotEmpty() }
    }
}
