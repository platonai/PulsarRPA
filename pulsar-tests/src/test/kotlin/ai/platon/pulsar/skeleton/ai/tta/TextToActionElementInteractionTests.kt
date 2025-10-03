package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.EnabledMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Element-specific interaction tests for TextToAction.generateWebDriverAction() method
 * Focus on different HTML element types and interaction patterns
 */
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionElementInteractionTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ======== INPUT FIELD TESTS ========

    @Test
    fun `Make sure interactive elements calculated correctly`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val interactiveElements = textToAction.extractInteractiveElements(driver)
        println("Extracted ${interactiveElements.size} interactive elements")
        interactiveElements.forEach { println(" - ${it.selector} [${it.tagName}] visible=${it.isVisible}") }

        // All returned elements must be visible and one of allowed interactive tags
        val allowedTags = setOf("input", "select", "textarea", "button", "a")
        assertTrue(interactiveElements.isNotEmpty(), "Should extract interactive elements from page")
        assertTrue(interactiveElements.all { it.isVisible }, "All interactive elements should be visible")
        assertTrue(interactiveElements.all { it.tagName.lowercase() in allowedTags }, "Only interactive tags should be returned")

        // Expected interactive elements on interactive-1.html
        val expectedSelectors = setOf(
            "#name",
            "#colorSelect",
            "#num1",
            "#num2",
            "button[onclick=\"addNumbers()\"]",
            "button[onclick=\"toggleMessage()\"]",
        )
        val selectors = interactiveElements.map { it.selector }.toSet()

        // Must include the expected set (and ideally equal for this page)
        assertTrue(expectedSelectors.all { it in selectors }, "Should include expected interactive elements: $expectedSelectors, actual: $selectors")
        assertEquals(expectedSelectors.size, selectors.size, "Should not include non-interactive elements on this page")

        // Ensure some known non-interactive node is not present
        assertTrue(selectors.none { it.contains("#hiddenMessage") }, "Hidden paragraph should not be included")
    }

    @Test
    fun `When given text input commands then generate appropriate fill actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "输入姓名张三" to "name",
            "在姓名框输入李四" to "name",
            "填写名字为王五" to "name",
            "输入name field John" to "name"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")

            val action = actionDescription.functionCalls.first()
            println("Input command: $command -> Generated action: $action")
            assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")
            assertTrue(action.contains(expectedField), "Should target the name field")
        }
    }

    @Test
    fun `When given number input commands then generate appropriate fill actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "输入数字10" to listOf("num1", "num2"),
            "在第一个数字框输入20" to listOf("num1"),
            "填写第二个数字为30" to listOf("num2"),
            "输入number 40" to listOf("num1", "num2")
        )

        testCases.forEach { (command, possibleFields) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")

            val action = actionDescription.functionCalls.first()
            println("Number input command: $command -> Generated action: $action")
            assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")

            // Should target one of the number fields
            val targetsCorrectField = possibleFields.any { field -> action.contains(field) }
            assertTrue(targetsCorrectField, "Should target one of: $possibleFields")
        }
    }

    // ======== SELECT/DROPDOWN TESTS ========

    @Test
    fun `When given select commands then generate appropriate selection actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "选择蓝色" to "colorSelect",
            "选择最喜欢的颜色为浅绿色" to "colorSelect",
            "从下拉框选择浅黄色" to "colorSelect",
            "select lightblue from dropdown" to "colorSelect"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Select command: $command -> Generated action: $action")
                assertTrue(action.contains("click") || action.contains("select"), "Should generate selection action")
                assertTrue(action.contains(expectedField), "Should target the select field")
        }
    }

    @Test
    fun `When given language selection commands then generate appropriate actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "选择Python语言" to "languageSelect",
            "选择编程语言为JavaScript" to "languageSelect",
            "从语言列表选择Kotlin" to "languageSelect",
            "select Rust from languages" to "languageSelect"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Language select command: $command -> Generated action: $action")
                assertTrue(action.contains("click") || action.contains("select"), "Should generate selection action")
                assertTrue(action.contains(expectedField), "Should target the language select")
        }
    }

    // ======== BUTTON CLICK TESTS ========

    @Test
    fun `When given button click commands then generate appropriate click actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击添加按钮" to "add",
            "单击计算按钮" to "add",
            "按下切换消息按钮" to "toggle",
            "click the add button" to "add"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Button click command: $command -> Generated action: $action")
                assertTrue(action.contains("click"), "Should generate click action")
                assertTrue(action.contains("button") || action.contains(expectedContext), "Should target button element")
        }
    }

    @Test
    fun `When given show summary commands then generate appropriate click actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击显示摘要" to "showSummary",
            "单击显示摘要按钮" to "showSummary",
            "按下摘要按钮" to "showSummary",
            "click show summary button" to "showSummary"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Show summary command: $command -> Generated action: $action")
                assertTrue(action.contains("click"), "Should generate click action")
                assertTrue(action.contains(expectedContext) || action.contains("button"), "Should target summary button")
        }
    }

    // ======== CHECKBOX/TOGGLE TESTS ========

    @Test
    fun `When given checkbox commands then generate appropriate check actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "勾选订阅通讯" to "check",
            "选中订阅复选框" to "check",
            "启用通讯订阅" to "check",
            "check the newsletter subscription" to "check"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Checkbox command: $command -> Generated action: $action")
                assertTrue(action.contains(expectedAction) || action.contains("click"), "Should generate check or click action")
                assertTrue(action.contains("subscribeToggle") || action.contains("checkbox"), "Should target subscribe checkbox")
        }
    }

    @Test
    fun `When given uncheck commands then generate appropriate uncheck actions`() = runWebDriverTest(browser) { driver ->
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
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Uncheck command: $command -> Generated action: $action")
                assertTrue(action.contains(expectedAction) || action.contains("click"), "Should generate uncheck or click action")
                assertTrue(action.contains("subscribeToggle") || action.contains("checkbox"), "Should target subscribe checkbox")
        }
    }

    // ======== SLIDER/RANGE TESTS ========

    @Test
    fun `When given slider commands then generate appropriate interaction actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "调整文本大小到24" to "textSizeSlider",
            "设置字体大小为20" to "textSizeSlider",
            "移动滑块到18" to "textSizeSlider",
            "adjust text size to 22" to "textSizeSlider"
        )

        testCases.forEach { (command, expectedField) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Slider command: $command -> Generated action: $action")
                // Slider might be handled via click, fill, or JavaScript evaluation
                assertTrue(action.contains("click") || action.contains("fill") || action.contains("evaluate"),
                          "Should generate interaction for slider")
                assertTrue(action.contains(expectedField) || action.contains("slider"), "Should target text size slider")
        }
    }

    // ======== FORM INTERACTION TESTS ========

    @Test
    fun `When given form filling commands then generate appropriate sequential actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "填写姓名为张三，选择语言为Python" to "multi-field",
            "输入名字John，勾选订阅" to "name-and-subscribe",
            "填写表单：姓名李四，语言Kotlin" to "form-with-name-language"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Form command: $command -> Generated action: $action")
                // Should handle the complex form instruction
                assertTrue(action.contains("fill") || action.contains("click") || action.contains("check"),
                          "Should generate form interaction")
        }
    }

    // ======== DYNAMIC CONTENT TESTS ========

    @Test
    fun `When given commands for dynamic elements then generate appropriate actions`() = runWebDriverTest(browser) { driver ->
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
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Dynamic content command: $command -> Generated action: $action")
                assertTrue(action.contains("click"), "Should generate click action")
                assertTrue(action.contains("toggle") || action.contains("button"), "Should target toggle button")
        }
    }

    // ======== SCROLL AND NAVIGATION TESTS ========

    @Test
    fun `When given scroll commands then generate appropriate scroll actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "滚动到页面中间" to "scrollToMiddle",
            "向下滚动" to "scrollDown",
            "滚动到顶部" to "scrollUp",
            "scroll to middle of page" to "scrollToMiddle"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Scroll command: $command -> Generated action: $action")
                assertTrue(action.contains("scroll"), "Should generate scroll action")
                assertTrue(action.contains(expectedAction), "Should generate correct scroll action")
        }
    }

    // ======== WAIT AND TIMING TESTS ========

    @Test
    fun `When given wait commands then generate appropriate wait actions`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "等待页面加载" to "waitForSelector",
            "等待按钮出现" to "waitForSelector",
            "等待输入框可用" to "waitForSelector",
            "wait for the add button" to "waitForSelector"
        )

        testCases.forEach { (command, expectedAction) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Wait command: $command -> Generated action: $action")
                assertTrue(action.contains("waitFor"), "Should generate wait action")
                assertTrue(action.contains(expectedAction), "Should generate correct wait action")
        }
    }

    // ======== ERROR HANDLING TESTS ========

    @Test
    fun `When given commands for non-existent elements then handle gracefully`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击不存在的按钮" to "non-existent",
            "输入密码到密码框" to "non-existent",
            "选择不存在的选项" to "non-existent",
            "click the submit button" to "non-existent"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")

            println("Non-existent element command: $command -> Generated: ${actionDescription.functionCalls}")
            println("Model response: ${actionDescription.modelResponse.content}")

            // Should handle non-existent elements gracefully - might generate empty function or fallback
            assertTrue(actionDescription.functionCalls.size <= 1, "Should handle non-existent elements gracefully")
        }
    }

    @Test
    fun `When given ambiguous commands then select most reasonable element`() = runWebDriverTest(browser) { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val testCases = listOf(
            "点击保存按钮" to "ambiguous-save",
            "点击编辑按钮" to "ambiguous-edit",
            "输入姓名" to "ambiguous-name",
            "click the button" to "ambiguous-button"
        )

        testCases.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generateWebDriverAction(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
                val action = actionDescription.functionCalls.first()
                println("Ambiguous command: $command -> Generated action: $action")
                assertTrue(action.contains("click") || action.contains("fill"), "Should generate some action")
                // Should select one of the available elements
                println("Selected element: ${actionDescription.selectedElement}")
        }
    }
}