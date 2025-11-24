package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.SimpleMessage
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.agentic.ai.tta.ContextToAction
import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.agentic.ActionDescription
import ai.platon.pulsar.agentic.AgentState
import ai.platon.pulsar.agentic.ExtractionSchema
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
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class ExtractParams constructor(
    val instruction: String,
    val agentState: AgentState,
    val schema: ExtractionSchema,
    val requestId: String = UUID.randomUUID().toString(),
    val userProvidedInstructions: String? = null,
    val logInferenceToFile: Boolean = false,
)

data class ObserveParams constructor(
    val context: ExecutionContext,
    /**
     * User provided additional system instructions
     * */
    val userProvidedInstructions: String? = null,
    val returnAction: Boolean = false,
    val resolve: Boolean = false,
    val logInferenceToFile: Boolean = false,
    val fromAct: Boolean = false,
) {
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
    private val auxLogDir: Path get() = AppPaths.detectAuxiliaryLogDir().resolve("agent")

    val domService: DomService
        get() = (session.getOrCreateBoundDriver() as? AbstractWebDriver)?.domService
            ?: throw IllegalStateException("Bound driver is not AbstractWebDriver")

    /**
     * Returns an ObjectNode with extracted fields expanded at top-level, plus:
     *   - metadata: { progress, completed }
     *   - prompt_tokens, completion_tokens, inferenceTimeMillis
     */
    suspend fun extract(params: ExtractParams): ObjectNode {
        val messages = AgentMessageList()

        // 1) Extraction call -----------------------------------------------------------------
        messages.addLast(promptBuilder.buildExtractSystemPrompt(params.userProvidedInstructions))
        messages.addUser(promptBuilder.buildExtractUserRequestPrompt(params), "user_request")
        messages.addLast(promptBuilder.buildExtractUserPrompt(params))

        val timestamp = AppPaths.fromNow()
        var callFile: Path? = null
        if (params.logInferenceToFile) {
            callFile = logModelCall(
                dirPrefix = "extractSummary",
                kind = "extractCall",
                timestamp = timestamp,
                requestId = params.requestId,
                modelCall = "extract",
                messages = messages.messages,
                enabled = true
            )
        }

        val extractStartTime = Instant.now()
        val extractResponse = cta.generateResponseRaw(messages)

        val extractedNode: ObjectNode = runCatching {
            mapper.readTree(extractResponse.content) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        var extractRespFile: Path
        if (params.logInferenceToFile) {
            extractRespFile = writeAuxLog(
                prefix = "extractSummary",
                kind = "extractResponse",
                timestamp = timestamp,
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to "extract",
                    "rawResponse" to safeJsonPreview(extractResponse.content),
                )
            )

            appendSummaryToFile(
                prefix = "extract",
                entry = mapOf(
                    "extractInferenceType" to "extract",
                    "timestamp" to timestamp,
                    "llmInputFile" to callFile,
                    "llmOutputFile" to extractRespFile,
                    "outputTokenCount" to extractResponse.tokenUsage.outputTokenCount,
                    "totalTokenCount" to extractResponse.tokenUsage.totalTokenCount,
                    "inferenceTimeMillis" to DateTimes.elapsedTime(extractStartTime).toMillis()
                )
            )
        }

        // 2) Metadata call -------------------------------------------------------------------
        val metadataMessages = AgentMessageList()
        val metadataSystem = promptBuilder.buildMetadataSystemPrompt()
        // For metadata, pass the extracted object directly
        val metadataUser = promptBuilder.buildMetadataUserPrompt(params.instruction, extractedNode, params.agentState)

        metadataMessages.addLast(metadataSystem)
        metadataMessages.addLast(metadataUser)

        var metadataCallFile: Path? = null
        if (params.logInferenceToFile) {
            metadataCallFile = logModelCall(
                dirPrefix = "extractSummary",
                kind = "metadataCall",
                timestamp = timestamp,
                requestId = params.requestId,
                modelCall = "metadata",
                messages = metadataMessages.messages,
                enabled = true
            )
        }

        val metadataStartTime = Instant.now()
        // BUGFIX: Use metadataMessages (not extraction messages) for metadata stage
        val metadataResponse = cta.generateResponseRaw(metadataMessages)

        val metaNode: ObjectNode = runCatching {
            mapper.readTree(metadataResponse.content) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }
        val progress = metaNode.path("progress").asText("")
        val completed = metaNode.path("completed").asBoolean(false)

        var metadataRespFile: Path
        if (params.logInferenceToFile) {
            metadataRespFile = writeAuxLog(
                prefix = "extractSummary",
                kind = "metadataResponse",
                timestamp = timestamp,
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to "metadata",
                    "completed" to completed,
                    "progress" to progress,
                )
            )

            appendSummaryToFile(
                prefix = "extract",
                entry = mapOf(
                    "extractInferenceType" to "metadata",
                    "timestamp" to timestamp,
                    "llmInputFile" to metadataCallFile,
                    "llmOutputFile" to metadataRespFile,
                    "inputTokenCount" to metadataResponse.tokenUsage.inputTokenCount,
                    "outputTokenCount" to metadataResponse.tokenUsage.outputTokenCount,
                    "totalTokenCount" to metadataResponse.tokenUsage.totalTokenCount,
                    "inferenceTimeMillis" to DateTimes.elapsedTime(metadataStartTime).toMillis()
                )
            )
        }

        val usage1 = extractResponse.tokenUsage
        // BUGFIX: usage2 should come from metadataResponse
        val usage2 = metadataResponse.tokenUsage
        val inputTokenCount = usage1.inputTokenCount + usage2.inputTokenCount
        val outputTokenCount = usage1.outputTokenCount + usage2.outputTokenCount
        val totalTokenCount = usage1.totalTokenCount + usage2.totalTokenCount

        val result: ObjectNode = (extractedNode.deepCopy()).apply {
            set<ObjectNode>("metadata", JsonNodeFactory.instance.objectNode().apply {
                put("progress", progress)
                put("completed", completed)
            })
            put("inputTokenCount", inputTokenCount)
            put("outputTokenCount", outputTokenCount)
            put("totalTokenCount", totalTokenCount)
            put("inferenceTimeMillis", DateTimes.elapsedTime(extractStartTime).toMillis())
        }

        return result
    }

    suspend fun observe(params: ObserveParams, context: ExecutionContext): ActionDescription {
        val messages = if (params.resolve) {
            promptBuilder.buildResolveMessageListAll(context)
        } else {
            promptBuilder.buildObserveMessageListAll(params, context)
        }

        val startTime = Instant.now()
        val prefix = if (params.fromAct) "act" else "observe"
        var callFile: Path? = null
        val timestamp = AppPaths.fromNow()
        if (params.logInferenceToFile) {
            callFile = logModelCall(
                dirPrefix = "${prefix}Summary",
                kind = "${prefix}Call",
                requestId = context.uuid,
                timestamp = timestamp,
                modelCall = prefix,
                messages = messages.messages,
                enabled = true
            )
        }

        val actionDescription = cta.generate(messages, context)
        // Minor typo fix: "Field" instead of "Filed"
        requireNotNull(context.agentState.actionDescription) { "Field should be set: context.agentState.actionDescription" }
        val modelResponse = actionDescription.modelResponse!!

        val tokenUsage = modelResponse.tokenUsage
        val responseContent = modelResponse.content

        var respFile: Path
        if (params.logInferenceToFile) {
            respFile = writeAuxLog(
                prefix = "${prefix}Summary",
                kind = "${prefix}Response",
                timestamp = timestamp,
                payload = mapOf(
                    "requestId" to context.uuid,
                    "modelResponse" to prefix,
                    "rawResponse" to safeJsonPreview(responseContent)
                )
            )

            appendSummaryToFile(
                prefix = prefix,
                entry = mapOf(
                    "${prefix}InferenceType" to prefix,
                    "timestamp" to timestamp,
                    "llmInputFile" to callFile,
                    "llmOutputFile" to respFile,
                    "inputTokenCount" to tokenUsage.inputTokenCount,
                    "outputTokenCount" to tokenUsage.outputTokenCount,
                    "totalTokenCount" to tokenUsage.totalTokenCount,
                    "inferenceTimeMillis" to DateTimes.elapsedTime(startTime).toMillis()
                )
            )
        }

        return actionDescription
    }

    suspend fun summary(instruction: String?, textContent: String): String {
        val messages = AgentMessageList()

        if (instruction.isNullOrBlank()) {
            messages.addUser("对下述文本给出一个总结。")
        } else {
            messages.addUser("根据用户指令，对下述文本给出一个总结。")
            messages.addUser("""<user_request>$instruction</user_request>""")
        }
        messages.addUser("\n\n$textContent\n\n".trimIndent())

        val response = cta.generateResponseRaw(messages)

        // TODO: count token usage

        return response.content
    }

    private fun safeJsonPreview(raw: String, limit: Int = 2000): String {
        return Strings.compactInline(raw, limit)
    }

    private fun writeAuxLog(
        prefix: String, kind: String, timestamp: String, payload: Any
    ): Path {
        val path = auxLogDir.resolve(prefix).resolve("${kind}_$timestamp.txt")
        val content = runCatching { prettyPulsarObjectMapper().writeValueAsString(payload) }
            .getOrNull() ?: payload
        MessageWriter.writeOnce(path, content)
        return path
    }

    private fun appendSummaryToFile(prefix: String, entry: Map<String, Any?>) {
        val file = auxLogDir.resolve("${prefix}_summary.json")
        val mapper = ObjectMapper()
        val current: ArrayNode = if (Files.exists(file)) {
            runCatching { mapper.readTree(Files.readAllBytes(file)) as? ArrayNode }
                .getOrNull() ?: JsonNodeFactory.instance.arrayNode()
        } else JsonNodeFactory.instance.arrayNode()
        val entryNode: JsonNode = mapper.valueToTree(entry)
        current.add(entryNode)
        Files.write(file, prettyPulsarObjectMapper().writeValueAsBytes(current))
    }

    // ------------------------------ Small utilities --------------------------------
    private fun logModelCall(
        dirPrefix: String,
        kind: String,
        requestId: String,
        timestamp: String,
        modelCall: String,
        messages: List<Any>,
        enabled: Boolean
    ): Path? {
        if (!enabled) return null

        return writeAuxLog(
            prefix = dirPrefix,
            kind = kind,
            timestamp = timestamp,
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
}
