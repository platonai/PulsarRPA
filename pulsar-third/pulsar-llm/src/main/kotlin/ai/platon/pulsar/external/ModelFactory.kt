package ai.platon.pulsar.external

import ai.platon.pulsar.external.impl.ZhipuChatModel
import java.util.concurrent.ConcurrentHashMap

/**
 * The factory for creating models.
 */
object ModelFactory {
    private val models = ConcurrentHashMap<String, ChatModel>()
    
    /**
     * Create a default model.
     *
     * @return The created model.
     */
    fun getOrCreate(): ChatModel {
        return getOrCreate("glm4")
    }
    
    /**
     * Create a model.
     *
     * @param model The name of model to create.
     * @return The created model.
     */
    fun getOrCreate(model: String): ChatModel {
        val apiKey = System.getenv("ZHIPU_API_KEY") ?: throw IllegalArgumentException("ZHIPU_API_KEY is not set")
        return getOrCreate(model, apiKey)
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
    
    private fun getOrCreateModel0(model: String, apiKey: String): ChatModel {
        val key = "$model:$apiKey"
        return models.computeIfAbsent(key) { doCreateModel(model, apiKey) }
    }
    
    private fun doCreateModel(model: String, apiKey: String): ChatModel {
        return when (model) {
            "glm4" -> ZhipuChatModel(apiKey)
            else -> ZhipuChatModel(apiKey)
        }
    }
}
