package ai.platon.pulsar.rest.api.entities

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import java.time.Instant
import java.util.*

/**
 * Request for chat
 *
 * @see [ai.platon.pulsar.rest.api.controller.AiController.chat]
 *
 * @property url The page url
 * @property prompt The prompt, e.g. "Tell me something about the page"
 * @property args The load arguments
 * @property actions Instructs, e.g. "click the button with id 'submit'", [actions]  are performed after the active DOM is ready
 * */
data class PromptRequest(
    /**
     * The page url
     * */
    var url: String,
    /**
     * The prompt, e.g. "Tell me something about the page"
     * */
    var prompt: String? = null,
    /**
     * The load arguments
     *
     * @see [ai.platon.pulsar.skeleton.common.options.LoadOptions]
     * */
    var args: String? = null,
    /**
     * Actions, e.g. "click the button with id 'submit'", [actions] are performed after the active DOM is ready
     * */
    var actions: List<String>? = null
)

data class ScrapeRequest(
    var sql: String,
)

data class ScrapeResponse(
    var uuid: String? = null,
    var statusCode: Int = ResourceStatus.SC_CREATED,
    var pageStatusCode: Int = ProtocolStatusCodes.CREATED,
    var pageContentBytes: Int = 0,
    var isDone: Boolean = false,
    var resultSet: List<Map<String, Any?>>? = null,
) {
    val status: String get() = ResourceStatus.getStatusText(statusCode)
    val pageStatus: String get() = ProtocolStatus.getMinorName(pageStatusCode)
    val createTime: Instant = Instant.now()
    var finishTime: Instant? = null
}

data class ScrapeStatusRequest(
    val uuid: String,
)

/**
 * W3 resources
 * */
data class W3DocumentRequest(
    var url: String,
    val args: String? = null,
)

/**
 * Advanced request for web page interactions with structured data extraction capabilities.
 *
 * @property url The target page URL to process.
 * @property args Optional load arguments to customize page loading behavior.
 * @property pageSummaryPrompt A prompt to analyze or discuss the HTML structure of the page.
 * @property dataExtractionRules Specifications for extracting structured fields from the HTML content.
 * @property onPageReadyActions Actions to perform when the document is fully loaded (e.g., "scroll down", "click button").
 * @property xsql An X-SQL query for structured data extraction, e.g.
 *              "select dom_first_text(dom, '#title') as title, llm_extract(dom, 'price') as price".
 */
data class PromptRequestL2(
    var url: String,
    var args: String? = null,
    var pageSummaryPrompt: String? = null,
    var dataExtractionRules: String? = null,
    var linkExtractionRules: String? = null,
    var richText: Boolean? = null,
    var onPageReadyActions: List<String>? = null,
    var xsql: String? = null,
)

data class PromptResponseL2(
    var uuid: String? = null,

    var statusCode: Int = ResourceStatus.SC_CREATED,
    var pageStatusCode: Int = ProtocolStatusCodes.CREATED,
    var pageContentBytes: Int = 0,
    var isDone: Boolean = false,

    var pageSummary: String? = null,
    var fields: String? = null,
    var links: String? = null,
//    var fields: List<String>? = null,
//    var links: List<String>? = null,
    var xsqlResultSet: List<Map<String, Any?>>? = null,
) {
    val status: String get() = ResourceStatus.getStatusText(statusCode)
    val pageStatus: String get() = ProtocolStatus.getMinorName(pageStatusCode)
    val createTime: Instant = Instant.now()
    var finishTime: Instant? = null

    companion object {
        fun failed(statusCode: Int): PromptResponseL2 {
            return PromptResponseL2(uuid = UUID.randomUUID().toString(), statusCode = statusCode)
        }
    }
}
