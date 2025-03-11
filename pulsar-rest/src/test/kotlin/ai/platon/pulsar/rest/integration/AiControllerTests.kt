package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

class AiControllerTests : IntegrationTestBase() {

    val urls = mapOf(
        "amazon" to "https://www.amazon.com/b?node=1292115011",
    )

    val url get() = urls["amazon"]!!

    @BeforeEach
    fun setUp() {
        Assumptions.assumeTrue(ChatModelFactory.hasModel(unmodifiedConfig))
    }

    @Ignore("Connection timeout, need a fix")
    @Test
    fun `When chat about a page then the result is not empty`() {
        val request = PromptRequest(url, "Tell me about something about this page")
        val response = restTemplate.postForObject("$baseUri/ai/chat", request, String::class.java)
        assertTrue { response.isNotBlank() }
    }
}
