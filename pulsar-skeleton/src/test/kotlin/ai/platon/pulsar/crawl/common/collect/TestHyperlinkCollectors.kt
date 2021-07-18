package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.collect.HyperlinkCollector
import ai.platon.pulsar.common.collect.PeriodicalLocalFileHyperlinkCollector
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import org.junit.After
import org.junit.Test
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestHyperlinkCollectors {
    private val session = PulsarContexts.createSession()

    @Test
    fun testHyperlinkCollector() {
        val options = session.options("-i 100d -ol a[href~=/dp/] -ignoreFailure")
        val seeds = LinkExtractors.fromResource("categories.txt")
            .take(10)
            .mapTo(LinkedList()) { session.normalize(it, options) as NormUrl }

        val collector = HyperlinkCollector(session, seeds)
        val sink = mutableListOf<UrlAware>()
        while (collector.hasMore()) {
            collector.collectTo(sink)
        }
        assertTrue { collector.seeds.isNotEmpty() }
        assertTrue { sink.size > 10 * collector.seeds.size }
    }

    @Test
    fun testPeriodicalHyperlinkCollector() {
        val resource = "categories.txt"
        val uri = ResourceLoader.getResource(resource)?.toURI() ?: throw NoSuchFieldException(resource)
        val path = Paths.get(uri)
        val options = session.options("-i 1s")
        val collector = PeriodicalLocalFileHyperlinkCollector(path, options)

        val sink = mutableListOf<UrlAware>()
        val sourceSize = collector.hyperlinks.size
        var i = 0
        while (i++ < 500) {
            collector.collectTo(sink)

            println("$i.\tRound ${collector.round} " +
                    "expected collected: ${collector.counters.collected}, actual collected: ${sink.size}, " +
                    "total hyperlinks: ${collector.hyperlinks.size}")

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
