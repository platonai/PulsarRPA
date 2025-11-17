package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

interface PerceptiveAgent : AutoCloseable {
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
     * High-level problem resolution entry. Implementations should construct an [ActionOptions]
     * from the raw problem string and delegate to [resolve] with the options.
     *
     * @param problem The user goal or instruction to fulfill.
     * @return The final action result produced by the agent.
     */
    suspend fun resolve(problem: String): ActResult

    /**
     * Run an autonomous loop (observe -> act -> ...) attempting to fulfill the user goal described
     * in the provided [ActionOptions]. Implementations may apply retry and timeout strategies; they
     * should record structured traces while keeping [stateHistory] focused on executed tool actions only.
     *
     * @param action The action options describing the user goal and context.
     * @return The final action result for the resolution attempt.
     */
    suspend fun resolve(action: ActionOptions): ActResult

    /**
     * Convenience overload to observe by instruction string. Implementations typically create
     * an [ObserveOptions] from the instruction and delegate to [observe].
     *
     * @param instruction The observation instruction from the user.
     * @return Zero or more observation results describing candidate elements and potential actions.
     */
    suspend fun observe(instruction: String): List<ObserveResult>

    /**
     * Observe the page given an instruction and options, returning zero or more [ObserveResult]
     * objects describing candidate elements and potential actions. When `returnAction=true`,
     * implementations may include an actionable method/arguments in the result.
     *
     * @param options Observation options including the instruction and flags.
     * @return A list of observation results; empty if nothing actionable is found.
     */
    suspend fun observe(options: ObserveOptions): List<ObserveResult>

    /**
     * Convenience wrapper building [ActionOptions] from a raw action string and delegating to [act].
     *
     * @param action The action to execute.
     * @return The result of executing the action.
     */
    suspend fun act(action: String): ActResult

    /**
     * Execute a single observe->act cycle for the supplied [ActionOptions]. Implementations may
     * apply a timeout to prevent indefinite hangs. Models may produce multiple candidate tool calls;
     * only one successful execution should be recorded in [stateHistory].
     *
     * @param action The action options to execute.
     * @return The action result for the attempted execution.
     */
    suspend fun act(action: ActionOptions): ActResult

    /**
     * Execute a tool call derived from a prior observation result. Implementations should perform
     * any necessary validation and update [stateHistory] on success or failure.
     *
     * @param observe The observation result containing the candidate action.
     * @return The result of executing the derived tool call.
     */
    suspend fun act(observe: ObserveResult): ActResult

    /**
     * Convenience overload for structured extraction. When only an instruction string is provided,
     * the implementation should use [ExtractionSchema.DEFAULT].
     *
     * @param instruction The extraction instruction from the user.
     * @return The extraction result produced by the model.
     */
    suspend fun extract(instruction: String): ExtractResult

    /**
     * Convenience overload for structured extraction that constrains the result with a JSON schema.
     *
     * @param instruction The extraction instruction from the user.
     * @param schema The JSON schema used to constrain the returned data structure.
     * @return The extraction result produced by the model.
     */
    suspend fun extract(instruction: String, schema: ExtractionSchema): ExtractResult

    /**
     * Structured extraction entry point. Implementations should build a rich prompt with the DOM
     * snapshot and optional JSON schema to produce a structured [JsonNode] payload.
     *
     * @param options Extraction options including instruction and schema.
     * @return The extraction result.
     */
    suspend fun extract(options: ExtractOptions): ExtractResult
}
