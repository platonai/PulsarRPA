package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PrivacyContextTests {
    private val contextPathBase = Files.createTempDirectory("test-")
    private val contextPath = contextPathBase.resolve("cx.5kDMDS2")
    private val contextPath2 = contextPathBase.resolve("cx.7KmtAC2")

    @Test
    fun testPrivacyContextIdComparision() {
        val id = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = PrivacyContextId(contextPath2, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertNotEquals(id.hashCode(), id2.hashCode())
        assertTrue { id < id2 }
        assertTrue { id.toString().contains(contextPath.toString()) }
    }

    @Test
    fun testPrivacyContextIdContains() {
        val activeContexts = ConcurrentHashMap<PrivacyContextId, Any>()
        val id = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = PrivacyContextId(contextPath, BrowserType.PLAYWRIGHT_CHROME)
        activeContexts[id] = 1
        assertTrue { activeContexts.containsKey(id) }
        assertFalse { activeContexts.containsKey(id2) }

        activeContexts.remove(id)
        assertFalse { activeContexts.containsKey(id) }
    }
}
