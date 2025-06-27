package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.CommandRequest
import ai.platon.pulsar.rest.api.entities.CommandStatus
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandControllerTest : ScrapeControllerTestBase() {

    /**
     * Test [CommandController.submitCommand]
     * */
    @Test
    fun `Test submitCommand with pageSummaryPrompt + sync mode`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            mode = "sync",
        )

        val status = restTemplate.postForObject("$baseUri/api/commands", request, CommandStatus::class.java)

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

    /**
     * Test [CommandController.submitCommand]
     * Test [CommandController.streamEvents]
     * */
    @Test
    fun `Test submitCommand with pageSummaryPrompt, dataExtractionRules + sync mode`() {
        val pageType = "productDetailPage"
        val url = requireNotNull(urls[pageType])

        val request = CommandRequest(url,
            "",
            pageSummaryPrompt = "Summarize the product.",
            dataExtractionRules = "product name, ratings, price",
            mode = "sync",
        )

        val status = restTemplate.postForObject("$baseUri/api/commands", request, CommandStatus::class.java)

        println(status)
        Assumptions.assumeTrue(status.pageStatusCode == 200)
        Assumptions.assumeTrue(status.isDone)
        Assumptions.assumeTrue(status.statusCode == 200)

        assertNotNull(status.commandResult)
        assertNotNull(status.commandResult?.pageSummary)
        assertNotNull(status.commandResult?.fields)

        assertNull(status.commandResult?.links)
        assertNull(status.commandResult?.xsqlResultSet)
    }

    @Test
    fun `test executeCommand with X-SQL + sync mode`() {
        val sqlTemplate = sqlTemplates["productDetailPage"]!!.template
        val request = CommandRequest(
            PRODUCT_DETAIL_URL,
            xsql = sqlTemplate,
            mode = "sync",
        )
        val status = restTemplate.postForObject("$baseUri/api/commands", request, CommandStatus::class.java)
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
}
