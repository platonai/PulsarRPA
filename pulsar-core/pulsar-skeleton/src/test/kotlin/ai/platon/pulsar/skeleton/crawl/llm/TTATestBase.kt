package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.BrowserChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.skeleton.context.PulsarContexts
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import kotlin.test.assertNull

open class TTATestBase {

    companion object {
        val session = PulsarContexts.getOrCreateSession()
        var lastResponse: ModelResponse? = null

        val textToAction = TextToAction(session.sessionConfig)

        private val conf = ImmutableConfig(loadDefaults = true)
        private val isModelConfigured get() = ChatModelFactory.isModelConfigured(conf)
        private lateinit var model: BrowserChatModel

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            assertNull(conf.environment)

            if (isModelConfigured) {
                model = ChatModelFactory.getOrCreate(conf)
                val response = runBlocking { model.call("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。") }
                Assumptions.assumeTrue(response.content.contains("121"))
            } else {
                printlnPro("=========================== LLM NOT CONFIGURED ==========================================")
                printlnPro("> Skip the tests because the API key is not set")
                printlnPro("> Please set the API key in the properties file or environment variable")
                printlnPro("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
            }

            Assumptions.assumeTrue(isModelConfigured)
        }
    }
}

