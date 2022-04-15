package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PrivacyContextTests {
    private val contextPathBase = Files.createTempDirectory("test-")
    private val contextPath = contextPathBase.resolve("cx.5kDMDS2")
    private val contextPath2 = contextPathBase.resolve("cx.7KmtAC2")

    @Test
    fun testBrowserInstanceId() {
        val id = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = PrivacyContextId(contextPath2, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertNotEquals(id.hashCode(), id2.hashCode())
        assertTrue { id < id2 }
        assertTrue { id.toString().contains(contextPath.toString()) }
    }
}
