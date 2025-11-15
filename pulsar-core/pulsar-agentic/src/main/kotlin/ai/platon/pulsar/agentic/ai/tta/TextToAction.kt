package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder.Companion.SINGLE_ACTION_GENERATION_PROMPT
import ai.platon.pulsar.agentic.ai.PromptBuilder.Companion.buildObserveResultSchema
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonElement
import java.nio.file.Files

open class TextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param actionDescriptions The action descriptions
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generateActions(
        actionDescriptions: String, driver: WebDriver, screenshotB64: String? = null
    ): List<ActionDescription> {
        require(driver is AbstractWebDriver)
        val domService = requireNotNull(driver.domService)

        val options = SnapshotOptions()
        val domState = domService.getDOMState(snapshotOptions = options)
        val browserUseState = domService.getBrowserUseState()
        val agentState = AgentState(0, "", browserUseState = browserUseState)
        val toolCallExpressions = SourceCodeToToolCallSpec.webDriverToolCallList.joinToString("\n") { it.expression }

        val promptTemplate = PromptTemplate(SINGLE_ACTION_GENERATION_PROMPT)
        val message = promptTemplate.render(
            mapOf(
                "ACTION_DESCRIPTIONS" to actionDescriptions,
                "TOOL_CALL_SPECIFICATION" to toolCallExpressions,
                "NANO_TREE_LAZY_JSON" to domState.nanoTreeLazyJson,
                "OUTPUT_SCHEMA_ACT" to buildObserveResultSchema(true),
            )
        )

        val messages = AgentMessageList()
        messages.addUser(message)

        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")
        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemMessage, userMessage)
        }

        val mapper = jacksonObjectMapper()
        val content = response.content
        val elements: ObserveResponseElements = mapper.readValue(content)

        return toActionDescription(actionDescriptions, elements, agentState, response).toActionDescriptions()
    }

    fun modelResponseToActionDescription(
        instruction: String,
        agentState: AgentState,
        modelResponse: ModelResponse
    ): ActionDescription {
        try {
            return modelResponseToActionDescription0(instruction, agentState, modelResponse)
        } catch (e: Exception) {
            logger.warn("Exception while parsing model response", e)
            return ActionDescription(instruction, modelResponse = modelResponse, exception = e)
        }
    }

    private fun modelResponseToActionDescription0(
        instruction: String,
        agentState: AgentState,
        modelResponse: ModelResponse
    ): ActionDescription {
        val modelResponse = reviseModelResponse(modelResponse)
        val content = modelResponse.content.trim()

        val contentStart = Strings.compactWhitespaces(content.take(30))

        val mapper = pulsarObjectMapper()
        return when {
            contentStart.contains("\"taskComplete\"") -> {
                val complete: ObserveResponseComplete = mapper.readValue(content)
                ActionDescription(
                    instruction = instruction,
                    isComplete = true,
                    errorCause = complete.errorCause,
                    summary = complete.summary,
                    nextSuggestions = complete.nextSuggestions ?: emptyList(),
                    modelResponse = modelResponse
                )
            }

            contentStart.contains("\"elements\"") -> {
                val elements: ObserveResponseElements = mapper.readValue(content)
                toActionDescription(instruction, elements, agentState, modelResponse)
            }

            else -> ActionDescription(instruction, modelResponse = modelResponse)
        }
    }

    private fun reviseModelResponse(modelResponse: ModelResponse): ModelResponse {
        var content = modelResponse.content.trim()

        val errorMessage = "非法响应，必须是合法 JSON 格式。客户端已经修正，未来需严格遵循格式。"
        var modelError: String? = null
        val heading20 = content.take(20)
        val tailing20 = content.takeLast(20)
        if (heading20.contains("<output_act>")) {
            content = content.replace("<output_act>", "")
            modelError = errorMessage
        }
        if (tailing20.contains("</output_act>")) {
            content = content.replace("</output_act>", "")
            modelError = errorMessage
        }
        if (tailing20.contains("<output_act>")) {
            content = content.replace("<output_act>", "")
            modelError = errorMessage
        }

        return if (modelError != null) {
            logger.info("""🖌️Model response revised""")
            modelResponse.copy(content = content, modelError = modelError)
        } else modelResponse
    }

    fun reviseActionDescription(action: ActionDescription): ActionDescription {
        requireNotNull(action.modelResponse) { "ModelResponse is required to reviseActionDescription" }

        if (action.exception != null) {
            return action
        }

        if (action.isComplete) {
            return action
        }

        // requireNotNull(action.agentState) { "Agent state has to be available" }
        val observeElements = action.observeElements?.map { reviseObserveElement(it, action) }
        return action.copy(observeElements = observeElements)
    }

    private fun reviseObserveElement(observeElement: ObserveElement, action: ActionDescription): ObserveElement {
        requireNotNull(action.modelResponse) { "ModelResponse is required to reviseObserveElement" }
        if (action.exception != null) {
            return observeElement
        }

        val agentState = requireNotNull(action.agentState) { "Agent state has to be available to reviseObserveElement" }
        val toolCall = observeElement.toolCall ?: return observeElement

        // 2. revise selector
        val domain = toolCall.domain
        val method = toolCall.method

        val locator = observeElement.locator
        val arguments = toolCall.arguments

        var node: DOMTreeNodeEx? = null
        if (!locator.isNullOrBlank()) {
            val fbnLocator = agentState.browserUseState.domState.getAbsoluteFBNLocator(locator)
            if (fbnLocator != null) {
                node = agentState.browserUseState.domState.locatorMap[fbnLocator]
                if ("selector" in arguments) {
                    // revise selector
                    arguments["selector"] = fbnLocator.absoluteSelector
                }
            }

            if (fbnLocator == null) {
                logger.warn("FBN locator not found. method={}, locator={}", method, locator)
            }
        }

        // CSS friendly expression
        val cssSelector = node?.cssSelector()
        val expression = toolCall.pseudoExpression
        val cssFriendlyExpression = if (locator != null && cssSelector != null) {
            expression.replace(locator, cssSelector)
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

        return revisedObserveElement
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


    companion object {

        val baseDir = AppPaths.get("tta")

        init {
            Files.createDirectories(baseDir)
        }

        fun toActionDescription(
            instruction: String,
            elements: ObserveResponseElements,
            agentState: AgentState,
            response: ModelResponse
        ): ActionDescription {
            val observeElements = elements.elements?.map { toObserveElement(it, response) } ?: emptyList()
            return ActionDescription(
                instruction,
                observeElements = observeElements,
                agentState = agentState,
                modelResponse = response
            )
        }

        fun toObserveElement(ele: ObserveResponseElement, response: ModelResponse): ObserveElement {
            val arguments = ele.arguments
                ?.mapNotNull { arg -> arg?.get("name") to arg?.get("value") }
                ?.filter { it.first != null && it.second != null }
                ?.associate { it.first!! to it.second!! }

            val observeElement = ObserveElement(
                locator = ele.locator?.removeSurrounding("[", "]"),

                screenshotContentSummary = ele.screenshotContentSummary,
                currentPageContentSummary = ele.currentPageContentSummary,
                evaluationPreviousGoal = ele.evaluationPreviousGoal,
                nextGoal = ele.nextGoal,

                toolCall = ToolCall(
                    domain = ele.domain ?: "",
                    method = ele.method ?: "",
                    arguments = arguments?.toMutableMap() ?: mutableMapOf(),
                ),

                modelResponse = response.content,
            )

            return observeElement
        }
    }

}
