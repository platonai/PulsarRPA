import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.common.printlnPro
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DebugTest : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)
    val testURL get() = "$generatedAssetsBaseURL/injected-js.test.html"

    @Test
    fun `debug queryComputedStyle`() = runEnhancedWebDriverTest(testURL, browser) { driver ->
        val expression = """__pulsar_utils__.queryComputedStyle('button', ['color', 'background-color'])"""

        val result = driver.evaluateValue(expression)
        printlnPro("Actual result: $result")
        printlnPro("Result type: ${result?.javaClass?.name}")
        printlnPro("Result toString: ${result.toString()}")

        // Let's also test what the actual computed styles are
        val actualStyles = driver.evaluateValue("""
            const button = document.querySelector('button');
            const computed = window.getComputedStyle(button);
            {
                'color': computed.color,
                'backgroundColor': computed.backgroundColor
            }
        """)
        printlnPro("Actual computed styles: $actualStyles")
    }
}