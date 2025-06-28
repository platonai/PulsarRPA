package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.browser.BrowserContextMode
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
    private lateinit var conf: ImmutableConfig

    @Autowired
    private lateinit var commandService: CommandService

    @BeforeEach
    fun setup() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf))
        BrowserSettings.withBrowserContextMode(BrowserContextMode.TEMPORARY)
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
        val testURL = "https://www.amazon.com/-/zh/ap/register?openid.pape.max_auth_age=0&openid.return_to=https://www.amazon.com/dp/B0C1H26C46/?_encoding=UTF8&ref_=nav_newcust&openid.identity=http://specs.openid.net/auth/2.0/identifier_select&openid.assoc_handle=usflex&openid.mode=checkid_setup&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select&openid.ns=http://specs.openid.net/auth/2.0"
        val testRegex = "https?://.+/dp/[\\w]+.*".toRegex()
        assertTrue { testURL.matches(testRegex) }

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
    fun `test translate plain command to CommandRequest with X-SQL`() {
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            xsql = """
                select
                  dom_base_uri(dom) as url,
                  dom_first_text(dom, '#productTitle') as title,
                  str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), 'node=') as category,
                  dom_first_slim_html(dom, '#bylineInfo') as brand,
                  cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
                  dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
                  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
                  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
                  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
                from load_and_select(@url, 'body');
            """.trimIndent()
        )
        val status = commandService.executeCommand(request)
        println(prettyPulsarObjectMapper().writeValueAsString(status))
        val result = status.commandResult

        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(result)
        assertTrue { status.isDone }

        assertNull(result.pageSummary)
        assertNotNull(result.xsqlResultSet)
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
