package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.CommandRequest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `test executeCommand WITHOUT instructions`() {
        val request = CommandRequest(PRODUCT_DETAIL_URL)
        val status = commandService.executeCommand(request)
        val result = status.commandResult
        // nothing to do if page is not loaded
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        assertTrue { status.isDone }

        assertNull(result)
        assertTrue { status.instructResults.isEmpty() }
    }

    @Test
    fun `test executeCommand with onPageReadyActions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = CommandRequest(
            PRODUCT_DETAIL_URL, "",
            pageSummaryPrompt = "Tell me something about the page",
            onPageReadyActions = actions
        )

        val status = commandService.executeCommand(request)

        println(status)
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(status.commandResult)
        assertNotNull(status.commandResult?.pageSummary)

        assertNull(status.commandResult?.fields)
        assertNull(status.commandResult?.links)
        assertNull(status.commandResult?.xsqlResultSet)
    }

    @Test
    fun `test executeCommand with onBrowserLaunchedActions`() {
        val actions = """
            clear cookies
            goto origin url of $PRODUCT_DETAIL_URL
        """.trimIndent().split("\n")
        val request = CommandRequest(
            PRODUCT_DETAIL_URL, "",
            onBrowserLaunchedActions = actions,
            pageSummaryPrompt = "Tell me something about the page",
        )

        val status = commandService.executeCommand(request)

        println(status)
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(status.commandResult)
        assertNotNull(status.commandResult?.pageSummary)

        assertNull(status.commandResult?.fields)
        assertNull(status.commandResult?.links)
        assertNull(status.commandResult?.xsqlResultSet)
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
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

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
            dataExtractionRules = "product name, ratings, price"
        )
        val status = commandService.executeCommand(request)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult

        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

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
    fun `test executeCommand with uriExtractionRules`() {
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            uriExtractionRules = "links containing /dp/"
        )
        val status = commandService.executeCommand(request)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult

        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(result)
        assertTrue { status.isDone }

        val links = result.links
        println(links)

        assertNull(result.pageSummary)
        assertNull(result.xsqlResultSet)

        assertNotNull(links)
        assertTrue { links.isNotEmpty() }
    }

    @Test
    fun `test executeCommand with uriExtractionRules in regex`() {
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            uriExtractionRules = "Regex: https://www.amazon.com/dp/\\w+"
        )
        val status = commandService.executeCommand(request)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult

        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(result)
        assertTrue { status.isDone }

        val links = result.links
        println(links)

        assertNull(result.pageSummary)
        assertNull(result.xsqlResultSet)

        assertNotNull(links)
        assertTrue { links.isNotEmpty() }
    }

    @Test
    fun `test executeCommand with simple and clean command`() {
        val prompt = API_COMMAND_PROMPT1
        val status = commandService.executeCommand(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        assertNotNull(status)

        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
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
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(status.commandResult?.pageSummary)
        assertNotNull(status.commandResult?.fields)
    }
}
