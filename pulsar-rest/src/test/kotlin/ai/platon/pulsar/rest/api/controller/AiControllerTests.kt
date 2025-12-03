package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ChatModelTestUtils
import ai.platon.pulsar.rest.api.TestUtils
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.junit.jupiter.api.*
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test [AiController]
 * */
@Tag("TimeConsumingTest")
@Tag("ExternalServiceTest")
class AiControllerTests : IntegrationTestBase() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initConfig() {
            ChatModelTestUtils.initConfig()
        }

        @JvmStatic
        @AfterAll
        fun resetConfig() {
            ChatModelTestUtils.resetConfig()
        }
    }

    val indexUrl = "https://www.amazon.com/b?node=1292115011"
    val productUrl = "https://www.amazon.com/dp/B08PP5MSVB"

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
     * Test [AiController.conversationsBackward]
     * */
    @Test
    fun `When chat then LLM responses`() {
        val prompt = "生命、宇宙以及任何事情的终极答案是什么？"
        val response = restTemplate.getForObject("$baseUri/api/ai/chat?prompt={prompt}", String::class.java, prompt)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.chatAboutPageBackward]
     * */
    @Test
    fun `When chat about a page then result is not empty`() {
        val request = PromptRequest(indexUrl, "Tell me about something about this page")
        val response = restTemplate.postForObject("$baseUri/api/ai/chat-about", request, String::class.java)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.executeExtractionBackward]
     * */
    @Test
    fun `Test extracting fields from a page`() {
        val request = PromptRequest(productUrl, "title, price, brand")
        val response = restTemplate.postForObject("$baseUri/api/ai/extract", request, String::class.java)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test [AiController.extractFieldsFromPage]
     * with [ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver.instruct]
     * */
    @Test
    fun `Test extracting fields from a page with instructions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(productUrl, "title, price, brand", actions = actions)
        val response = restTemplate.postForObject("$baseUri/api/ai/extract", request, String::class.java)

        println(response)

        assertTrue { response.isNotBlank() }
    }
}
