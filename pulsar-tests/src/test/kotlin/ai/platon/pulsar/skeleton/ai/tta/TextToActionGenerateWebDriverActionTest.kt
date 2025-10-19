package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.EnabledMockServerApplication
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ModelResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Specific tests for TextToAction.generateWebDriverAction() method
 * Testing the Tool Call style implementation
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionGenerateWebDriverActionTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    @Test
    fun `When generateWebDriverAction is called then return ActionDescription with single action`() {
        val prompt = "点击搜索按钮"

        runEnhancedWebDriverTest { driver ->
            // Navigate to a test page first
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

            assertNotNull(actionDescription)
            assertNotNull(actionDescription.modelResponse)

            // Should generate at most one action (EXACT ONE requirement)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

            // If there are function calls, they should be valid WebDriver calls
            actionDescription.functionCalls.forEach { call ->
                assertTrue(call.startsWith("driver.") || call.contains("driver."), "Function call should be a valid WebDriver call")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles click actions then generate appropriate click command`() {
        val prompts = listOf(
            "点击搜索按钮",
            "点击提交",
            "单击登录",
            "Click the search button"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")

                // Check if the generated action is appropriate

                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("click") || action.contains("driver.click"), "Should generate click-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles fill actions then generate appropriate fill command`() {
        val prompts = listOf(
            "在搜索框输入 'test'",
            "填写用户名",
            "输入密码 'secret'",
            "Fill the search input with 'query'"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")
                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("fill") || action.contains("driver.fill") ||
                             action.contains("type") || action.contains("driver.type"),
                             "Should generate fill-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles navigation actions then generate appropriate navigation command`() {
        val prompts = listOf(
            "打开 https://www.google.com",
            "导航到百度",
            "访问 example.com",
            "Navigate to https://github.com"
        )

        runEnhancedWebDriverTest { driver ->
            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")
                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("navigateTo") || action.contains("driver.navigateTo"),
                             "Should generate navigation-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles scroll actions then generate appropriate scroll command`() {
        val prompts = listOf(
            "滚动到页面中间",
            "向下滚动",
            "滚动到顶部",
            "Scroll to the middle of the page"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")
                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("scroll") || action.contains("driver.scroll"),
                             "Should generate scroll-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles wait actions then generate appropriate wait command`() {
        val prompts = listOf(
            "等待按钮出现",
            "等待页面加载",
            "等待搜索框",
            "Wait for the submit button"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")
                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("waitFor") || action.contains("driver.waitFor"),
                             "Should generate wait-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles checkbox actions then generate appropriate checkbox command`() {
        val prompts = listOf(
            "勾选复选框",
            "选中同意条款",
            "取消勾选通知",
            "Check the agreement checkbox"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should generate action for prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")
                    val action = actionDescription.functionCalls.first()
                    printlnPro("Prompt: $prompt -> Generated action: $action")
                    assertTrue(action.contains("check") || action.contains("driver.check") ||
                             action.contains("uncheck") || action.contains("driver.uncheck"),
                             "Should generate checkbox-related action")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction is called with empty elements then handle gracefully`() {
        val prompt = "点击按钮"

        val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, listOf())

        assertNotNull(actionDescription)
        // Should handle empty elements gracefully
        assertEquals(1, actionDescription.functionCalls.size)
    }

    @Test
    fun `When generateWebDriverAction is called with empty page then handle appropriately`() {
        val prompt = "点击按钮"

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("about:blank")

            val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

            assertNotNull(actionDescription)
            // Should handle pages with no/few interactive elements
            assertEquals(1, actionDescription.functionCalls.size)
        }
    }

    @Test
    fun `When generateWebDriverAction is called with ambiguous prompt then return single reasonable action`() {
        val prompts = listOf(
            "操作页面",
            "Do something",
            "Interact with the page",
            "Perform an action"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { prompt ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should handle ambiguous prompt: $prompt")
                assertTrue(actionDescription.functionCalls.size <= 1, "Should generate at most one action for ambiguous: $prompt")

                printlnPro("Ambiguous prompt: $prompt -> Generated: ${actionDescription.functionCalls}")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction selects best matching element then include it in result`() {
        val prompt = "点击搜索按钮"

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

            assertNotNull(actionDescription)
            // The selectedElement field should be populated when elements are found
            // Note: selectedElement might be null if no suitable element is found, which is acceptable
            printlnPro("Selected element: ${actionDescription.selectedElement}")
            printlnPro("Function calls: ${actionDescription.functionCalls}")
        }
    }

    @Test
    fun `When generateWebDriverAction returns ActionDescription then all fields are properly set`() {
        val prompt = "点击提交按钮"

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

            assertNotNull(actionDescription)
            assertNotNull(actionDescription.functionCalls, "Function calls should not be null")
            assertNotNull(actionDescription.modelResponse, "Model response should not be null")

            // Function calls should be a list (might be empty)
            assertTrue(actionDescription.functionCalls.size <= 1, "Should have at most one function call")

            // Model response should have content
            assertTrue(actionDescription.modelResponse.content.isNotBlank() || actionDescription.modelResponse == ModelResponse.LLM_NOT_AVAILABLE)
        }
    }

    @Test
    fun `When generateWebDriverAction uses tool call style then parse tool calls correctly`() {
        val prompt = "点击搜索按钮"

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

            // Test that the tool call parsing works correctly

                val action = actionDescription.functionCalls.first()
                printlnPro("Generated tool call style action: $action")

                // Should be a valid WebDriver method call
                assertTrue(action.startsWith("driver.") || action.contains("driver."), "Should be a valid WebDriver call")
                assertTrue(action.contains("("), "Should contain method parameters")
                assertTrue(action.contains(")"), "Should close method parameters")
        }
    }

    @Test
    fun `When generateWebDriverAction is called with various element types then handle appropriately`() {
        val prompts = listOf(
            "点击按钮" to "button",
            "点击链接" to "link",
            "点击输入框" to "input",
            "点击图片" to "img",
            "点击 div" to "div"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { (prompt, elementType) ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should handle $elementType element type for: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")

                printlnPro("Element type $elementType, prompt: $prompt -> Generated: ${actionDescription.functionCalls}")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction handles different Chinese prompts then generate appropriate actions`() {
        val prompts = listOf(
            "点击登录" to "click",
            "输入用户名" to "fill",
            "滚动页面" to "scroll",
            "等待加载" to "wait",
            "勾选复选框" to "check"
        )

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            prompts.forEach { (prompt, expectedAction) ->
                val actionDescription = textToAction.generateWebDriverActionBlocking(prompt, driver)

                assertNotNull(actionDescription, "Should handle Chinese prompt: $prompt")
                assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $prompt")

                printlnPro("Chinese prompt: $prompt -> Generated: ${actionDescription.functionCalls}")
            }
        }
    }

    @Test
    fun `When generateWebDriverAction is called repeatedly then maintain consistency`() {
        val prompt = "点击搜索按钮"

        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("https://example.com")
            driver.waitForSelector("body")

            // Call multiple times to test consistency
            val results = (1..3).map {
                textToAction.generateWebDriverActionBlocking(prompt, driver)
            }

            results.forEach { result ->
                assertNotNull(result)
                assertTrue(result.functionCalls.size <= 1, "Should generate at most one action consistently")
            }

            // Results should be consistent (same number of actions, similar content)
            val firstResult = results.first()
            results.forEach { result ->
                assertEquals(firstResult.functionCalls.size, result.functionCalls.size, "Should be consistent in number of actions")
            }
        }
    }
}

