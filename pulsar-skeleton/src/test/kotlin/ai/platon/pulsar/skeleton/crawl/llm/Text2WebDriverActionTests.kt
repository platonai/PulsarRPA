package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class Text2WebDriverActionTests {

    companion object {
        private val projectRoot = Paths.get(System.getProperty("user.dir"))
        private val sourceFile =
            projectRoot.resolve("src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt")
        private val webDriverSourceCode = Files.readString(sourceFile)

        private val systemMessage = """
以下是操作网页的 API 接口及其注释，你可以使用这些接口来操作网页，比如打开网页、点击按钮、输入文本等等。

$webDriverSourceCode
        """.trimIndent()

        private val session = PulsarContexts.createSession()
        private var lastResponse: ModelResponse? = null

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            TestHelper.checkConfiguration(session)
            assertTrue("WebDriver.kt should be found") { webDriverSourceCode.isNotBlank() }
        }
    }
    
    @BeforeTest
    fun checkResource() {
    }
    
    @AfterTest
    fun checkTokenUsage() {
        lastResponse?.let { TestHelper.checkTokenUsage(it) }
    }
    
    @Test
    fun `When ask to open a web page then generate correct kotlin code`() {
        val userMessage = """
如何打开一个网页？
        """.trimIndent()
        
        val response = session.chat(userMessage, systemMessage)
        lastResponse = response
        println(response.content)
        
        assertTrue { response.content.contains(".navigateTo") }
    }
    
    @Test
    fun `When ask to open a web page and scroll then generate correct kotlin code`() {
        val userMessage = """
生成一个挂起函数，实现如下功能：打开一个网页，然后滚动到页面30%位置。仅返回该挂起函数，不需要其他代码，也不需要注释和解释。
        """.trimIndent()
        
        val response = session.chat(userMessage, systemMessage)
        lastResponse = response
        println(response.content)
        
        assertTrue { response.content.contains(".navigateTo") }
        assertTrue { response.content.contains(".scrollToMiddle") }
    }
    
    @Test
    fun `When ask to open a web page, scroll and take snapshot then generate correct kotlin code`() {
        val userMessage = """
生成一个挂起函数，实现如下功能：打开一个网页，然后滚动到页面30%位置，然后滚动到页面顶部，最后截取页面快照。仅返回该挂起函数，不需要其他代码，也不需要注释和解释。
        """.trimIndent()
        
        val response = session.chat(userMessage, systemMessage)
        lastResponse = response
        println(response.content)
        
        assertTrue { response.content.contains(".navigateTo") }
        assertTrue { response.content.contains(".scrollToMiddle") }
        assertTrue { response.content.contains(".scrollToTop") }
        assertTrue { response.content.contains(".captureScreenshot") }
    }
}
