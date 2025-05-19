package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.PromptRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.*

/**
 * [WARNING] Tests run: 14, Failures: 0, Errors: 0, Skipped: 4, Time elapsed: 111.2 s -- in ai.platon.pulsar.rest.api.service.PromptServiceTest
 * */
@Tag("TimeConsumingTest")
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class CommandServiceTest {

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var conf: ImmutableConfig

    @Autowired
    private lateinit var commandService: CommandService

    @BeforeEach
    fun setup() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf))
    }

    @Test
    fun `test executeCommand without instructions`() {
        val request = CommandRequest(PRODUCT_DETAIL_URL)
        val status = commandService.executeCommand(request)
        val result = status.commandResult
        // nothing to do if page is not loaded
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        assertTrue { status.isDone }

        assertNull(result)
        assertTrue { status.instructResult.isEmpty() }
    }

    @Test
    fun `test executeCommand with actions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "Tell me something about the page", "", actions = actions
        )

        val response = chatService.chat(request)

        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test executeCommand with pageSummaryPrompt`() {
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            pageSummaryPrompt = "Give me the product name",
        )
        val status = commandService.executeCommand(request)
        val result = status.commandResult
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        assertNotNull(result)
        assertTrue { status.isDone }

        println(result.pageSummary)

        assertNull(result.fields)
        assertNull(result.xsqlResultSet)
        assertTrue { !result.pageSummary.isNullOrBlank() }
    }

    @Test
    fun `test executeCommand with dataExtractionRules`() {
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            dataExtractionRules = "product name, ratings, price",
        )
        val status = commandService.executeCommand(request)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        assertNotNull(result)
        assertTrue { status.isDone }

        val fields = result.fields
        println(fields)

        assertNull(result.pageSummary)
        assertNull(result.xsqlResultSet)

        assertNotNull(fields)
        assertTrue { fields.isNotEmpty() }
    }

    @Test
    fun `test executeCommand with simple and clean command`() {
        val prompt = API_COMMAND_PROMPT1
        val status = commandService.executeCommand(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        assertNotNull(status)
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.statusCode == 200)
        assertNotNull(status.commandResult?.pageSummary)
        assertNotNull(status.commandResult?.fields)
    }

    @Test
    fun `test executeCommand with detailed and verbose command`() {
        val prompt = API_COMMAND_PROMPT3
        val status = commandService.executeCommand(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        assertNotNull(status)
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.statusCode == 200)
        assertNotNull(status.commandResult?.pageSummary)
        assertNotNull(status.commandResult?.fields)
    }
}
