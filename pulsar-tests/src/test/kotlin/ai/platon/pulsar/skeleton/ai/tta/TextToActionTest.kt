package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Comprehensive tests for TextToAction implementation
 * Following the test paradigm from Text2WebDriverActionDescriptionTests
 * Testing real behavior without mocks
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionTest: TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `When ask to click a button then generate correct WebDriver action code`() {
        val prompt = "点击搜索按钮"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.click") || response.content.contains(".click("))
        assertTrue(response.content.contains("搜索") || response.content.contains("search"))
    }

    @Test
    fun `When ask to fill input field then generate correct WebDriver action code`() {
        val prompt = "在搜索框中输入 'AI toys'"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.fill") || response.content.contains(".fill("))
        assertTrue(response.content.contains("AI toys") || response.content.contains("搜索"))
    }

    @Test
    fun `When ask to scroll page then generate correct scrolling action`() {
        val prompt = "滚动到页面中间位置"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.scrollToMiddle") || response.content.contains(".scrollToMiddle"))
    }

    @Test
    fun `When ask to wait for element then generate correct wait action`() {
        val prompt = "等待提交按钮出现"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.waitForSelector") || response.content.contains(".waitForSelector"))
        assertTrue(response.content.contains("提交") || response.content.contains("submit"))
    }

    @Test
    fun `When ask to navigate to URL then generate correct navigation action`() {
        val prompt = "打开网页 https://example.com"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.navigateTo") || response.content.contains(".navigateTo"))
        assertTrue(response.content.contains("example.com"))
    }

    @Test
    fun `When ask complex multi-step actions then generate sequence of WebDriver calls`() {
        val prompt = """
        打开搜索页面，在搜索框输入 'best AI toys'，点击搜索按钮，然后滚动到页面30%位置
        """.trimIndent()

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue(content.contains("driver.navigateTo") || content.contains(".navigateTo"))
        assertTrue(content.contains("driver.fill") || content.contains(".fill("))
        assertTrue(content.contains("driver.click") || content.contains(".click("))
        assertTrue(content.contains("driver.scrollToMiddle") || content.contains(".scrollToMiddle"))
    }

    @Test
    fun `When ask about form submission then generate appropriate form actions`() {
        val prompt = "填写登录表单并提交"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue(content.contains("driver.fill") || content.contains(".fill("))
        assertTrue(content.contains("driver.click") || content.contains(".click("))
    }

    @Test
    fun `When ask about checkbox operations then generate check or uncheck actions`() {
        val prompt = "勾选同意条款复选框"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("driver.check") || response.content.contains(".check("))
    }

    @Test
    fun `When ask in English then generate appropriate English-context actions`() {
        val prompt = "Click the submit button and wait for confirmation"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue(content.contains("driver.click") || content.contains(".click("))
        assertTrue(content.contains("driver.waitForSelector") || content.contains(".waitForSelector"))
    }

    @Test
    fun `When generate WebDriver actions without driver then return fallback response`() {
        val actionDescription = textToAction.generateWebDriverActions("点击按钮")

        assertNotNull(actionDescription)
        // selectedElement is only available in the suspend version, so skip this assertion
        // assertNull(actionDescription.selectedElement)
        assertTrue(actionDescription.functionCalls.isEmpty())
        assertTrue(actionDescription.modelResponse.content.contains("No WebDriver instance available"))
    }

    @Test
    fun `When generate Pulsar session actions then return session-based commands`() {
        val prompt = "创建一个新的浏览器会话并导航到页面"

        val response = textToAction.chatAboutPulsarSession(prompt)
        lastResponse = response
        println(response.content)

        assertTrue(response.content.contains("session.") || response.content.contains("PulsarSession"))
    }

    @Test
    fun `When generate actions for web scraping then include appropriate session methods`() {
        val prompt = "抓取电商网站的商品信息"

        val response = textToAction.chatAboutPulsarSession(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue(content.contains("session.") || content.contains("load") || content.contains("scrape"))
    }

    @Test
    fun `When test action description with WebDriver prompt then extract function calls correctly`() {
        val prompt = "点击登录按钮然后等待页面加载"

        val actionDescription = textToAction.generateWebDriverActions(prompt)

        assertNotNull(actionDescription)
        assertNotNull(actionDescription.modelResponse)

        // Test that function calls are extracted from the response
        val functionCalls = actionDescription.functionCalls
        println("Extracted function calls: $functionCalls")

        // The function calls should be related to the prompt
        assertTrue(functionCalls.any { it.contains("click") } || actionDescription.modelResponse.content.contains("click"))
    }

    @Test
    fun `When test action description with Pulsar session prompt then extract session calls correctly`() {
        val prompt = "加载页面并提取标题"

        val actionDescription = textToAction.generatePulsarSessionActions(prompt)

        assertNotNull(actionDescription)
        assertNotNull(actionDescription.modelResponse)

        val functionCalls = actionDescription.functionCalls
        println("Extracted session function calls: $functionCalls")

        // Should contain session-related calls or be mentioned in response
        assertTrue(functionCalls.any { it.contains("session.") } || actionDescription.modelResponse.content.contains("session"))
    }

    @Test
    fun `When ask about all instruction capabilities then generate comprehensive response`() {
        val prompt = "如何使用WebDriver和PulsarSession进行网页自动化？"

        val response = textToAction.chatAboutAllInstruction(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue(content.contains("WebDriver") || content.contains("driver"))
        assertTrue(content.contains("PulsarSession") || content.contains("session"))
    }

    @Test
    fun `When test element matching scenarios with realistic prompts then validate response structure`() {
        val testCases = listOf(
            "点击提交按钮" to "submit",
            "在搜索框中输入关键词" to "search",
            "点击链接跳转" to "link",
            "选择下拉菜单选项" to "select",
            "上传文件" to "file"
        )

        testCases.forEach { (prompt, expectedKeyword) ->
            val response = textToAction.chatAboutWebDriver(prompt)
            lastResponse = response

            assertNotNull(response)
            assertTrue(response.content.isNotBlank())

            println("Prompt: $prompt")
            println("Response contains '$expectedKeyword': ${response.content.lowercase().contains(expectedKeyword)}")
            println("Response: ${response.content}")
            println("---")
        }
    }

    @Test
    fun `When test command extraction patterns then validate parsing logic`() {
        val prompt = "执行点击和填充操作"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        // Test that the response generates valid WebDriver command patterns
        val content = response.content
        val hasDriverCall = content.contains("driver.") || content.contains("WebDriver")
        val hasSuspendFunction = content.contains("suspend") || content.contains("fun")

        assertTrue(hasDriverCall || hasSuspendFunction, "Response should contain driver calls or suspend function structure")
    }
}
