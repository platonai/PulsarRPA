package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder.Companion.buildObserveResultSchema
import ai.platon.pulsar.agentic.ai.support.AgentTool.TOOL_CALL_SPECIFICATION
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.ai.llm.PromptTemplate
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.AgentState
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files

data class TextToActionParams(
    val messages: AgentMessageList,
    val agentState: AgentState,
    val screenshotB64: String? = null
)

open class TextToAction(
    val conf: ImmutableConfig
) {
    companion object {

        const val SINGLE_ACTION_GENERATION_PROMPT = """
根据动作描述和网页内容，选择最合适一个或多个工具。

## 动作描述

{{ACTION_DESCRIPTIONS}}

---

## 工具列表

{{TOOL_CALL_SPECIFICATION}}

---

## 网页内容

网页内容以无障碍树的形式呈现:

{{NANO_TREE_LAZY_JSON}}

---

## 输出

- 仅输出 JSON 内容，无多余文字

动作输出格式：
{{OUTPUT_SCHEMA_ACT}}

---

        """

        fun toActionDescription(ele: ObserveResponseElement, response: ModelResponse): ActionDescription {
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

                modelResponse = response,
            )

            return ActionDescription(observeElement = observeElement, modelResponse = response)
        }
    }

    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    init {
        Files.createDirectories(baseDir)
    }

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param actionDescriptions The action descriptions
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @ExperimentalApi
    open suspend fun generateActions(
        actionDescriptions: String,
        driver: WebDriver,
        screenshotB64: String? = null
    ): List<ActionDescription> {
        require(driver is AbstractWebDriver)
        val domService = requireNotNull(driver.domService)

        val options = SnapshotOptions()
        val domState = domService.getDOMState(snapshotOptions = options)

        val promptTemplate = PromptTemplate(SINGLE_ACTION_GENERATION_PROMPT)
        val message = promptTemplate.render(
            mapOf(
                "ACTION_DESCRIPTIONS" to actionDescriptions,
                "TOOL_CALL_SPECIFICATION" to TOOL_CALL_SPECIFICATION,
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
        return elements.elements?.map { toActionDescription(it, response) } ?: emptyList()
    }

    @Deprecated("Use contextToAction.generate() instead")
    @ExperimentalApi
    open suspend fun generate(params: TextToActionParams) = ContextToAction(conf).generate(params)

    @Deprecated("Use contextToAction.generate() instead")
    @ExperimentalApi
    open suspend fun generate(
        messages: AgentMessageList, agentState: AgentState, screenshotB64: String? = null
    ) = ContextToAction(conf).generate(messages, agentState, screenshotB64)

    /**
     * Generate EXACT ONE WebDriver action with interactive elements.
     *
     * @param instruction The action description with plain text
     * @param driver The driver to use to collect the context, such as interactive elements
     * @return The action description
     * */
    @Deprecated("Use contextToAction.generate() instead")
    @ExperimentalApi
    open suspend fun generate(
        instruction: String, agentState: AgentState, screenshotB64: String? = null
    ) = ContextToAction(conf).generate(instruction, agentState, screenshotB64)

    @Deprecated("Use contextToAction.generateResponse() instead")
    @ExperimentalApi
    open suspend fun generateResponse(
        instruction: String, agentState: AgentState, screenshotB64: String? = null, toolCallLimit: Int = 100,
    ) = ContextToAction(conf).generateResponse(instruction, agentState, screenshotB64, toolCallLimit)
}
