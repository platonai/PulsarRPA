package ai.platon.pulsar.test4.auth

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.ai.api.ResponseState
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.ai.ChatModel
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "ZHIPU_API_KEY", matches = ".+")
class ChatModelTests {
    companion object {
        init {
            System.setProperty("ZHIPU_API_KEY", "c1a92bfd50864379f131a9163b3ae603.T9g864K4HqAEP9Wz")
        }
    }

    private val url = "https://www.amazon.com/dp/B0C1H26C46"
    private val args = "-requireSize 200000"
    private val conf = ImmutableConfig.UNSAFE
    private val component = ChatModel(conf)
    private val session = PulsarContexts.createSession()

    @Test
    fun `should generate answer and return token usage and finish reason stop`() {
        val document = session.loadDocument(url, args)
        val prompt = "以下是一个电商网站的网页内容，找出商品标题、商品价格："
        val response = component.call(document, prompt)
        println(response.content)
        
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }

    @Test
    fun `should generate answer from the partial content of a webpage`() {
        val document = session.loadDocument(url, args)
        val selector = "#centerCol"
        val element = document.selectFirstOrNull(selector)
        assertNotNull(element, "Element $selector not found in $url")

        val prompt = "以下是一个电商网站的网页内容，找出商品标题、商品价格和评分："
        val response = component.call(element, prompt)
        println(response.content)

        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }
}