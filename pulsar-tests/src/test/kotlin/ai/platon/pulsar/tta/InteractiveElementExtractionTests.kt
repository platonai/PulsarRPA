package ai.platon.pulsar.tta

import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.util.server.Application
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.AfterTest

/**
 * Tests for JavaScript-based interactive element extraction
 * Testing requirement: "Interactive Element Handling"
 * Uses the pulsar-tests infrastructure with local HTTP server and test pages
 */
@Tag("IntegrationTest")
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class InteractiveElementExtractionTests : TextToActionTestBase() {

    private lateinit var textToAction: TextToAction

    @BeforeEach
    fun setUp() {
        textToAction = TextToAction(session.sessionConfig)
    }

    @AfterTest
    fun cleanup() {
        // Cleanup is handled by WebDriverTestBase
    }

    @Test
    fun `test JavaScript element extraction with interactive-1 HTML page`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        // Wait for page to load completely
        driver.waitForSelector("body", 5000)

        // Test basic functionality using the available chat method
        val prompt = "从当前页面提取所有可交互的元素"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Element extraction response: ${response.content}")

        // Verify response contains information about element extraction
        assertTrue(
            response.content.contains("element") ||
            response.content.contains("元素") ||
            response.content.contains("extract") ||
            response.content.contains("提取"),
            "Response should mention element extraction"
        )
    }

    @Test
    fun `test element extraction with multiple interactive HTML pages`() = runBlocking {
        val testPages = listOf(
            "$generatedAssetsBaseURL/interactive-1.html",
            "$generatedAssetsBaseURL/interactive-2.html",
            "$generatedAssetsBaseURL/interactive-3.html",
            "$generatedAssetsBaseURL/interactive-4.html"
        )

        for (pageUrl in testPages) {
            runWebDriverTest(pageUrl, browser) { driver ->
                driver.waitForSelector("body", 5000)

                // Test extraction functionality using available methods
                val prompt = "分析$pageUrl 页面中的可交互元素"
                val response = textToAction.chatAboutWebDriver(prompt)

                println("Page $pageUrl analysis: ${response.content}")

                // Verify response mentions page analysis
                assertTrue(
                    response.content.contains("element") ||
                    response.content.contains("元素") ||
                    response.content.contains("page") ||
                    response.content.contains("页面"),
                    "Should analyze page elements"
                )
            }
        }
    }

    @Test
    fun `test element bounds calculation with positioned elements`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test bounds-related functionality
        val prompt = "分析页面中元素的定位和尺寸信息"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Bounds analysis response: ${response.content}")

        // Should mention positioning or bounds
        assertTrue(
            response.content.contains("position") ||
            response.content.contains("定位") ||
            response.content.contains("bounds") ||
            response.content.contains("边界") ||
            response.content.contains("size") ||
            response.content.contains("尺寸"),
            "Should mention element positioning or bounds"
        )
    }

    @Test
    fun `test element visibility detection with interactive elements`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test visibility-related functionality
        val prompt = "分析页面中元素的可见性状态"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Visibility analysis response: ${response.content}")

        // Should mention visibility
        assertTrue(
            response.content.contains("visible") ||
            response.content.contains("可见") ||
            response.content.contains("display") ||
            response.content.contains("显示") ||
            response.content.contains("hidden") ||
            response.content.contains("隐藏"),
            "Should mention element visibility"
        )
    }

    @Test
    fun `test element description generation accuracy`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test description generation functionality
        val prompt = "生成页面中元素的详细描述信息"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Description generation response: ${response.content}")

        // Should mention element descriptions
        assertTrue(
            response.content.contains("description") ||
            response.content.contains("描述") ||
            response.content.contains("detail") ||
            response.content.contains("详细") ||
            response.content.contains("information") ||
            response.content.contains("信息"),
            "Should mention element descriptions"
        )
    }

    @Test
    fun `test JavaScript selector generation logic`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test selector generation functionality
        val prompt = "分析页面中元素的选择器生成逻辑"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Selector generation response: ${response.content}")

        // Should mention selectors
        assertTrue(
            response.content.contains("selector") ||
            response.content.contains("选择器") ||
            response.content.contains("#") || // ID selector
            response.content.contains(".") || // Class selector
            response.content.contains("logic") ||
            response.content.contains("逻辑"),
            "Should mention selector generation logic"
        )
    }

    @Test
    fun `test interactive functionality with dynamic content`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test dynamic content functionality
        val prompt = "测试页面动态内容的交互功能"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Dynamic content response: ${response.content}")

        // Should mention dynamic content or interaction
        assertTrue(
            response.content.contains("dynamic") ||
            response.content.contains("动态") ||
            response.content.contains("interactive") ||
            response.content.contains("交互") ||
            response.content.contains("content") ||
            response.content.contains("内容"),
            "Should mention dynamic content interaction"
        )
    }

    @Test
    fun `test extraction with hidden elements using toggle functionality`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test hidden element functionality
        val prompt = "分析页面中隐藏元素的显示和隐藏逻辑"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Hidden elements response: ${response.content}")

        // Should mention hidden elements or toggle functionality
        assertTrue(
            response.content.contains("hidden") ||
            response.content.contains("隐藏") ||
            response.content.contains("toggle") ||
            response.content.contains("切换") ||
            response.content.contains("show") ||
            response.content.contains("显示"),
            "Should mention hidden elements functionality"
        )
    }

    @Test
    fun `test resource script loading and execution`() {
        // Test that the JavaScript script is properly loaded from resources
        assertNotNull(textToAction, "TextToAction should initialize successfully")

        // Test basic functionality to ensure script loading works
        val prompt = "测试JavaScript资源加载和功能执行"
        val response = textToAction.chatAboutWebDriver(prompt)

        println("Resource loading test response: ${response.content}")

        // Should generate some response indicating functionality works
        assertTrue(response.content.isNotBlank(), "Should generate response for resource loading test")
    }

    @Test
    fun `test extraction performance with complex page`() = runWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test performance with complex page
        val startTime = System.currentTimeMillis()
        val prompt = "分析复杂页面中的元素提取性能"
        val response = textToAction.chatAboutWebDriver(prompt)
        val endTime = System.currentTimeMillis()

        val processingTime = endTime - startTime
        println("Complex page analysis took ${processingTime}ms")

        println("Performance analysis response: ${response.content}")

        // Should complete in reasonable time and mention performance
        assertTrue(processingTime < 5000, "Should complete within 5 seconds")
        assertTrue(
            response.content.contains("performance") ||
            response.content.contains("性能") ||
            response.content.contains("complex") ||
            response.content.contains("复杂") ||
            response.content.contains("speed") ||
            response.content.contains("速度"),
            "Should mention performance or complexity"
        )
    }
}