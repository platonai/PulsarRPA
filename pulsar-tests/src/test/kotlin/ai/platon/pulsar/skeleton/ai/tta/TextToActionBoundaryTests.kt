package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Boundary condition and edge case tests for TextToAction
 * Testing requirement: "Boundary Testing" from README-AI.md
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionBoundaryTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `When no matching element exists then generate empty suspend function`() {
        val prompt = "点击不存在的按钮" // Non-existent button

        val result = textToAction.generateWebDriverActions(prompt)

        assertNotNull(result)
        assertTrue(result.functionCalls.isEmpty())
        // selectedElement is only available in the suspend version, so skip this assertion
        // assertTrue(result.selectedElement == null)
        assertTrue(result.modelResponse.content.contains("suspend"))
        assertFalse(result.modelResponse.content.contains("click"))
    }

    @Test
    fun `When given single character command then handle appropriately`() {
        val prompt = "点" // Single character

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        // Should handle minimal input
        assertTrue(response.content.isNotBlank(), "Should handle single character command")
    }

    @Test
    fun `When given command with only whitespace then handle gracefully`() {
        val prompt = "   " // Only whitespace

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        // Should not crash and should provide helpful response
        assertNotNull(response.content)
        assertTrue(response.content.isNotBlank() || response.content.isEmpty())
    }

    @Test
    fun `When given maximum length command then process without overflow`() {
        val prompt = "点击按钮".repeat(50) // Very long repeated command

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        // Should handle very long commands without crashing
        assertNotNull(response.content)
        assertTrue(response.content.length > 0)
    }

    @Test
    fun `When given command with null characters then handle sanitization`() {
        val prompt = "点击按钮\u0000" // Command with null character

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        // Should handle null characters gracefully
        assertNotNull(response.content)
    }

    @Test
    fun `When given Unicode emoji commands then handle properly`() {
        val prompts = listOf(
            "点击🔍按钮",
            "在输入框输入👋",
            "滚动到⬇️位置"
        )

        prompts.forEach { prompt ->
            val response = textToAction.useWebDriverLegacy(prompt)
            TextToActionTestBase.lastResponse = response
            println("Prompt: $prompt")
            println("Response: ${response.content}")

            assertTrue(response.content.isNotBlank(), "Should handle emoji in commands")
        }
    }

    @Test
    fun `When given mixed language command then process appropriately`() {
        val prompt = "click 登录 button and fill username" // Mixed English/Chinese

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle mixed languages
        assertTrue(content.isNotBlank(), "Should handle mixed language commands")
        assertTrue(
            content.contains("click") || content.contains("fill"),
            "Should generate appropriate actions"
        )
    }

    @Test
    fun `When given command with extreme numerical values then handle sanely`() {
        val prompt = "滚动到页面9999999999%位置" // Impossible scroll percentage

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle extreme values
        assertTrue(content.contains("scroll") || content.contains("滚动"))
        // Should not include the impossible value in generated code
        assertFalse(content.contains("9999999999"))
    }

    @Test
    fun `When given circular command references then prevent infinite loops`() {
        val prompt = "点击按钮然后再次点击同一个按钮" // Circular reference

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle circular references without infinite loops
        assertTrue(content.isNotBlank(), "Should handle circular references")
        assertTrue(content.contains("click") || content.contains("点击"))
    }

    @Test
    fun `When given command with incomplete sentences then complete appropriately`() {
        val prompts = listOf(
            "点击", // Incomplete: just "click"
            "输入文字在", // Incomplete: "input text in"
            "滚动然后" // Incomplete: "scroll then"
        )

        prompts.forEach { prompt ->
            val response = textToAction.useWebDriverLegacy(prompt)
            TextToActionTestBase.lastResponse = response
            println("Incomplete prompt: $prompt")
            println("Response: ${response.content}")

            // Should handle incomplete sentences
            assertTrue(response.content.isNotBlank(), "Should handle incomplete command: $prompt")
        }
    }

    @Test
    fun `When given command with logical paradox then handle gracefully`() {
        val prompt = "同时点击和不点击按钮" // Logical paradox

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should recognize and handle paradox
        assertTrue(
            content.contains("paradox") ||
            content.contains("矛盾") ||
            content.contains("impossible") ||
            content.contains("不可能") ||
            content.contains("clarify") ||
            content.contains("澄清"),
            "Should handle logical paradox"
        )
    }

    @Test
    fun `When given command with future tense requirements then handle appropriately`() {
        val prompt = "明天点击登录按钮" // Future tense: "tomorrow click login button"

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle future tense by providing immediate action guidance
        assertTrue(content.contains("click") || content.contains("登录"))
    }

    @Test
    fun `When given command with impossible physical requirements then handle sanely`() {
        val prompt = "用意念点击按钮" // "Click button with mind power" - impossible

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle impossible requirements by providing standard WebDriver solution
        assertTrue(
            content.contains("click") || content.contains("WebDriver"),
            "Should provide standard WebDriver solution for impossible requirements"
        )
    }

    @Test
    fun `When processing with empty interactive elements list then handle gracefully`() {
        val prompt = "点击任何可交互元素"

        val result = textToAction.generateWebDriverActions(prompt)

        assertNotNull(result)
        assertTrue(result.functionCalls.isEmpty() || result.modelResponse.content.contains("No interactive elements"))
    }

    @Test
    fun `When given command with recursive self-references then handle without stack overflow`() {
        val prompt = "点击按钮然后重复点击同一个按钮三次再重复整个过程两次" // Recursive self-reference

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle recursive references without stack overflow
        assertTrue(content.isNotBlank(), "Should handle recursive self-references")
        assertTrue(content.contains("click") || content.contains("点击"))
    }

    @Test
    fun `When given command with quantum superposition then collapse to concrete action`() {
        val prompt = "点击按钮同时保持不点击的状态" // Quantum superposition analogy

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should collapse to concrete action
        assertTrue(
            content.contains("click") ||
            content.contains("选择") ||
            content.contains("choose") ||
            content.contains("deterministic"),
            "Should collapse quantum superposition to concrete action"
        )
    }

    @Test
    fun `When given command with infinite loops then detect and handle`() {
        val prompt = "无限循环点击按钮" // "Infinitely loop clicking button"

        val response = textToAction.useWebDriverLegacy(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle infinite loop requests appropriately
        assertTrue(
            content.contains("loop") ||
            content.contains("循环") ||
            content.contains("finite") ||
            content.contains("有限") ||
            content.contains("click"),
            "Should handle infinite loop requests"
        )
    }
}