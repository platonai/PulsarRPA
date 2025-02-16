package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Paths

open class TTATestBase {

    companion object {
        val projectRoot = Paths.get(System.getProperty("user.dir"))
        val sourceFile =
            projectRoot.resolve("src/main/kotlin/ai/platon/pulsar/skeleton/crawl/fetch/driver/WebDriver.kt")
        val webDriverSourceCode = Files.readString(sourceFile)

        val systemMessage = """
以下是操作网页的 API 接口及其注释，你可以使用这些接口来操作网页，比如打开网页、点击按钮、输入文本等等。

$webDriverSourceCode
        """.trimIndent()

        val session = PulsarContexts.createSession()
        var lastResponse: ModelResponse? = null

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            TestHelper.checkConfiguration(session)
            Assumptions.assumeTrue(webDriverSourceCode.isNotBlank(), "WebDriver.kt should be found")
        }
    }

}