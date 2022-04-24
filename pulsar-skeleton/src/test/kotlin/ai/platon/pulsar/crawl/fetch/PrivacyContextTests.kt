package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.*

class PrivacyContextTests {
    private val contextPathBase = Files.createTempDirectory("test-")
    private val contextPath = contextPathBase.resolve("cx.5kDMDS2")
    private val contextPath2 = contextPathBase.resolve("cx.7KmtAC2")

    @Test
    fun testPrivacyContextIdComparison() {
        val id = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = PrivacyContextId(contextPath2, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertNotEquals(id.hashCode(), id2.hashCode())
        assertTrue { id < id2 }
        assertTrue { id.toString().contains(contextPath.toString()) }
    }

    @Test
    fun testPrivacyContextIdEquality() {
        val id = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = PrivacyContextId(contextPath, BrowserType.PULSAR_CHROME)
        assertEquals(id, id2)
        assertEquals(id.hashCode(), id2.hashCode())
        assertTrue { id == id2 }

        val activeContexts = ConcurrentHashMap<PrivacyContextId, Any>()
        activeContexts[id] = 1
        assertTrue { activeContexts.containsKey(id) }
        assertTrue { activeContexts.containsKey(id2) }
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

        activeContexts.clear()
        assertTrue { activeContexts.isEmpty() }
        activeContexts.computeIfAbsent(id) { 0 }
        assertTrue { activeContexts.containsKey(id) }
        activeContexts.computeIfAbsent(id) { 0 }
        assertTrue { activeContexts.containsKey(id) }
    }
}
