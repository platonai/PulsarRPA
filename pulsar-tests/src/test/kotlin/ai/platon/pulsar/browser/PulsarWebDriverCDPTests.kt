package ai.platon.pulsar.browser

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class PulsarWebDriverCDPTests : WebDriverTestBase() {
    fun setLogLevel(loggerName: String?, level: Level?) {
        val targetLogger: Logger = LoggerFactory.getLogger(loggerName) as Logger
        targetLogger.level = level
    }

    val testURL get() = "$generatedAssetsBaseURL/interactive-4.html"

    @BeforeEach
    fun setup() {
        setLogLevel("ai.platon.pulsar.protocol.browser", Level.TRACE)
    }

    @Test
    fun `test evaluate that returns primitive values`() = runWebDriverTest(testURL, browser) { driver ->
        val code = """1+1"""

        val result = driver.evaluate(code)
        assertEquals(2, result)
    }
}
