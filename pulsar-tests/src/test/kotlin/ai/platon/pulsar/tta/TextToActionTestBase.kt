package ai.platon.pulsar.tta

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll

class TextToActionTestBase: WebDriverTestBase() {

    companion object {
        val session = PulsarContexts.getOrCreateSession()
        var lastResponse: ModelResponse? = null

        val textToAction = TextToAction(session.sessionConfig)

        private val conf = ImmutableConfig(loadDefaults = true)
        private val isModelConfigured get() = ChatModelFactory.isModelConfigured(conf)
        private lateinit var model: ChatModel

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            if (isModelConfigured) {
                model = ChatModelFactory.getOrCreate(conf)
                val response = model.call("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。")
                Assumptions.assumeTrue(response.content.contains("121"))
            } else {
                println("=========================== LLM NOT CONFIGURED ==========================================")
                println("> Skip the tests because the API key is not set")
                println("> Please set the API key in the properties file or environment variable")
                println("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
            }

            Assumptions.assumeTrue(isModelConfigured)
        }
    }
}