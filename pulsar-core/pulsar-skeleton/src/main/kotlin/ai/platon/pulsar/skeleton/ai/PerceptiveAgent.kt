package ai.platon.pulsar.skeleton.ai

import com.fasterxml.jackson.databind.JsonNode
import java.util.*

data class ActionOptions(
    val action: String,
    val modelName: String? = null,
    val variables: Map<String, String>? = null,
    val domSettleTimeoutMs: Int? = null,
    val timeoutMs: Int? = null,
    val iframes: Boolean? = null
)

data class ActResult(
    val success: Boolean,
    val message: String,
    val action: String
)

data class ExtractOptions(
    val instruction: String? = null,
    val schema: Map<String, String>? = null,
    val modelName: String? = null,
    val modelClientOptions: Map<String, Any>? = null,
    val domSettleTimeoutMs: Long? = null,
    val selector: String? = null,
    val iframes: Boolean? = null,
    val frameId: String? = null
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
    val frameId: String? = null
)

data class ObserveResult(
    val selector: String,
    val description: String,
    val backendNodeId: Int? = null,
    val method: String? = null,
    val arguments: List<String>? = null
)

interface PerceptiveAgent {
    val uuid: UUID
    val history: List<String>

    /**
     * Run `observe -> act -> observe -> act -> ...` loop to resolve the problem.
     * */
    suspend fun resolve(instruction: String): ActResult
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
