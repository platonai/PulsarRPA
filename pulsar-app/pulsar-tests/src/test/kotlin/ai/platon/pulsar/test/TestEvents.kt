package ai.platon.pulsar.test

import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.common.url.ListenableHyperlink
import ai.platon.pulsar.crawl.component.FetchComponent
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestEvents : TestBase() {
    private val log = LoggerFactory.getLogger(TestEvents::class.java)

    @Autowired
    lateinit var fetchComponent: FetchComponent

    @Before
    fun setup() {
        val metrics = fetchComponent.fetchMetrics
        assertNotNull(metrics)
        metrics.tasks.mark(-metrics.tasks.count)
        metrics.successTasks.mark(-metrics.successTasks.count)
        metrics.persists.reset()
    }

    @Test
    fun `When a page is fetched then events are fired and metrics are recorded`() {
        val metrics = fetchComponent.fetchMetrics
        assertNotNull(metrics)

        val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"
        val hyperlink = ListenableHyperlink(url, args = "-i 0s")

        val firedEvents = mutableListOf<String>()
        hyperlink.loadEventHandler.apply {
            onBeforeLoadPipeline.addLast { url ->
                firedEvents.add("onBeforeLoad")
                assertEquals(0, metrics.tasks.count)
            }

            onAfterFetchPipeline.addLast { page ->
                firedEvents.add("onAfterFetch")
                assertTrue { page.crawlStatus.isFetched }
                assertEquals(1, metrics.tasks.count)
                assertEquals(1, metrics.successTasks.count)
                assertEquals(0, metrics.persistContentMBytes.counter.count)
            }

            onAfterLoadPipeline.addLast { page ->
                firedEvents.add("onAfterLoad")

                assertTrue { page.protocolStatus.isSuccess }
                assertTrue { page.crawlStatus.isFetched }
                assertTrue { page.isContentUpdated }

                assertTrue { page.options.persist }

                assertEquals(1, metrics.tasks.count)
                assertEquals(1, metrics.successTasks.count)
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
