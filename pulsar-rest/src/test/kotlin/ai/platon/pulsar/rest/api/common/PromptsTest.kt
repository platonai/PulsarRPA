package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.rest.api.TestHelper.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.entities.CommandRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PromptsTest {
    private val request = CommandRequest(
        url = PRODUCT_DETAIL_URL,
        onBrowserLaunchedActions = listOf(
            "clear browser cookies",
            "goto the website homepage",
        ),
        onPageReadyActions = listOf(
            "move cursor to the element with id 'title' and click it",
            "scroll to middle",
            "scroll to top",
            "get the text of the element with id 'title'"
        ),
        pageSummaryPrompt = "Tell me something about the page",
        dataExtractionRules = "Extract the title and price of the product",
        uriExtractionRules = "Links containing /dp/"
    )
    private val textContent = "This is a sample product page with title 'Sample Product' and price '$19.99'."

    @Test
    fun `test pageSummaryPrompt`() {
        RestAPIPromptUtils.normalizePageSummaryPrompt(request.pageSummaryPrompt)
        val pageSummaryPrompt = RestAPIPromptUtils.normalizePageSummaryPrompt(request.pageSummaryPrompt)
        assertNotNull(pageSummaryPrompt)
        val prompt = PromptTemplate(pageSummaryPrompt, mapOf(PLACEHOLDER_PAGE_CONTENT to textContent)).render()
        assertTrue { prompt.isNotBlank() }
    }
}
