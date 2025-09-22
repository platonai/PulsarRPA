package ai.platon.pulsar.tta

import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue

/**
 * Simplified tests demonstrating the improvements made to TTA testing
 * based on README-AI.md guidelines
 */
@Tag("ExternalServiceTest")
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TextToActionImprovementsTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
    }

    // Advanced Element Types Tests
    @Test
    fun `When ask to fill password field then generate correct password input action`() {
        val prompt = "在密码输入框中输入 'securePass123'"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "fill", "password", "securePass123")
    }

    @Test
    fun `When ask to search for products then generate correct search input action`() {
        val prompt = "在产品搜索框中输入 'laptop'"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "fill", "search", "laptop")
    }

    @Test
    fun `When ask to select date then generate correct date picker action`() {
        val prompt = "选择日期 2024-12-25"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "fill", "date", "2024-12-25")
    }

    @Test
    fun `When ask to select radio button then generate correct radio selection action`() {
        val prompt = "选择 Premium 订阅选项"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "radio", "premium", "check")
    }

    // Dynamic Content Tests
    @Test
    fun `When ask to wait for async content then generate appropriate wait action`() {
        val prompt = "等待异步内容加载完成"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "wait", "async", "load")
    }

    @Test
    fun `When ask to handle dynamic list then generate list management actions`() {
        val prompt = "在动态列表中添加新项目 'Test Item'"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        validateWebDriverResponse(response, "add", "list", "item")
    }

    // Ambiguity Resolution Tests
    @Test
    fun `When multiple buttons have same text then use context to disambiguate`() {
        val prompt = "点击用户管理部分的保存按钮"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("user") || content.contains("用户") ||
            content.contains("management") || content.contains("管理")
        )
        assertTrue(content.contains("save") || content.contains("保存"))
    }

    @Test
    fun `When position matters then use spatial context`() {
        val prompt = "点击右上角的菜单按钮"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("top") || content.contains("right") ||
            content.contains("上") || content.contains("右")
        )
        assertTrue(content.contains("menu") || content.contains("菜单"))
    }

    @Test
    fun `When billing and shipping forms exist then use form context`() {
        val prompt = "填写收货地址的城市字段"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("shipping") || content.contains("收货") ||
            content.contains("delivery")
        )
        assertTrue(content.contains("city") || content.contains("城市"))
    }

    // Boundary Condition Tests
    @Test
    fun `When no matching element exists then generate empty fallback function`() {
        val prompt = "点击不存在的按钮"

        val actionDescription = textToAction.generateWebDriverActions(prompt)

        assertTrue(actionDescription.functionCalls.isEmpty())
        assertTrue(
            actionDescription.modelResponse.content.contains("not found") ||
            actionDescription.modelResponse.content.contains("empty") ||
            actionDescription.modelResponse.content.contains("No WebDriver")
        )
    }

    @Test
    fun `When input is empty then provide helpful error message`() {
        val prompt = ""

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("empty") || content.contains("provide") ||
            content.contains("instruction") || content.contains("input")
        )
    }

    @Test
    fun `When timeout is very short then handle timeout appropriately`() {
        val prompt = "设置1毫秒超时等待元素"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("timeout") || content.contains("1") ||
            content.contains("millisecond") || content.contains("毫秒")
        )
    }

    @Test
    fun `When conflicting instructions are provided then handle contradiction`() {
        val prompt = "同时点击保存按钮和取消按钮（矛盾指令）"

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(
            content.contains("conflict") || content.contains("contradiction") ||
            content.contains("矛盾") || content.contains("impossible") ||
            content.contains("cannot")
        )
    }

    // Complex Multi-step Tests
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
    fun `When test complex dynamic workflow then generate comprehensive dynamic actions`() {
        val prompt = """
            执行复杂的动态工作流：
            1. 加载初始内容
            2. 等待异步数据加载（最多5秒）
            3. 验证数据完整性
            4. 处理用户交互
            5. 清理临时数据
            6. 验证最终状态
        """.trimIndent()

        val response = textToAction.chatAboutWebDriver(prompt)
        lastResponse = response
        println(response.content)

        val content = response.content.lowercase()
        assertTrue(content.contains("load") || content.contains("initial"))
        assertTrue(content.contains("wait") || content.contains("async"))
        assertTrue(content.contains("verify") || content.contains("integrity"))
        assertTrue(content.contains("interact") || content.contains("user"))
        assertTrue(content.contains("clean") || content.contains("temp"))
        assertTrue(content.contains("final") || content.contains("state"))
    }

    // Test data-testid helper method
    @Test
    fun `When use data-testid helper then generate correct selector`() {
        val selector = byTestId("tta-submit-button")
        assertTrue(selector == "[data-testid=\"tta-submit-button\"]")
    }

    // Test validation methods
    @Test
    fun `When validate WebDriver response then check expected actions`() {
        val response = textToAction.chatAboutWebDriver("点击按钮")

        // Should not throw exception
        validateWebDriverResponse(response, "click")
        validateWebDriverResponse(response, "button")
    }

    @Test
    fun `When validate Pulsar session response then check expected actions`() {
        val response = textToAction.chatAboutPulsarSession("加载页面")

        // Should not throw exception
        validatePulsarSessionResponse(response, "session", "load")
    }

    // Test element selection strategy
    @Test
    fun `When test element selection strategy then validate strategy detection`() {
        val response = textToAction.chatAboutWebDriver("使用data-testid选择元素")
        assertTrue(response.content.contains("data-testid"))
    }
}
