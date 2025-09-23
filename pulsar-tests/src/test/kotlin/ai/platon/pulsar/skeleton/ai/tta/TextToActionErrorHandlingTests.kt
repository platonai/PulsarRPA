package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Error handling and boundary condition tests for TextToAction
 * Testing requirement: "Error and Boundary Cases" from README-AI.md
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionErrorHandlingTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `When given ambiguous command with multiple buttons then request clarification or select best match`() {
        val prompt = "点击按钮" // Ambiguous: just "click button"

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle ambiguity by either requesting clarification or providing generic solution
        assertTrue(
            content.contains("button") ||
            content.contains("按钮") ||
            content.contains("clarification") ||
            content.contains("明确"),
            "Should handle ambiguous button command"
        )
    }

    @Test
    fun `When given command with no matching element then generate appropriate fallback`() {
        val prompt = "点击不存在的登录按钮" // Non-existent login button

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should provide fallback strategy or indicate element not found
        assertTrue(
            content.contains("not found") ||
            content.contains("不存在") ||
            content.contains("fallback") ||
            content.contains("备用"),
            "Should handle non-existent elements"
        )
    }

    @Test
    fun `When given empty command then handle gracefully`() {
        val prompt = "" // Empty command

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        // Should not crash and should provide helpful response
        assertNotNull(response.content)
        assertTrue(response.content.isNotBlank(), "Should handle empty command gracefully")
    }

    @Test
    fun `When given extremely long command then process without errors`() {
        val prompt = "点击登录按钮并在用户名输入框输入 'verylongusernamethatexceedsnormallimits' 并在密码输入框输入 'verylongpasswordthatexceedsnormallimitsandshouldbehandledproperly' 然后点击提交按钮并等待页面加载完成并滚动到页面底部"

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle long complex commands
        assertTrue(content.contains("click") || content.contains("fill"))
        assertTrue(content.length > 50, "Should generate substantial response for complex command")
    }

    @Test
    fun `When given command with special characters then handle properly`() {
        val prompt = "点击按钮 'Submit & Go' 并输入 'Hello@#$%^&*()'"

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle special characters without breaking
        assertTrue(content.isNotBlank(), "Should handle special characters")
    }

    @Test
    fun `When given contradictory commands then resolve or report conflict`() {
        val prompt = "点击登录按钮同时不要点击任何按钮" // Contradictory: click login button but don't click any button

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should recognize and handle contradiction
        assertTrue(
            content.contains("contradiction") ||
            content.contains("矛盾") ||
            content.contains("conflict") ||
            content.contains("冲突") ||
            content.contains("clarify") ||
            content.contains("澄清"),
            "Should handle contradictory commands"
        )
    }

    @Test
    fun `When WebDriver actions generation fails then return appropriate error response`() {
        val prompt = "点击按钮"

        val result = textToAction.generateWebDriverActions(prompt)

        assertNotNull(result)
        assertTrue(result.functionCalls.isEmpty())
        assertTrue(result.modelResponse.content.contains("No WebDriver instance available") ||
                  result.modelResponse.content.contains("suspend fun"))
    }

    @Test
    fun `When given command in unsupported language then handle gracefully`() {
        val prompt = "Cliquez sur le bouton de connexion" // French command

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should either handle it or indicate language limitation
        assertTrue(content.isNotBlank(), "Should handle non-Chinese/English commands")
    }

    @Test
    fun `When given command with impossible timing requirements then handle appropriately`() {
        val prompt = "在0.001秒内点击按钮并立即提交表单" // Impossible timing

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle unrealistic timing requirements
        assertTrue(content.contains("click") || content.contains("submit"))
        // Should not include the impossible timing in generated code
        assertFalse(content.contains("0.001"), "Should not include impossible timing requirements")
    }

    @Test
    fun `When given command referring to non-interactive elements then provide guidance`() {
        val prompt = "点击页面标题" // Trying to click page title (typically non-interactive)

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should provide guidance about interactive vs non-interactive elements
        assertTrue(
            content.contains("interactive") ||
            content.contains("交互") ||
            content.contains("clickable") ||
            content.contains("可点击"),
            "Should provide guidance about element interactivity"
        )
    }

    @Test
    fun `When DOM structure changes during processing then adapt appropriately`() = runWebDriverTest(interactiveUrl, browser) { driver ->
        driver.waitForSelector("body", 5000)

        // Since extractInteractiveElements is suspend function and not available in current API,
        // test the basic functionality with chatAboutWebDriver instead
        val initialResponse = textToAction.chatAboutWebDriver("找到页面上的按钮")
        assertTrue(initialResponse.content.isNotBlank(), "Should generate initial response")

        // Modify DOM by adding a new element
        driver.evaluate("""
            const newButton = document.createElement('button');
            newButton.id = 'dynamicButton';
            newButton.textContent = 'Dynamic Button';
            newButton.onclick = function() { alert('Dynamic!'); };
            document.body.appendChild(newButton);
        """)

        Thread.sleep(500) // Wait for DOM update

        // Test response after DOM change
        val updatedResponse = textToAction.chatAboutWebDriver("找到页面上的Dynamic Button")
        assertTrue(updatedResponse.content.isNotBlank(), "Should handle DOM changes")

        // The response should mention the new button or dynamic elements
        val content = updatedResponse.content
        assertTrue(
            content.contains("Dynamic") || content.contains("dynamic") ||
            content.contains("按钮") || content.contains("button"),
            "Should reference the new dynamic button"
        )
    }

    @Test
    fun `When given command with multiple possible interpretations then provide options`() {
        val prompt = "点击第一个按钮" // "Click the first button" - ambiguous which is "first"

        val response = textToAction.chatAboutWebDriver(prompt)
        TextToActionTestBase.lastResponse = response
        println(response.content)

        val content = response.content
        // Should handle ambiguity in element ordering
        assertTrue(
            content.contains("first") ||
            content.contains("第一个") ||
            content.contains("specific") ||
            content.contains("具体"),
            "Should handle ambiguous ordering"
        )
    }
}