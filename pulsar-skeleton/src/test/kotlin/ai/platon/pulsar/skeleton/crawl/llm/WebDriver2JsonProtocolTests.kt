package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("ExternalServiceTest")
class WebDriver2JsonProtocolTests {
    
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
    
    @Test
    fun `When ask to translate WebDriver interface to Json protocol then generate json protocol`() {
        val userMessage = """
你的任务是将WebDriver的操作转换为Json协议，以便在浏览器中执行。
        """.trimIndent()
        
        val response = session.chat(userMessage, systemMessage)
        lastResponse = response
        println(response.content)
    }
}
