package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestHelper.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.common.MockEcServerTestBase
import ai.platon.pulsar.rest.api.config.MockEcServerConfiguration
import ai.platon.pulsar.rest.api.entities.PromptRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("TimeConsumingTest")
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@Import(MockEcServerConfiguration::class)
class ExtractServiceTest : MockEcServerTestBase() {

    @Autowired
    private lateinit var conf: ImmutableConfig

    @Autowired
    private lateinit var extractService: ExtractService

    @BeforeEach
    override fun setup() {
        super.setup()
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf))
    }

    @Test
    fun `test extract`() {
        val request = PromptRequest(PRODUCT_DETAIL_URL, "title, price, images")
        val response = runBlocking { extractService.extract(request) }
        println(response.toString())
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test extract with actions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "title, price, images", "", actions = actions
        )

        val response = runBlocking { extractService.extract(request) }
        println(response.toString())
        assertTrue { response.isNotEmpty() }
    }
}
