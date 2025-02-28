package ai.platon.pulsar.test

import ai.platon.pulsar.skeleton.common.persist.ext.options
import ai.platon.pulsar.skeleton.crawl.common.url.StatefulListenableHyperlink
import ai.platon.pulsar.skeleton.crawl.component.FetchComponent
import kotlin.test.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestEvents : TestBase() {
    private val logger = LoggerFactory.getLogger(TestEvents::class.java)

    @Autowired
    lateinit var fetchComponent: FetchComponent

    @BeforeTest
    fun setup() {
        val metrics = fetchComponent.coreMetrics
        assertNotNull(metrics)

        metrics.fetchTasks.mark(-metrics.fetchTasks.count)
        metrics.successFetchTasks.mark(-metrics.successFetchTasks.count)
        metrics.persists.reset()
    }

    @Test
    fun `When a page is fetched then events are fired and metrics are recorded`() {
        val metrics = fetchComponent.coreMetrics
        assertNotNull(metrics)

        val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
        val hyperlink = StatefulListenableHyperlink(url, "", args = "-i 0s")

        val firedEvents = mutableListOf<String>()
        val eventHandler = hyperlink.eventHandlers.loadEventHandlers
        eventHandler.apply {
            onWillLoad.addLast { _ ->
                firedEvents.add("onBeforeLoad")
                assertEquals(0, metrics.fetchTasks.count)
                null
            }

            onFetched.addLast { page ->
                firedEvents.add("onAfterFetch")
                assertTrue { page.crawlStatus.isFetched }
                assertEquals(1, metrics.fetchTasks.count)
                assertEquals(1, metrics.successFetchTasks.count)
                assertEquals(0, metrics.persistContentMBytes.counter.count)
            }

            onLoaded.addLast { page ->
                firedEvents.add("onAfterLoad")

                assertTrue { page.protocolStatus.isSuccess }
                assertTrue { page.crawlStatus.isFetched }
                assertTrue { page.isContentUpdated }

                assertTrue { page.options.persist }

                assertEquals(1, metrics.fetchTasks.count)
                assertEquals(1, metrics.successFetchTasks.count)
                // TODO:
                // 17:23:15.813 [main] WARN  a.p.p.c.l.AbstractChainedFunction1$addLast$1 - The exception is unexpected, it's best managed within custom code, such as event handlers.
                //org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
                //	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
                //	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
                //	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
                //	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
                //	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1156)
                assertEquals(1, metrics.persists.counter.count)
                assertEquals(0, metrics.persistContentMBytes.counter.count)
            }
        }

        session.load(hyperlink)

        assertTrue { "onBeforeLoad" in firedEvents }
        assertTrue { "onAfterFetch" in firedEvents }
        assertTrue { "onAfterLoad" in firedEvents }
    }
}
