package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_LIST_URL
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.entities.PromptRequestL2
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class PromptServiceTest {

    @Autowired
    private lateinit var conf: ImmutableConfig

    @Autowired
    private lateinit var service: PromptService

    @BeforeEach
    fun setup() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf))
    }

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
    fun `test actions on document ready`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent()
        val request = PromptRequest(
            PRODUCT_DETAIL_URL,
            "Tell me something about the page",
            "",
            actions = actions
        )

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test extract`() {
        val request = PromptRequest(PRODUCT_DETAIL_URL, "title, price, images")
        val response = service.extract(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test extract with actions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent()
        val request = PromptRequest(
            PRODUCT_DETAIL_URL,
            "title, price, images",
            "",
            actions = actions
        )

        val response = service.extract(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test command with talkAboutPage`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            talkAboutPage = "Give me the product name",
        )
        val response = service.command(request)
        println(response.talkAboutPageResponse)

        assertTrue { response.isDone }
        assertNull(response.fields)
        assertNull(response.xsqlResultSet)
        assertTrue { !response.talkAboutPageResponse.isNullOrBlank() }
    }

    @Test
    fun `test command with fieldDescriptions`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            fieldDescriptions = "product name, ratings, price",
        )
        val response = service.command(request)
        println(response.fields)

        assertTrue { response.isDone }
        assertNull(response.talkAboutPageResponse)
        assertNull(response.xsqlResultSet)
        assertTrue { !response.fields.isNullOrBlank() }
    }
}
