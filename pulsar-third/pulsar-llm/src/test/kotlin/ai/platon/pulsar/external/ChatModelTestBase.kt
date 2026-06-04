package ai.platon.pulsar.external

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.common.config.ImmutableConfig
import dev.langchain4j.exception.AuthenticationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll

class ChatModelTestBase {

    companion object {
        private val conf = ImmutableConfig(loadDefaults = true)
        private val isModelConfigured get() = ChatModelFactory.isModelConfigured(conf)
        lateinit var model: BrowserChatModel

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            if (!isModelConfigured) {
                printlnPro("=========================== LLM NOT CONFIGURED ==========================================")
                printlnPro("> Skip the tests because the API key is not set")
                printlnPro("> Please set the API key in the properties file or environment variable")
                printlnPro("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
                Assumptions.assumeTrue(false, "LLM not configured")
                return
            }

            try {
                model = ChatModelFactory.getOrCreate(conf)
                val response = runBlocking {
                    model.call("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。")
                }
                Assumptions.assumeTrue(response.content.contains("121"), "LLM health check failed")
            } catch (e: AuthenticationException) {
                printlnPro("> Skip AI tests because authentication is missing or invalid")
                Assumptions.assumeTrue(false, "Missing/invalid LLM authentication")
            } catch (e: ChatModelException) {
                if (e.message?.contains("Missing Authentication header") == true) {
                    printlnPro("> Skip AI tests because authentication is missing")
                    Assumptions.assumeTrue(false, "Missing LLM authentication")
                }
                throw e
            }
        }
    }
}
