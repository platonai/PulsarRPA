package ai.platon.pulsar.test.collect

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.skeleton.common.collect.HyperlinkCollector
import ai.platon.pulsar.skeleton.common.collect.PeriodicalLocalFileHyperlinkCollector
import ai.platon.pulsar.common.config.AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.skeleton.common.urls.NormURL
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.skeleton.context.PulsarContexts
import kotlin.test.*
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HyperlinkCollectorTests {
    private val context = PulsarContexts.create(PULSAR_CONTEXT_CONFIG_LOCATION)
    private val session = context.createSession()

    @Test
    fun testHyperlinkCollector() {
        val options = session.options("-i 1000d -ol a[href~=/dp/] -ignoreFailure -storeContent true")
        val seeds = LinkExtractors.fromResource("categories.txt")
            .take(10)
            .mapTo(LinkedList()) { session.normalize(it, options) }

        val collector = HyperlinkCollector(session, seeds)
        val sink = mutableListOf<UrlAware>()
        assertTrue { collector.hasMore() }
        while (collector.hasMore()) {
            collector.collectTo(sink)
        }
        assertTrue { collector.seeds.isEmpty() }
        assertTrue { sink.size > 10 * collector.seeds.size }
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
