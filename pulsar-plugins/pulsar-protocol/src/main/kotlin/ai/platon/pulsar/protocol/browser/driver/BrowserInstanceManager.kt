package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.LauncherOptions
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowserInstance
import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BrowserInstanceManager: AutoCloseable {
    private val closed = AtomicBoolean()
    private val browserInstances = ConcurrentHashMap<String, BrowserInstance>()

    val instanceCount get() = browserInstances.size

    @Synchronized
    fun hasLaunched(userDataDir: String): Boolean {
        return browserInstances.containsKey(userDataDir)
    }

    @Synchronized
    fun launchIfAbsent(
        instanceId: BrowserInstanceId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions): BrowserInstance {
        val userDataDir = instanceId.dataDir
        return browserInstances.computeIfAbsent(userDataDir.toString()) {
            createAndLaunch(instanceId, launcherOptions, launchOptions)
        }
    }

    @Synchronized
    fun closeIfPresent(dataDir: Path) {
        browserInstances.remove(dataDir.toString())?.close()
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            doClose()
        }
    }

    private fun createAndLaunch(instanceId: BrowserInstanceId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions): BrowserInstance {
        val userDataDir = instanceId.dataDir
        return when(launcherOptions.browserType.uppercase()) {
            "PLAYWRIGHT_CHROME" -> PlaywrightBrowserInstance(userDataDir, launchOptions.proxyServer, launcherOptions, launchOptions)
            else -> ChromeDevtoolsBrowserInstance(userDataDir, launchOptions.proxyServer, launcherOptions, launchOptions)
        }.apply { launch() }
    }

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browserInstances.values.toList()
            browserInstances.clear()
            unSynchronized.parallelStream().forEach { it.close() }
        }.onFailure {
            getLogger(this).warn("Failed to close | {}", it.message)
        }
    }
}
