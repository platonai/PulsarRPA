package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.agentic.common.FileSystem
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.TcEvaluate
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.nio.file.Path

class AgentToolManager(
    val agent: BrowserPerceptiveAgent,
) {
    private val logger = getLogger(AgentToolManager::class)
    private val executor = BasicToolCallExecutor()

    val baseDir: Path = AppPaths.get("agent")
        .resolve(DateTimes.PATH_SAFE_FORMAT_101.format(agent.startTime))
        .resolve(agent.uuid.toString())
    val fs: FileSystem

    val session: AgenticSession get() = agent.session
    val driver: WebDriver get() = session.getOrCreateBoundDriver()

    init {
        Files.createDirectories(baseDir)
        fs = FileSystem(baseDir)
    }

    suspend fun execute(toolCall: ToolCall, action: ActionDescription, message: String? = null): ToolCallResult {
        try {
            val evaluate = when (toolCall.domain) {
                "driver" -> executor.execute(toolCall, driver)
                "browser" -> executor.execute(toolCall, driver.browser)
                "fs" -> executor.execute(toolCall, fs)
                "agent" -> executor.execute(toolCall, this)
                else -> throw IllegalArgumentException("❓ Unsupported domain: ${toolCall.domain} | $toolCall")
            }

            val method = toolCall.method
            when (method) {
                "switchTab" -> onDidSwitchTab(evaluate)
                "navigateTo" -> onDidNavigateTo(driver, toolCall, evaluate)
            }

            return ToolCallResult(
                success = true,
                evaluate = evaluate,
                message = message,
                expression = action.cssFriendlyExpression,
                modelResponse = action.modelResponse?.content
            )
        } catch (e: Exception) {
            logger.warn("Failed to execute tool call $action", e)

            return ToolCallResult(
                success = false,
                message = e.message,
                expression = action.cssFriendlyExpression,
                modelResponse = action.modelResponse?.content
            )
        }
    }

    /**
     * Handle switching to a new tab by binding the target driver to the session.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onDidSwitchTab(evaluate: TcEvaluate) {
        val frontDriver = session.boundBrowser?.frontDriver
        val boundDriver = session.boundDriver
        if (frontDriver == null) {
            logger.warn("⚠️ No driver is in front after switchTab")
            return
        }

        if (frontDriver == boundDriver) {
            logger.warn("⚠️ The bound driver does not change after switchTab")
            return
        }

        session.bindDriver(frontDriver)
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun onDidNavigateTo(driver: WebDriver, toolCall: ToolCall, evaluate: TcEvaluate) {
        driver.waitForNavigation()
        driver.waitForSelector("body")
        delay(3000)
    }
}
