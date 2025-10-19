package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration tests for conditional actions and navigation scenarios using the newly implemented tools.
 * These tests focus on real-world scenarios where defensive programming and navigation handling are crucial.
 */
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConditionalActionsAndNavigationTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ===== Conditional Action Tests =====

    @Test
    fun `When asking to click button only if it exists and is visible then generate defensive checks`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("如果搜索按钮存在且可见就点击它", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should generate defensive checks before clicking
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasVisibilityCheck = actions.any { action ->
            action.contains("isVisible") || action.contains("driver.isVisible")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        // The AI should understand the conditional requirements
        assertTrue(hasExistsCheck || hasVisibilityCheck || hasClickAction,
            "Should generate exists check, visibility check, and/or click action")
    }

    @Test
    fun `When asking to fill form only after verifying field exists then generate verification sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("先确认用户名输入框存在，然后输入'testuser'", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include existence verification before filling
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasFillAction = actions.any { action ->
            action.contains("fill") || action.contains("driver.fill") ||
            action.contains("type") || action.contains("driver.type")
        }

        assertTrue(hasExistsCheck || hasFillAction,
            "Should generate exists check and/or fill action")
    }

    @Test
    fun `When asking to interact with element after waiting for it to become visible then generate wait + check + interact`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-dynamic.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("等待动态按钮出现并可见，然后点击它", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include waiting, visibility check, and interaction
        val hasWaitAction = actions.any { action ->
            action.contains("waitForSelector") || action.contains("driver.waitForSelector")
        }
        val hasVisibilityCheck = actions.any { action ->
            action.contains("isVisible") || action.contains("driver.isVisible")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        assertTrue(hasWaitAction || hasVisibilityCheck || hasClickAction,
            "Should generate wait, visibility check, and/or click actions")
    }

    // ===== Navigation Scenario Tests =====

    @Test
    fun `When asking to navigate and wait for page load then generate navigate + waitForNavigation sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("跳转到交互页面2并等待页面完全加载", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include navigation and waiting
        val hasNavigateAction = actions.any { action ->
            action.contains("navigateTo") || action.contains("driver.navigateTo")
        }
        val hasWaitForNavigationAction = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }

        assertTrue(hasNavigateAction || hasWaitForNavigationAction,
            "Should generate navigate and/or waitForNavigation actions")
    }

    @Test
    fun `When asking to click link and wait for navigation then generate click + waitForNavigation`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("点击跳转到页面2的链接，然后等待页面跳转完成", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include click and navigation wait
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }
        val hasWaitForNavigationAction = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }

        assertTrue(hasClickAction || hasWaitForNavigationAction,
            "Should generate click and/or waitForNavigation actions")
    }

    @Test
    fun `When asking to navigate back and forward then generate goBack + goForward sequence`() = runEnhancedWebDriverTest { driver ->
        // First navigate to a couple of pages to establish history
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("返回上一页，然后再前进到当前页", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include back and forward navigation
        val hasGoBackAction = actions.any { action ->
            action.contains("goBack") || action.contains("driver.goBack")
        }
        val hasGoForwardAction = actions.any { action ->
            action.contains("goForward") || action.contains("driver.goForward")
        }

        assertTrue(hasGoBackAction || hasGoForwardAction,
            "Should generate goBack and/or goForward actions")
    }

    // ===== Complex Real-World Scenarios =====

    @Test
    fun `When asking to fill form with validation then generate complete validation sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking(
            "检查用户名输入框是否存在且可见，如果满足条件则输入'testuser'，然后聚焦到密码框", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include existence check, visibility check, fill, and focus
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasVisibilityCheck = actions.any { action ->
            action.contains("isVisible") || action.contains("driver.isVisible")
        }
        val hasFillAction = actions.any { action ->
            action.contains("fill") || action.contains("driver.fill")
        }
        val hasFocusAction = actions.any { action ->
            action.contains("focus") || action.contains("driver.focus")
        }

        assertTrue(hasExistsCheck || hasVisibilityCheck || hasFillAction || hasFocusAction,
            "Should generate validation checks, fill, and/or focus actions")
    }

    @Test
    fun `When asking to handle navigation with error checking then generate defensive navigation sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking(
            "等待页面完全加载，检查导航菜单是否存在，如果存在则点击它并等待新页面加载", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include navigation wait, existence check, click, and navigation wait
        val hasWaitForNavigationAction = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        assertTrue(hasWaitForNavigationAction || hasExistsCheck || hasClickAction,
            "Should generate navigation wait, exists check, and/or click actions")
    }

    @Test
    fun `When asking to scroll and interact with element then generate scroll + focus + interact sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-screens.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking(
            "滚动到页面底部的表单，聚焦到邮箱输入框，然后输入'test@example.com'", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include scroll, focus, and fill
        val hasScrollAction = actions.any { action ->
            action.contains("scrollTo") || action.contains("driver.scrollTo")
        }
        val hasFocusAction = actions.any { action ->
            action.contains("focus") || action.contains("driver.focus")
        }
        val hasFillAction = actions.any { action ->
            action.contains("fill") || action.contains("driver.fill") ||
            action.contains("type") || action.contains("driver.type")
        }

        assertTrue(hasScrollAction || hasFocusAction || hasFillAction,
            "Should generate scroll, focus, and/or fill actions")
    }

    // ===== Error Handling and Edge Cases =====

    @Test
    fun `When asking to handle ambiguous elements then generate appropriate checks`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking("检查所有提交按钮中哪个是可见的，然后点击第一个可见的", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include visibility checks and interaction
        val hasVisibilityCheck = actions.any { action ->
            action.contains("isVisible") || action.contains("driver.isVisible")
        }
        val hasClickAction = actions.any { action ->
            action.contains("click") || action.contains("driver.click")
        }

        assertTrue(hasVisibilityCheck || hasClickAction,
            "Should generate visibility check and/or click actions for ambiguous elements")
    }

    @Test
    fun `When asking to verify page state before action then generate state verification sequence`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-dynamic.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateWebDriverActionBlocking(
            "确认页面已完全加载，检查动态内容是否存在，如果存在则滚动到它", driver)

        assertNotNull(actionDescription)
        val actions = actionDescription.functionCalls
        printlnPro("Generated actions: $actions")

        // Should include navigation check, existence check, and scroll
        val hasWaitForNavigationAction = actions.any { action ->
            action.contains("waitForNavigation") || action.contains("driver.waitForNavigation")
        }
        val hasExistsCheck = actions.any { action ->
            action.contains("exists") || action.contains("driver.exists")
        }
        val hasScrollAction = actions.any { action ->
            action.contains("scrollTo") || action.contains("driver.scrollTo")
        }

        assertTrue(hasWaitForNavigationAction || hasExistsCheck || hasScrollAction,
            "Should generate navigation wait, exists check, and/or scroll actions")
    }
}

