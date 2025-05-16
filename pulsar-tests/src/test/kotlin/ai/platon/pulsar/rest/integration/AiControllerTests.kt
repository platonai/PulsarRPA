package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

@Ignore("Websites might be fall, run these integration tests manually")
@Tag("TimeConsumingTest")
@Tag("ExternalServiceTest")
class AiControllerTests : IntegrationTestBase() {

    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(ChatModelFactory.hasModel(unmodifiedConfig))
    }

    /**
     * Test controller [ai.platon.pulsar.rest.api.controller.AiController.chat]
     * */
    fun testChat(prompt: String, url: String) {
        println("Chat about webpage $url")
        val request = PromptRequest(url, prompt)
        val response = restTemplate.postForObject("$baseUri/ai/chat", request, String::class.java)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    /**
     * Test controller [ai.platon.pulsar.rest.api.controller.AiController.extract]
     * */
    fun testExtract(prompt: String, url: String) {
        println("Extracting fields from $url")
        val request = PromptRequest(url, prompt)
        val response = restTemplate.postForObject("$baseUri/ai/extract", request, String::class.java)
        println(response)
        assertTrue { response.isNotBlank() }
    }

    fun testChat(url: String) {
        testChat("Tell me something about the page", url)
    }

    fun testExtract(url: String) {
        testExtract("title, price, images", url)
    }

    @Test
    fun testChatAboutPages() {
        testChat("https://www.amazon.com")
        testChat("https://www.amazon.com/dp/B0C1H26C46")

        testChat("https://www.jd.com/")
        testChat("https://www.ebay.com/")
    }

    @Test
    fun testExtractPages() {
        testExtract("https://www.amazon.com/dp/B0C1H26C46")
    }
}
