package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.protocol.browser.driver.chrome.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.test.MockWebDriver
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.NoSuchSessionException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class WebDriverAdapter(
    browserInstanceId: BrowserInstanceId,
    val driver: WebDriver,
    val priority: Int = 1000,
) : AbstractWebDriver(browserInstanceId, instanceSequencer.incrementAndGet()) {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val logger = LoggerFactory.getLogger(WebDriverAdapter::class.java)

    val pageViews = AtomicInteger()

    /**
     * The actual url return by the browser
     * */
    override val currentUrl: String?
        get() = if (isQuit) null else
            driver.runCatching { currentUrl }
                .onFailure { logger.warn("Unexpected exception", it) }
                .getOrNull()

    /**
     * The real time page source return by the browser
     * */
    override val pageSource: String get() = driver.runCatching { pageSource }.getOrThrow()

    /**
     * The id of the session to the browser
     * */
    override val sessionId: String?
        get() = when {
            isQuit -> null
            driver is MockWebDriver || driver is ChromeDevtoolsDriver -> driver.runCatching { sessionId }.getOrNull()
            else -> StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
        }

    /**
     * The browser type
     * */
    override val browserType: BrowserType
        get() = when (driver) {
            is MockWebDriver -> BrowserType.MOCK_CHROME
            is ChromeDevtoolsDriver -> BrowserType.CHROME
            else -> BrowserType.CHROME
        }

    override val supportJavascript: Boolean
        get() = when (driver) {
            is MockWebDriver -> driver.supportJavascript
            else -> true
        }

    override val isMockedPageSource: Boolean
        get() = when (driver) {
            is MockWebDriver -> driver.isMockedPageSource
            else -> false
        }

    /**
     * Navigate to the url
     * The browser might redirect, so it might not be the same to [currentUrl]
     * */
    @Throws(NoSuchSessionException::class)
    override fun navigateTo(url: String) {
        if (isWorking) {
            lastActiveTime = Instant.now()
            driver.navigateTo(url)
            this.url = url
            pageViews.incrementAndGet()
            lastActiveTime = Instant.now()
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun evaluate(expression: String): Any? {
        return when {
            isNotWorking -> null
            driver is MockWebDriver -> driver.evaluate(expression)
            driver is ChromeDevtoolsDriver -> driver.evaluate(expression)
            else -> null
        }
    }

    override fun bringToFront() {
        when (driver) {
            is MockWebDriver -> {
                driver.bringToFront()
            }
            is ChromeDevtoolsDriver -> {
                driver.takeIf { isWorking }?.runCatching { bringToFront() }
            }
            else -> {
                evaluateSilently(";document.blur();")
            }
        }
    }

    override fun stopLoading() {
        when (driver) {
            is MockWebDriver -> {
                driver.takeIf { isWorking }?.runCatching { stopLoading() }
            }
            is ChromeDevtoolsDriver -> {
                driver.takeIf { isWorking }?.runCatching { stopLoading() }
            }
            else -> {
                evaluateSilently(";window.stop();")
            }
        }
    }

    override fun setTimeouts(driverConfig: BrowserSettings) {
        if (isNotWorking) {
            return
        }

        driver.setTimeouts(driverConfig)
    }

    /**
     * Quits this driver, close every associated window
     * */
    override fun quit() {
        if (!isQuit) {
            synchronized(status) {
                if (!isQuit) {
                    status.set(Status.QUIT)
                    driver.runCatching { quit() }.onFailure { logger.warn("Unexpected exception", it) }
                }
            }
        }
    }

    override fun close() {
        driver.close()
    }
}
