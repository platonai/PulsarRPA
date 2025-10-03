package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Comprehensive tests for WebDriver.act() method implementation
 * Testing the Tool Call style implementation with interactive elements
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WebDriverActMethodTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    @Test
    fun `When act method is called with click action then generate and execute single click action`() {
        val prompt = "点击搜索按钮"

        runWebDriverTest { driver ->
            // Navigate to a test page with interactive elements first
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.isNotEmpty() || result.functionResults.isNotEmpty() || result.modelResponse.content.isNotBlank())

            // Should contain exactly one action
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to clicking
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("click") || action.contains("driver.click"), "Should generate click action")
            }
        }
    }

    @Test
    fun `When act method is called with fill action then generate and execute single fill action`() {
        val prompt = "在搜索框中输入 'test input'"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to filling
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("fill") || action.contains("driver.fill"), "Should generate fill action")
            }
        }
    }

    @Test
    fun `When act method is called with navigation action then generate single navigation action`() {
        val prompt = "打开网页 https://www.google.com"

        runWebDriverTest { driver ->
            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to navigation
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("navigateTo") || action.contains("driver.navigateTo"), "Should generate navigation action")
            }
        }
    }

    @Test
    fun `When act method is called with scroll action then generate single scroll action`() {
        val prompt = "滚动到页面中间位置"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to scrolling
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("scroll") || action.contains("driver.scroll"), "Should generate scroll action")
            }
        }
    }

    @Test
    fun `When act method is called with wait action then generate single wait action`() {
        val prompt = "等待提交按钮出现"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to waiting
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("waitFor") || action.contains("driver.waitFor"), "Should generate wait action")
            }
        }
    }

    @Test
    fun `When act method is called with checkbox action then generate single checkbox action`() {
        val prompt = "勾选同意条款复选框"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // If there are function calls, they should be related to checkbox
            if (result.functionCalls.isNotEmpty()) {
                val action = result.functionCalls.first()
                assertTrue(action.contains("check") || action.contains("driver.check"), "Should generate checkbox action")
            }
        }
    }

    @Test
    fun `When act method is called with English prompt then handle appropriately`() {
        val prompt = "Click the search button"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action")

            // Should handle English prompts appropriately
            assertNotNull(result.modelResponse)
            assertTrue(result.modelResponse.content.isNotBlank() || result.functionCalls.isNotEmpty())
        }
    }

    @Test
    fun `When act method encounters ambiguous prompt then handle gracefully`() {
        val prompt = "Do something on the page"

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result = driver.act(prompt)

            assertNotNull(result)
            // Should handle ambiguous prompts gracefully - either no action or a reasonable default
            assertTrue(result.functionCalls.size <= 1, "Should generate at most one action even for ambiguous prompts")
        }
    }

    @Test
    fun `When act method is called multiple times then each call is independent`() {
        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val result1 = driver.act("点击搜索")
            val result2 = driver.act("滚动页面")
            val result3 = driver.act("等待元素")

            // Each call should be independent
            assertNotNull(result1)
            assertNotNull(result2)
            assertNotNull(result3)

            // Each should generate at most one action
            assertTrue(result1.functionCalls.size <= 1)
            assertTrue(result2.functionCalls.size <= 1)
            assertTrue(result3.functionCalls.size <= 1)
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

        runWebDriverTest { driver ->
            // Navigate to a test page
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            testCases.forEach { (prompt, expectedKeywords) ->
                val result = driver.act(prompt)

                assertNotNull(result, "Result should not be null for prompt: $prompt")
                assertTrue(result.functionCalls.size <= 1, "Should generate at most one action for: $prompt")

                println("Prompt: $prompt")
                println("Function calls: ${result.functionCalls}")
                println("Model response: ${result.modelResponse.content}")
                println("---")
            }
        }
    }
}