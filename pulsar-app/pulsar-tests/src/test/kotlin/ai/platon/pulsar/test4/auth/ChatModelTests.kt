package ai.platon.pulsar.test4.auth

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.external.ModelFactory
import ai.platon.pulsar.external.ResponseState
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "LARGE_LANGUAGE_MODEL", matches = ".+")
class ChatModelTests {

    private val url = "https://www.amazon.com/dp/B0C1H26C46"
    private val args = "-requireSize 200000"
    private val productHtml = ResourceLoader.readString("pages/amazon/B0C1H26C46.original.htm")
    private val productText = ResourceLoader.readString("prompts/product.txt")
    private val conf = ImmutableConfig(loadDefaults = true)
    private val llm = conf["LARGE_LANGUAGE_MODEL"]
    private val apiKey = conf["LLM_API_KEY"]
    private val model = if (llm != null && apiKey != null) ModelFactory.getOrCreate(llm, apiKey) else null
    
    @Test
    fun `should generate answer and return token usage and finish reason stop`() {
        println(conf)
        println(conf.size())
        println(conf.get("LARGE_LANGUAGE_MODEL"))
        conf.unbox().forEach { (k, v) -> println("$k: $v")}
        
        if (model == null) {
            println("Skip test because the model is not available")
            return
        }

//        val document = session.loadDocument(url, args)
        val document = Documents.parse(productHtml, url)
        val prompt = "以下是一个电商网站的网页内容，找出商品标题、商品价格："
        val response = model.call(document, prompt)
        println(response.content)
        
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }
    
    @Test
    fun `should generate answer from the partial content of a webpage`() {
        if (model == null) return
        
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
        val response = model.call(text, prompt)
        println(response.content)
        
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }
}
