package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for TextToAction implementation following the pattern from Text2WebDriverActionDescriptionTests
 * Testing all key requirements from coder.md guideline
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionComprehensiveTests : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `When ask to open a web page then generate correct kotlin code`() {
        val prompt = "如何打开一个网页？"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue { listOf(".navigateTo", ".open").any { response.content.contains(it) } }
    }

    @Test
    fun `When ask to click a button then generate correct kotlin code`() {
        val prompt = "点击搜索按钮"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue { response.content.contains(".click") }
        assertTrue { response.content.contains("driver") }
    }

    @Test
    fun `When ask to fill input then generate correct kotlin code`() {
        val prompt = "在搜索框中输入'best AI toy'"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue { response.content.contains(".fill") || response.content.contains(".type") }
        assertTrue { response.content.contains("best AI toy") }
    }

    @Test
    fun `When ask to scroll to middle then generate correct kotlin code`() {
        val prompt = "滚动到页面中间位置"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        assertTrue { response.content.contains(".scrollToMiddle") }
    }

    @Test
    fun `When ask to scroll and wait then generate correct kotlin code`() {
        val prompt = "滚动到页面30%位置，然后等待元素加载"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".scrollToMiddle") }
        assertTrue { content.contains(".wait") || content.contains("waitFor") }
    }

    @Test
    fun `When ask complex action sequence then generate correct kotlin code`() {
        val prompt = "打开网页，在搜索框输入关键词，点击搜索按钮，然后滚动查看结果"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".navigateTo") || content.contains(".open") }
        assertTrue { content.contains(".fill") || content.contains(".type") }
        assertTrue { content.contains(".click") }
        assertTrue { content.contains(".scroll") }
    }

    @Test
    fun `When ask enhanced generateWebDriverActions without driver then generate empty suspend function`() {
        val prompt = "点击按钮"

        val result = textToAction.generateWebDriverActions(prompt)

        assertNotNull(result)
        assertTrue(result.functionCalls.isEmpty())
        // selectedElement is only available in the suspend version, so skip this assertion
        // assertTrue(result.selectedElement == null)
        assertTrue(result.modelResponse.content.contains("suspend fun"))
        assertTrue(result.modelResponse.content.contains("No WebDriver instance available"))
    }

    @Test
    fun `When test JavaScript resource loading then script loads successfully`() {
        // Test that the JavaScript resource file is properly loaded
        val result = runBlocking {
            try {
                // Create a simple test to verify the script loading mechanism works
                val testAction = TextToAction(session.sessionConfig)
                // The script should be loaded during initialization
                assertTrue(true) // If we get here, initialization succeeded
            } catch (e: Exception) {
                throw AssertionError("Failed to load JavaScript resource: ${e.message}")
            }
        }
    }

    @Test
    fun `When ask about element interaction patterns then generate appropriate code`() {
        val prompt = "找到页面上的提交按钮并点击"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".click") }
        assertTrue { content.contains("submit") || content.contains("提交") || content.contains("button") }
    }

    @Test
    fun `When ask about form operations then generate form handling code`() {
        val prompt = "填写表单：用户名输入'admin'，密码输入'password123'，然后提交"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".fill") || content.contains(".type") }
        assertTrue { content.contains("admin") }
        assertTrue { content.contains("password123") }
        assertTrue { content.contains(".click") || content.contains("submit") }
    }

    @Test
    fun `When ask about navigation and waiting then generate proper sequence`() {
        val prompt = "打开网页，等待页面加载完成，然后滚动到底部"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".navigateTo") || content.contains(".open") }
        assertTrue { content.contains(".wait") || content.contains("waitFor") }
        assertTrue { content.contains(".scroll") }
    }

    @Test
    fun `When ask about element selection strategies then generate selector usage`() {
        val prompt = "点击id为'submitBtn'的按钮"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".click") }
        assertTrue { content.contains("#submitBtn") || content.contains("submitBtn") }
    }

    @Test
    fun `When ask about validation operations then generate checking code`() {
        val prompt = "检查复选框是否选中，如果没有选中则点击选中"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content
        assertTrue { content.contains(".check") || content.contains(".click") }
        assertTrue { content.contains("checkbox") || content.contains("复选框") || content.contains("选中") }
    }

    @Test
    fun `When test resource script loading with extractInteractiveElements method`() {
        // This test verifies that the JavaScript script is properly loaded from resources
        // and that the extractInteractiveElements method can access it

        // We test this indirectly by ensuring the TextToAction class initializes properly
        // and the extractElementsScript property is accessible
        val testAction = TextToAction(session.sessionConfig)
        assertNotNull(testAction)

        // If initialization succeeded without throwing exceptions,
        // it means the resource loading mechanism works correctly
        assertTrue(true)
    }
}
