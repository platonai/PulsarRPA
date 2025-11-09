package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
import ai.platon.pulsar.agentic.ai.PromptBuilder
import ai.platon.pulsar.agentic.ai.agent.ObserveParams
import ai.platon.pulsar.agentic.ai.agent.detail.ExecutionContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ExperimentalApi
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.AgentState
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files

open class ContextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    val tta = TextToAction(conf)

    val promptBuilder = PromptBuilder()

    init {
        Files.createDirectories(baseDir)
    }

    @ExperimentalApi
    open suspend fun generate(messages: AgentMessageList, context: ExecutionContext): ActionDescription {
        return generate(messages, context.agentState, context.screenshotB64)
    }

    @ExperimentalApi
    open suspend fun generate(
        messages: AgentMessageList, agentState: AgentState, screenshotB64: String? = null
    ): ActionDescription {
        try {
            val instruction = agentState.instruction

            val response = generateResponse(messages, agentState, screenshotB64, 1)

            val actionDescription = tta.modelResponseToActionDescription(instruction, agentState, response)

            return tta.reviseActionDescription(actionDescription)
        } catch (e: Exception) {
            val errorResponse = ModelResponse("Unknown exception" + e.brief(), ResponseState.OTHER)
            return ActionDescription(agentState.instruction, modelResponse = errorResponse, exception = e)
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
            return ActionDescription(agentState.instruction, modelResponse = errorResponse)
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
            returnAction = true,
            logInferenceToFile = true
        )

        promptBuilder.buildObserveUserMessage(messages, params)

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
        val actionDescription = tta.modelResponseToActionDescription(instruction, agentState, response)
        return tta.reviseActionDescription(actionDescription)
    }
}
