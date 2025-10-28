package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonElement
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files

open class TextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    init {
        Files.createDirectories(baseDir)
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The instruction
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generate(
        instruction: String,
        driver: WebDriver,
        screenshotB64: String? = null
    ): ActionDescription {
        require(driver is PulsarWebDriver) { "PulsarWebDriver is required to use agents" }
        val browserUseState = driver.domService.getBrowserUseState(snapshotOptions = SnapshotOptions())

        return generate(instruction, browserUseState, screenshotB64)
    }

    @ExperimentalApi
    open suspend fun generate(
        messages: AgentMessageList,
        browserUseState: BrowserUseState,
        screenshotB64: String? = null
    ): ActionDescription {
        try {
            val response = generateResponse(messages, browserUseState, screenshotB64, 1)

            val action = modelResponseToActionDescription(response)

            return reviseActionDescription(action, browserUseState)
        } catch (e: Exception) {
            val errorResponse = ModelResponse(
                "Unknown exception" + e.brief(), ResponseState.OTHER
            )
            return ActionDescription(modelResponse = errorResponse)
        }
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generate(
        instruction: String,
        browserUseState: BrowserUseState,
        screenshotB64: String? = null
    ): ActionDescription {
        try {
            return generateWithToolCallSpecs(instruction, browserUseState, screenshotB64, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorResponse = ModelResponse(
                "Unknown exception" + e.brief(), ResponseState.OTHER
            )
            return ActionDescription(modelResponse = errorResponse)
        }
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        instruction: String, browserUseState: BrowserUseState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ModelResponse {
        val messages = AgentMessageList()
        messages.add("user", instruction)
        return generateResponse(messages, browserUseState, screenshotB64, toolCallLimit)
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        messages: AgentMessageList,
        browserUseState: BrowserUseState,
        screenshotB64: String? = null,
        toolCallLimit: Int = 100,
    ): ModelResponse {
        var overallGoal: String? = messages.find("overallGoal")?.content
        overallGoal = StringUtils.substringBetween(overallGoal, "<overallGoal>", "</overallGoal>")
        val params = ObserveParams(
            overallGoal ?: "",
            browserUseState = browserUseState,
            returnAction = true,
            logInferenceToFile = true
        )

        PromptBuilder().buildObserveUserMessage(messages, params)

        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")
        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemMessage, userMessage)
        }

        return response
    }

    @ExperimentalApi
    private suspend fun generateWithToolCallSpecs(
        instruction: String, browserUseState: BrowserUseState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ActionDescription {
        val response = generateResponse(instruction, browserUseState, screenshotB64, toolCallLimit)

        return reviseActionDescription(modelResponseToActionDescription(response), browserUseState)
    }

    fun parseObserveElements1(root: JsonNode, returnAction: Boolean): List<ObserveElement> {
        if (root.isEmpty) {
            return emptyList()
        }

        var node = root
        if (node.has("elements")) {
            node = node.get("elements")
        }
        return pulsarObjectMapper().readerForArrayOf(ObserveElement::class.java).readValue(node)
    }

    // ----------------------------------- Helpers -----------------------------------
    /**
     * Observe elements schema:
     * ```json
     *  { "elements": [ { "selector": string, "description": string, "method": string, "arguments": [{"name": string, "value": string}] } ] }
     * ```
     *
     * This schema is build by [PromptBuilder.buildObserveResultSchemaContract] and is passed to LLM for a restricted response.
     * */
    fun parseObserveElementsLegacy(root: JsonNode, returnAction: Boolean): List<ObserveElement> {
        // Determine the array of items to read
        val arr: ArrayNode = when {
            root.isObject && root.has("elements") && root.get("elements").isArray -> root.get("elements") as ArrayNode
            root.isArray -> root as ArrayNode
            root.isObject -> {
                // Single element object fallback
                val single = root as ObjectNode
                val tmp = JsonNodeFactory.instance.arrayNode()
                tmp.add(single)
                tmp
            }

            else -> JsonNodeFactory.instance.arrayNode()
        }

        val result = mutableListOf<ObserveElement>()
        for (i in 0 until arr.size()) {
            val el: JsonNode = arr.get(i)
            // Support both "locator" and a tolerant fallback "selector"
            val locator = el.path("locator").asText(null)
            val description = el.path("description").asText("")

            // Parse domain + method, allowing combined form like "browser.switchTab"
            val domainField = el.path("domain").asText(null)
            val methodField = el.path("method").asText(null)
            var domain: String? = domainField
            var methodName: String? = methodField
            if (!methodField.isNullOrBlank() && methodField.contains('.')) {
                val parts = methodField.split('.', limit = 2)
                if (parts.size == 2) {
                    if (domain.isNullOrBlank()) domain = parts[0]
                    methodName = parts[1]
                }
            }

            // Parse arguments according to schema: array of { name, value } or tolerant object map
            val argsNode = el.get("tool")?.get("arguments")
            val arguments: Map<String, String?>? = when {
                argsNode == null || argsNode.isNull -> null
                argsNode.isArray -> {
                    val m = linkedMapOf<String, String>()
                    for (j in 0 until argsNode.size()) {
                        val item = argsNode.get(j)
                        val name = item.path("name").asText(null)
                        // Accept both "value" and fallback to text of node if needed
                        val value = item.path("value").asText(null)
                        if (!name.isNullOrBlank()) {
                            m[name] = value ?: ""
                        }
                    }
                    if (m.isEmpty()) null else m
                }

                argsNode.isObject -> {
                    // Be tolerant: support either a single {name,value} object or a map-like object
                    if (argsNode.has("name") || argsNode.has("value")) {
                        val name = argsNode.path("name").asText(null)
                        val value = argsNode.path("value").asText(null)
                        if (!name.isNullOrBlank()) mapOf(name to (value ?: "")) else null
                    } else {
                        val m = linkedMapOf<String, String>()
                        val fields = argsNode.fields()
                        while (fields.hasNext()) {
                            val entry = fields.next()
                            m[entry.key] = entry.value.asText("")
                        }
                        if (m.isEmpty()) null else m
                    }
                }

                else -> null
            }

            // Additional fields per ACTION_SCHEMA
            val currentPageContentSummary = el.path("currentPageContentSummary").asText(null)
            val actualLastActionImpact = el.path("actualLastActionImpact").asText(null)
            val expectedNextActionImpact = el.path("expectedNextActionImpact").asText(null)

            var toolCall: ToolCall? = null
            if (domain != null && methodName != null) {
                toolCall = ToolCall(
                    domain = domain,
                    method = methodName,
                    description = description,
                )
                arguments?.forEach { (key, value) -> toolCall.arguments[key] = value?.toString() }
            }

            val item = ObserveElement(
                locator = locator,
                toolCall = toolCall,
                currentPageContentSummary = currentPageContentSummary,
                actualLastActionImpact = actualLastActionImpact,
                expectedNextActionImpact = expectedNextActionImpact,
            )

            result.add(item)
        }

        return result
    }

    fun modelResponseToActionDescription(response: ModelResponse): ActionDescription {
        val content = response.content
        val contentStart = Strings.compactWhitespaces(content.take(10))

        val mapper = pulsarObjectMapper()
        return when {
            contentStart.contains("\"taskComplete\"") -> {
                val complete: ObserveResponseComplete = mapper.readValue(content)
                ActionDescription(
                    isComplete = true,
                    summary = complete.summary,
                    nextSuggestions = complete.nextSuggestions ?: emptyList()
                )
            }
            contentStart.contains("\"elements\"") -> {
                val elements: ObserveResponseElements = mapper.readValue(content)
                when (val ele = elements.elements?.firstOrNull()) {
                    null -> ActionDescription(modelResponse = response)
                    else -> {
                        val observeElement = ObserveElement(
                            locator = ele.locator,

                            currentPageContentSummary = ele.currentPageContentSummary,
                            actualLastActionImpact = ele.actualLastActionImpact,
                            expectedNextActionImpact = ele.expectedNextActionImpact,

                            toolCall = ToolCall(
                                domain = ele.domain ?: "",
                                method = ele.method ?: "",
                                arguments = ele.arguments?.flatMap { it?.entries ?: emptyList() }
                                    ?.associate { it.toPair() }?.toMutableMap()
                                    ?: mutableMapOf(),
                            ),

                            modelResponse = response,
                        )

                        ActionDescription(observeElement = observeElement, modelResponse = response)
                    }
                }
            }
            else -> ActionDescription(modelResponse = response)
        }
    }

    fun reviseActionDescription(action: ActionDescription, browserUseState: BrowserUseState): ActionDescription {
        val observeElement = action.observeElement ?: return action
        val toolCall = observeElement.toolCall ?: return action

        val locator = observeElement.locator
        val arguments = observeElement.toolCall?.arguments
        val fbnLocator = browserUseState.domState.getAbsoluteFBNLocator(locator)
        val node = if (fbnLocator != null) browserUseState.domState.locatorMap[fbnLocator] else null
        val fbnSelector = fbnLocator?.absoluteSelector

        if (arguments != null && arguments.contains("selector")) {
            arguments["selector"] = fbnSelector
        }

        // CSS friendly expression
        val cssSelector = node?.cssSelector()
        val expression = ToolCallExecutor.toolCallToExpression(toolCall)
        val cssFriendlyExpression = if (locator != null && cssSelector != null) {
            expression?.replace(locator, cssSelector)
        } else null

        val revisedObserveElement = observeElement.copy(
            node = node,
            backendNodeId = node?.backendNodeId,
            cssSelector = cssSelector,
            expressions = expression?.let { listOf(it) } ?: emptyList(),
            cssFriendlyExpressions = cssFriendlyExpression?.let { listOf(it) } ?: emptyList(),
        )

        return action.copy(
            observeElement = revisedObserveElement
        )
    }

    private fun jsonElementToKotlin(e: JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val num = p.asNumber
                    val d = num.toDouble()
                    val i = num.toInt()
                    if (d == i.toDouble()) i else d
                }

                else -> p.asString
            }
        }

        e.isJsonArray -> e.asJsonArray.map { jsonElementToKotlin(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().associate { it.key to jsonElementToKotlin(it.value) }
        else -> null
    }
}
