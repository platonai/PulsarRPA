package ai.platon.pulsar.test.crawl

import ai.platon.pulsar.test.MockDegeneratedListenableHyperlink
import ai.platon.pulsar.test.MockListenableHyperlink
import ai.platon.pulsar.test.TestBase
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
 */
@Tag("BatchTestFailed")
class TestEventHandlers : TestBase() {

    @Test
    fun whenLoadAListenableLink_ThenEventsAreTriggered() {
        logger.info("Testing - whenLoadAListenableLink_ThenEventsAreTriggered")

        val url = MockListenableHyperlink("https://www.amazon.com")
        context.submit(url).await()
        url.await()
        assertTrue(url.isDone())

        url.triggeredEvents.forEach {
            println(it)
        }

        assertContentEquals(url.expectedEvents, url.triggeredEvents)

        logger.info("Tested - whenLoadAListenableLink_ThenEventsAreTriggered")
    }

    @Test
    fun `When load degenerated link then load event is performed`() {
        logger.info("Testing - whenLoadDegeneratedLink_ThenEventsAreTriggered")

        val url = MockDegeneratedListenableHyperlink()
        context.submit(url).await()
        url.await()

        url.triggeredEvents.forEach {
            println(it)
        }

        assertContentEquals(url.expectedEvents, url.triggeredEvents)

        logger.info("Tested - whenLoadDegeneratedLink_ThenEventsAreTriggered")
    }
}
