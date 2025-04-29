package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_LIST_URL
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class PromptServiceTest {

    @Autowired
    private lateinit var service: PromptService

    /**
     * Execute a normal sql
     * */
    @Test
    fun `When chat about a page then the result is not empty`() {
        val request = PromptRequest(PRODUCT_LIST_URL, "Tell me something about the page")

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test instructOnDocumentReady`() {
        val instruct = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent()
        val request = PromptRequest(
            PRODUCT_DETAIL_URL,
            "Tell me something about the page",
            "",
            instructOnDocumentReady = instruct
        )

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }
}
