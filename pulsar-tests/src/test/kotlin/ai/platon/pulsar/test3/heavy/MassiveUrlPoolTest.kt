package ai.platon.pulsar.test3.heavy

import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.crawl.common.url.ListenableHyperlink
import org.junit.jupiter.api.Tag
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Test the performance of PulsarRPA, every test url will be a local file, so the performance is not affected by network latency.
 *
 * The test first generate 10000 temporary files in the local file system, and then run the test.
 *
 * Notice: before we load the local files using PulsarRPA, we have to transform the paths using [UrlUtils.pathToLocalURL].
 * */
@Ignore("TimeConsumingTest, you should run the tests separately")
@Tag("TimeConsumingTest")
class MassiveUrlPoolTest: MassiveTestBase() {

    @Test
    fun test() {
        PulsarSettings().maxBrowsers(4).maxOpenTabs(8)

        val links = testPaths.asSequence().map { UrlUtils.pathToLocalURL(it) }.map { createHyperlink(it) }

        links.forEach {
            session.submit(it, "-refresh")
        }

        session.context.await()
    }

    private fun createHyperlink(url: String): ListenableHyperlink {
        val link = ListenableHyperlink(url, "")
        val le = link.eventHandlers.loadEventHandlers
        val be = link.eventHandlers.browseEventHandlers

        be.onDocumentActuallyReady.addLast { page, driver ->
            val text = driver.selectFirstTextOrNull("body")
            text
        }

        return link
    }
}
