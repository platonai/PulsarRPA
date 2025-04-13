package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertTrue

class AiControllerTests : IntegrationTestBase() {
    val indexUrl = "https://www.amazon.com/b?node=1292115011"
    val productUrl = "https://www.amazon.com/dp/B0C1H26C46"

    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(ChatModelFactory.hasModel(unmodifiedConfig))
    }

    /**
     * Test [AiController.chat]
     * */
    @Test
    fun `When chat about a page then the result is not empty`() {
        val request = PromptRequest(indexUrl, "Tell me about something about this page")
        val response = restTemplate.postForObject("$baseUri/ai/chat", request, String::class.java)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.extract]
     * */
    @Test
    fun `When extract fields from a page then the result is not empty`() {
        val request = PromptRequest(productUrl, "title, price, brand")
        val response = restTemplate.postForObject("$baseUri/ai/extract", request, String::class.java)

        println(response)

        assertTrue { response.isNotBlank() }
    }
}
