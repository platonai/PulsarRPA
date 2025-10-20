package ai.platon.pulsar.agentic.ai.tta

import ai.platon.pulsar.common.printlnPro
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("ExternalServiceTest")
@Tag("SkippableLowerLevelTest")
class Text2WebDriverActionDescriptionTests: TTATestBase() {

    @AfterTest
    fun checkTokenUsage() {
        lastResponse?.let { TestHelper.checkTokenUsage(it) }
    }

    @Test
    fun `When ask to open a web page then generate correct kotlin code`() {
        val prompt = """
如何打开一个网页？
        """.trimIndent()

        val response = runBlocking { textToAction.generateWithSourceCode(prompt) }
        lastResponse = response
        printlnPro(response.content)

        assertTrue { listOf(".navigateTo", ".open").any { response.content.contains(it) } }
    }

    @Test
    fun `When ask to open a web page and scroll then generate correct kotlin code`() {
        val prompt = """
打开一个网页，然后滚动到页面30%位置。
        """.trimIndent()

        val response = runBlocking { textToAction.generateWithSourceCode(prompt) }
        lastResponse = response
        printlnPro(response.content)

        assertTrue { response.content.contains(".scrollToMiddle") }
    }

    @Test
    fun `When ask to open a web page, scroll and take snapshot then generate correct kotlin code`() {
        val prompt = """
打开一个网页，然后滚动到页面30%位置，然后滚动到页面顶部，最后截取页面快照。
        """.trimIndent()

        val response = runBlocking { textToAction.generateWithSourceCode(prompt) }
        lastResponse = response
        printlnPro(response.content)

        val content = response.content
        assertTrue { content.contains(".scrollToMiddle") }
        assertTrue { content.contains(".scrollToTop") }
        assertTrue { content.contains(".captureScreenshot") }
    }
}
