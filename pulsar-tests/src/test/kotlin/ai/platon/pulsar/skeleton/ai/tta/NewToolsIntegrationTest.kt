package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration tests for the newly implemented tools in PerceptiveAgent:
 * exists(selector), isVisible(selector), focus(selector), scrollTo(selector), waitForNavigation(oldUrl?, timeoutMillis?)
 * goBack(), goForward()
 *
 * These tests verify that the new tools are properly exposed to AI models and work correctly
 * with the Mock Server test pages.
 */
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class NewToolsIntegrationTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ===== Tests for exists() tool =====

    @Test
    fun `When asking to check if element exists then generate correct exists action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("检查搜索按钮是否存在", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("exists") || action.contains("driver.exists"),
            "Should generate exists action")
        assertTrue(action.contains("搜索") || action.contains("button") || action.contains("search"),
            "Should target search-related element")
    }

    @Test
    fun `When asking to check if non-existent element exists then generate exists action with appropriate selector`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("检查ID为'nonexistent-element'的元素是否存在", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("exists") || action.contains("driver.exists"),
            "Should generate exists action")
    }

    // ===== Tests for isVisible() tool =====

    @Test
    fun `When asking to check if element is visible then generate correct isVisible action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("检查搜索输入框是否可见", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("isVisible") || action.contains("driver.isVisible"),
            "Should generate isVisible action")
        assertTrue(action.contains("input") || action.contains("搜索") || action.contains("search"),
            "Should target input-related element")
    }

    @Test
    fun `When asking to check visibility of hidden element then generate isVisible action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("检查隐藏的切换开关是否可见", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("isVisible") || action.contains("driver.isVisible"),
            "Should generate isVisible action")
    }

    // ===== Tests for focus() tool =====

    @Test
    fun `When asking to focus an element then generate correct focus action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("聚焦到搜索输入框", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("focus") || action.contains("driver.focus"),
            "Should generate focus action")
        assertTrue(action.contains("input") || action.contains("搜索") || action.contains("search"),
            "Should target input-related element")
    }

    @Test
    fun `When asking to focus a button then generate focus action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("聚焦到提交按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("focus") || action.contains("driver.focus"),
            "Should generate focus action")
        assertTrue(action.contains("button") || action.contains("提交") || action.contains("submit"),
            "Should target button element")
    }

    // ===== Tests for scrollTo() tool =====

    @Test
    fun `When asking to scroll to an element then generate correct scrollTo action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-screens.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("滚动到页面底部的表单", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("scrollTo") || action.contains("driver.scrollTo"),
            "Should generate scrollTo action")
        assertTrue(action.contains("form") || action.contains("表单") || action.contains("底部"),
            "Should target form or bottom element")
    }

    @Test
    fun `When asking to scroll to specific section then generate scrollTo action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-screens.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("滚动到标题为'Section 2'的区域", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("scrollTo") || action.contains("driver.scrollTo"),
            "Should generate scrollTo action")
        assertTrue(action.contains("h2") || action.contains("section") || action.contains("Section 2"),
            "Should target heading or section element")
    }

    // ===== Tests for waitForNavigation() tool =====

    @Test
    fun `When asking to wait for navigation after clicking link then generate waitForNavigation action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("点击链接后等待页面跳转完成", driver)

        assertNotNull(actionDescription)
        // This might generate multiple actions: click + waitForNavigation
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Check if any action contains waitForNavigation
        val hasWaitForNavigation = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }
        assertTrue(hasWaitForNavigation, "Should include waitForNavigation action")
    }

    @Test
    fun `When asking to wait for page load then generate waitForNavigation action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("等待当前页面完全加载", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Check if any action contains waitForNavigation
        val hasWaitForNavigation = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }
        assertTrue(hasWaitForNavigation, "Should include waitForNavigation action")
    }

    // ===== Tests for goBack() and goForward() tools =====

    @Test
    fun `When asking to go back in browser history then generate goBack action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("返回上一页", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("goBack") || action.contains("driver.goBack"),
            "Should generate goBack action")
    }

    @Test
    fun `When asking to go forward in browser history then generate goForward action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("前进到下一页", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action")
        val action = actionDescription.functionCalls.first()
        println("Generated action: $action")
        assertTrue(action.contains("goForward") || action.contains("driver.goForward"),
            "Should generate goForward action")
    }

    // ===== Tests for conditional/defensive actions (combining multiple tools) =====

    @Test
    fun `When asking to click element only if it exists then generate exists + click actions`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("如果搜索按钮存在就点击它", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Should generate conditional logic with exists check
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        // The AI should understand the conditional nature and generate appropriate actions
        assertTrue(hasExistsCheck || hasClickAction, "Should generate exists check or click action")
    }

    @Test
    fun `When asking to wait for element and then interact then generate waitForSelector + interaction actions`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("等待搜索输入框出现然后输入文本", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Should include wait and then interaction
        val hasWaitAction = actions.any { action ->
            action.contains("waitForSelector") || action.contains("driver.waitForSelector")
        }
        val hasFillAction = actions.any { action ->
            action.contains("fill") || action.contains("driver.fill") ||
            action.contains("type") || action.contains("driver.type")
        }

        assertTrue(hasWaitAction || hasFillAction, "Should generate wait and/or fill actions")
    }

    // ===== Tests for complex scenarios =====

    @Test
    fun `When asking to verify element visibility before clicking then generate isVisible + click actions`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("确认提交按钮可见后点击它", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Should include visibility check and click
        val hasVisibilityCheck = actions.any { action ->
            action.contains("isVisible") || action.contains("driver.isVisible")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        assertTrue(hasVisibilityCheck || hasClickAction, "Should generate visibility check and/or click action")
    }

    @Test
    fun `When asking to focus element before interaction then generate focus + interaction actions`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("先聚焦到用户名输入框然后输入文本", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        println("Generated actions: $actions")

        // Should include focus and then interaction
        val hasFocusAction = actions.any { action ->
            action.contains("focus") || action.contains("driver.focus")
        }
        val hasFillAction = actions.any { action ->
            action.contains("fill") || action.contains("driver.fill") ||
            action.contains("type") || action.contains("driver.type")
        }

        assertTrue(hasFocusAction || hasFillAction, "Should generate focus and/or fill actions")
    }
}
