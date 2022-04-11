package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.common.BlockRules
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.hotfix.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.hotfix.sites.jd.JdBlockRules
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Position
import com.microsoft.playwright.options.WaitUntilState
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class PlaywrightDriver(
    private val browserSettings: WebDriverSettings,
    override val browserInstance: PlaywrightBrowserInstance,
) : AbstractWebDriver(browserInstance) {
    companion object {
        val sessionIdGenerator = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(PlaywrightDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.PLAYWRIGHT_CHROME

    val openSequence = 1 + browserInstance.tabCount.get()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking

    private var isFirstLaunch = openSequence == 1
    private val _sessionId: String = "playwright-" + sessionIdGenerator.incrementAndGet()

    private var navigateUrl = ""

    private val pageInitialized = AtomicBoolean()
    private lateinit var page: Page

    private val enableBlockingReport = false
    private val closed = AtomicBoolean()

    val numSessionLost = AtomicInteger()
    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || numSessionLost.get() > 1
    val isActive get() = !isGone && !page.isClosed

    override suspend fun setTimeouts(driverConfig: BrowserSettings) {
    }

    override suspend fun navigateTo(url: String) {
        if (pageInitialized.compareAndSet(false, true)) {
            page = browserInstance.createTab()
        }

        initSpecialSiteBeforeVisit(url)

        browserInstance.navigateHistory.add(NavigateEntry(url))
        lastActiveTime = Instant.now()
        takeIf { browserSettings.jsInvadingEnabled }?.getInvaded(url) ?: getNoInvaded(url)
    }

    /**
     * TODO: use an event handler to do this stuff
     * */
    private fun initSpecialSiteBeforeVisit(url: String) {
        if (isFirstLaunch) {
            // the first visit to jd.com
            val isFirstJdVisit = url.contains("jd.com")
                    && browserInstance.navigateHistory.none { it.url.contains("jd.com") }
            if (isFirstJdVisit) {
                // JdInitializer().init(page)
            }
        }
    }

    override suspend fun stop() {
        if (!isActive) return

        try {
            page.pause()
            // page.stopLoading()
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            logger.warn("Failed to call stop loading | {}", e.message)
        }
    }

    override suspend fun evaluate(expression: String): Any? {
        if (!isActive) return null

        return try {
            val evaluate = page.evaluate(expression)
            evaluate?.toString()
        } catch (e: Exception) {
            val stackTrace = e.stringify()
            if (!stackTrace.contains("Error: Target closed")) {
                logger.warn("Failed to evaluate | {}", e.message)
            }
            null
        }
    }

    override suspend fun mainRequestHeaders(): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun mainRequestCookies(): List<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getCookies(): List<Map<String, String>> {
        val mapper = pulsarObjectMapper()
        return page.context().cookies().map {
            val json = mapper.writeValueAsString(it)
            val map: Map<String, String?> = mapper.readValue(json)
            map.filterValues { it != null }.mapValues { it.toString() }
        }
    }

    /**
     * Simulate a session to the browser
     * */
    override val sessionId: String get() = _sessionId

    override suspend fun currentUrl(): String {
        try {
            navigateUrl = if (!isActive) navigateUrl else page.url()
        } catch (e: Exception) {
            logger.warn("Failed to query url | {}", e.message)
        }
        return navigateUrl
    }

    override suspend fun exists(selector: String): Boolean {
        try {
            val locator = page.locator(selector)
            return locator.count() > 0
        } catch (e: Exception) {
            logger.warn("Failed to locate | {}", e.message)
        }

        return false
    }

    override suspend fun waitForSelector(selector: String, timeout: Duration): Long {
        try {
            val startTime = System.currentTimeMillis()
            page.waitForSelector(selector)
            return timeout.toMillis() - (System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            logger.warn("Failed to wait | {}", e.message)
        }

        return 0
    }

    override suspend fun waitForNavigation(timeout: Duration): Long {
        val startTime = System.currentTimeMillis()

        // TODO: fix this
        page.waitForNavigation {  }

        val elapsedTime = System.currentTimeMillis() - startTime

        return timeout.toMillis() - elapsedTime
    }

    override suspend fun type(selector: String, text: String) {
        try {
            val locator = page.locator(selector)
            locator.scrollIntoViewIfNeeded()
            locator.type(text)
        } catch (e: Exception) {
            logger.warn("Failed to type | {}", e.message)
        }
    }

    override suspend fun click(selector: String, count: Int) {
        try {
            val locator = page.locator(selector)
            locator.scrollIntoViewIfNeeded()
            val box = locator.boundingBox()
            var x = box.width / 3
            var y = box.height / 3
            x += Random.nextInt(x.toInt())
            y += Random.nextInt(y.toInt())
            val position = Position(x, y)

            val delayMillis = 500.0 + Random.nextInt(1500)
            val options = Locator.ClickOptions()
                .setDelay(delayMillis)
                .setNoWaitAfter(true)
                .setPosition(position)
                .setClickCount(count)
            locator.click(options)
        } catch (e: Exception) {
            logger.warn("Failed to click | {}", e.message)
        }
    }

    override suspend fun scrollTo(selector: String) {
        try {
            val locator = page.locator(selector)
            locator.scrollIntoViewIfNeeded()
        } catch (e: Exception) {
            logger.warn("Failed to click | {}", e.message)
        }
    }

    override suspend fun pageSource(): String? = kotlin.runCatching { page.content() }
            .onFailure { logger.warn("Failed to get page source | {}", it.message) }.getOrNull()

    override suspend fun bringToFront() = page.bringToFront()

    fun screenshot(path: Path) {
        kotlin.runCatching { page.screenshot(Page.ScreenshotOptions().setPath(path)) }
            .onFailure { logger.warn("Failed to screenshot | {}", it.message) }.getOrNull()
    }

    override fun toString() = sessionId

    /**
     * Quit the browser instance
     * */
    override fun quit() {
        close()
        // browserInstanceManager.closeIfPresent(launchOptions.userDataDir)
    }

    /**
     * Close the tab hold by this driver
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            page.close()

        }
    }

    private fun getInvaded(url: String) {
        if (!isActive) return

        try {
            if (enableUrlBlocking) {
                setupUrlBlocking(url)
            }

            navigateUrl = url
            val options = Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.COMMIT)
//                .setTimeout(0.0)
            page.navigate(url, options)
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    private fun getNoInvaded(url: String) {
        if (!isActive) return

        try {
            navigateUrl = url
            page.navigate(url)
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            logger.warn("Failed to navigate | {}", e.message)
        }
    }

    private fun setupUrlBlocking(url: String) {
        val blockRules = when {
            "amazon.com" in url -> AmazonBlockRules()
            "jd.com" in url -> JdBlockRules()
            else -> BlockRules()
        }
    }
}
