package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BrowserInstanceTests {
    private val contextPath = Files.createTempDirectory("test-")

    @Test
    fun testBrowserInstanceId() {
        val id = BrowserInstanceId(contextPath, BrowserType.CHROME)
        val id2 = BrowserInstanceId(contextPath, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertTrue { id < id2 }
        assertTrue { id.toString().contains(contextPath.toString()) }
    }
}
