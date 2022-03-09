package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.browser.driver.chrome.LauncherOptions
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.protocol.browser.driver.BrowserInstance
import com.microsoft.playwright.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class PlaywrightBrowserInstance(
    userDataDir: Path,
    proxyServer: String?,
    launcherConfig: LauncherOptions,
    launchOptions: ChromeOptions
): BrowserInstance(userDataDir, proxyServer, launcherConfig, launchOptions) {

    private val logger = LoggerFactory.getLogger(PlaywrightBrowserInstance::class.java)

    lateinit var context: BrowserContext
        private set
    val drivers = ConcurrentLinkedQueue<PlaywrightDriver>()
    val driverCount get() = drivers.size

    override fun launch() {
        if (launched.compareAndSet(false, true)) {
            val vp = BrowserSettings.viewPort

            val playwright = Playwright.create()
            val browserType = playwright.chromium()
            val options = BrowserType.LaunchPersistentContextOptions().apply {
                headless = launchOptions.headless
                setViewportSize(vp.width, vp.height)
                if (proxyServer != null) {
                    setProxy(proxyServer)
                }
                // userAgent = browserSettings.randomUserAgent()
                executablePath = Browsers.searchChromeBinary()
                ignoreHTTPSErrors = true
                ignoreDefaultArgs = arrayListOf("--hide-scrollbars")
                args = launchOptions.toList(launchOptions.additionalArguments).toMutableList()
            }

            logger.info(options.args.joinToString(" "))
            context = browserType.launchPersistentContext(userDataDir, options)

            context.setDefaultTimeout(Duration.ofMinutes(1).toMillis().toDouble())
            context.setDefaultNavigationTimeout(Duration.ofMinutes(3).toMillis().toDouble())
        }
    }

    @Synchronized
    fun createTab(): Page {
        lastActiveTime = Instant.now()
        tabCount.incrementAndGet()

        val page = context.newPage()

        return page
    }

    @Synchronized
    fun closeTab(page: Page) {
        // TODO: anything to do?
        // page.close()
        tabCount.decrementAndGet()
    }

    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            logger.info("Closing {} playwright drivers ... | {}", drivers.size, id.display)

            val nonSynchronized = drivers.toList().also { drivers.clear() }
            nonSynchronized.parallelStream().forEach {
                it.runCatching { close() }
                    .onFailure { logger.warn("Failed to close the playwright driver", it) }
            }

            context.close()

            logger.info("Launcher is closed | {}", id.display)
        }
    }
}
