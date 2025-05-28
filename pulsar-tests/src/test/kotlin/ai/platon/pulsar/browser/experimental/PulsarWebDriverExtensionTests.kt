package ai.platon.pulsar.browser.experimental

import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.browser.WebDriverTestBase
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PulsarWebDriverExtensionTests : WebDriverTestBase() {
    fun setLogLevel(loggerName: String?, level: Level?) {
        val targetLogger: Logger = LoggerFactory.getLogger(loggerName) as Logger
        targetLogger.level = level
    }

    override val webDriverService get() = FastWebDriverService(browserFactory)

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
        resetLogs()
    }

    @Test
    fun `test evaluate 1+1`() = runWebDriverTest(testURL, browser) { driver ->
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

    @Test
    fun `test DOM console message`() = runEventTest({ devtools ->
        devtools.dom.onAttributeModified { e ->
            val message = MessageFormat.format("> {0}. node changed | {1} := {2}", e.nodeId, e.name, e.value)
            println(message)
        }
    }, { _, driver ->
        val code = """console.log("Hello, Pulsar!");"""
        val result = driver.evaluate(code)
    })

    @Test
    fun `test network onDataReceived`() = runEventTest({ devtools ->
        devtools.network.onDataReceived { data -> println(data.requestId + ": " + data.dataLength) }
    }, { _, driver ->

    })

    private fun runEventTest(
        init: suspend (ChromeDevTools) -> Unit,
        block: suspend (ChromeDevTools, WebDriver) -> Unit
    ) {
        runBlocking {
            browser.newDriver().use { driver ->
                assertIs<PulsarWebDriver>(driver)
                val devTools = driver.implementation
                init(devTools)
                open(testURL, driver)
                block(devTools, driver)
            }
        }
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

                open(url, driver)
                block(driver)
            }
        }
    }

    private fun runWebDriverDOMEventConsoleMessageTest(url: String, browser: Browser, block: suspend (WebDriver) -> Unit) {
        runBlocking {
            browser.newDriver().use { driver ->
                assertIs<PulsarWebDriver>(driver)

                val devTools = driver.implementation

                devTools.console.enable()
                devTools.console.onMessageAdded { e ->
                    println("< console message added: " + e.message.text)
                }

                open(url, driver)
                block(driver)
            }
        }
    }
}
