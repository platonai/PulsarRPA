package ai.platon.pulsar.external

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.impl.ChatModelImpl
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.zhipu.ZhipuAiChatModel
import java.util.concurrent.ConcurrentHashMap

/**
 * The factory for creating models.
 */
object ChatModelFactory {
    private val models = ConcurrentHashMap<String, ChatModel>()
    
    fun isModelConfigured(conf: ImmutableConfig): Boolean {
        val llm = conf["llm.name"]
        val apiKey = conf["llm.apiKey"]
        
        return llm != null && apiKey != null
    }
    
    /**
     * Create a default model.
     *
     * @return The created model.
     */
    fun getOrCreate(conf: ImmutableConfig): ChatModel {
        val llm = conf["llm.name"] ?: throw IllegalArgumentException("llm.name is not set")
        val apiKey = conf["llm.apiKey"] ?: throw IllegalArgumentException("llm.apiKey is not set")
        return getOrCreate(llm, apiKey)
    }
    
    /**
     * Create a model.
     *
     * @param model The name of model to create.
     * @param apiKey The API key to use.
     * @return The created model.
     */
    fun getOrCreate(model: String, apiKey: String): ChatModel {
        return getOrCreateModel0(model, apiKey)
    }
    
    /**
     * Create a default model.
     *
     * @return The created model.
     */
    fun getOrCreateOrNull(conf: ImmutableConfig): ChatModel? {
        val llm = conf["llm.name"] ?: return null
        val apiKey = conf["llm.apiKey"] ?: return null
        return getOrCreate(llm, apiKey)
    }

    private fun getOrCreateModel0(model: String, apiKey: String): ChatModel {
        val key = "$model:$apiKey"
        return models.computeIfAbsent(key) { doCreateModel(model, apiKey) }
    }
    
    private fun doCreateModel(model: String, apiKey: String): ChatModel {
        return when (model) {
            "glm4" -> createZhipuChatModel(apiKey)
            "deepseek" -> createDeepSeekChatModel(apiKey)
            else -> createDeepSeekChatModel(apiKey)
        }
    }
    
    private fun createZhipuChatModel(apiKey: String): ChatModel {
        val lm = ZhipuAiChatModel.builder()
            .apiKey(apiKey)
            .logRequests(true)
            .logResponses(true)
            .maxRetries(1)
            .build()
        return ChatModelImpl(lm)
    }

    /**
     * DeepSeek API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     *
     * @see https://github.com/deepseek-ai/DeepSeek-V2/issues/18
     * */
    private fun createDeepSeekChatModel(apiKey: String): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://api.deepseek.com")
            .modelName("deepseek-chat")
            .logRequests(false)
            .logResponses(true)
            .maxRetries(1)
            .build()
        return ChatModelImpl(lm)
    }
}
