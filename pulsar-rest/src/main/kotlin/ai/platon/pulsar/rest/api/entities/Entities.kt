package ai.platon.pulsar.rest.api.entities

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import java.time.Instant

/**
 * Request for chat
 *
 * @see [ai.platon.pulsar.rest.api.controller.AiController.chat]
 *
 * @property url The page url
 * @property prompt The prompt, e.g. "Tell me something about the page"
 * @property args The load arguments
 * @property instructs Instructs, e.g. "click the button with id 'submit'", [instructs] is alias for [instructsOnDocumentReady]
 * @property instructsOnDocumentReady Instructs on document ready, e.g. "click the button with id 'submit'"
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
     * Instructs, e.g. "click the button with id 'submit'", [instructs] is alias for [instructsOnDocumentReady]
     * */
    var instructs: String? = null,
    /**
     * Instructs on document ready, e.g. "click the button with id 'submit'",
     * */
    var instructsOnDocumentReady: String? = null,
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
