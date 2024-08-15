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
        val provider = conf["llm.provider"] ?: throw IllegalArgumentException("llm.provider is not set")
        val modelName = conf["llm.name"] ?: throw IllegalArgumentException("llm.name is not set")
        val apiKey = conf["llm.apiKey"] ?: throw IllegalArgumentException("llm.apiKey is not set")
        return getOrCreate(provider, modelName, apiKey)
    }
    
    /**
     * Create a model.
     *
     * @param provider The provider of the model.
     * @param modelName The name of model to create.
     * @param apiKey The API key to use.
     * @return The created model.
     */
    fun getOrCreate(provider: String, modelName: String, apiKey: String) = getOrCreateModel0(provider, modelName, apiKey)
    
    /**
     * Create a default model.
     *
     * @return The created model.
     */
    fun getOrCreateOrNull(conf: ImmutableConfig) = getOrCreate(conf)

    private fun getOrCreateModel0(provider: String, modelName: String, apiKey: String): ChatModel {
        val key = "$modelName:$apiKey"
        return models.computeIfAbsent(key) { doCreateModel(provider, modelName, apiKey) }
    }

    private fun doCreateModel(provider: String, modelName: String, apiKey: String): ChatModel {
        return when (provider) {
            "zhipu" -> createZhipuChatModel(apiKey)
            "bailian" -> createBaiLianChatModel(modelName, apiKey)
            "deepseek" -> createDeepSeekChatModel(modelName, apiKey)
            else -> createDeepSeekChatModel(modelName, apiKey)
        }
    }

    /**
     * QianWen API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     * @see <a href='https://help.aliyun.com/zh/model-studio/getting-started/what-is-model-studio'>What is Model Studio</a>
     * */
    private fun createBaiLianChatModel(modelName: String, apiKey: String): ChatModel {
        val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(1)
            .build()
        return ChatModelImpl(lm)
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
     * @see <a href='https://github.com/deepseek-ai/DeepSeek-V2/issues/18'>DeepSeek-V2 Issue 18</a>
     * */
    private fun createDeepSeekChatModel(modelName: String, apiKey: String): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://api.deepseek.com")
            .modelName(modelName)
            .logRequests(false)
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
    private fun createOpenAICompatibleChatModel(modelName: String, apiKey: String, baseUrl: String): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(1)
            .build()
        return ChatModelImpl(lm)
    }
}
