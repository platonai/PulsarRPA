package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Comprehensive tests for TextToAction.generateWebDriverAction() method
 * using real mock server pages (interactive-*.html)
 *
 * Each test opens a real webpage on the mock server and tests
 * the generateWebDriverAction method with actual interactive elements
 */
@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionMockServerTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ===== Tests using interactive-1.html (Basic interactions) =====

    @Test
    fun `When on interactive-1 page and ask to click search button then generate correct click action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击搜索按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click-related action")
    }

    @Test
    fun `When on interactive-1 page and ask to fill search input then generate correct fill action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("在搜索框输入 'AI工具'", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("type") || expression.contains("driver.type"),
                      "Should generate fill-related action")
    }

    @Test
    fun `When on interactive-1 page and ask to select from dropdown then generate correct select action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("选择选项2", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("select") || expression.contains("driver.select") ||
                      expression.contains("click") || expression.contains("driver.click"),
                      "Should generate select-related action")
    }

    @Test
    fun `When on interactive-1 page and ask to click toggle then generate correct click action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击切换开关", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action for toggle")
    }

    // ===== Tests using interactive-2.html (Complex form) =====

    @Test
    fun `When on interactive-2 page and ask to fill username field then generate correct fill action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("填写用户名 'testuser'", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("type") || expression.contains("driver.type"),
                      "Should generate fill-related action")
    }

    @Test
    fun `When on interactive-2 page and ask to fill email field then generate correct fill action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("输入邮箱 'test@example.com'", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("type") || expression.contains("driver.type"),
                      "Should generate fill-related action for email")
    }

    @Test
    fun `When on interactive-2 page and ask to adjust slider then generate correct slider action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("调整滑块到75", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("click") || expression.contains("driver.click"),
                      "Should generate action for slider interaction")
    }

    @Test
    fun `When on interactive-2 page and ask to toggle subscription switch then generate correct toggle action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("切换订阅开关", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action for toggle switch")
    }

    // ===== Tests using interactive-3.html (Dynamic content) =====

    @Test
    fun `When on interactive-3 page and ask to scroll to trigger animations then generate scroll action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-3.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("滚动到页面中间", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("scroll") || expression.contains("driver.scroll"),
                      "Should generate scroll-related action")
    }

    @Test
    fun `When on interactive-3 page and ask to adjust range input then generate correct range action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-3.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("调整范围输入到50", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("click") || expression.contains("driver.click"),
                      "Should generate action for range input")
    }

    // ===== Tests using interactive-4.html (Dark mode + Drag) =====

    @Test
    fun `When on interactive-4 page and ask to toggle dark mode then generate correct click action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-4.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("切换暗色模式", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action for dark mode toggle")
    }

    @Test
    fun `When on interactive-4 page and ask to drag item then generate correct drag action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-4.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("拖拽第一个项目", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("drag") || expression.contains("driver.drag") ||
                      expression.contains("click") || expression.contains("driver.click"),
                      "Should generate action for drag interaction")
    }

    // ===== Tests using advanced forms page =====

    @Test
    fun `When on forms-advanced-test page and ask to fill password field then generate correct fill action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/forms-advanced-test.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("输入密码 'secure123'", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("type") || expression.contains("driver.type"),
                      "Should generate fill-related action for password")
    }

    @Test
    fun `When on forms-advanced-test page and ask to select date then generate correct date action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/forms-advanced-test.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("选择日期", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("click") || expression.contains("driver.click"),
                      "Should generate action for date selection")
    }

    // ===== Tests using dynamic content page =====

    @Test
    fun `When on interactive-dynamic page and ask to wait for content then generate wait action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-dynamic.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("等待动态内容加载", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("waitFor") || expression.contains("driver.waitFor"),
                      "Should generate wait-related action")
    }

    // ===== Tests using ambiguity page =====

    @Test
    fun `When on interactive-ambiguity page and ask to click specific button then resolve ambiguity correctly`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击第一个提交按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action")
    }

    // ===== Tests for navigation actions =====

    @Test
    fun `When ask to navigate to different page then generate correct navigation action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("导航到 interactive-2 页面", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("navigateTo") || expression.contains("driver.navigateTo"),
                      "Should generate navigation-related action")
    }

    @Test
    fun `When ask to go back then generate correct back action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")
        // Navigate to another page first
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("返回上一页", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Generated action: $expression")
            assertTrue(expression.contains("back") || expression.contains("driver.back"),
                      "Should generate back navigation action")
    }

    // ===== Tests for error handling and edge cases =====

    @Test
    fun `When ask to interact with non-existent element then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击不存在的魔法按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")

        // Should either generate an empty action or a reasonable fallback
        printlnPro("Non-existent element request generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When ask with ambiguous command then select best matching action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("操作页面", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")

        // Should generate some reasonable action based on available elements
        printlnPro("Ambiguous command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for English language support =====

    @Test
    fun `When ask in English to click button then generate correct action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("Click the search button", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("English prompt generated: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action for English prompt")
    }

    @Test
    fun `When ask in English to fill form then generate correct action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("Fill in the username field with 'testuser'", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("English form fill generated: $expression")
            assertTrue(expression.contains("fill") || expression.contains("driver.fill") ||
                      expression.contains("type") || expression.contains("driver.type"),
                      "Should generate fill action for English prompt")
    }

    // ===== Tests for complex multi-step scenarios =====

    @Test
    fun `When ask to perform complex form submission then generate appropriate action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("提交表单", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Complex form submission generated: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click") ||
                      expression.contains("submit") || expression.contains("driver.submit"),
                      "Should generate submission-related action")
    }

    @Test
    fun `When ask to interact with dynamic elements then handle correctly`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-dynamic.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击加载更多按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Dynamic element interaction generated: $expression")
            assertTrue(expression.contains("click") || expression.contains("driver.click"),
                      "Should generate click action for dynamic element")
    }
}

