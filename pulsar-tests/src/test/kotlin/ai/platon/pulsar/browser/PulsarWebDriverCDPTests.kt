package ai.platon.pulsar.browser

import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PulsarWebDriverCDPTests : WebDriverTestBase() {
    fun setLogLevel(loggerName: String?, level: Level?) {
        val targetLogger: Logger = LoggerFactory.getLogger(loggerName) as Logger
        targetLogger.level = level
    }

    private val browserLoggerName = "ai.platon.pulsar.protocol.browser"
    private val chromeLoggerName = "ai.platon.pulsar.browser.driver.chrome"
    private val transportLoggerName = "ai.platon.pulsar.browser.driver.chrome.impl"
    private val testURL get() = "$generatedAssetsBaseURL/interactive-4.html"

    fun increasesLogLevels() {
        setLogLevel(browserLoggerName, Level.TRACE)
        setLogLevel(chromeLoggerName, Level.TRACE)
        setLogLevel(transportLoggerName, Level.TRACE)
    }

    fun resetLogs() {
        setLogLevel(browserLoggerName, Level.INFO)
        setLogLevel(chromeLoggerName, Level.INFO)
        setLogLevel(transportLoggerName, Level.INFO)
    }

    @BeforeEach
    fun setup() {
        increasesLogLevels()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `test evaluate`() = runWebDriverTest(testURL, browser) { driver ->
        val code = """1+1"""

        val result = driver.evaluate(code)
        assertEquals(2, result)
    }

    @Test
    fun `test DOM event`() = runWebDriverDOMEventTest(testURL, browser) { driver ->
        assertIs<PulsarWebDriver>(driver)

        val code = """1+1"""
        val result = driver.evaluate(code)
        assertEquals(2, result)
    }

    private fun runWebDriverDOMEventTest(url: String, browser: Browser, block: suspend (WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
                assertIs<PulsarWebDriver>(driver)

                val devTools = driver.implementation

                devTools.dom.onAttributeModified { e ->
                    val message = MessageFormat.format("> {0}. node changed | {1} := {2}", e.nodeId, e.name, e.value)
                    println(message)
                }

                devTools.console.enable()
                devTools.console.onMessageAdded { e ->
                    println(e.message)
                }

                open(url, driver)
                block(driver)
            }
        }
    }
}
