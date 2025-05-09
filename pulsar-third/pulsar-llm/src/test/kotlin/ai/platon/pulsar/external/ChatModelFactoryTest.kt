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
     * curl https://ark.cn-beijing.volces.com/api/v3/chat/completions \
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
        System.setProperty("OPENAI_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3")
        System.setProperty("OPENAI_MODEL_NAME", "doubao-1-5-pro-32k-250115")
        System.setProperty("OPENAI_API_KEY", "9cc8e998-4655-4e90-a54c1-66659a524a97")

        val conf = ImmutableConfig()
        val model = ChatModelFactory.getOrCreate(conf)
        assertNotNull(model)
        assertIs<ChatModelImpl>(model)

        val response = model.call("This is a fake API key so you must fail")
        // the response would be like:
        // {"error":{"code":"AuthenticationError","message":"The API key in the request is missing or invalid. Request id: xxx","param":"","type":"Unauthorized"}}
        println(response)
        assertTrue { response.content.contains("AuthenticationError") }
        assertTrue { response.state == ResponseState.OTHER }
    }
}
