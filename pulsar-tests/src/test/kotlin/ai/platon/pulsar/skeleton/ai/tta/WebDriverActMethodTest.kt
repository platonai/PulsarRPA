package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Comprehensive tests for Webtta.generateWebDriverAction() method implementation
 * Testing the Tool Call style implementation with interactive elements
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WebDriverActMethodTest : TextToActionTestBase() {

    private val tta by lazy { TextToAction(conf) }

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    @Test
    fun `When act method is called with click action then generate and execute single click action`() {
        val prompt = "点击搜索按钮"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page with interactive elements first
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to clicking
            val action = result.expressions.first()
            assertTrue(action.contains("click") || action.contains("driver.click"), "Should generate click action")
        }
    }

    @Test
    fun `When act method is called with fill action then generate and execute single fill action`() {
        val prompt = "在搜索框中输入 'test input'"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to filling
            val action = result.expressions.first()
            assertTrue(action.contains("fill") || action.contains("driver.fill"), "Should generate fill action")
        }
    }

    @Test
    fun `When act method is called with navigation action then generate single navigation action`() {
        val prompt = "打开网页 https://www.google.com"

        runEnhancedWebDriverTest { driver ->
            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to navigation
            val action = result.expressions.first()
            assertTrue(action.contains("navigateTo") || action.contains("driver.navigateTo"), "Should generate navigation action")
        }
    }

    @Test
    fun `When act method is called with scroll action then generate single scroll action`() {
        val prompt = "滚动到页面中间位置"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to scrolling
            val action = result.expressions.first()
            assertTrue(action.contains("scroll") || action.contains("driver.scroll"), "Should generate scroll action")
        }
    }

    @Test
    fun `When act method is called with wait action then generate single wait action`() {
        val prompt = "等待提交按钮出现"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to waiting
            val action = result.expressions.first()
            assertTrue(action.contains("waitFor") || action.contains("driver.waitFor"), "Should generate wait action")
        }
    }

    @Test
    fun `When act method is called with checkbox action then generate single checkbox action`() {
        val prompt = "勾选同意条款复选框"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")

            // The generated action should be related to checkbox
            val action = result.expressions.first()
            assertTrue(action.contains("check") || action.contains("driver.check"), "Should generate checkbox action")
        }
    }

    @Test
    fun `When act method is called with English prompt then handle appropriately`() {
        val prompt = "Click the search button"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action")
        }
    }

    @Test
    fun `When act method encounters ambiguous prompt then handle gracefully`() {
        val prompt = "Do something on the page"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result = tta.generate(prompt)

            assertNotNull(result)
            // Must generate at least one function call
            assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action even for ambiguous prompts")
            // Should contain at most one action
            assertTrue(result.expressions.size <= 1, "Should generate at most one action even for ambiguous prompts")
        }
    }

    @Test
    fun `When act method is called multiple times then each call is independent`() {
        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            val result1 = tta.generate("点击搜索")
            val result2 = tta.generate("滚动页面")
            val result3 = tta.generate("等待元素")

            // Each call should be independent
            assertNotNull(result1)
            assertNotNull(result2)
            assertNotNull(result3)

            // Each should generate exactly one action (at least one and at most one)
            assertTrue(result1.expressions.isNotEmpty(), "First call should generate at least one action")
            assertTrue(result1.expressions.size <= 1)
            assertTrue(result2.expressions.isNotEmpty(), "Second call should generate at least one action")
            assertTrue(result2.expressions.size <= 1)
            assertTrue(result3.expressions.isNotEmpty(), "Third call should generate at least one action")
            assertTrue(result3.expressions.size <= 1)
        }
    }

    @Test
    fun `When act method handles complex single actions then generate appropriate response`() {
        val testCases = listOf(
            "点击提交按钮" to listOf("click", "submit"),
            "在用户名输入框输入 'admin'" to listOf("fill", "admin"),
            "滚动到页面底部" to listOf("scroll", "bottom"),
            "等待加载完成" to listOf("wait", "load"),
            "取消勾选通知复选框" to listOf("uncheck", "notification")
        )

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo(actMockSiteHomeURL)
            driver.waitForSelector("body")

            testCases.forEach { (prompt, _) ->
                val result = tta.generate(prompt)

                assertNotNull(result, "Result should not be null for prompt: $prompt")
                assertTrue(result.expressions.isNotEmpty(), "Should generate at least one action for: $prompt")
                assertTrue(result.expressions.size <= 1, "Should generate at most one action for: $prompt")

                printlnPro("Prompt: $prompt")
                printlnPro("Function calls: ${result.expressions}")
                printlnPro("Model response: ${result.modelResponse.content}")
                printlnPro("---")
            }
        }
    }
}

