package ai.platon.pulsar.protocol.browser.driver

//import ai.platon.pulsar.protocol.browser.driver.playwright.PlaywrightBrowserInstance
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowser
import ai.platon.pulsar.crawl.fetch.driver.Browser
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.protocol.browser.DriverLaunchException
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsBrowser
import ai.platon.pulsar.protocol.browser.driver.test.MockBrowser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Similar to puppeteer's BrowserContext
 * */
open class BrowserContext(
    val conf: ImmutableConfig
): AutoCloseable {
    private val closed = AtomicBoolean()
    private val browsers = ConcurrentHashMap<String, Browser>()
    val browserCount get() = browsers.size

    @Synchronized
    fun hasLaunched(userDataDir: String): Boolean {
        return browsers.containsKey(userDataDir)
    }

    @Throws(DriverLaunchException::class)
    @Synchronized
    fun launchIfAbsent(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): Browser {
        val userDataDir = browserId.userDataDir
        return browsers.computeIfAbsent(userDataDir.toString()) {
            launch(browserId, launcherOptions, launchOptions)
        }
    }

    @Synchronized
    fun closeIfPresent(browserId: BrowserId) {
        browsers.remove(browserId.userDataDir.toString())?.close()
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            doClose()
        }
    }

    @Throws(DriverLaunchException::class)
    private fun launch(
        browserId: BrowserId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): AbstractBrowser {
        return when(browserId.browserType) {
            BrowserType.MOCK_CHROME -> MockBrowser(browserId, launcherOptions)
//            BrowserType.PLAYWRIGHT_CHROME -> PlaywrightBrowserInstance(instanceId, launcherOptions, launchOptions)
            else -> launchChromeDevtoolsBrowserInstance(browserId, launcherOptions, launchOptions)
        }
    }

    @Synchronized
    @Throws(DriverLaunchException::class)
    fun launchChromeDevtoolsBrowserInstance(
        instanceId: BrowserInstanceId, launcherOptions: LauncherOptions, launchOptions: ChromeOptions
    ): ChromeDevtoolsBrowser {
//        val shutdownHookRegistry = ChromeDevtoolsBrowserInstance.BrowserShutdownHookRegistry(this)
        val launcher = ChromeLauncher(
            options = launcherOptions,
//            shutdownHookRegistry = shutdownHookRegistry
        )

        val chrome = launcher.runCatching { launch(launchOptions) }
            .getOrElse { throw DriverLaunchException("launch", it) }

        return ChromeDevtoolsBrowser(instanceId, chrome, launcher)
    }

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browsers.values.toList()
            browsers.clear()

            getLogger(this).info("Closing {} browser instances", unSynchronized.size)
            unSynchronized.parallelStream().forEach { it.close() }
        }.onFailure {
            getLogger(this).warn("Failed to close | {}", it.message)
        }
    }
}
