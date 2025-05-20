package ai.platon.pulsar.rest.api.entities

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import java.time.Instant
import java.util.*

/**
 * Request for chat
 *
 * @see [ai.platon.pulsar.rest.api.controller.AiController.chatBackward]
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
    var id: String? = null,
    var statusCode: Int = ResourceStatus.SC_CREATED,
    var pageStatusCode: Int = ProtocolStatusCodes.SC_CREATED,
    var pageContentBytes: Int = 0,
    var isDone: Boolean = false,
    var resultSet: List<Map<String, Any?>>? = null,

    var event: String = "",
) {
    val status: String get() = ResourceStatus.getStatusText(statusCode)
    val pageStatus: String get() = ProtocolStatus.getMinorName(pageStatusCode)
    val createTime: Instant = Instant.now()
    var lastModifiedTime: Instant? = null
    var finishTime: Instant? = null

    companion object {
        fun notFound(id: String) = ScrapeResponse(id, ResourceStatus.SC_NOT_FOUND, ResourceStatus.SC_NOT_FOUND)
        fun failed(id: String, statusCode: Int, pageStatusCode: Int) =
            ScrapeResponse(id, statusCode = statusCode, pageStatusCode = pageStatusCode)

        fun failed(id: String, e: Exception) =
            ScrapeResponse(
                id,
                statusCode = ResourceStatus.SC_EXPECTATION_FAILED,
                pageStatusCode = ResourceStatus.SC_EXPECTATION_FAILED
            )
    }
}

fun ScrapeResponse.refresh(isDone: Boolean = false) {
    lastModifiedTime = Instant.now()
    this.isDone = isDone
}

fun ScrapeResponse.refresh(statusCode: Int) = refresh(statusCode, this.pageStatusCode, false)

fun ScrapeResponse.refresh(statusCode: Int, pageStatusCode: Int, isDone: Boolean) {
    lastModifiedTime = Instant.now()
    this.statusCode = statusCode
    this.pageStatusCode = pageStatusCode
    this.isDone = isDone
}

fun ScrapeResponse.failed(statusCode: Int): ScrapeResponse {
    // do not change pageStatusCode
    refresh(statusCode, pageStatusCode, isDone = true)
    return this
}

fun ScrapeResponse.refresh(event: String) {
    this.event = event
    this.lastModifiedTime = Instant.now()
}

fun ScrapeResponse.failed(statusCode: Int, pageStatusCode: Int): ScrapeResponse {
    refresh(statusCode, pageStatusCode, isDone = true)
    return this
}

fun ScrapeResponse.done() {
    refresh(isDone = true)
    finishTime = Instant.now()
}

data class ScrapeStatusRequest(
    val id: String,
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
 * @property mode The execution mode, either "sync" (synchronous) or "async" (asynchronous).
 */
data class CommandRequest(
    var url: String,
    var args: String? = null,
    var onPageReadyActions: List<String>? = null,
    var pageSummaryPrompt: String? = null,
    var dataExtractionRules: String? = null,
    var linkExtractionRules: String? = null,
    var xsql: String? = null,
    var richText: Boolean? = null,
    var mode: String = "sync", // "sync" | "async"
)

/**
 * Command result
 *
 * @property pageSummary The summary of the page.
 * @property fields The extracted fields from the page.
 * @property links The extracted links from the page.
 * @property xsqlResultSet The result set from the X-SQL query.
 */
data class CommandResult(
    var pageSummary: String? = null,
    var fields: String? = null,
    var links: String? = null,
//    var fields: List<String>? = null,
//    var links: List<String>? = null,
    var xsqlResultSet: List<Map<String, Any?>>? = null,
)

/**
 * Instruct result
 *
 * @property name The name of the instruction.
 * @property statusCode The status code of the instruction result.
 * @property result The result of the instruction.
 * @property instruct The instruction text.
 * */
data class InstructResult(
    var name: String,
    var statusCode: Int = ResourceStatus.SC_CREATED,
    var result: String? = null,
    var instruct: String? = null,
) {
    companion object {

        fun ok(name: String, result: String): InstructResult {
            return InstructResult(name, ResourceStatus.SC_OK, result = result)
        }

        fun failed(name: String, statusCode: Int = ResourceStatus.SC_EXPECTATION_FAILED): InstructResult {
            return InstructResult(name, statusCode)
        }
    }
}

/**
 * Command status
 *
 * @property id The unique identifier for the command status.
 * @property statusCode The HTTP status code representing the command status.
 * @property event The last event associated with the command status.
 * @property isDone Indicates whether the command has been completed.
 * @property pageStatusCode The HTTP status code representing the page status.
 * @property pageContentBytes The size of the page content in bytes.
 * @property message An optional message providing additional information about the command status.
 * @property request The original command request associated with this status.
 * @property commandResult The result of the command execution.
 * @property instructResults A list of results from the instructions executed during the command.
 * */
data class CommandStatus(
    val id: String = UUID.randomUUID().toString(),

    var statusCode: Int = ResourceStatus.SC_CREATED,
    var event: String = "",
    var isDone: Boolean = false,

    var pageStatusCode: Int = ProtocolStatusCodes.SC_CREATED,
    var pageContentBytes: Int = 0,

    var message: String? = null, // additional message, e.g. the action flow

    var request: CommandRequest? = null,
    var commandResult: CommandResult? = null,
    var instructResults: MutableList<InstructResult> = mutableListOf()
) {
    val status: String get() = ResourceStatus.getStatusText(statusCode)
    val pageStatus: String get() = ProtocolStatus.getStatusText(pageStatusCode)
    val createTime: Instant = Instant.now()
    var lastModifiedTime: Instant? = null
    var finishTime: Instant? = null

    companion object {
        fun notFound(id: String) = CommandStatus(id, ResourceStatus.SC_NOT_FOUND, isDone = true)

        fun failed(id: String) = CommandStatus(id, ResourceStatus.SC_EXPECTATION_FAILED, isDone = true)

        fun failed(id: String, statusCode: Int, pageStatusCode: Int = statusCode) =
            CommandStatus(id, statusCode = statusCode, pageStatusCode = pageStatusCode, isDone = true)

        fun failed(statusCode: Int, pageStatusCode: Int = statusCode) = failed("", statusCode, pageStatusCode)

        fun failed(id: String, e: Exception): CommandStatus {
            return CommandStatus(id, statusCode = ResourceStatus.SC_EXPECTATION_FAILED, isDone = true)
        }
    }
}

fun CommandStatus.ensureCommandResult(): CommandResult {
    val r = commandResult ?: CommandResult()
    commandResult = r
    return r
}

fun CommandStatus.refresh(isDone: Boolean = false) {
    lastModifiedTime = Instant.now()
    this.isDone = isDone
}

fun CommandStatus.refresh(statusCode: Int) = refresh(statusCode, this.pageStatusCode, false)

fun CommandStatus.refresh(statusCode: Int, pageStatusCode: Int, isDone: Boolean) {
    lastModifiedTime = Instant.now()
    this.statusCode = statusCode
    this.pageStatusCode = pageStatusCode
    this.isDone = isDone
}

fun CommandStatus.failed(statusCode: Int): CommandStatus {
    // do not change pageStatusCode
    refresh(statusCode, pageStatusCode, isDone = true)
    return this
}

fun CommandStatus.refresh(event: String) {
    this.event = event
    message = if (message != null) "$message,$event" else event
}

fun CommandStatus.failed(statusCode: Int, pageStatusCode: Int): CommandStatus {
    refresh(statusCode, pageStatusCode, isDone = true)
    return this
}

fun CommandStatus.addInstructResult(result: InstructResult) {
    instructResults.add(result)

    val name = result.name
    val commandResult = ensureCommandResult()
    when (name) {
        "pageSummary" -> {
            commandResult.pageSummary = result.result
        }

        "fields" -> {
            commandResult.fields = result.result
        }

        "links" -> {
            commandResult.links = result.result
        }
    }
    refresh(result.name)
}

fun CommandStatus.done() {
    refresh(isDone = true)
    finishTime = Instant.now()
}
