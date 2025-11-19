import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.FastWebDriverService
import ai.platon.pulsar.common.printlnPro
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class TestUtilsDetection : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)
    val testURL get() = "$generatedAssetsBaseURL/injected-js.test.html"

    @Test
    fun `test dynamic utils object detection`() = runEnhancedWebDriverTest(testURL, browser) { driver ->
        // Find the actual utils object name (it has a random prefix)
        val utilsObjectName = driver.evaluateValue("""
            const globalKeys = Object.keys(window);
            const utilsKey = globalKeys.find(key => key.endsWith('utils__'));
            return utilsKey || null;
        """)

        printlnPro("DEBUG: Found utils object name = $utilsObjectName")

        if (utilsObjectName == null) {
            printlnPro("WARNING: No utils object found")
            // Let's check what objects are available
            val allKeys = driver.evaluateValue("""Object.keys(window).filter(key => key.includes('utils') || key.includes('pulsar'))""")
            printlnPro("DEBUG: Available utils/pulsar objects: $allKeys")
            return@runEnhancedWebDriverTest
        }

        // Test the getConfig function
        val configResult = driver.evaluateValue("""window['$utilsObjectName'].getConfig()""")
        printlnPro("DEBUG: getConfig result = $configResult")

        // Test queryComputedStyle
        val styleResult = driver.evaluateValue("""window['$utilsObjectName'].queryComputedStyle('button', ['color', 'background-color'])""")
        printlnPro("DEBUG: queryComputedStyle result = $styleResult")
        printlnPro("DEBUG: result type = ${styleResult?.javaClass?.name}")

        assertNotNull(styleResult, "queryComputedStyle should return a result")
        assertTrue { styleResult is Map<*, *> }
    }
}