package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.agentic.ai.AgentMessageList
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
import java.nio.file.Files

open class ContextToAction(
    val conf: ImmutableConfig
) {
    private val logger = getLogger(this)

    val baseDir = AppPaths.get("tta")

    val chatModel: BrowserChatModel get() = ChatModelFactory.getOrCreate(conf)

    val tta = TextToAction(conf)

    init {
        Files.createDirectories(baseDir)
    }

    @ExperimentalApi
    open suspend fun generate(messages: AgentMessageList, context: ExecutionContext): ActionDescription {
        try {
            val instruction = context.instruction

            val response = generateResponseRaw(messages, context.screenshotB64)

            val actionDescription = tta.modelResponseToActionDescription(instruction, context.agentState, response)

            return tta.reviseActionDescription(actionDescription)
        } catch (e: Exception) {
            val errorResponse = ModelResponse("Unknown exception" + e.brief(), ResponseState.OTHER)
            return ActionDescription(
                context.instruction,
                exception = e,
                modelResponse = errorResponse,
                context = context
            )
        }
    }

    @ExperimentalApi
    open suspend fun generateResponseRaw(messages: AgentMessageList, screenshotB64: String? = null): ModelResponse {
        val systemMessage = messages.systemMessages().joinToString("\n")
        val userMessage = messages.userMessages().joinToString("\n")

        val response = if (screenshotB64 != null) {
            chatModel.call(systemMessage, userMessage, null, screenshotB64, "image/jpeg")
        } else {
            chatModel.call(systemMessage, userMessage)
        }

        return response
    }
}
