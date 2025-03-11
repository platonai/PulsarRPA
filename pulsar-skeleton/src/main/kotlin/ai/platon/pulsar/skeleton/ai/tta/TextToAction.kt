package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import java.nio.file.Files

data class ActionDescription(
    val functionCalls: List<String>,
    val modelResponse: ModelResponse,
) {
}

data class InstructionResult(
    val functionCalls: List<String>,
    val functionResults : List<Any?>,
    val modelResponse: ModelResponse,
) {
    fun hasResults() = functionResults.isNotEmpty()

    companion object {
        val LLM_NOT_AVAILABLE = InstructionResult(
            listOf(),
            listOf(),
            modelResponse = ModelResponse.LLM_NOT_AVAILABLE,
        )
    }
}

class TextToAction(val conf: ImmutableConfig) {
    private val model = ChatModelFactory.getOrCreateOrNull(conf)

    val baseDir = AppPaths.get("tta")
    val pulsarSessionFile = baseDir.resolve("PulsarSession.kt")
    var pulsarSessionSourceCode: String
        private set
    var pulsarSessionMessage: String
        private set

    val webDriverFile = baseDir.resolve("WebDriver.kt")
    var webDriverSourceCode: String
        private set
    var webDriverMessage: String
        private set

    var actionInstructionMessage: String
        private set
    val actionInterfaceMessageFile = baseDir.resolve("system-message.txt")

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverFile)
        webDriverSourceCode = Files.readString(webDriverFile)
        webDriverMessage = WEB_DRIVER_MESSAGE_TEMPLATE.replace("{{webDriverSourceCode}}", webDriverSourceCode)

        LLMUtils.copyPulsarSessionFile(pulsarSessionFile)
        pulsarSessionSourceCode = Files.readString(pulsarSessionFile)
        pulsarSessionMessage = PULSAR_SESSION_MESSAGE_TEMPLATE.replace("{{pulsarSessionSourceCode}}", pulsarSessionSourceCode)

        actionInstructionMessage = webDriverSourceCode + pulsarSessionSourceCode
        Files.writeString(actionInterfaceMessageFile, actionInstructionMessage)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutAllInstruction(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $actionInstructionMessage
            $prompt
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutWebDriver(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $webDriverMessage
            $prompt
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun chatAboutPulsarSession(prompt: String): ModelResponse {
        val promptWithSystemMessage = """
            $webDriverMessage
            $prompt
        """.trimIndent()

        return model?.call(promptWithSystemMessage) ?: ModelResponse.LLM_NOT_AVAILABLE
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generateWebDriverActions(prompt: String): ActionDescription {
        val response = chatAboutWebDriver(prompt)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("driver.") }

        return ActionDescription(functionCalls, response)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generatePulsarSessionActions(prompt: String): ActionDescription {
        val response = chatAboutPulsarSession(prompt)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("session.") }

        return ActionDescription(functionCalls, response)
    }

    companion object {
        val PULSAR_SESSION_MESSAGE_TEMPLATE = """
以下是抓取网页的 API 接口及其注释，你可以使用这些接口来抓取网页。

{{pulsarSessionSourceCode}}

PulsarSession 代码结束。

如果用户要求你抓取，你需要生成一个函数实现用户的需求。
该函数接收一个 PulsarSession 对象，你仅允许使用 PulsarSession 接口中的函数。
你的返回结果仅包含该函数，不需要任何注释或者解释，返回格式如下：
```kotlin
suspend fun llmGeneratedFunction(session: PulsarSession) {
    // 你的代码
}
```


        """.trimIndent()

        val WEB_DRIVER_MESSAGE_TEMPLATE = """
以下是操作网页的 API 接口及其注释，你可以使用这些接口来操作网页，比如打开网页、点击按钮、输入文本等等。

{{webDriverSourceCode}}

WebDriver 代码结束。

如果用户要求你进行页面操作，你需要生成一个挂起函数实现用户的需求。
该函数接收一个 WebDriver 对象，你仅允许使用 WebDriver 接口中的函数。
你的返回结果仅包含该函数，不需要任何注释或者解释，返回格式如下：
```kotlin
suspend fun llmGeneratedFunction(driver: WebDriver) {
    // 你的代码
}
```


        """.trimIndent()
    }
}
