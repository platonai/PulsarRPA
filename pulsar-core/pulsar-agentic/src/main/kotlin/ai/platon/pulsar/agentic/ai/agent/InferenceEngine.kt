package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.SimpleMessage
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.ai.tta.ContextToAction
import ai.platon.pulsar.agentic.ai.tta.TextToAction
import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.TokenUsage
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// ----------------------------------- Data models -----------------------------------
data class LLMUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
)

data class ExtractParams(
    val instruction: String,
    val agentState: AgentState,
    val schema: ExtractionSchema,
    val requestId: String = UUID.randomUUID().toString(),
    val userProvidedInstructions: String? = null,
    val logInferenceToFile: Boolean = false,
)

data class ObserveParams constructor(
    /**
     * The user's instruction/request
     * */
    val instruction: String,
    val agentState: AgentState,
    val requestId: String = UUID.randomUUID().toString(),
    /**
     * User provided additional system instructions
     * */
    val userProvidedInstructions: String? = null,
    val returnAction: Boolean = false,
    val logInferenceToFile: Boolean = false,
    val fromAct: Boolean = false,
    val context: ExecutionContext? = null
) {
    val browserUseState get() = agentState.browserUseState
}

class InferenceEngine(
    private val session: AgenticSession,
    private val chatModel: BrowserChatModel,
) {
    private val logger = getLogger(this)
    private val promptBuilder = PromptBuilder()

    // Reuse a single ObjectMapper for JSON parsing within this class
    private val mapper = ObjectMapper()
    private val cta = ContextToAction(session.sessionConfig)

    val domService: DomService
        get() = (session.getOrCreateBoundDriver() as? AbstractWebDriver)?.domService
            ?: throw IllegalStateException("Bound driver is not AbstractWebDriver")

    /**
     * Returns an ObjectNode with extracted fields expanded at top-level, plus:
     *   - metadata: { progress, completed }
     *   - prompt_tokens, completion_tokens, inference_time_ms
     */
    suspend fun extract(params: ExtractParams): ObjectNode {
        // 1) Extraction call -----------------------------------------------------------------
        val systemMsg = promptBuilder.buildExtractSystemPrompt(params.userProvidedInstructions)
        val userMsg = promptBuilder.buildExtractUserPrompt(params)

        val messages = listOf(systemMsg, userMsg)
        var callFile = ""
        var extractCallTs = ""
        if (params.logInferenceToFile) {
            val (file, ts) = logCallIfEnabled(
                dirPrefix = "extract_summary",
                kind = "extract_call",
                requestId = params.requestId,
                modelCall = "extract",
                messages = messages,
                enabled = true
            )
            callFile = file
            extractCallTs = ts
        }

        val (extractResp, extractElapsedMs) = doLangChainChat(systemMsg, userMsg)

        val messageText = extractResp.aiMessage().text().trim()
        val usage1 = toUsage(extractResp)

        val extractedNode: ObjectNode = runCatching {
            mapper.readTree(messageText) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        var extractRespFile = ""
        if (params.logInferenceToFile) {
            extractRespFile = writeTimestampedTxtFile(
                prefix = "extract_summary",
                kind = "extract_response",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to "extract",
                    "rawResponse" to safeJsonPreview(messageText)
                )
            ).first

            appendSummary(
                prefix = "extract",
                entry = mapOf(
                    "extract_inference_type" to "extract",
                    "timestamp" to extractCallTs,
                    "LLM_input_file" to callFile,
                    "LLM_output_file" to extractRespFile,
                    "prompt_tokens" to usage1.promptTokens,
                    "completion_tokens" to usage1.completionTokens,
                    "inference_time_ms" to extractElapsedMs
                )
            )
        }

        // 2) Metadata call -------------------------------------------------------------------
        val metadataSystem = promptBuilder.buildMetadataSystemPrompt()
        // For metadata, pass the extracted object directly
        val metadataUser = promptBuilder.buildMetadataPrompt(
            params.instruction,
            extractedNode,
            params.agentState
        )

        var metadataCallFile = ""
        var metadataCallTs = ""
        if (params.logInferenceToFile) {
            val (file, ts) = logCallIfEnabled(
                dirPrefix = "extract_summary",
                kind = "metadata_call",
                requestId = params.requestId,
                modelCall = "metadata",
                messages = listOf(metadataSystem, metadataUser),
                enabled = true
            )
            metadataCallFile = file; metadataCallTs = ts
        }

        val (metadataResp, metadataElapsedMs) = doLangChainChat(metadataSystem, metadataUser)

        val usage2 = toUsage(metadataResp)

        val metaText = metadataResp.aiMessage().text().trim()
        val metaNode: ObjectNode = runCatching {
            mapper.readTree(metaText) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }
        val progress = metaNode.path("progress").asText("")
        val completed = metaNode.path("completed").asBoolean(false)

        var metadataRespFile = ""
        if (params.logInferenceToFile) {
            metadataRespFile = writeTimestampedTxtFile(
                prefix = "extract_summary",
                kind = "metadata_response",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to "metadata",
                    "completed" to completed,
                    "progress" to progress,
                )
            ).first

            appendSummary(
                prefix = "extract",
                entry = mapOf(
                    "extract_inference_type" to "metadata",
                    "timestamp" to metadataCallTs,
                    "LLM_input_file" to metadataCallFile,
                    "LLM_output_file" to metadataRespFile,
                    "prompt_tokens" to usage2.promptTokens,
                    "completion_tokens" to usage2.completionTokens,
                    "inference_time_ms" to metadataElapsedMs
                )
            )
        }

        // 3) Merge & return ------------------------------------------------------------------
        val totalPrompt = (usage1.promptTokens) + (usage2.promptTokens)
        val totalCompletion = (usage1.completionTokens) + (usage2.completionTokens)
        val totalTime = metadataElapsedMs + extractElapsedMs

        val result: ObjectNode = (extractedNode.deepCopy()).apply {
            set<ObjectNode>("metadata", JsonNodeFactory.instance.objectNode().apply {
                put("progress", progress)
                put("completed", completed)
            })
            put("prompt_tokens", totalPrompt)
            put("completion_tokens", totalCompletion)
            put("inference_time_ms", totalTime)
        }
        return result
    }

    suspend fun observe(params: ObserveParams, messages: AgentMessageList): ActionDescription {
        requireNotNull(messages.instruction) { "User instruction is required | $messages" }
        require(params.instruction == messages.instruction?.content)
        requireNotNull(params.agentState.browserUseState) { "Agent state has to be available" }

        val instruction = params.instruction
        // promptBuilder.buildObservePrompt(params, messages)
        // observe guide
        promptBuilder.buildObserveGuideSystemPrompt(messages, params)
        // browser state, viewport info, interactive elements, DOM
        promptBuilder.buildObserveUserMessage(messages, params)

        val prefix = if (params.fromAct) "act" else "observe"
        var callFile = ""
        var callTs = ""
        if (params.logInferenceToFile) {
            val (f, ts) = logCallIfEnabled(
                dirPrefix = "${prefix}_summary",
                kind = "${prefix}_call",
                requestId = params.requestId,
                modelCall = prefix,
                messages = messages.messages,
                enabled = true
            )
            callFile = f
            callTs = ts
        }

        val systemMessages = messages.systemMessages()
        val userMessages = messages.userMessages()
        val (resp, elapsedMs) = doLangChainChat(systemMessages, userMessages)

        val tu = resp.tokenUsage()
        val tokenUsage = TokenUsage(
            inputTokenCount = tu.inputTokenCount(),
            outputTokenCount = tu.outputTokenCount(),
            totalTokenCount = tu.outputTokenCount()
        )

        val responseContent = resp.aiMessage().text().trim()

        val modeResponse = ModelResponse(content = responseContent, tokenUsage = tokenUsage)
        var actionDescription = cta.tta.modelResponseToActionDescription(instruction, params.agentState, modeResponse)
        actionDescription = cta.tta.reviseActionDescription(actionDescription)

        var respFile = ""
        if (params.logInferenceToFile) {
            respFile = writeTimestampedTxtFile(
                prefix = "${prefix}_summary",
                kind = "${prefix}_response",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to prefix,
                    "rawResponse" to safeJsonPreview(responseContent)
                )
            ).first

            appendSummary(
                prefix = prefix,
                entry = mapOf(
                    "${prefix}_inference_type" to prefix,
                    "timestamp" to callTs,
                    "LLM_input_file" to callFile,
                    "LLM_output_file" to respFile,
                    "inputTokenCount" to tokenUsage.inputTokenCount,
                    "outputTokenCount" to tokenUsage.outputTokenCount,
                    "inference_time_ms" to elapsedMs
                )
            )
        }

        return actionDescription
    }

    private fun safeJsonPreview(raw: String, limit: Int = 2000): String {
        // Bound file payload length for safety
        return Strings.compactLog(raw, limit)
    }

    private fun logsDir(): Path = Path.of("logs")

    private fun ensureDir(p: Path) {
        if (!Files.exists(p)) Files.createDirectories(p)
    }

    private fun writeTimestampedTxtFile(prefix: String, kind: String, payload: Any): Pair<String, String> {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        val dir = logsDir().resolve(prefix)
        ensureDir(dir)
        val file = dir.resolve("${kind}_${ts}.txt")
        val mapper = ObjectMapper()
        val bytes = runCatching { mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload) }
            .getOrElse { payload.toString().toByteArray(StandardCharsets.UTF_8) }
        Files.write(file, bytes)
        return Pair(file.toString(), ts)
    }

    private fun appendSummary(prefix: String, entry: Map<String, Any?>) {
        val file = logsDir().resolve("${prefix}_summary.json")
        val mapper = ObjectMapper()
        val current: ArrayNode = if (Files.exists(file)) {
            runCatching { mapper.readTree(Files.readAllBytes(file)) as? ArrayNode }
                .getOrNull() ?: JsonNodeFactory.instance.arrayNode()
        } else JsonNodeFactory.instance.arrayNode()
        // Explicitly convert the entry Map to a JsonNode to disambiguate ArrayNode.add overloads
        val entryNode: JsonNode = mapper.valueToTree(entry)
        current.add(entryNode)
        Files.write(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(current))
    }

    // ------------------------------ Small utilities --------------------------------
    private fun logCallIfEnabled(
        dirPrefix: String,
        kind: String,
        requestId: String,
        modelCall: String,
        messages: List<Any>,
        enabled: Boolean
    ): Pair<String, String> {
        if (!enabled) return "" to ""
        return writeTimestampedTxtFile(
            prefix = dirPrefix,
            kind = kind,
            payload = mapOf(
                "requestId" to requestId,
                "modelCall" to modelCall,
                "messages" to messages,
            )
        )
    }

    private suspend fun doLangChainChat(
        systemMessage: SimpleMessage, userMessage: SimpleMessage
    ): Pair<ChatResponse, Long> {
        return doLangChainChat(
            SystemMessage.systemMessage(systemMessage.content),
            UserMessage.userMessage(userMessage.content)
        )
    }

    private suspend fun doLangChainChat(
        systemMessages: List<SimpleMessage>, userMessages: List<SimpleMessage>
    ): Pair<ChatResponse, Long> {
        return doLangChainChat(
            SystemMessage.systemMessage(systemMessages.joinToString("\n") { it.content }),
            UserMessage.userMessage(userMessages.joinToString("\n") { it.content })
        )
    }

    private suspend fun doLangChainChat(
        systemMessage: SystemMessage,
        userMessage: UserMessage
    ): Pair<ChatResponse, Long> {
        val temperature = 0.1
        val chatRequest = ChatRequest.builder()
            .messages(systemMessage, userMessage)
            // use provider default temperature
//            .temperature(temperature)
            .topP(1.0)
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .build()

        val t0 = System.currentTimeMillis()
        val resp: ChatResponse = chatModel.langChainChat(chatRequest)
        val t1 = System.currentTimeMillis()

        return resp to (t1 - t0)
    }

    private fun toUsage(resp: ChatResponse): LLMUsage = LLMUsage(
        promptTokens = resp.tokenUsage().inputTokenCount(),
        completionTokens = resp.tokenUsage().outputTokenCount(),
    )
}
