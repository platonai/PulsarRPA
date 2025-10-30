package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import kotlin.test.assertTrue

object TestHelper {

    fun checkLLMConfiguration(session: PulsarSession) {
        val conf = session.sessionConfig
        val isModelConfigured = ChatModelFactory.isModelConfigured(conf)

        if (!isModelConfigured) {
            printlnPro("=========================== LLM NOT CONFIGURED ==========================================")
            printlnPro("> Skip the tests because the API key is not set")
            printlnPro("> Please set the API key in the properties file or environment variable")
            printlnPro("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
        } else {
            var response = runBlocking { session.chat("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。") }
            Assumptions.assumeTrue(response.content.contains("121"))

            response = runBlocking { session.chat("Who are you?") }
            printlnPro("##########\n" + response.content + "\n##########")
        }

        Assumptions.assumeTrue(isModelConfigured)
    }

    fun checkTokenUsage(response: ModelResponse) {
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
        printlnPro("Token usage: " + response.tokenUsage)
    }
}

