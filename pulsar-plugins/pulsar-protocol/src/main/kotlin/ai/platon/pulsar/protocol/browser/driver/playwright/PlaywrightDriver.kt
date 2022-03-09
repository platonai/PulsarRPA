package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.browser.driver.BlockRules
import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.sleep
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.conf.sites.amazon.AmazonBlockRules
import ai.platon.pulsar.protocol.browser.conf.sites.jd.JdBlockRules
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import com.github.kklisura.cdt.protocol.types.page.Viewport
import com.google.gson.Gson
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Mouse
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Position
import com.microsoft.playwright.options.WaitUntilState
import org.openqa.selenium.NoSuchSessionException
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class PlaywrightDriver(
    private val browserSettings: WebDriverSettings,
    private val browserInstance: PlaywrightBrowserInstance,
) : AbstractWebDriver(browserInstance.id) {
    companion object {
        val sessionIdGenerator = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(PlaywrightDriver::class.java)!!

    override val browserType: BrowserType = BrowserType.CHROME

    val openSequence = 1 + browserInstance.tabCount.get()
    val enableUrlBlocking get() = browserSettings.enableUrlBlocking
    val preloadJs get() = browserSettings.generatePreloadJs(false)

    private var isFirstLaunch = openSequence == 1
    private val _sessionId: String = "playwright-" + sessionIdGenerator.incrementAndGet()

    private var navigateUrl = ""

    val page: Page = browserInstance.createTab()
    val mouse: Mouse = page.mouse()

    private val enableBlockingReport = false
    private val closed = AtomicBoolean()

    val numSessionLost = AtomicInteger()
    override var lastActiveTime = Instant.now()
    val isGone get() = closed.get() || numSessionLost.get() > 1
    val isActive get() = !isGone

    val viewport = Viewport().apply {
        x = 0.0
        y = 0.0
        width = BrowserSettings.viewPort.getWidth()
        height = BrowserSettings.viewPort.getHeight()
        scale = 1.0
    }

    override fun setTimeouts(driverConfig: BrowserSettings) {
    }

    @Throws(NoSuchSessionException::class)
    override fun navigateTo(url: String) {
        initSpecialSiteBeforeVisit(url)

        browserInstance.navigateHistory.add(url)
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
                    && browserInstance.navigateHistory.none { it.contains("jd.com") }
            if (isFirstJdVisit) {
                // JdInitializer().init(page)
            }
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun stopLoading() {
        if (!isActive) return

        try {
            page.pause()
            // page.stopLoading()
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            logger.warn("Failed to call stop loading, session is already closed, {}", Strings.simplifyException(e))
        }
    }

    override fun evaluate(expression: String): Any? {
        if (!isActive) return null

        try {
            val evaluate = page.evaluate(expression)
            val result = evaluate?.toString()
            return result
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            throw e
        }
    }

    /**
     * Simulate a session to the browser
     * */
    override val sessionId: String get() = _sessionId

    override val currentUrl: String
        get() {
            navigateUrl = if (!isActive) navigateUrl else page.url()
            return navigateUrl
        }

    override fun exists(selector: String): Boolean {
        val locator = page.locator(selector)
        return locator.count() > 0
    }

    override fun type(selector: String, text: String) {
        val locator = page.locator(selector)
        locator.scrollIntoViewIfNeeded()
        locator.type(text)
    }

    override fun click(selector: String, count: Int) {
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
    }

    override val pageSource: String get() = page.content()

    override fun bringToFront() = page.bringToFront()

    fun screenshot(path: Path) {
        page.screenshot(Page.ScreenshotOptions().setPath(path))
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
            if (preloadJs.isNotBlank()) {
                page.addInitScript(preloadJs)
            }

            if (enableUrlBlocking) {
                setupUrlBlocking(url)
                // network.enable()
            }
//            fetch.enable()

            navigateUrl = url
            val options = Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.COMMIT)
//                .setTimeout(0.0)
            page.navigate(url, options)
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            throw e
        }
    }

    private fun getNoInvaded(url: String) {
        if (!isActive) return

        try {
            navigateUrl = url
            page.navigate(url)
        } catch (e: Exception) {
            numSessionLost.incrementAndGet()
            throw e
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
