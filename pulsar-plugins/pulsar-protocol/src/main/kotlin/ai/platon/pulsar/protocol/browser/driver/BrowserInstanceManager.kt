package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowserInstance
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowserInstance
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class BrowserInstanceManager(
    val conf: ImmutableConfig
): AutoCloseable {
    private val closed = AtomicBoolean()
    private val browserInstances = ConcurrentHashMap<String, AbstractBrowserInstance>()

    val instanceCount get() = browserInstances.size

    @Synchronized
    fun hasLaunched(userDataDir: String): Boolean {
        return browserInstances.containsKey(userDataDir)
    }

    @Synchronized
    fun launchIfAbsent(
        instanceId: BrowserInstanceId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): AbstractBrowserInstance {
        val userDataDir = instanceId.userDataDir
        return browserInstances.computeIfAbsent(userDataDir.toString()) {
            createAndLaunch(instanceId, launcherOptions, launchOptions)
        }
    }

    @Synchronized
    fun closeIfPresent(instanceId: BrowserInstanceId) {
        browserInstances.remove(instanceId.userDataDir.toString())?.close()
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            doClose()
        }
    }

    private fun createAndLaunch(
        instanceId: BrowserInstanceId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): AbstractBrowserInstance {
        return when(instanceId.browserType) {
            BrowserType.MOCK_CHROME -> MockBrowserInstance(instanceId, launcherOptions, launchOptions)
            BrowserType.PLAYWRIGHT_CHROME -> PlaywrightBrowserInstance(instanceId, launcherOptions, launchOptions)
            else -> ChromeDevtoolsBrowserInstance(instanceId, launcherOptions, launchOptions)
        }.apply { launch() }
    }

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browserInstances.values.toList()
            browserInstances.clear()

            getLogger(this).info("Closing {} browser instances", unSynchronized.size)
            unSynchronized.parallelStream().forEach { it.close() }
        }.onFailure {
            getLogger(this).warn("Failed to close | {}", it.message)
        }
    }
}
