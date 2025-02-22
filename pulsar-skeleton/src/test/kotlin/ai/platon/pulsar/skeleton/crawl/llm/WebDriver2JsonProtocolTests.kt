package ai.platon.pulsar.skeleton.crawl.llm

import org.junit.jupiter.api.Tag
import kotlin.test.Test

@Tag("ExternalServiceTest")
class WebDriver2JsonProtocolTests: TTATestBase() {

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
