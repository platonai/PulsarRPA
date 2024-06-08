package ai.platon.pulsar.crawl.ai

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.ai.api.ModelResponse
import ai.platon.pulsar.common.ai.api.ResponseState
import ai.platon.pulsar.common.ai.api.TokenUsage
import ai.platon.pulsar.dom.FeaturedDocument
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.FinishReason
import dev.langchain4j.model.zhipu.ZhipuAiChatModel
import org.jsoup.nodes.Element

class ChatModel(
    val conf: ImmutableConfig
) {
    private val apiKey = conf["EXTERNAL_AI_API_KEY"] ?: conf["ZHIPU_API_KEY"]
    private val chatModel: ZhipuAiChatModel = ZhipuAiChatModel.builder()
        .apiKey(apiKey)
        .logRequests(true)
        .logResponses(true)
        .maxRetries(1)
        .build()

    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param content The text context.
     * @return The response generated by the model.
     */
    fun call(content: String, prompt: String): ModelResponse {
        val prompt1 = prompt + "\n\n" + content
        val message = UserMessage.userMessage(prompt1)
        val response = chatModel.generate(message)
        
        val u = response.tokenUsage()
        val tokenUsage = TokenUsage(u.inputTokenCount(), u.outputTokenCount(), u.totalTokenCount())
        val r = response.finishReason()
        val state = when(r) {
            FinishReason.STOP -> ResponseState.STOP
            FinishReason.LENGTH -> ResponseState.LENGTH
            FinishReason.TOOL_EXECUTION -> ResponseState.TOOL_EXECUTION
            FinishReason.CONTENT_FILTER -> ResponseState.CONTENT_FILTER
            else -> ResponseState.OTHER
        }
        return ModelResponse(response.content().text(), state, tokenUsage)
    }
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param document An array of messages.
     * @return The response generated by the model.
     */
    fun call(document: FeaturedDocument, prompt: String): ModelResponse {
        return call(document.document, prompt)
    }
    
    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param ele The Element to ask.
     * @return The response generated by the model.
     */
    fun call(ele: Element, prompt: String): ModelResponse {
        val text = ele.text()
        return call(text, prompt)
    }
}