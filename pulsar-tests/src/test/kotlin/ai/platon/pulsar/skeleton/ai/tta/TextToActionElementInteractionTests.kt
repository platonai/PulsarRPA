package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.util.server.EnableMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Element-specific interaction tests for TextToAction.generateWebDriverAction() method
 * Focus on different HTML element types and interaction patterns
 */
@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@Disabled("Takes very long time, run it manually. The test cases are suitable for multiple actions, but the actually one action is forced")
@SpringBootTest(classes = [EnableMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionElementInteractionTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    @Test
    fun `When given text input commands then generate appropriate fill actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "输入姓名张三" to "name",
            "在姓名框输入李四" to "name",
            "填写名字为王五" to "name",
            "输入name field John" to "name"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")

            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Input command: $command -> Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("type"), "Should generate fill action")
            // no selectors since 20251023, advanced locators are supported
//            assertTrue(expression.contains(expectedField), "Should target the name field")

        }
    }

    @Test
    fun `When given number input commands then generate appropriate fill actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "输入数字10" to listOf("num1", "num2"),
            "在第一个数字框输入20" to listOf("num1"),
            "填写第二个数字为30" to listOf("num2"),
            "输入number 40" to listOf("num1", "num2")
        )

        testCases.forEach { (command, possibleFields) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")

            val expression = actionDescription.cssFriendlyExpressions.first()
            printlnPro("Number input command: $command -> Generated action: $expression")
            assertTrue(expression.contains("fill") || expression.contains("type"), "Should generate fill action")

            // Should target one of the number fields
            // no selectors since 20251023, advanced locators are supported
//            val targetsCorrectField = possibleFields.any { field -> expression.contains(field) }
//            assertTrue(targetsCorrectField, "Should target one of: $possibleFields")
        }
    }

    // ======== SELECT/DROPDOWN TESTS ========

    @Test
    fun `When given select commands then generate appropriate selection actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "选择蓝色" to "colorSelect",
            "选择最喜欢的颜色为浅绿色" to "colorSelect",
            "从下拉框选择浅黄色" to "colorSelect",
            "select lightblue from dropdown" to "colorSelect"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Select command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click") || expression.contains("select"), "Should generate selection action")
                // no selectors since 20251023, advanced locators are supported
                // assertTrue(expression.contains(expectedField), "Should target the select field")
        }
    }

    @Test
    fun `When given language selection commands then generate appropriate actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "选择Python语言" to "languageSelect",
            "选择编程语言为JavaScript" to "languageSelect",
            "从语言列表选择Kotlin" to "languageSelect",
            "select Rust from languages" to "languageSelect"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Language select command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click") || expression.contains("select"), "Should generate selection action")
                assertTrue(expression.contains(expectedField), "Should target the language select")
        }
    }

    // ======== BUTTON CLICK TESTS ========

    @Test
    fun `When given button click commands then generate appropriate click actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击添加按钮" to "add",
            "单击计算按钮" to "add",
            "按下切换消息按钮" to "toggle",
            "click the add button" to "add"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Button click command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click"), "Should generate click action")
            // no selectors since 20251023, advanced locators are supported
//                assertTrue(expression.contains("button") || expression.contains(expectedContext), "Should target button element")
        }
    }

    @Test
    fun `When given show summary commands then generate appropriate click actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击显示摘要" to "showSummary",
            "单击显示摘要按钮" to "showSummary",
            "按下摘要按钮" to "showSummary",
            "click show summary button" to "showSummary"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Show summary command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click"), "Should generate click action")
            // no selectors since 20251023, advanced locators are supported
//                assertTrue(expression.contains(expectedContext) || expression.contains("button"), "Should target summary button")
        }
    }

    // ======== CHECKBOX/TOGGLE TESTS ========

    @Test
    fun `When given checkbox commands then generate appropriate check actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "勾选订阅通讯" to "check",
            "选中订阅复选框" to "check",
            "启用通讯订阅" to "check",
            "check the newsletter subscription" to "check"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Checkbox command: $command -> Generated action: $expression")
                assertTrue(expression.contains(expectedAction) || expression.contains("click"), "Should generate check or click action")
            // no selectors since 20251023, advanced locators are supported
//                assertTrue(expression.contains("subscribeToggle") || expression.contains("checkbox"), "Should target subscribe checkbox")
        }
    }

    @Test
    fun `When given uncheck commands then generate appropriate uncheck actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        // First check the checkbox
        driver.click("#subscribeToggle")

        val testCases = listOf(
            "取消勾选订阅" to "uncheck",
            "取消选中通讯订阅" to "uncheck",
            "禁用通讯订阅" to "uncheck",
            "uncheck the newsletter subscription" to "uncheck"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Uncheck command: $command -> Generated action: $expression")
                assertTrue(expression.contains(expectedAction) || expression.contains("click"), "Should generate uncheck or click action")
            // no selectors since 20251023, advanced locators are supported
//                assertTrue(expression.contains("subscribeToggle") || expression.contains("checkbox"), "Should target subscribe checkbox")
        }
    }

    // ======== SLIDER/RANGE TESTS ========

    @Test
    fun `When given slider commands then generate appropriate interaction actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "调整文本大小到24" to "textSizeSlider",
            "设置字体大小为20" to "textSizeSlider",
            "移动滑块到18" to "textSizeSlider",
            "adjust text size to 22" to "textSizeSlider"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Slider command: $command -> Generated action: $expression")
                // Slider might be handled via click, fill, or JavaScript evaluation
                assertTrue(expression.contains("click") || expression.contains("fill") || expression.contains("evaluate"),
                          "Should generate interaction for slider")
            // no selectors since 20251023, advanced locators are supported
//                assertTrue(expression.contains(expectedField) || expression.contains("slider"), "Should target text size slider")
        }
    }

    // ======== FORM INTERACTION TESTS ========

    @Test
    fun `When given form filling commands then generate appropriate sequential actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "填写姓名为张三，选择语言为Python" to "multi-field",
            "输入名字John，勾选订阅" to "name-and-subscribe",
            "填写表单：姓名李四，语言Kotlin" to "form-with-name-language"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Form command: $command -> Generated action: $expression")
                // Should handle the complex form instruction
                assertTrue(expression.contains("fill") || expression.contains("click") || expression.contains("check"),
                          "Should generate form interaction")
        }
    }

    // ======== DYNAMIC CONTENT TESTS ========

    @Test
    fun `When given commands for dynamic elements then generate appropriate actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        // First toggle the message to make it visible
        driver.click("button[onclick='toggleMessage()']")

        val testCases = listOf(
            "点击切换消息" to "toggle",
            "隐藏消息" to "toggle",
            "再次点击切换" to "toggle",
            "toggle the message again" to "toggle"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Dynamic content command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click"), "Should generate click action")
                assertTrue(expression.contains("toggle") || expression.contains("button"), "Should target toggle button")
        }
    }

    // ======== SCROLL AND NAVIGATION TESTS ========

    @Test
    fun `When given scroll commands then generate appropriate scroll actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "滚动到页面中间" to "scrollToMiddle",
            "向下滚动" to "scrollDown",
            "滚动到顶部" to "scrollUp",
            "scroll to middle of page" to "scrollToMiddle"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Scroll command: $command -> Generated action: $expression")
                assertTrue(expression.contains("scroll"), "Should generate scroll action")
                assertTrue(expression.contains(expectedAction), "Should generate correct scroll action")
        }
    }

    // ======== WAIT AND TIMING TESTS ========

    @Test
    fun `When given wait commands then generate appropriate wait actions`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "等待页面加载" to "waitForSelector",
            "等待按钮出现" to "waitForSelector",
            "等待输入框可用" to "waitForSelector",
            "wait for the add button" to "waitForSelector"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Wait command: $command -> Generated action: $expression")
                assertTrue(expression.contains("waitFor"), "Should generate wait action")
                assertTrue(expression.contains(expectedAction), "Should generate correct wait action")
        }
    }

    // ======== ERROR HANDLING TESTS ========

    @Test
    fun `When given commands for non-existent elements then handle gracefully`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击不存在的按钮" to "non-existent",
            "输入密码到密码框" to "non-existent",
            "选择不存在的选项" to "non-existent",
            "click the submit button" to "non-existent"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")

            printlnPro("Non-existent element command: $command -> Generated: ${actionDescription.cssFriendlyExpressions}")
            printlnPro("Model response: ${actionDescription.modelResponse!!.content}")

            // Should handle non-existent elements gracefully - might generate empty function or fallback
            assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should handle non-existent elements gracefully")
        }
    }

    @Test
    fun `When given ambiguous commands then select most reasonable element`() = runEnhancedWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击保存按钮" to "ambiguous-save",
            "点击编辑按钮" to "ambiguous-edit",
            "输入姓名" to "ambiguous-name",
            "click the button" to "ambiguous-button"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
                val expression = actionDescription.cssFriendlyExpressions.first()
                printlnPro("Ambiguous command: $command -> Generated action: $expression")
                assertTrue(expression.contains("click") || expression.contains("fill"), "Should generate some action")
                // Should select one of the available elements

        }
    }
}

