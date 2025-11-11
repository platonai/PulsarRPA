package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.lang3.StringUtils
import java.time.Instant
import java.util.*

data class ActionOptions(
    val action: String,  // the user's action command
    val modelName: String? = null,
    val variables: Map<String, String>? = null,
    val domSettleTimeoutMs: Int? = null,
    val timeoutMs: Int? = null,
    val iframes: Boolean? = null,
    val agentState: AgentState? = null,
)

data class ActResult(
    val success: Boolean,
    val message: String,

    val action: String? = null,
    val result: ToolCallResult? = null
) {
    @get:JsonIgnore
    val expression get() = result?.expression

    @get:JsonIgnore
    val tcEvalValue get() = result?.evaluate?.value

    override fun toString(): String {
        return "[$action] expr: $expression eval: $tcEvalValue message: $message"
    }

    companion object {
        fun failed(message: String, action: String? = null) = ActResult(false, message, action)
    }
}

data class ExtractOptions(
    val instruction: String,
    val schema: ExtractionSchema,
    val modelName: String? = null,
    val modelClientOptions: Map<String, Any>? = null,
    val domSettleTimeoutMs: Long? = null,
    val selector: String? = null,
    val iframes: Boolean? = null,
    val frameId: String? = null,
    val agentState: AgentState? = null,
)

data class ExtractResult(
    val success: Boolean,
    val message: String,
    val data: JsonNode
)

data class ObserveOptions(
    val instruction: String? = null,
    val modelName: String? = null,
    val modelClientOptions: Map<String, Any>? = null,
    val domSettleTimeoutMs: Long? = null,
    val returnAction: Boolean? = null,

    val drawOverlay: Boolean? = null,
    val iframes: Boolean? = null,
    val frameId: String? = null,
)

data class ToolCallSpec constructor(
    val domain: String,
    val method: String,
    val arguments: List<Arg> = listOf(),
    val returnType: String = "Unit",
    val description: String? = null,
) {
    data class Arg(
        val name: String,
        val type: String,
        val defaultValue: String? = null,
    ) {
        @get:JsonIgnore
        val expression: String get() {
            return if (defaultValue != null) "$name: $type = $defaultValue" else "$name: $type"
        }
    }

    @get:JsonIgnore
    val expression: String get() {
        val args = arguments.joinToString(prefix = "(", postfix = ")")
        return "$domain.$method$args"
    }
}

data class ToolCall constructor(
    val domain: String,
    val method: String,
    val arguments: MutableMap<String, String?> = mutableMapOf(),
    val description: String? = null,
) {
    @get:JsonIgnore
    val pseudoNamedArguments get() = arguments.entries.joinToString { (k, v) -> "$k=\"$v\"" }

    @get:JsonIgnore
    val pseudoExpression: String get() = "$domain.${method}($pseudoNamedArguments)"

    override fun toString() = pseudoExpression
}

data class TcException(
    val expression: String,
    val cause: Exception,
    var help: String,
) {
    val domain: String get() = expression.substringBefore('.')
    val method: String get() = StringUtils.substringBetween(expression, ".", ")")
}

data class TcEvaluate constructor(
    var value: Any? = null,
    var description: String? = null,
    val className: String? = null,
    val expression: String? = null,
    var exception: TcException? = null
) {
    val preview get() = doGetPreview()

    constructor(expression: String, cause: Exception, help: String) :
            this(expression = expression, exception = TcException(expression, cause, help))

    private fun doGetPreview(): String {
        return when (value) {
            is Number -> "$value"
            is Boolean -> "$value"
            else -> Strings.compactLog("$value")
        }
    }
}

data class ToolCallResult constructor(
    val success: Boolean,
    val evaluate: TcEvaluate? = null,
    val message: String? = null,
    val expression: String? = null,
    val modelResponse: String? = null,
)

data class ObserveElement constructor(
    val locator: String? = null,

    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val evaluationPreviousGoal: String? = null,
    val nextGoal: String? = null,

    val modelResponse: String? = null,

    // Revised fields
    val toolCall: ToolCall? = null,
    val node: DOMTreeNodeEx? = null,
    val backendNodeId: Int? = null,
    val xpath: String? = null,
    val cssSelector: String? = null,
    val expression: String? = null,
    val cssFriendlyExpression: String? = null,
) {
    @get:JsonIgnore
    val description: String? get() = toolCall?.description

    @get:JsonIgnore
    val domain: String? get() = toolCall?.domain

    @get:JsonIgnore
    val method: String? get() = toolCall?.method

    @get:JsonIgnore
    val arguments: Map<String, Any?>? get() = toolCall?.arguments
}

data class ObserveResult constructor(
    val agentState: AgentState,

    val locator: String? = null,

    val domain: String? = null,
    val method: String? = null,
    val arguments: Map<String, Any?>? = null,
    val description: String? = null,

    val screenshotContentSummary: String? = null,
    val currentPageContentSummary: String? = null,
    val evaluationPreviousGoal: String? = null,
    val nextGoal: String? = null,

    val backendNodeId: Int? = null,

    val observeElement: ObserveElement? = null,

    val actionDescription: ActionDescription? = null,
) {
    @Deprecated("Use observeElement instead", ReplaceWith("observeElement"))
    val observeElements: List<ObserveElement>? get() = actionDescription?.observeElements
}

data class AgentState constructor(
    var step: Int,
    // The user instruction
    var instruction: String,
    // The current browser use state
    @JsonIgnore
    var browserUseState: BrowserUseState,
    // AI:
    var domain: String? = null,
    // AI:
    var action: String? = null,
    // AI:
    var description: String? = null,
    // AI: the summary of the screenshot provided in this step
    var screenshotContentSummary: String? = null,
    // AI: the summary of the page content provided in this step
    var currentPageContentSummary: String? = null,
    // AI: an evaluation for the previous goal: evaluation and state: [success, failed, partial success]
    var evaluationPreviousGoal: String? = null,
    // AI: the next goal to archive
    var nextGoal: String? = null,
    // timestamp
    var timestamp: Instant = Instant.now(),
    @JsonIgnore
    var toolCallResult: ToolCallResult? = null,
    @JsonIgnore
    var prevState: AgentState? = null
) {
    // the url to handle in this step
    val url: String get() = browserUseState.browserState.url

    override fun toString(): String {
        val summary = listOfNotNull(
            description,
            currentPageContentSummary, screenshotContentSummary,
            evaluationPreviousGoal,
            nextGoal
        )
            .joinToString("\n")
        return "$timestamp $action - $summary"
    }
}

/**
 * The action description for the agent to go forward in the next step.
 * */
data class ActionDescription constructor(
    /**
     * The original instruction.
     * */
    val instruction: String,
    /**
     * AI: observe elements
     * */
    val observeElements: List<ObserveElement>? = null,

    /**
     * AI: whether the task is complete
     * */
    val isComplete: Boolean = false,
    /**
     * AI: a summary about this task
     * */
    val summary: String? = null,
    /**
     * AI: next suggestions
     * */
    val nextSuggestions: List<String> = emptyList(),
    /**
     * AI: model response
     * */
    val modelResponse: ModelResponse? = null,
    /**
     * The exception if any
     * */
    val exception: Exception? = null,
    /**
     * The agent state
     * */
    val agentState: AgentState? = null,
) {
    val observeElement: ObserveElement? get() = observeElements?.firstOrNull()
    val toolCall: ToolCall? get() = observeElement?.toolCall
    val locator: String? get() = observeElement?.locator
    val xpath: String? get() = observeElement?.xpath
    val expression: String? get() = observeElement?.expression
    val cssFriendlyExpression: String? get() = observeElement?.cssFriendlyExpression

    fun toActionDescriptions(): List<ActionDescription> {
        val elements = observeElements ?: return emptyList()
        return elements.map { this.copy(observeElements = listOf(it)) }
    }

    fun toObserveResults(agentState: AgentState): List<ObserveResult> {
        val results = observeElements?.map { ele ->
            ObserveResult(
                agentState = agentState,
                locator = ele.locator,
                domain = ele.domain?.ifBlank { null },
                method = ele.method?.ifBlank { null },
                arguments = ele.arguments?.takeIf { it.isNotEmpty() },
                description = ele.description ?: "(No comment ...)",
                screenshotContentSummary = ele.screenshotContentSummary,
                currentPageContentSummary = ele.currentPageContentSummary,
                evaluationPreviousGoal = ele.evaluationPreviousGoal,
                nextGoal = ele.nextGoal,
                backendNodeId = ele.backendNodeId,
                observeElement = ele,
                actionDescription = this,
            )
        }

        return results ?: emptyList()
    }

    override fun toString(): String {
        return if (isComplete) "Completed. Summary: $summary"
        else (cssFriendlyExpression ?: modelResponse?.toString() ?: "")
    }
}

data class ProcessTrace(
    val step: Int,
    val items: Map<String, Any?> = emptyMap(),
    val message: String? = null,
    val timestamp: Instant = Instant.now(),
) {
    override fun toString(): String {
        val itemStr = items.entries.joinToString { (k, v) -> "$k=" + Strings.compactLog(v.toString()) }
        val msg = message?.let { " | $it" } ?: ""
        return "$timestamp [$step] $itemStr$msg"
    }
}


interface PerceptiveAgent {
    val uuid: UUID

    /**
     * The agent state history exists to give the AI agent a concise, sequential memory of what has been done.
     * This helps the model select the next step and summarize outcomes.
     * For this to work well, the history should reflect only the agent’s actual, single-step actions
     * with clear success/failure signals and the observation context that impacted/was impacted by the action.
     * */
    val stateHistory: List<AgentState>

    /**
     * The process trace.
     * */
    val processTrace: List<ProcessTrace>

    /**
     * Run `observe -> act -> observe -> act -> ...` loop to resolve the problem.
     * */
    suspend fun resolve(problem: String): ActResult

    /**
     * Run `observe -> act -> observe -> act -> ...` loop to resolve the problem.
     * */
    suspend fun resolve(action: ActionOptions): ActResult

    suspend fun observe(instruction: String): List<ObserveResult>
    suspend fun observe(options: ObserveOptions): List<ObserveResult>
    suspend fun act(action: String): ActResult
    suspend fun act(action: ActionOptions): ActResult
    suspend fun act(observe: ObserveResult): ActResult
    suspend fun extract(instruction: String): ExtractResult
    suspend fun extract(instruction: String, schema: ExtractionSchema): ExtractResult
    suspend fun extract(options: ExtractOptions): ExtractResult
}
