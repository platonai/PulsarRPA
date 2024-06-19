package ai.platon.pulsar.test4.auth

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.ai.api.ResponseState
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.ai.ChatModel
import ai.platon.pulsar.dom.Documents
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test
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
    private val productHtml = ResourceLoader.readString("pages/amazon/B0C1H26C46.original.htm")
    private val productText = ResourceLoader.readString("prompts/product.txt")
    private val conf = ImmutableConfig.UNSAFE
    private val component = ChatModel(conf)
    private val session = PulsarContexts.createSession()

    @Test
    fun `should generate answer and return token usage and finish reason stop`() {
//        val document = session.loadDocument(url, args)
        val document = Documents.parse(productHtml, url)
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
        val text = productText
        val prompt = """
以下我将提供一个典型电商网站的商品页面内容，找出商品标题、商品价格和评分，并且以以下格式输出：

从提供的网页内容中，以下是商品的相关信息：

- **商品标题**:
  （这里是商品标题）

- **商品价格**:
   （这里是商品价格）

- **商品评分**:
    （这里是商品评分）
        """.trimIndent()
        val response = component.call(text, prompt)
        println(response.content)

        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }
}
