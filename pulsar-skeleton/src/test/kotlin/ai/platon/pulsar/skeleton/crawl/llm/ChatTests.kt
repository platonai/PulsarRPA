package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.context.support.ContextDefaults
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("ExternalServiceTest")
class ChatTests {
    
    companion object {
        private val url = "https://www.amazon.com/dp/B0C1H26C46"
        private val args = "-requireSize 200000"
        private val productHtml = ResourceLoader.readString("pages/amazon/B0C1H26C46.original.htm")
        private val productText = ResourceLoader.readString("prompts/product.txt")
        
        private val session = PulsarContexts.createSession()
        private var lastResponse: ModelResponse? = null
        
        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            TestHelper.checkConfiguration(session)
        }
    }
    
    @AfterTest
    fun checkTokenUsage() {
        lastResponse?.let { TestHelper.checkTokenUsage(it) }
    }
    
    /**
     * Test configuration
     * */
    @Test
    fun `When check configuration then it works`() {
        val conf = ContextDefaults().unmodifiedConfig
        
        val model = conf.get("llm.name")
        val apiKey = conf.get("llm.apiKey")
        
        println(model)
        println(apiKey)
        
        val model2 = session.unmodifiedConfig.get("llm.name")
        val apiKey2 = session.unmodifiedConfig.get("llm.apiKey")
        
        assertEquals(model, model2)
        assertEquals(apiKey, apiKey2)
    }
    
    @Test
    fun `When chat to LLM then it responds`() {
        val prompt = "以下是一个电商网站的网页内容，找出商品标题和商品价格：$productText"
        val response = session.chat(prompt)
        lastResponse = response
        println(response.content)
        assertTrue { response.content.isNotEmpty() }
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
    }
    
    @Test
    fun `Should generate answer and return token usage and finish reason stop`() {
        val document = Documents.parse(productHtml, url)
        val prompt = "以下是一个电商网站的网页内容，找出商品标题、商品价格："
        val response = session.chat(document, prompt)
        lastResponse = response
        println(response.content)
        
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
    }
}
