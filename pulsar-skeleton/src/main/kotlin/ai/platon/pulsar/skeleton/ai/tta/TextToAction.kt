package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.session.PulsarSession
import java.nio.file.Files

data class FunctionCall(
    val name: String,
    val arguments: List<String>,
) {
    var result: Any? = null

    override fun toString(): String {
        return "FunctionCall(name='$name', arguments='$arguments')"
    }
}

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

}

class TextToAction(
    private val session: PulsarSession = PulsarContexts.createSession()
) {
    private val baseDir = AppPaths.get("tta")
    private val pulsarSessionFile = baseDir.resolve("PulsarSession.kt")
    private val webDriverFile = baseDir.resolve("WebDriver.kt")

    private var systemMessage: String = ""
    private val systemMessageFile = baseDir.resolve("system-message.txt")

    init {
        Files.createDirectories(baseDir)

        LLMUtils.copyWebDriverFile(webDriverFile)

        val webDriverSourceCode = Files.readString(webDriverFile)

        systemMessage = LLMUtils.webDriverMessageTemplate.replace("{{webDriverSourceCode}}", webDriverSourceCode)
        Files.writeString(systemMessageFile, systemMessage)

        systemMessage = Files.readString(systemMessageFile)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generate(prompt: String): String {
        val promptWithSystemMessage = """
            $systemMessage
            $prompt
        """.trimIndent()

        return session.chat(promptWithSystemMessage).content
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generateWebDriverActions(prompt: String): ActionDescription {
        val promptWithSystemMessage = """
            $systemMessage
            $prompt
        """.trimIndent()

        val response = session.chat(promptWithSystemMessage)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("driver.") }

        return ActionDescription(functionCalls, response)
    }

    /**
     * Generate the action code from the prompt.
     * */
    fun generatePulsarSessionActions(prompt: String): ActionDescription {
        val promptWithSystemMessage = """
            $systemMessage
            $prompt
        """.trimIndent()

        val response = session.chat(promptWithSystemMessage)
        val functionCalls = response.content.split("\n")
            .map { it.trim() }.filter { it.startsWith("session.") }

        return ActionDescription(functionCalls, response)
    }
}
