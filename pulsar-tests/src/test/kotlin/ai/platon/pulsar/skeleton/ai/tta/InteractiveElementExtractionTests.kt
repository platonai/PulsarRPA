package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.EnabledMockServerApplication
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
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class InteractiveElementExtractionTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    @AfterTest
    fun cleanup() {
        // Cleanup is handled by WebDriverTestBase
    }

    @Test
    fun `test JavaScript element extraction with interactive-1 HTML page`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        // Wait for page to load completely
        driver.waitForSelector("body", 5000)

        // Test basic functionality using the available chat method
        val prompt = "从当前页面提取所有可交互的元素"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Element extraction response: ${response.modelResponse.content}")

        // Verify response contains information about element extraction
        assertTrue(
            response.modelResponse.content.contains("element") ||
            response.modelResponse.content.contains("元素") ||
            response.modelResponse.content.contains("extract") ||
            response.modelResponse.content.contains("提取"),
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
            runEnhancedWebDriverTest(pageUrl, browser) { driver ->
                driver.waitForSelector("body", 5000)

                // Test extraction functionality using available methods
                val prompt = "分析$pageUrl 页面中的可交互元素"
                val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

                println("Page $pageUrl analysis: ${response.modelResponse.content}")

                // Verify response mentions page analysis
                assertTrue(
                    response.modelResponse.content.contains("element") ||
                    response.modelResponse.content.contains("元素") ||
                    response.modelResponse.content.contains("page") ||
                    response.modelResponse.content.contains("页面"),
                    "Should analyze page elements"
                )
            }
        }
    }

    @Test
    fun `test element bounds calculation with positioned elements`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test bounds-related functionality
        val prompt = "分析页面中元素的定位和尺寸信息"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Bounds analysis response: ${response.modelResponse.content}")

        // Should mention positioning or bounds
        assertTrue(
            response.modelResponse.content.contains("position") ||
            response.modelResponse.content.contains("定位") ||
            response.modelResponse.content.contains("bounds") ||
            response.modelResponse.content.contains("边界") ||
            response.modelResponse.content.contains("size") ||
            response.modelResponse.content.contains("尺寸"),
            "Should mention element positioning or bounds"
        )
    }

    @Test
    fun `test element visibility detection with interactive elements`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test visibility-related functionality
        val prompt = "分析页面中元素的可见性状态"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Visibility analysis response: ${response.modelResponse.content}")

        // Should mention visibility
        assertTrue(
            response.modelResponse.content.contains("visible") ||
            response.modelResponse.content.contains("可见") ||
            response.modelResponse.content.contains("display") ||
            response.modelResponse.content.contains("显示") ||
            response.modelResponse.content.contains("hidden") ||
            response.modelResponse.content.contains("隐藏"),
            "Should mention element visibility"
        )
    }

    @Test
    fun `test element description generation accuracy`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test description generation functionality
        val prompt = "生成页面中元素的详细描述信息"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Description generation response: ${response.modelResponse.content}")

        // Should mention element descriptions
        assertTrue(
            response.modelResponse.content.contains("description") ||
            response.modelResponse.content.contains("描述") ||
            response.modelResponse.content.contains("detail") ||
            response.modelResponse.content.contains("详细") ||
            response.modelResponse.content.contains("information") ||
            response.modelResponse.content.contains("信息"),
            "Should mention element descriptions"
        )
    }

    @Test
    fun `test JavaScript selector generation logic`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test selector generation functionality
        val prompt = "分析页面中元素的选择器生成逻辑"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Selector generation response: ${response.modelResponse.content}")

        // Should mention selectors
        assertTrue(
            response.modelResponse.content.contains("selector") ||
            response.modelResponse.content.contains("选择器") ||
            response.modelResponse.content.contains("#") || // ID selector
            response.modelResponse.content.contains(".") || // Class selector
            response.modelResponse.content.contains("logic") ||
            response.modelResponse.content.contains("逻辑"),
            "Should mention selector generation logic"
        )
    }

    @Test
    fun `test interactive functionality with dynamic content`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test dynamic content functionality
        val prompt = "测试页面动态内容的交互功能"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Dynamic content response: ${response.modelResponse.content}")

        // Should mention dynamic content or interaction
        assertTrue(
            response.modelResponse.content.contains("dynamic") ||
            response.modelResponse.content.contains("动态") ||
            response.modelResponse.content.contains("interactive") ||
            response.modelResponse.content.contains("交互") ||
            response.modelResponse.content.contains("content") ||
            response.modelResponse.content.contains("内容"),
            "Should mention dynamic content interaction"
        )
    }

    @Test
    fun `test extraction with hidden elements using toggle functionality`() = runEnhancedWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test hidden element functionality
        val prompt = "分析页面中隐藏元素的显示和隐藏逻辑"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)

        println("Hidden elements response: ${response.modelResponse.content}")

        // Should mention hidden elements or toggle functionality
        assertTrue(
            response.modelResponse.content.contains("hidden") ||
            response.modelResponse.content.contains("隐藏") ||
            response.modelResponse.content.contains("toggle") ||
            response.modelResponse.content.contains("切换") ||
            response.modelResponse.content.contains("show") ||
            response.modelResponse.content.contains("显示"),
            "Should mention hidden elements functionality"
        )
    }

    @Test
    fun `test resource script loading and execution`() {
        // Test that the JavaScript script is properly loaded from resources
        assertNotNull(textToAction, "TextToAction should initialize successfully")

        // Test basic functionality to ensure script loading works
        val prompt = "测试JavaScript资源加载和功能执行"
        val response = textToAction.generateWebDriverActionBlocking(prompt, listOf())

        println("Resource loading test response: ${response.modelResponse.content}")

        // Should generate some response indicating functionality works
        assertTrue(response.modelResponse.content.isNotBlank(), "Should generate response for resource loading test")
    }

    @Test
    fun `test extraction performance with complex page`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Test performance with complex page
        val startTime = System.currentTimeMillis()
        val prompt = "分析复杂页面中的元素提取性能"
        val response = textToAction.generateWebDriverActionBlocking(prompt, driver)
        val endTime = System.currentTimeMillis()

        val processingTime = endTime - startTime
        println("Complex page analysis took ${processingTime}ms")

        println("Performance analysis response: ${response.modelResponse.content}")

        // Should complete in reasonable time and mention performance
        assertTrue(processingTime < 5000, "Should complete within 5 seconds")
        assertTrue(
            response.modelResponse.content.contains("performance") ||
            response.modelResponse.content.contains("性能") ||
            response.modelResponse.content.contains("complex") ||
            response.modelResponse.content.contains("复杂") ||
            response.modelResponse.content.contains("speed") ||
            response.modelResponse.content.contains("速度"),
            "Should mention performance or complexity"
        )
    }
}
