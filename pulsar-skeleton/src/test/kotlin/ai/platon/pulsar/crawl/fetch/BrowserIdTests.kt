package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext.Companion.USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext.Companion.USER_DEFAULT_DATA_DIR_PLACEHOLDER
import java.nio.file.Files
import kotlin.test.*

class BrowserIdTests {
    private val contextPath = Files.createTempDirectory("test-")

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(contextPath)
    }

    @Test
    fun testBrowserComparison() {
        val id = BrowserId(contextPath, BrowserType.PULSAR_CHROME)
        val id2 = BrowserId(contextPath, BrowserType.PLAYWRIGHT_CHROME)
        assertNotEquals(id, id2)
        assertNotEquals(id.hashCode(), id2.hashCode())
        assertTrue("id.browserType.toString() > id2.browserType.toString()") { id.browserType.toString() > id2.browserType.toString() }
        assertTrue("id.browserType < id2.browserType") { id.browserType < id2.browserType }
        assertTrue("id.fingerprint < id2.fingerprint") { id.fingerprint > id2.fingerprint }
        assertTrue("id > id2") { id > id2 }
        assertTrue("contains") { id.toString().contains(contextPath.toString()) }
        assertTrue("startsWith") { id.userDataDir.startsWith(contextPath) }
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

    @Test
    fun testSystemDefaultBrowserId() {
        val id = BrowserId.USER_DEFAULT
        println(id)
        println(id.contextDir)
        println(id.userDataDir)
        assertFalse { id.userDataDir.toString().contains("google-chrome") }
        assertEquals(id.contextDir, USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER)
        assertTrue { id.userDataDir.startsWith(USER_DEFAULT_CONTEXT_DIR_PLACEHOLDER) }
        assertEquals(id.userDataDir, USER_DEFAULT_DATA_DIR_PLACEHOLDER)
    }
}
