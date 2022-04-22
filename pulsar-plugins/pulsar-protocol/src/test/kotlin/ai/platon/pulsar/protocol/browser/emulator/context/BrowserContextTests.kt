package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserContextTests {
    private val conf = ImmutableConfig()
    private val webDriverPoolManager = DefaultWebDriverPoolManager(conf)
    private val contextPath = AppPaths.getTmp("test-context")

    @Test
    fun `When close a privacy context then it's removed from the active contexts queue`() {
        val manager = MultiPrivacyContextManager(webDriverPoolManager, conf)
        val id = PrivacyContextId(contextPath, BrowserType.MOCK_CHROME)
        val privacyContext = manager.computeIfAbsent(id)
        assertTrue { manager.activeContexts.containsKey(id) }
        manager.close(privacyContext)
        assertFalse { manager.activeContexts.containsKey(id) }
    }
}
