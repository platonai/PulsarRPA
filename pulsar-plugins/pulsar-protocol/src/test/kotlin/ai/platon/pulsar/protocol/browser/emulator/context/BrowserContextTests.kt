package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.privacy.SequentialPrivacyContextIdGenerator
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserContextTests {
    private val conf = ImmutableConfig()
    private val webDriverPoolManager = DefaultWebDriverPoolManager(conf)
    private val contextPath = AppPaths.getTmp("test-context")

    init {
        System.setProperty(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS,
            SequentialPrivacyContextIdGenerator::class.java.name)
    }

    @Test
    fun `When close a privacy context then it's removed from the active contexts queue`() {
        val manager = MultiPrivacyContextManager(webDriverPoolManager, conf)
        val id = PrivacyContextId(contextPath, BrowserType.MOCK_CHROME)
        val privacyContext = manager.computeIfAbsent(id)
        assertTrue { manager.activeContexts.containsKey(id) }
        manager.close(privacyContext)
        assertFalse { manager.activeContexts.containsKey(id) }
    }

    @Test
    fun `When run tasks the contexts rotates`() {
        val manager = MultiPrivacyContextManager(webDriverPoolManager, conf)
        val url = "about:blank"
        val page = WebPage.newTestWebPage(url)

        runBlocking {
            repeat(10) {
                val task = createFetchTask(page)
                manager.run(task) { task, driver -> mockFetch(task, driver) }
                assertTrue { manager.activeContexts.size <= manager.maxAllowedBadContexts }
            }
        }
    }

    private suspend fun mockFetch(task: FetchTask, driver: WebDriver): FetchResult {
        return FetchResult.canceled(task)
    }

    private fun createFetchTask(page: WebPage): FetchTask {
        val conf = page.conf
        val priority = conf.getUint(CapabilityTypes.BROWSER_WEB_DRIVER_PRIORITY, 0)
        val browserType = conf.getEnum(CapabilityTypes.BROWSER_TYPE, BrowserType.PULSAR_CHROME)
        val fingerprint = Fingerprint(browserType)
        return FetchTask(0, priority, page, conf, fingerprint = fingerprint)
    }
}
