package ai.platon.pulsar.common

import ai.platon.pulsar.common.collect.HyperlinkCollector
import ai.platon.pulsar.common.collect.PeriodicalLocalFileHyperlinkCollector
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.Hyperlink
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

    @After
    fun tearDown() {
        session.close()
    }

    @Test
    fun testHyperlinkCollector() {
        val options = LoadOptions.parse("-i 100d -ol a[href~=/dp/] -retry")
        val seeds = LinkExtractors.fromResource("categories.txt")
                .take(10)
                .mapTo(LinkedList()) { session.normalize(it, options) }

        val collector = HyperlinkCollector(session, seeds)
        val sink = mutableListOf<Hyperlink>()
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
        val options = LoadOptions.parse("-i 1s")
        val collector = PeriodicalLocalFileHyperlinkCollector(path, options)

        val sink = mutableListOf<Hyperlink>()
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
