package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test [AiController]
 * */
class AiControllerTests : IntegrationTestBase() {
    val indexUrl = "https://www.amazon.com/b?node=1292115011"
    val productUrl = "https://www.amazon.com/dp/B0C1H26C46"

    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(ChatModelFactory.hasModel(unmodifiedConfig))
    }

    @BeforeEach
    fun `Ensure resources are prepared`() {
        TestUtils.ensurePage(indexUrl)
        TestUtils.ensurePage(productUrl)
    }

    /**
     * Test [AiController.chat]
     * */
    @Test
    fun `When chat then LLM responses`() {
        val prompt = "生命、宇宙以及任何事情的终极答案是什么？"
        val response = restTemplate.getForObject("$baseUri/ai/chat?prompt={prompt}", String::class.java, prompt)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.chatAboutPage]
     * */
    @Test
    fun `When chat about a page then result is not empty`() {
        val request = PromptRequest(indexUrl, "Tell me about something about this page")
        val response = restTemplate.postForObject("$baseUri/ai/chat-about", request, String::class.java)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.extractFieldsFromPage]
     * */
    @Test
    fun `Test extracting fields from a page`() {
        val request = PromptRequest(productUrl, "title, price, brand")
        val response = restTemplate.postForObject("$baseUri/ai/extract", request, String::class.java)

        println(response)

        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.extractFieldsFromPage]
     * with [ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver.instruct]
     * */
    @Test
    fun `Test extracting fields from a page with interactions`() {
        val instruct = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent()
        val request = PromptRequest(productUrl, "title, price, brand", instructOnDocumentReady = instruct)
        val response = restTemplate.postForObject("$baseUri/ai/extract", request, String::class.java)

        println(response)

        assertTrue { response.isNotBlank() }
    }
}
