package ai.platon.pulsar.rest.api.serivces

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.service.PromptService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class PromptServiceTest {

    private val url = "https://www.amazon.com/b?node=1292115011"

    @Autowired
    private lateinit var service: PromptService

    /**
     * Execute a normal sql
     * */
    @Test
    fun `When chat about a page then the result is not empty`() {
        val request = PromptRequest(url, "Tell me something about the page")

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }
}
