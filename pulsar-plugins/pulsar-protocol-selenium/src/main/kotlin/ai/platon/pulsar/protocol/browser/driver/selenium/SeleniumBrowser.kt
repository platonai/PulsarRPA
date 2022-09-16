package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.common.simplify
import ai.platon.pulsar.crawl.fetch.driver.AbstractBrowserInstance
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import com.microsoft.playwright.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class SeleniumBrowser(
    id: BrowserInstanceId,
    launcherOptions: LauncherOptions,
    launchOptions: ChromeOptions
): AbstractBrowserInstance(id, launcherOptions, launchOptions) {
    companion object {
        private val createOptions = Playwright.CreateOptions().setEnv(mutableMapOf("PWDEBUG" to "0"))
        private val playwright = Playwright.create(createOptions)
        private val instanceCount = AtomicInteger()

        init {
            // System.setProperty("playwright.cli.dir", Paths.get("/tmp/playwright-java").toString())
        }
    }

    private val logger = LoggerFactory.getLogger(SeleniumBrowser::class.java)

    private val browserSettings get() = launcherOptions.browserSettings
    private lateinit var context: BrowserContext
    val drivers = ConcurrentLinkedQueue<PlaywrightDriver>()
    val driverCount get() = drivers.size

    val preloadJs get() = browserSettings.generatePreloadJs(false)

    override fun launch() {
        if (launched.compareAndSet(false, true)) {
            launch0()
        }
    }

    private fun launch0() {
        val executablePath = Browsers.searchChromeBinaryOrNull()
        // TODO: no way to disable inspector

        val browserType = playwright.chromium()
        val options = BrowserType.LaunchPersistentContextOptions().apply {
            headless = launchOptions.headless
//            headless = true
            val vp = BrowserSettings.viewPort
            setViewportSize(vp.width, vp.height)
            // setChannel()
//            setScreenSize(vp.width, vp.height)
            if (id.proxyServer != null) {
                setProxy(id.proxyServer)
            }
            // userAgent = BrowserSettings.randomUserAgent()
            this.executablePath = executablePath
            ignoreHTTPSErrors = true

            // ignoreAllDefaultArgs = true
            ignoreDefaultArgs = arrayListOf("--hide-scrollbars", "--enable-crashpad", "--window-size")
            args = launchOptions.toList(launchOptions.additionalArguments).toMutableList()
            args.add("--start-maximized")
            acceptDownloads = false
            // slowMo = 100.0
        }

        logger.info(options.args.joinToString(" "))
        context = browserType.launchPersistentContext(id.userDataDir, options)
        instanceCount.incrementAndGet()

        if (preloadJs.isNotBlank()) {
            context.addInitScript(preloadJs)
        }

        context.setDefaultTimeout(1.0 * DateTimes.MILLIS_PER_MINUTE)
        context.setDefaultNavigationTimeout(3.0 * DateTimes.MILLIS_PER_MINUTE)
    }

    @Synchronized
    fun createTab(): Page {
        activeTime = Instant.now()
        tabCount.incrementAndGet()

        val page = context.newPage()

        return page!!
    }

    @Synchronized
    fun closeTab(page: Page) {
        // TODO: anything to do?
        // page.close()
        tabCount.decrementAndGet()
    }

    override fun close() {
        if (launched.get() && closed.compareAndSet(false, true)) {
            if (drivers.isNotEmpty()) {
                logger.info("Closing {} playwright drivers ... | {}", drivers.size, id.display)

                val nonSynchronized = drivers.toList().also { drivers.clear() }
                nonSynchronized.parallelStream().forEach {
                    runCatching { it.close() }
                        .onFailure { logger.warn("{}", it.simplify()) }
                }
            }

            instanceCount.decrementAndGet()
            kotlin.runCatching { context.close() }
                .onFailure { logger.warn("Failed to close playwright context | {}", it.message) }

            logger.info("Launcher is closed | {}", id.display)

            if (instanceCount.get() == 0) {
                kotlin.runCatching { Playwright.create().close() }
                    .onFailure { logger.warn("Failed to close playwright | {}", it.message) }
            }
        }
    }
}
