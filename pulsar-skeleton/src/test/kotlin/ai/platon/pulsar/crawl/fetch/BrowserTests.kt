package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

class BrowserTests {
    private val contextPath = Files.createTempDirectory("test-")

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(contextPath)
    }

    @Test
    fun testBrowserId() {
        val id = BrowserId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = BrowserId(contextPath, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertNotEquals(id.hashCode(), id2.hashCode())
        assertTrue { id.browserType.toString() > id2.browserType.toString() }
        assertTrue { id.fingerprint > id2.fingerprint }
        assertTrue { id > id2 }
        assertTrue { id.toString().contains(contextPath.toString()) }
        assertTrue { id.userDataDir.startsWith(contextPath) }
    }

    @Test
    fun testPrototypeBrowserId() {
        val id = BrowserId.PROTOTYPE
        println(id)
        println(id.contextDir)
        println(id.userDataDir)
        assertTrue { id.userDataDir.toString().contains("google-chrome") }
        assertEquals(id.contextDir, PrivacyContext.PROTOTYPE_CONTEXT_DIR)
        assertEquals(id.userDataDir, PrivacyContext.PROTOTYPE_DATA_DIR)
        assertTrue { id.userDataDir.startsWith(PrivacyContext.PROTOTYPE_DATA_DIR) }
    }
}
