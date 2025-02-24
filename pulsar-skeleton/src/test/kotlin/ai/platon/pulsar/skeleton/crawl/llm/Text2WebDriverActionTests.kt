package ai.platon.pulsar.skeleton.crawl.llm

import org.junit.jupiter.api.Tag
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("ExternalServiceTest")
class Text2WebDriverActionTests: TTATestBase() {

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

        val content = response.content
        assertTrue { content.contains(".navigateTo") || content.contains(".open") }
        assertTrue { content.contains(".scrollToMiddle") }
        assertTrue { content.contains(".scrollToTop") }
        assertTrue { content.contains(".captureScreenshot") }
    }
}
