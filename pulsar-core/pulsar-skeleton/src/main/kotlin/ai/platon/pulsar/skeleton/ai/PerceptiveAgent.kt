package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.external.ModelResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode

import java.time.Instant
import java.util.*

data class ActionOptions(
    val action: String,
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
)

data class ExtractOptions(
    val instruction: String? = null,
    val schema: Map<String, String>? = null,
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
    val agentState: AgentState? = null,
)

data class ToolCall constructor(
    val domain: String,
    val method: String,
    val arguments: MutableMap<String, String?> = mutableMapOf(),
    val description: String? = null,
)

data class ObserveElement constructor(
    val locator: String? = null,

    val currentPageContentSummary: String? = null,
    val actualLastActionImpact: String? = null,
    val expectedNextActionImpact: String? = null,

    val modelResponse: ModelResponse? = null,

    // Revised fields
    val toolCall: ToolCall? = null,
    val node: DOMTreeNodeEx? = null,
    val backendNodeId: Int? = null,
    val xpath: String? = null,
    val cssSelector: String? = null,
    val expressions: List<String> = emptyList(),
    val cssFriendlyExpressions: List<String> = emptyList()
) {
    val description: String? get() = toolCall?.description
    val domain: String? get() = toolCall?.domain
    val method: String? get() = toolCall?.method
    val arguments: Map<String, Any?>? get() = toolCall?.arguments
}

data class ObserveResult constructor(
    val agentState: AgentState,

    val locator: String? = null,

    val domain: String? = null,
    val method: String? = null,
    val arguments: Map<String, Any?>? = null,
    val description: String? = null,

    val currentPageContentSummary: String? = null,
    val actualLastActionImpact: String? = null,
    val expectedNextActionImpact: String? = null,

    val backendNodeId: Int? = null,

    val observeElements: List<ObserveElement>? = null,
)

data class AgentState constructor(
    val step: Int,
    val action: String,
    val description: String? = null,
    val currentPageContentSummary: String? = null,
    val actualLastActionImpact: String? = null,
    val expectedNextActionImpact: String? = null,
    val timestamp: Instant = Instant.now(),
    // The last browser use state
    @JsonIgnore
    val browserUseState: BrowserUseState? = null,
    @JsonIgnore
    val prevState: AgentState? = null,
) {
    override fun toString(): String {
        val summary = listOfNotNull(description, currentPageContentSummary, currentPageContentSummary)
            .joinToString("\n")
        return "$timestamp $action - $summary"
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
    val processTrace: List<String>

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
    suspend fun extract(options: ExtractOptions): ExtractResult
}
