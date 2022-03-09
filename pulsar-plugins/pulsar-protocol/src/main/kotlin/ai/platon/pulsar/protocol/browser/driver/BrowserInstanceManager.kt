package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.LauncherOptions
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
        userDataDir: Path, launcherOptions: LauncherOptions, launchOptions: ChromeOptions): BrowserInstance {
        return browserInstances.computeIfAbsent(userDataDir.toString()) {
            PlaywrightBrowserInstance(userDataDir, launchOptions.proxyServer, launcherOptions, launchOptions).apply { launch() }
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

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browserInstances.values.toList()
            browserInstances.clear()
            unSynchronized.parallelStream().forEach { it.close() }
            // Playwright.create().close()
        }.onFailure {
            // kill -9
            it.printStackTrace()
        }
    }
}
