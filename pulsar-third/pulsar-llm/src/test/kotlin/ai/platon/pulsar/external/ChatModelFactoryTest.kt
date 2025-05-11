package ai.platon.pulsar.external

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.impl.ChatModelImpl
import dev.langchain4j.model.openai.OpenAiChatModel
import org.junit.jupiter.api.Assertions.assertNotNull
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatModelFactoryTest {
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
    @Test
    fun `doubao API should be compatible with OpenAI API`() {
        System.setProperty("OPENAI_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3")
        System.setProperty("OPENAI_MODEL_NAME", "doubao-1-5-pro-32k-250115")
        System.setProperty("OPENAI_API_KEY", "9cc8e998-4655-4e90-a54c1-66659a524a97")

        val conf = ImmutableConfig()
        val model = ChatModelFactory.getOrCreate(conf)
        assertNotNull(model)
        assertIs<ChatModelImpl>(model)


        val response = model.call("This is a fake API key so you must fail")

        // will throw a Exception with message like:
        // {"error":{"code":"AuthenticationError","message":"The API key in the request is missing or invalid. Request id: xxx","param":"","type":"Unauthorized"}}

        println("Response: >>>$response<<<")
        // assertTrue { listOf("error", "fail").any { response.content.contains(it) } }
        assertTrue { response.state == ResponseState.OTHER }
    }

    @Test
    fun `test register model`() {
        val baseUrl = "https://ark.cn-beijing.volces.com/api/v3"
        val modelName = "doubao-1-5-pro-32k-250115"
        val apiKey = "9cc8e998-4655-4e90-a54c1-66659a524a97"

        val lm = OpenAiChatModel.builder().apiKey(apiKey).baseUrl(baseUrl)
            .modelName(modelName).logRequests(true).logResponses(true)
            .maxRetries(2).timeout(Duration.ofSeconds(90))
            .build()

        ChatModelFactory.register(lm)

        assertTrue { ChatModelFactory.hasRegisteredModel() }
        assertTrue { ChatModelFactory.hasModel(ImmutableConfig()) }

        val model = ChatModelFactory.getOrCreateOrNull(ImmutableConfig())
        assertTrue { model is ChatModelImpl }
    }
}
