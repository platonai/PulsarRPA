package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.config.KConfiguration
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.junit.jupiter.api.Assumptions
import kotlin.test.assertTrue

object TestHelper {

    fun checkConfiguration(session: PulsarSession) {
        val conf = session.sessionConfig
        val isModelConfigured = ChatModelFactory.isModelConfigured(conf)

        if (!isModelConfigured) {
            println("=========================== LLM NOT CONFIGURED ==========================================")
            println("> Skip the tests because the API key is not set")
            println("> Please set the API key in the configuration file or environment variable")
            println("> The configuration file can be found in: " + KConfiguration.EXTERNAL_RESOURCE_BASE_DIR)
            println("> All xml files in the directory will be loaded as the configuration file")
        } else {
            var response = session.chat("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。")
            Assumptions.assumeTrue(response.content.contains("121"))
            
            response = session.chat("Who are you?")
            println("##########\n" + response.content + "\n##########")
        }
        
        Assumptions.assumeTrue(isModelConfigured)
    }
    
    fun checkTokenUsage(response: ModelResponse) {
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
        println("Token usage: " + response.tokenUsage)
    }
}
