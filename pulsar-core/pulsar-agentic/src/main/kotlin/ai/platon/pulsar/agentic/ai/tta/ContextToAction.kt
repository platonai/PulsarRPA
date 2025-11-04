package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.support.AgentTool.TOOL_ALIASES
import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.AgentState
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonElement
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files

open class ContextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    init {
        Files.createDirectories(baseDir)
    }

    @ExperimentalApi
    open suspend fun generate(params: TextToActionParams): ActionDescription {
        return generate(params.messages, params.agentState, params.screenshotB64)
    }

    @ExperimentalApi
    open suspend fun generate(
        messages: AgentMessageList, agentState: AgentState, screenshotB64: String? = null
    ): ActionDescription {
        try {
            val response = generateResponse(messages, agentState, screenshotB64, 1)

            val action = modelResponseToActionDescription(response)

            return reviseActionDescription(action, agentState.browserUseState!!)
        } catch (e: Exception) {
            val errorResponse = ModelResponse("Unknown exception" + e.brief(), ResponseState.OTHER)
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
        instruction: String, agentState: AgentState, screenshotB64: String? = null
    ): ActionDescription {
        try {
            return generateWithToolCallSpecs(instruction, agentState, screenshotB64, 1)
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
        instruction: String, agentState: AgentState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ): ModelResponse {
        val messages = AgentMessageList()
        messages.addLast("user", instruction, "user_request")
        return generateResponse(messages, agentState, screenshotB64, toolCallLimit)
    }

    @ExperimentalApi
    open suspend fun generateResponse(
        messages: AgentMessageList,
        agentState: AgentState,
        screenshotB64: String? = null,
        toolCallLimit: Int = 100,
    ): ModelResponse {
        var userRequest: String? = messages.find("user_request")?.content
        requireNotNull(userRequest) { "user_request not found in message list: $messages" }
        if (userRequest.contains("<user_request>")) {
            userRequest = StringUtils.substringBetween(userRequest, "<user_request>", "</user_request>")
        }

        val params = ObserveParams(
            userRequest ?: "",
            agentState = agentState,
            browserUseState = agentState.browserUseState!!,
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
        instruction: String,
        agentState: AgentState,
        screenshotB64: String? = null,
        toolCallLimit: Int = 100,
    ): ActionDescription {
        val response = generateResponse(instruction, agentState, screenshotB64, toolCallLimit)

        return reviseActionDescription(modelResponseToActionDescription(response), agentState.browserUseState!!)
    }

    fun modelResponseToActionDescription(response: ModelResponse): ActionDescription {
        try {
            return modelResponseToActionDescription0(response)
        } catch (e: Exception) {
            logger.warn("Exception while parsing model response", e)
            return ActionDescription(modelResponse = response, errors = e.brief())
        }
    }

    private fun modelResponseToActionDescription0(response: ModelResponse): ActionDescription {
        val content = response.content
        val contentStart = Strings.compactWhitespaces(content.take(30))

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
                elements.elements?.map { TextToAction.toActionDescription(it, response) }?.firstOrNull()
                    ?: ActionDescription(modelResponse = response)
            }

            else -> ActionDescription(modelResponse = response)
        }
    }

    fun reviseActionDescription(action: ActionDescription, browserUseState: BrowserUseState): ActionDescription {
        val observeElement = action.observeElement ?: return action
        var toolCall = observeElement.toolCall ?: return action

        // 1. revised tool call
        val domain = toolCall.domain
        val method = toolCall.method
        val revised = TOOL_ALIASES["$domain.$method"]
        if (revised != null) {
            val (domain2, method2) = revised.split(".")
            toolCall = toolCall.copy(domain = domain2, method = method2)
        }

        // 2. revise selector
        val locator = observeElement.locator
        val arguments = toolCall.arguments
        val fbnLocator = browserUseState.domState.getAbsoluteFBNLocator(locator)
        if (!locator.isNullOrBlank() && fbnLocator == null) {
            logger.warn("FBN locator not found | {}", locator)
        }

        val node = if (fbnLocator != null) {
            browserUseState.domState.locatorMap[fbnLocator]
        } else null

        val fbnSelector = fbnLocator?.absoluteSelector

        arguments["selector"] = fbnSelector

        // CSS friendly expression
        val cssSelector = node?.cssSelector()
        val expression = ToolCallExecutor.toolCallToExpression(toolCall)
        val cssFriendlyExpression = if (locator != null && cssSelector != null) {
            expression?.replace(locator, cssSelector)
        } else null

        // 3. copy new object
        val revisedObserveElement = observeElement.copy(
            node = node,
            backendNodeId = node?.backendNodeId,
            toolCall = toolCall,
            cssSelector = cssSelector,
            expression = expression,
            cssFriendlyExpression = cssFriendlyExpression,
        )

        return action.copy(observeElement = revisedObserveElement)
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
