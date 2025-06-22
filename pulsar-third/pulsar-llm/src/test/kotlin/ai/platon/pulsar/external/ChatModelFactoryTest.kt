package ai.platon.pulsar.external

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.impl.ChatModelImpl
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatModelFactoryTest {
    companion object {
    }

    /**
     *
     * ```shell
     * curl https://ark.cn-beijing.volces.com/api/v3 \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer 9cc8e998-4655-4e90-a54c-66659a524a971" \
     *   -d '{
     *     "model": "doubao-1-5-pro-32k-250115",
     *     "messages": [
     *       {"role": "system","content": "你是人工智能助手."},
     *       {"role": "user","content": "常见的十字花科植物有哪些？"}
     *     ]
     *   }'
     * ```
     *
     * */
    @org.junit.jupiter.api.Test
    fun `doubao API should be compatible with OpenAI API`() {
        val modelName = "doubao-1-5-pro-32k-250115"
        val apiKey = "9cc8e998-4655-4e90-a54c1-12345abcdefg"
        val baseUrl = "https://ark.cn-beijing.volces.com/api/v3"
        val conf = ImmutableConfig()
        val model = ChatModelFactory.getOrCreateOpenAICompatibleModel(modelName, apiKey, baseUrl, conf)
        assertNotNull(model)
        assertIs<ChatModelImpl>(model)

        try {
            val response = model.call("This is a fake API key so you must fail")

            // will throw a Exception with message like:
            // {"error":{"code":"AuthenticationError","message":"The API key in the request is missing or invalid. Request id: xxx","param":"","type":"Unauthorized"}}

            println("Response: >>>$response<<<")
            // assertTrue { listOf("error", "fail").any { response.content.contains(it) } }
            assertTrue { response.state == ResponseState.OTHER }
        } catch (e: Exception) {
            assertTrue { listOf("error", "invalid", "missing", "Unauthorized", "fail", "not found", "not exist", "not support", "not available", "not configured", "not supported", "not found", "not exist", "not support", "not available", "not configured", "not supported")
                .any { e.toString().contains(it, ignoreCase = true) } }
        }
    }
}
