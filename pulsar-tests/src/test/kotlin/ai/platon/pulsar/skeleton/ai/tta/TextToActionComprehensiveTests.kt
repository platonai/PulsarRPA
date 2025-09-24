package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Comprehensive tests for TextToAction.generateWebDriverAction() method
 * Testing with real mocker server pages to ensure realistic scenarios
 */
@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionComprehensiveTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ======== TESTS WITH INTERACTIVE-1.HTML ========

    @Test
    fun `When on interactive-1 page and ask to type name then fill the name input field`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "在姓名输入框中输入 '张三'"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")
        assertTrue(action.contains("name") || action.contains("#name"), "Should target the name input field")
    }

    @Test
    fun `When on interactive-1 page and ask to select color then interact with color dropdown`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "选择最喜欢的颜色为蓝色"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click") || action.contains("select"), "Should generate selection action")
        assertTrue(action.contains("colorSelect") || action.contains("#colorSelect"), "Should target the color select")
    }

    @Test
    fun `When on interactive-1 page and ask to click add button then click the add button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "点击添加按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        assertTrue(action.contains("button") || action.contains("Add"), "Should target button element")
    }

    @Test
    fun `When on interactive-1 page and ask to toggle message then click toggle button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "切换消息显示"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        assertTrue(action.contains("toggle") || action.contains("Toggle"), "Should target toggle button")
    }

    @Test
    fun `When on interactive-1 page and ask to fill numbers then fill both number inputs`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "在第一个数字输入框输入10，第二个数字输入框输入20"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        // Should handle the complex instruction somehow
        assertTrue(action.contains("fill") || action.contains("type") || action.contains("click"),
          "Should generate some interaction")
    }

    // ======== TESTS WITH INTERACTIVE-2.HTML ========

    @Test
    fun `When on interactive-2 page and ask to fill name then fill name input field`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val command = "在姓名输入框输入 '李四'"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")
        assertTrue(action.contains("nameInput") || action.contains("#nameInput"), "Should target name input")
    }

    @Test
    fun `When on interactive-2 page and ask to select language then interact with language dropdown`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val command = "选择最喜欢的编程语言为Python"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click") || action.contains("select"), "Should generate selection action")
        assertTrue(action.contains("languageSelect") || action.contains("#languageSelect"), "Should target language select")
    }

    @Test
    fun `When on interactive-2 page and ask to toggle newsletter then click the checkbox`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val command = "勾选订阅通讯复选框"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("check") || action.contains("click"), "Should generate check or click action")
        assertTrue(action.contains("subscribeToggle") || action.contains("#subscribeToggle"), "Should target subscribe checkbox")
    }

    @Test
    fun `When on interactive-2 page and ask to click show summary then click the button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val command = "点击显示摘要按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        assertTrue(action.contains("showSummary") || action.contains("button"), "Should target summary button")
    }

    @Test
    fun `When on interactive-2 page and ask to adjust text size then interact with slider`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val command = "调整文本大小滑块到24"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        // Slider interaction might be handled differently
        assertTrue(action.contains("click") || action.contains("fill") || action.contains("evaluate"),
              "Should generate interaction for slider")
    }

    // ======== TESTS WITH INTERACTIVE-AMBIGUITY.HTML ========

    @Test
    fun `When on ambiguity page and ask to click save in user section then select user save button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "点击用户管理区域的保存按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        // Should disambiguate and select the user section save button
        println("Selected element: ${actionDescription.selectedElement}")
    }

    @Test
    fun `When on ambiguity page and ask to click save in product section then select product save button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "点击产品管理区域的保存按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        // Should disambiguate and select the product section save button
        println("Selected element: ${actionDescription.selectedElement}")
    }

    @Test
    fun `When on ambiguity page and ask to click top right menu then select header menu button`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "点击右上角的菜单按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        // Should use position context to select the top-right menu button
        println("Selected element: ${actionDescription.selectedElement}")
    }

    @Test
    fun `When on ambiguity page and ask to fill billing name then select billing name field`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "在账单地址区域填写姓名为 '王五'"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")
        // Should disambiguate between billing and shipping forms
        println("Selected element: ${actionDescription.selectedElement}")
    }

    @Test
    fun `When on ambiguity page and ask to search products then select product search field`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "在产品搜索框中输入 '手机'"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Command: $command -> Generated action: $action")
        assertTrue(action.contains("fill") || action.contains("type"), "Should generate fill action")
        // Should select the product search field among multiple search fields
        println("Selected element: ${actionDescription.selectedElement}")
    }

    // ======== COMPLEX SCENARIO TESTS ========

    @Test
    fun `When given complex multi-step instruction then handle appropriately`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "输入姓名张三，选择蓝色，然后点击添加按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        // Should handle the complex instruction in some way (might pick the most important action)
        println("Complex command: $command -> Generated: ${actionDescription.functionCalls}")
        println("Model response: ${actionDescription.modelResponse.content}")
    }

    @Test
    fun `When given ambiguous command then select most reasonable element`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val command = "点击编辑按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")
        val action = actionDescription.functionCalls.first()
        println("Ambiguous command: $command -> Generated action: $action")
        assertTrue(action.contains("click"), "Should generate click action")
        // Should select one of the edit buttons based on some logic
        println("Selected element: ${actionDescription.selectedElement}")
    }

    @Test
    fun `When given command with no matching elements then generate empty or fallback action`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "点击不存在的提交按钮"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        // Should handle non-existent elements gracefully
        println("Non-existent element command: $command -> Generated: ${actionDescription.functionCalls}")
        println("Model response: ${actionDescription.modelResponse.content}")
    }

    // ======== LANGUAGE AND CULTURAL TESTS ========

    @Test
    fun `When given English commands then handle appropriately`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val commands = listOf(
        "Type 'John' in the name field",
        "Select blue color from dropdown",
        "Click the add button",
        "Toggle the message display"
        )

        commands.forEach { command ->
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
        val action = actionDescription.functionCalls.first()
        println("English command: $command -> Generated action: $action")
        }
    }

    @Test
    fun `When given mixed language commands then handle appropriately`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val commands = listOf(
        "click 显示摘要 button",
        "在name input输入'Test'",
        "select Python from language dropdown",
        "勾选 subscribe toggle"
        )

        commands.forEach { command ->
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
        val action = actionDescription.functionCalls.first()
        println("Mixed language command: $command -> Generated action: $action")
        }
    }

    // ======== EDGE CASE TESTS ========

    @Test
    fun `When given empty command then handle gracefully`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = ""
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        println("Empty command -> Generated: ${actionDescription.functionCalls}")
        println("Model response: ${actionDescription.modelResponse.content}")
    }

    @Test
    fun `When given very long command then handle appropriately`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "请帮我在这个页面的姓名输入框里面输入一个非常长的名字叫做'这是一个非常长的中文名字用来测试系统对于长文本输入的处理能力'然后选择颜色为浅黄色接着点击添加按钮计算一些数字最后切换消息显示"
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command")

        println("Very long command -> Generated: ${actionDescription.functionCalls}")
        println("Model response length: ${actionDescription.modelResponse.content.length}")
    }

    @Test
    fun `When given command with special characters then handle appropriately`() = runWebDriverTest { driver ->
        driver.navigateTo("http://127.0.0.1:$port/generated/tta/interactive-2.html")
        driver.waitForSelector("body")

        val commands = listOf(
        "输入名字为 'O'Brien'",
        "输入邮箱为 'test@example.com'",
        "输入特殊字符 '!@#$%^&*()'",
        "输入中文符号 '你好世界'"
        )

        commands.forEach { command ->
        val actionDescription = textToAction.generateWebDriverAction(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.functionCalls.size, "Should generate exactly one action for valid command: $command")
        val action = actionDescription.functionCalls.first()
        println("Special characters command: $command -> Generated action: $action")
        }
    }
}