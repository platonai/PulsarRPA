package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import kotlin.test.assertTrue

object TestHelper {

    fun checkConfiguration(session: PulsarSession) {
        val conf = session.sessionConfig
        val isModelConfigured = ChatModelFactory.isModelConfigured(conf)

        if (!isModelConfigured) {
            logPrintln("=========================== LLM NOT CONFIGURED ==========================================")
            logPrintln("> Skip the tests because the API key is not set")
            logPrintln("> Please set the API key in the properties file or environment variable")
            logPrintln("> You can copy application.properties to " + AppPaths.CONFIG_ENABLED_DIR)
        } else {
            var response = runBlocking { session.chat("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。") }
            Assumptions.assumeTrue(response.content.contains("121"))

            response = runBlocking { session.chat("Who are you?") }
            logPrintln("##########\n" + response.content + "\n##########")
        }

        Assumptions.assumeTrue(isModelConfigured)
    }

    fun checkTokenUsage(response: ModelResponse) {
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
        logPrintln("Token usage: " + response.tokenUsage)
    }
}

