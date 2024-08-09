package ai.platon.pulsar.test.crawl

import ai.platon.pulsar.test.MockDegeneratedListenableHyperlink
import ai.platon.pulsar.test.MockListenableHyperlink
import ai.platon.pulsar.test.TestBase
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class TestCrawlLoop : TestBase() {

    @Test
    fun `When load a listenable link then events are triggered`() {
        val url = MockListenableHyperlink("https://www.jd.com")
        context.submit(url).await()
        url.await()
        assertTrue(url.isDone())

        url.triggeredEvents.forEach {
            println(it)
        }

        assertContentEquals(url.expectedEvents, url.triggeredEvents)
    }

    @Test
    fun `When load degenerated link then load event is performed`() {
        val url = MockDegeneratedListenableHyperlink()
        context.submit(url).await()
        url.await()

        url.triggeredEvents.forEach {
            println(it)
        }

        assertContentEquals(url.expectedEvents, url.triggeredEvents)
    }
}
