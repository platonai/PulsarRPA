package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.impl.BrowserChatModelImpl
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.response.ChatResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// ----------------------------------- Data models -----------------------------------
data class LLMUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
)

data class Metadata(val progress: String = "", val completed: Boolean = false)

data class ObserveElement(
    val elementId: String,
    val description: String,
    val method: String? = null,
    val arguments: List<String>? = null,
)

data class InternalObserveResult(
    val elements: List<ObserveElement>,
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val inference_time_ms: Long,
)

data class ExtractParams(
    val instruction: String,
    val domElements: List<String>,
    /** JSON Schema string describing the desired extraction output */
    val schema: String,
    val chunksSeen: Int = 0,
    val chunksTotal: Int = 0,
    val requestId: String = UUID.randomUUID().toString(),
    val userProvidedInstructions: String? = null,
    val logInferenceToFile: Boolean = false,
)

data class ObserveParams(
    val instruction: String,
    val domElements: List<String>,
    val requestId: String = UUID.randomUUID().toString(),
    val userProvidedInstructions: String? = null,
    val returnAction: Boolean = false,
    val logInferenceToFile: Boolean = false,
    val fromAct: Boolean = false,
)

class InferenceEngine(
    private val driver: WebDriver,
    private val chatModel: BrowserChatModel
) {
    private val domService: DomService = (driver as AbstractWebDriver).domService!!
    private val langchainModel get() = (chatModel as BrowserChatModelImpl).langchainModel

    // ----------------------------------- Public simple stubs (kept for compatibility) -----------------------------------
    fun extract(instruction: String) {
        // Fetch trees for potential future use; current overloads provide richer interfaces
        domService.getAllTrees(PageTarget(), SnapshotOptions())
    }

    fun extract(instruction: String, domElements: List<String>) {
        // Intentionally left as lightweight wrapper; prefer using the structured extract(params) API below
        // ...existing code...
    }

    fun observe(instruction: String, domElements: List<String>) {
        // Intentionally left as lightweight wrapper; prefer using the structured observe(params) API below
        // ...existing code...
    }

    /**
     * Returns an ObjectNode with extracted fields expanded at top-level, plus:
     *   - metadata: { progress, completed }
     *   - prompt_tokens, completion_tokens, inference_time_ms
     */
    suspend fun extract(params: ExtractParams): ObjectNode {
        val mapper = ObjectMapper()
        val domText = params.domElements.joinToString("\n\n")
        val isGPT5 = (System.getProperty("llm.name") ?: "").lowercase().contains("gpt-5")
        val temperature = if (isGPT5) 1.0 else 0.1

        // 1) Extraction call -----------------------------------------------------------------
        val extractSystem = buildExtractSystemPrompt(false, params.userProvidedInstructions)
        val extractUser = buildExtractUserPrompt(
            params.instruction,
            // Inject schema hint to strongly guide JSON output
            buildString {
                append(domText)
                append("\n\n")
                append("You MUST respond with a valid JSON object that strictly conforms to the following JSON Schema. ")
                append("Do not include any extra commentary.\n")
                append("JSON Schema:\n")
                append(params.schema)
            },
            false,
        )

        val extractMessages = listOf(extractSystem, extractUser)
        var extractCallFile = ""
        var extractCallTs = ""
        if (params.logInferenceToFile) {
            val (file, ts) = writeTimestampedTxtFile(
                prefix = "extract_summary",
                kind = "extract_call",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelCall" to "extract",
                    "messages" to extractMessages,
                )
            )
            extractCallFile = file; extractCallTs = ts
        }

        val t0 = System.currentTimeMillis()
        val extractResp: ChatResponse = langchainModel.chat(
            SystemMessage.systemMessage(extractSystem.content.toString()),
            UserMessage.userMessage(extractUser.content.toString())
        )
        val t1 = System.currentTimeMillis()

        val extractText = extractResp.aiMessage().text().trim()
        val usage1 = LLMUsage(
            prompt_tokens = extractResp.tokenUsage().inputTokenCount(),
            completion_tokens = extractResp.tokenUsage().outputTokenCount(),
        )
        var extractedNode: ObjectNode = runCatching {
            mapper.readTree(extractText) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        }.getOrElse { JsonNodeFactory.instance.objectNode() }

        var extractRespFile = ""
        if (params.logInferenceToFile) {
            extractRespFile = writeTimestampedTxtFile(
                prefix = "extract_summary",
                kind = "extract_response",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to "extract",
                    "rawResponse" to safeJsonPreview(extractText)
                )
            ).first

            appendSummary(
                prefix = "extract",
                entry = mapOf(
                    "extract_inference_type" to "extract",
                    "timestamp" to extractCallTs,
                    "LLM_input_file" to extractCallFile,
                    "LLM_output_file" to extractRespFile,
                    "prompt_tokens" to usage1.prompt_tokens,
                    "completion_tokens" to usage1.completion_tokens,
                    "inference_time_ms" to (t1 - t0)
                )
            )
        }

        // 2) Metadata call -------------------------------------------------------------------
        val metadataSystem = buildMetadataSystemPrompt()
        // For metadata, pass the extracted object directly
        val metadataUser = buildMetadataPrompt(
            params.instruction,
            extractedNode,
            params.chunksSeen,
            params.chunksTotal
        )

        var metadataCallFile = ""
        var metadataCallTs = ""
        if (params.logInferenceToFile) {
            val (file, ts) = writeTimestampedTxtFile(
                prefix = "extract_summary",
                kind = "metadata_call",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelCall" to "metadata",
                    "messages" to listOf(metadataSystem, metadataUser),
                )
            )
            metadataCallFile = file; metadataCallTs = ts
        }

        val t2 = System.currentTimeMillis()
        val metadataResp: ChatResponse = langchainModel.chat(
            SystemMessage.systemMessage(metadataSystem.content.toString()),
            UserMessage.userMessage(metadataUser.content.toString())
        )
        val t3 = System.currentTimeMillis()
        val usage2 = LLMUsage(
            prompt_tokens = metadataResp.tokenUsage().inputTokenCount(),
            completion_tokens = metadataResp.tokenUsage().outputTokenCount(),
        )

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
                    "prompt_tokens" to usage2.prompt_tokens,
                    "completion_tokens" to usage2.completion_tokens,
                    "inference_time_ms" to (t3 - t2)
                )
            )
        }

        // 3) Merge & return ------------------------------------------------------------------
        val totalPrompt = (usage1.prompt_tokens) + (usage2.prompt_tokens)
        val totalCompletion = (usage1.completion_tokens) + (usage2.completion_tokens)
        val totalTime = (t1 - t0) + (t3 - t2)

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

    fun observe(params: ObserveParams): InternalObserveResult {
        val domText = params.domElements.joinToString("\n\n")
        val isGPT5 = (System.getProperty("llm.name") ?: "").lowercase().contains("gpt-5")
        val temperature = if (isGPT5) 1.0 else 0.1

        // Build dynamic schema hint for the LLM (prompt-enforced)
        val schemaHint = buildString {
            append("You MUST respond with a valid JSON object matching this schema: { \"elements\": [ { \"elementId\": string, \"description\": string")
            if (params.returnAction) {
                append(", \"method\": string, \"arguments\": [string]")
            }
            append(" } ] } . The elementId must follow the 'number-number' format and MUST NOT include square brackets. Do not include any extra text.")
        }

        val systemMsg = buildObserveSystemPrompt(params.userProvidedInstructions)
        val userMsg = buildObserveUserMessage(
            params.instruction,
            domText + "\n\n" + schemaHint
        )

        val prefix = if (params.fromAct) "act" else "observe"
        var callFile = "";
        var callTs = ""
        if (params.logInferenceToFile) {
            val (f, ts) = writeTimestampedTxtFile(
                prefix = "${prefix}_summary",
                kind = "${prefix}_call",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelCall" to prefix,
                    "messages" to listOf(systemMsg, userMsg)
                )
            )
            callFile = f; callTs = ts
        }

        val t0 = System.currentTimeMillis()
        val resp: ChatResponse = langchainModel.chat(
            SystemMessage.systemMessage(systemMsg.content.toString()),
            UserMessage.userMessage(userMsg.content.toString())
        )
        val t1 = System.currentTimeMillis()
        val usage = LLMUsage(
            prompt_tokens = resp.tokenUsage().inputTokenCount(),
            completion_tokens = resp.tokenUsage().outputTokenCount(),
        )

        val raw = resp.aiMessage().text().trim()
        val mapper = ObjectMapper()
        val node: ObjectNode =
            runCatching { mapper.readTree(raw) as? ObjectNode ?: JsonNodeFactory.instance.objectNode() }
                .getOrElse { JsonNodeFactory.instance.objectNode() }

        val elements: List<ObserveElement> = parseObserveElements(node, params.returnAction)

        var respFile = ""
        if (params.logInferenceToFile) {
            respFile = writeTimestampedTxtFile(
                prefix = "${prefix}_summary",
                kind = "${prefix}_response",
                payload = mapOf(
                    "requestId" to params.requestId,
                    "modelResponse" to prefix,
                    "rawResponse" to safeJsonPreview(raw)
                )
            ).first

            appendSummary(
                prefix = prefix,
                entry = mapOf(
                    "${prefix}_inference_type" to prefix,
                    "timestamp" to callTs,
                    "LLM_input_file" to callFile,
                    "LLM_output_file" to respFile,
                    "prompt_tokens" to usage.prompt_tokens,
                    "completion_tokens" to usage.completion_tokens,
                    "inference_time_ms" to (t1 - t0)
                )
            )
        }

        return InternalObserveResult(
            elements = elements,
            prompt_tokens = usage.prompt_tokens,
            completion_tokens = usage.completion_tokens,
            inference_time_ms = (t1 - t0)
        )
    }

    // ----------------------------------- Helpers -----------------------------------
    private fun parseObserveElements(root: ObjectNode, returnAction: Boolean): List<ObserveElement> {
        val arr: ArrayNode = root.path("elements") as? ArrayNode ?: JsonNodeFactory.instance.arrayNode()
        val result = mutableListOf<ObserveElement>()
        for (i in 0 until arr.size()) {
            val el: JsonNode = arr.get(i)
            var id = el.path("elementId").asText("")
            // Normalize: strip surrounding brackets if present
            if (id.startsWith("[") && id.endsWith("]") && id.length > 2) {
                id = id.substring(1, id.length - 1)
            }
            val desc = el.path("description").asText("")
            val baseMethod = el.path("method").asText(null)
            val argsNode = el.path("arguments")
            val args: List<String>? = if (argsNode.isArray) {
                argsNode.map { it.asText("") }
            } else null

            val item = if (returnAction) {
                ObserveElement(
                    elementId = id,
                    description = desc,
                    method = baseMethod ?: "",
                    arguments = args ?: emptyList(),
                )
            } else {
                ObserveElement(
                    elementId = id,
                    description = desc,
                    method = null,
                    arguments = null,
                )
            }
            result.add(item)
        }
        return result
    }

    private fun safeJsonPreview(raw: String, limit: Int = 2000): String {
        // Bound file payload length for safety
        return if (raw.length > limit) raw.substring(0, limit) + "...<truncated>" else raw
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
}
