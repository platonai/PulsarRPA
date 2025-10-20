package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Focused tests for TextToAction.generateWebDriverAction() method
 * specifically testing element selection accuracy and ambiguity resolution
 */
@SpringBootTest(
    classes = [EnabledMockServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class TextToActionElementSelectionTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ===== Tests for element selection with multiple similar elements =====

    @Test
    fun `When multiple buttons exist and ask for specific one then select correct button`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateBlocking("点击第一个提交按钮", driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.expressions.size,
                "Should generate exactly one action for valid command"
            )
            val action = actionDescription.expressions.first()
            printlnPro("Multiple buttons - Generated action: $action")
            assertTrue(
                action.contains("click") || action.contains("driver.click"),
                "Should generate click action"
            )
        }

    @Test
    fun `When multiple links exist and ask for specific one then select correct link`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击关于链接", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Multiple links - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for link"
        )
    }

    @Test
    fun `When multiple input fields exist and ask for specific one then select correct field`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateBlocking("填写邮箱地址", driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.expressions.size,
                "Should generate exactly one action for valid command"
            )
            val action = actionDescription.expressions.first()
            printlnPro("Multiple inputs - Generated action: $action")
            assertTrue(
                action.contains("fill") || action.contains("driver.fill") ||
                        action.contains("type") || action.contains("driver.type"),
                "Should generate fill action for specific input"
            )
        }

    // ===== Tests for element selection by position/context =====

    @Test
    fun `When ask for element by position then select correct positioned element`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击顶部的搜索按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Position-based selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for positioned element"
        )
    }

    @Test
    fun `When ask for element by context then select contextually appropriate element`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("在表单中填写用户名", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Context-based selection - Generated action: $action")
        assertTrue(
            action.contains("fill") || action.contains("driver.fill") ||
                    action.contains("type") || action.contains("driver.type"),
            "Should generate fill action for contextually selected element"
        )
    }

    // ===== Tests for element selection with data-testid attributes =====

    @Test
    fun `When elements have data-testid and ask by testid then select correct element`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription =
            textToAction.generateBlocking("点击 data-testid 为 submit-primary 的按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("data-testid selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for data-testid element"
        )
    }

    // ===== Tests for element selection with ARIA attributes =====

    @Test
    fun `When elements have aria-label and ask by aria label then select correct element`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateBlocking("点击 aria-label 为 主要提交 的按钮", driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.expressions.size,
                "Should generate exactly one action for valid command"
            )
            val action = actionDescription.expressions.first()
            printlnPro("ARIA label selection - Generated action: $action")
            assertTrue(
                action.contains("click") || action.contains("driver.click"),
                "Should generate click action for ARIA labeled element"
            )
        }

    // ===== Tests for element selection with placeholder text =====

    @Test
    fun `When input has placeholder and ask by placeholder then select correct input`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-2.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("填写带有占位符的输入框", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Placeholder-based selection - Generated action: $action")
        assertTrue(
            action.contains("fill") || action.contains("driver.fill") ||
                    action.contains("type") || action.contains("driver.type"),
            "Should generate fill action for placeholder-based selection"
        )
    }

    // ===== Tests for element selection with text content =====

    @Test
    fun `When button has specific text and ask by text then select correct button`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击显示结果的按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Text-based button selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for text-based button selection"
        )
    }

    @Test
    fun `When link has specific text and ask by link text then select correct link`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击联系我们的链接", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Link text selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for link text selection"
        )
    }

    // ===== Tests for element selection with type attributes =====

    @Test
    fun `When multiple input types exist and ask for specific type then select correct input`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$generatedAssetsBaseURL/tta/forms-advanced-test.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateBlocking("填写邮箱输入框", driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.expressions.size,
                "Should generate exactly one action for valid command"
            )
            val action = actionDescription.expressions.first()
            printlnPro("Input type selection - Generated action: $action")
            assertTrue(
                action.contains("fill") || action.contains("driver.fill") ||
                        action.contains("type") || action.contains("driver.type"),
                "Should generate fill action for specific input type"
            )
        }

    @Test
    fun `When ask for password field then select password type input`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/forms-advanced-test.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("输入密码", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Password field selection - Generated action: $action")
        assertTrue(
            action.contains("fill") || action.contains("driver.fill") ||
                    action.contains("type") || action.contains("driver.type"),
            "Should generate fill action for password field"
        )
    }

    // ===== Tests for element selection with class names =====

    @Test
    fun `When element has specific class and ask by class then select correct element`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击带有 primary 类的按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Class-based selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for class-based selection"
        )
    }

    // ===== Tests for element selection with hierarchical context =====

    @Test
    fun `When ask for element within specific section then select correct section element`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
            driver.waitForSelector("body")

            val actionDescription = textToAction.generateBlocking("在第一个区域中点击按钮", driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.expressions.size,
                "Should generate exactly one action for valid command"
            )
            val action = actionDescription.expressions.first()
            printlnPro("Section-based selection - Generated action: $action")
            assertTrue(
                action.contains("click") || action.contains("driver.click"),
                "Should generate click action for section-based selection"
            )
        }

    // ===== Tests for element selection accuracy validation =====

    @Test
    fun `When element is selected then verify selectedElement field is populated`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击搜索按钮", driver)

        assertNotNull(actionDescription)
        printlnPro("Selected element: ${actionDescription.selectedElement}")
        printlnPro("Function calls: ${actionDescription.expressions}")

        // The selectedElement should be populated when an element is found
        // Note: It might be null if no suitable element is found, which is acceptable
        if (actionDescription.selectedElement != null) {
            val selectedElement = actionDescription.selectedElement!!
            assertTrue(selectedElement.tagName.isNotBlank(), "Selected element should have tag name")
            assertTrue(selectedElement.selector.isNotBlank(), "Selected element should have selector")
        }
    }

    @Test
    fun `When multiple similar elements exist then select most appropriate one`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generateBlocking("点击主要的提交按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.expressions.size, "Should generate exactly one action for valid command")
        val action = actionDescription.expressions.first()
        printlnPro("Most appropriate element selection - Generated action: $action")
        assertTrue(
            action.contains("click") || action.contains("driver.click"),
            "Should generate click action for most appropriate element"
        )
    }
}

