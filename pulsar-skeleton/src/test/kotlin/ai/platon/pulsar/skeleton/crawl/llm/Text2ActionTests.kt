package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.KConfiguration
import ai.platon.pulsar.external.ChatModel
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.external.ResponseState
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

class Text2ActionTests {

    companion object {
        private val projectRoot = Paths.get(System.getProperty("user.dir"))
        private val sourceFile = projectRoot.resolve("src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt")
        private val webDriverSourceCode = Files.readString(sourceFile)

        private val systemMessage = """
以下是操作网页的 API 接口及其注释，你可以使用这些接口来操作网页，比如打开网页、点击按钮、输入文本等等。

$webDriverSourceCode
        """.trimIndent()

        private val conf = ImmutableConfig(loadDefaults = true)
        private val isModelConfigured get() = ChatModelFactory.isModelConfigured(conf)
        private lateinit var model: ChatModel
        private var lastResponse: ModelResponse? = null

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            if (isModelConfigured) {
                model = ChatModelFactory.getOrCreate(conf)
            } else {
                println("=========================== LLM NOT CONFIGURED ==========================================")
                println("> Skip the tests because the API key is not set")
                println("> Please set the API key in the configuration file or environment variable")
                println("> The configuration file can be found in: " + KConfiguration.EXTERNAL_RESOURCE_BASE_DIR)
                println("> All xml files in the directory will be loaded as the configuration file")
            }

            Assumptions.assumeTrue(isModelConfigured)
            var response = model.call("这是一个测试，来测试你是否工作正常。计算11的平方，仅返回数字。")
            Assumptions.assumeTrue("121" == response.content)

            response = model.call("Who are you?")
            println(response.content)
        }
    }

    @BeforeTest
    fun checkResource() {
        assertTrue("WebDriver.kt should be found") { webDriverSourceCode.isNotBlank() }
    }

    @AfterTest
    fun checkTokenUsage() {
        val response = lastResponse ?: return
        assertTrue { response.tokenUsage.inputTokenCount > 0 }
        assertTrue { response.tokenUsage.outputTokenCount > 0 }
        assertTrue { response.tokenUsage.totalTokenCount > 0 }
        assertTrue { response.state == ResponseState.STOP }
        println("Token usage: " + response.tokenUsage)
    }
    
    @Test
    fun `When ask to open a web page then generate correct kotlin code`() {
        val userMessage = """
如何打开一个网页？
        """.trimIndent()

        val response = model.call(userMessage, systemMessage)
        println(response.content)
        
        assertTrue { response.content.contains(".navigateTo") }
    }
    
    @Test
    fun `When ask to open a web page and scroll then generate correct kotlin code`() {
        val userMessage = """
生成一个挂起函数，实现如下功能：打开一个网页，然后滚动到页面30%位置。仅返回该挂起函数，不需要其他代码，也不需要注释和解释。
        """.trimIndent()
        
        val response = model.call(userMessage, systemMessage)
        println(response.content)
        
        assertTrue { response.content.contains(".navigateTo") }
        assertTrue { response.content.contains(".scrollToMiddle") }
    }
}
