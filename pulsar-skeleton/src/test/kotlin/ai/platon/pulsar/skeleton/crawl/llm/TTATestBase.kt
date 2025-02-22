package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.context.PulsarContexts
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import java.net.URL
import java.nio.file.*
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.walk
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertTrue

open class TTATestBase {

    companion object {
        val dataBaseDir = AppPaths.getProcTmpTmp("test").resolve("llm")
        val webDriverSourceFile = dataBaseDir.resolve("WebDriver.kt")

        val systemMessageFile = dataBaseDir.resolve("system-message.txt")

        lateinit var systemMessage: String

        val session = PulsarContexts.createSession()
        var lastResponse: ModelResponse? = null

        @BeforeAll
        @JvmStatic
        fun checkConfiguration() {
            TestHelper.checkConfiguration(session)
            Files.createDirectories(dataBaseDir)

            LLMUtils.copyWebDriverFile(webDriverSourceFile)

            val webDriverSourceCode = Files.readString(webDriverSourceFile)
            assertTrue("WebDriver.kt should be found") { webDriverSourceCode.isNotBlank() }

            systemMessage = LLMUtils.webDriverMessageTemplate.replace("{{webDriverSourceCode}}", webDriverSourceCode)
            Files.writeString(systemMessageFile, systemMessage)

            systemMessage = Files.readString(systemMessageFile)
        }
    }
}