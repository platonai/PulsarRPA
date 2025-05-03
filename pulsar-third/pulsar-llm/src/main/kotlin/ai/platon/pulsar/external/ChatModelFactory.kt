package ai.platon.pulsar.external

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.warn
import ai.platon.pulsar.external.impl.ChatModelImpl
import com.alibaba.dashscope.utils.Constants.apiKey
import com.ibm.icu.impl.CurrencyData.provider
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.zhipu.ZhipuAiChatModel
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * The factory for creating models.
 *
 * TODO: integrate with LangChain4j or spring-ai
 */
object ChatModelFactory {
    private val models = ConcurrentHashMap<String, ChatModel>()

    fun isModelConfigured(conf: ImmutableConfig): Boolean {
        // deepseek official
        val deepseekAPIKey = conf["DEEPSEEK_API_KEY"]
        if (deepseekAPIKey != null) {
            return true
        }

        val openaiAPIKey = conf["OPENAI_API_KEY"]
        if (openaiAPIKey != null) {
            return true
        }

        val provider = conf["llm.provider"]
        val llm = conf["llm.name"]
        val apiKey = conf["llm.apiKey"]

        return provider != null && llm != null && apiKey != null
    }

    fun hasModel(conf: ImmutableConfig) = isModelConfigured(conf)

    /**
     * Create a default model.
     *
     * @return The created model.
     * @throws IllegalArgumentException If the configuration is not configured.
     */
    @Throws(IllegalArgumentException::class)
    fun getOrCreate(conf: ImmutableConfig): ChatModel {
        // Notice: all keys are transformed to dot.separated.kebab.case using KStrings.toDotSeparatedKebabCase(),
        // so the following keys are equal:
        // - DEEPSEEK_API_KEY, deepseek.apiKey
        val deepseekAPIKey = conf["DEEPSEEK_API_KEY"]
        if (deepseekAPIKey != null) {
            val deepseekModelName = conf["DEEPSEEK_MODEL_NAME"] ?: conf["LLM_NAME"] ?: "deepseek-chat"
            return getOrCreate("deepseek", deepseekModelName, deepseekAPIKey, conf)
        }

        val openaiAPIKey = conf["OPENAI_API_KEY"]
        if (openaiAPIKey != null) {
            val openaiBaseURL = conf["OPENAI_BASE_URL"] ?: "https://api.openai.com/v1/chat/completions"
            val openaiModelName = conf["OPENAI_MODEL_NAME"] ?: conf["LLM_NAME"] ?: "gpt-4o"
            return getOrCreateOpenAICompatibleModel(openaiModelName, openaiAPIKey, openaiBaseURL, conf)
        }

        val documentPath = "https://github.com/platonai/PulsarRPA/blob/master/docs/config/llm/llm-config-advanced.md"
        val provider = requireNotNull(conf["llm.provider"])  { "llm.provider is not set, see $documentPath" }
        val modelName = requireNotNull(conf["llm.name"]) { "llm.name is not set, see $documentPath" }
        val apiKey = requireNotNull(conf["llm.apiKey"]) { "llm.apiKey is not set, see $documentPath" }

        return getOrCreate(provider, modelName, apiKey, conf)
    }

    /**
     * Create a model.
     *
     * @param provider The provider of the model.
     * @param modelName The name of model to create.
     * @param apiKey The API key to use.
     * @return The created model.
     * @throws IllegalArgumentException If the configuration is not configured.
     */
    @Throws(IllegalArgumentException::class)
    fun getOrCreate(provider: String, modelName: String, apiKey: String, conf: ImmutableConfig) =
        getOrCreateModel0(provider, modelName, apiKey, conf)

    /**
     * Create a default model.
     *
     * @return The created model.
     */
    fun getOrCreateOrNull(conf: ImmutableConfig): ChatModel? {
        if (!isModelConfigured(conf)) {
            return null
        }

        return kotlin.runCatching { getOrCreate(conf) }
            .onFailure { warn(this, it.message ?: "Failed to create chat model") }
            .getOrNull()
    }

    fun getOrCreateOpenAICompatibleModel(modelName: String, apiKey: String, baseUrl: String, conf: ImmutableConfig): ChatModel {
        val key = "$modelName:$apiKey:$baseUrl"
        return models.computeIfAbsent(key) { createOpenAICompatibleModel0(modelName, apiKey, baseUrl, conf) }
    }

    private fun getOrCreateModel0(provider: String, modelName: String, apiKey: String, conf: ImmutableConfig): ChatModel {
        val key = "$modelName:$apiKey"
        return models.computeIfAbsent(key) { doCreateModel(provider, modelName, apiKey, conf) }
    }

    private fun doCreateModel(provider: String, modelName: String, apiKey: String, conf: ImmutableConfig): ChatModel {
        return when (provider) {
            "zhipu" -> createZhipuChatModel(apiKey, conf)
            "bailian" -> createBaiLianChatModel(modelName, apiKey, conf)
            "deepseek" -> createDeepSeekChatModel(modelName, apiKey, conf)
            "volcengine" -> createVolcengineChatModel(modelName, apiKey, conf)
            else -> createDeepSeekChatModel(modelName, apiKey, conf)
        }
    }

    /**
     * QianWen API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     * @see <a href='https://help.aliyun.com/zh/model-studio/getting-started/what-is-model-studio'>What is Model Studio</a>
     * */
    private fun createBaiLianChatModel(modelName: String, apiKey: String, conf: ImmutableConfig): ChatModel {
        val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(2)
            .timeout(Duration.ofSeconds(60))
            .build()
        return ChatModelImpl(lm, conf)
    }

    private fun createZhipuChatModel(apiKey: String, conf: ImmutableConfig): ChatModel {
        val lm = ZhipuAiChatModel.builder()
            .apiKey(apiKey)
            .logRequests(true)
            .logResponses(true)
            .maxRetries(2)
            .build()
        return ChatModelImpl(lm, conf)
    }

    /**
     * DeepSeek API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     *
     * @see <a href='https://github.com/deepseek-ai/DeepSeek-V2/issues/18'>DeepSeek-V2 Issue 18</a>
     * */
    private fun createDeepSeekChatModel(modelName: String, apiKey: String, conf: ImmutableConfig): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://api.deepseek.com/")
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(2)
            .timeout(Duration.ofSeconds(90))
            .build()
        return ChatModelImpl(lm, conf)
    }

    /**
     * Volcengine API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     *
     * @see <a href='https://www.volcengine.com/docs/82379/1399008'>快速入门-调用模型服务</a>
     * */
    private fun createVolcengineChatModel(modelName: String, apiKey: String, conf: ImmutableConfig): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(2)
            .timeout(Duration.ofSeconds(90))
            .build()
        return ChatModelImpl(lm, conf)
    }

    /**
     * DeepSeek API is compatible with OpenAI API, so it's OK to use OpenAIChatModel.
     *
     * @see https://github.com/deepseek-ai/DeepSeek-V2/issues/18
     * */
    private fun createOpenAICompatibleModel0(
        modelName: String, apiKey: String, baseUrl: String, conf: ImmutableConfig
    ): ChatModel {
        val lm = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .logRequests(false)
            .logResponses(true)
            .maxRetries(2)
            .timeout(Duration.ofSeconds(90))
            .build()
        return ChatModelImpl(lm, conf)
    }
}
