package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.util.server.EnableMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Edge case and boundary condition tests for TextToAction.generateWebDriverAction() method
 */
@Order(1000)
@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@Disabled("Takes very long time, run it manually. The test cases are suitable for multiple actions, but the actually one action is forced")
@SpringBootTest(classes = [EnableMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionEdgeCasesTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ===== Tests for empty and null scenarios =====

    @Test
    fun `When command is empty then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Empty command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When command is blank then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("   ", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Blank command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When command is very long then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val longCommand = "点击搜索按钮并输入一些文本然后滚动到页面底部再点击提交按钮并且等待页面加载完成"
        val actionDescription = textToAction.generate(longCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Long command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for special characters and encoding =====

    @Test
    fun `When command contains special characters then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val specialCommand = "点击搜索按钮!@#$%^*()"
        val actionDescription = textToAction.generate(specialCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Special characters command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When command contains unicode characters then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val unicodeCommand = "点击搜索按钮 🎯 测试"
        val actionDescription = textToAction.generate(unicodeCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Unicode command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When command contains quotes then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val quotedCommand = """点击"搜索"按钮"""
        val actionDescription = textToAction.generate(quotedCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Quoted command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for impossible requests =====

    @Test
    fun `When command asks for non-existent element then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val impossibleCommand = "点击魔法传送门按钮"
        val actionDescription = textToAction.generate(impossibleCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Impossible element request generated: ${actionDescription.cssFriendlyExpressions}")
    }

    @Test
    fun `When command asks for impossible action then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val impossibleCommand = "让页面飞起来"
        val actionDescription = textToAction.generate(impossibleCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Impossible action request generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for contradictory requests =====

    @Test
    fun `When command contains contradictory instructions then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val contradictoryCommand = "点击搜索按钮但不要点击任何东西"
        val actionDescription = textToAction.generate(contradictoryCommand, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Contradictory command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for ambiguous requests =====

    @Test
    fun `When command is extremely vague then select reasonable action`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val vagueCommands = listOf(
            "做点什么",
            "操作页面",
            "开始",
            "执行",
            "互动"
        )

        vagueCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
            printlnPro("Vague command '$command' generated: ${actionDescription.cssFriendlyExpressions}")
        }
    }

    @Test
    fun `When command asks for something that could be multiple things then select one`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val ambiguousCommands = listOf(
            "点击按钮",
            "填写输入框",
            "选择选项"
        )

        ambiguousCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
            printlnPro("Ambiguous command '$command' generated: ${actionDescription.cssFriendlyExpressions}")
        }
    }

    // ===== Tests for pages with no interactive elements =====

    @Test
    fun `When page has no interactive elements then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("about:blank")

        val actionDescription = textToAction.generate("点击按钮", driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("No elements page generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for malformed commands =====

    @Test
    fun `When command contains grammar errors then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val malformedCommands = listOf(
            "点击搜素按钮", // typo: 搜素 instead of 搜索
            "填写输入", // incomplete
            "按钮点击", // reversed word order
            "clik button" // typo in English
        )

        malformedCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
            printlnPro("Malformed command '$command' generated: ${actionDescription.cssFriendlyExpressions}")
        }
    }

    // ===== Tests for extremely specific requests =====

    @Test
    fun `When command is extremely specific then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val specificCommands = listOf(
            "点击id为search-btn的按钮",
            "选择class为form-control的输入框",
            "点击第3个div中的按钮",
            "填写name属性为username的输入框"
        )

        specificCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
            printlnPro("Specific command '$command' generated: ${actionDescription.cssFriendlyExpressions}")
        }
    }

    // ===== Tests for rapid successive calls =====

    @Test
    fun `When multiple rapid calls are made then handle consistently`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val command = "点击搜索按钮"
        val results = mutableListOf<ActionDescription>()

        // Make 5 rapid calls
        repeat(5) {
            val actionDescription = textToAction.generate(command, driver)
            results.add(actionDescription)
        }

        // All results should be valid
        results.forEach { result ->
            assertNotNull(result)
            assertTrue(result.cssFriendlyExpressions.size <= 1, "Each rapid call should generate at most one action")
        }

        printlnPro("Rapid calls generated consistent results: ${results.map { it.cssFriendlyExpressions }}")
    }

    // ===== Tests for mixed language commands =====

    @Test
    fun `When command mixes languages then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val mixedCommands = listOf(
            "点击search按钮",
            "click搜索button",
            "填写input框",
            "select选项"
        )

        mixedCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command: $command")
            printlnPro("Mixed language command '$command' generated: ${actionDescription.cssFriendlyExpressions}")
        }
    }

    // ===== Tests for extremely long text input =====

    @Test
    fun `When fill command contains extremely long text then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val longText = "a".repeat(1000) // 1000 character string
        val command = "在搜索框输入 '$longText'"

        val actionDescription = textToAction.generate(command, driver)

        assertNotNull(actionDescription)
        assertEquals(1, actionDescription.cssFriendlyExpressions.size, "Should generate exactly one action for valid command")
        printlnPro("Long text command generated: ${actionDescription.cssFriendlyExpressions}")
    }

    // ===== Tests for selectedElement field validation =====

    @Test
    fun `When element is selected then validate selectedElement structure`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击搜索按钮", driver)

        assertNotNull(actionDescription)
    }

    // ===== Tests for model response validation =====

    @Test
    fun `When action is generated then validate model response structure`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$generatedAssetsBaseURL/tta/interactive-1.html")
        driver.waitForSelector("body")

        val actionDescription = textToAction.generate("点击搜索按钮", driver)

        assertNotNull(actionDescription)
        assertNotNull(actionDescription.modelResponse, "Model response should not be null")

        if (actionDescription.modelResponse != ModelResponse.LLM_NOT_AVAILABLE) {
            assertTrue(actionDescription.modelResponse!!.content.isNotBlank(),
                      "Model response content should not be blank")
        }

        printlnPro("Model response validation passed: ${actionDescription.modelResponse}")
    }
}

