package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
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
import kotlin.test.*

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

    @Test
    fun `test prompt convertion to request with cache`() {
        val url1 = "https://www.amazon.com/dp/B0C1H26C46"
        val url2 = "https://www.amazon.com/dp/B07PX3ZRJ6"

        val prompt1 = """
Visit $url1

Page summary prompt: Provide a brief introduction of this product.

        """.trimIndent()

        val result1 = service.convertAPIRequestCommandToJSON(prompt1, url1)

        val prompt2 = """
Visit $url2

Page summary prompt: Provide a brief introduction of this product.

        """.trimIndent()

        val result2 = service.convertAPIRequestCommandToJSON(prompt2, url2)

        assertEquals(result2, result1)
    }

    @Test
    fun `test prompt conversion without URL`() {
        val prompt = """
Visit amazon.com/dp/B0C1H26C46

Page summary prompt: Provide a brief introduction of this product.
        """.trimIndent()

        val request = service.convertPromptToRequest(prompt)
        assertNull(request)
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
    fun `test actions on page ready`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "Tell me something about the page", "", actions = actions
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
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "title, price, images", "", actions = actions
        )

        val response = service.extract(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test command with pageSummaryPrompt`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            pageSummaryPrompt = "Give me the product name",
        )
        val response = service.command(request)
        Assumptions.assumeTrue(response.pageStatusCode == 200)
        println(response.pageSummary)

        assertTrue { response.isDone }
        assertNull(response.fields)
        assertNull(response.xsqlResultSet)
        assertTrue { !response.pageSummary.isNullOrBlank() }
    }

    @Test
    fun `test command with dataExtractionRules`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            dataExtractionRules = "product name, ratings, price",
        )
        val response = service.command(request)
        Assumptions.assumeTrue(response.pageStatusCode == 200)
        val fields = response.fields
        println(fields)

        assertTrue { response.isDone }
        assertNull(response.pageSummary)
        assertNull(response.xsqlResultSet)

        assertNotNull(fields)
        assertTrue { fields.isNotEmpty() }
    }

    @Test
    fun `test prompt convertion to request`() {
        var prompt = """
Visit https://www.amazon.com/dp/B0C1H26C46

Page summary prompt: Provide a brief introduction of this product.
Extract fields: product name, price, and ratings.
Extract links: all links containing `/dp/` on the page.

When the page is ready, click the element with id "title" and scroll to the middle.

        """.trimIndent()

        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyConvertedPrompt(request)
    }

    @Test
    fun `test prompt convertion to request 2`() {
        val prompt = """
Visit https://www.amazon.com/dp/B0C1H26C46.
Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
After page load: click #title, then scroll to the middle.
"""
        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyConvertedPrompt(request)
    }

    @Test
    fun `test prompt convertion to request 3`() {
        val prompt = """
Visit the page: https://www.amazon.com/dp/B0C1H26C46

### 📝 Tasks:

**1. Page Summary**  
Provide a brief introduction to the product.

**2. Field Extraction**  
Extract the following information from the page content:
- Product name
- Price
- Ratings

**3. Link Extraction**  
Collect all hyperlinks on the page that contain the substring `/dp/`.

**4. Page Interaction**  
Once the document is fully loaded:
- Click the element with `id="title"`
- Scroll to the middle of the page
"""
        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyConvertedPrompt(request)
    }

    private fun verifyConvertedPrompt(request: PromptRequestL2) {
        assertTrue { request.url == "https://www.amazon.com/dp/B0C1H26C46" }
        assertEquals("https://www.amazon.com/dp/B0C1H26C46", request.url)
        assertNotNull(request.pageSummaryPrompt)
        assertNotNull(request.dataExtractionRules)
        assertNotNull(request.linkExtractionRules)
        assertNotNull(request.onPageReadyActions)
    }
}
