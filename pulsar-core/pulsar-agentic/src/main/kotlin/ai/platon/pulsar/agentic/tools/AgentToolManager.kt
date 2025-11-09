package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.BrowserPerceptiveAgent
import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.tools.executors.*
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ActionDescription
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

    val baseDir: Path = AppPaths.get("agent")
        .resolve(DateTimes.PATH_SAFE_FORMAT_101.format(agent.startTime))
        .resolve(agent.uuid.toString())
    val fs: AgentFileSystem

    init {
        Files.createDirectories(baseDir)
        fs = AgentFileSystem(baseDir)
    }

    val session: AgenticSession get() = agent.session
    val driver: WebDriver get() = session.getOrCreateBoundDriver()

    val concreteExecutors: List<ToolExecutor> = listOf(
        WebDriverToolExecutor(),
        BrowserToolExecutor(),
        FileSystemToolExecutor(),
        AgentToolExecutor(),
    )

    val executor = BasicToolCallExecutor(concreteExecutors)

    @Throws(UnsupportedOperationException::class)
    suspend fun execute(actionDescription: ActionDescription, message: String? = null): ToolCallResult {
        try {
            val tc = requireNotNull(actionDescription.toolCall) { "Tool call is required" }

            val evaluate = when (tc.domain) {
                "driver" -> executor.execute(tc, driver)
                "browser" -> executor.execute(tc, driver.browser)
                "fs" -> executor.execute(tc, fs)
                "agent" -> executor.execute(tc, agent)
                else -> throw UnsupportedOperationException("❓ Unsupported domain: ${tc.domain} | $tc")
            }

            val tcResult = ToolCallResult(
                success = true,
                evaluate = evaluate,
                message = message,
                expression = actionDescription.cssFriendlyExpression,
                modelResponse = actionDescription.modelResponse?.content
            )

            val method = tc.method
            when (method) {
                "switchTab" -> onDidSwitchTab(evaluate)
                "navigateTo" -> onDidNavigateTo(driver, tc, evaluate)
            }

            // If a timeout is provided and the action likely triggers navigation, wait for navigation
            // val timeoutMs = action.timeoutMs?.toLong()?.takeIf { it > 0 }
            val timeoutMs = 3_000L
            val oldUrl = actionDescription.agentState?.browserUseState?.browserState?.url
            val expression = actionDescription.cssFriendlyExpression
            val maybeNavMethod = method in ToolSpecification.MAY_NAVIGATE_ACTIONS
            if (timeoutMs != null && oldUrl != null && maybeNavMethod) {
                // High Priority #4: Fail explicitly on navigation timeout
                val remainingTime = driver.waitForNavigation(oldUrl, timeoutMs)
                if (remainingTime <= 0) {
                    val navError = "⏳ Navigation timeout after ${timeoutMs}ms for expression: $expression"
                    logger.warn(navError)
                    return tcResult
                }
            }

            return tcResult
        } catch (e: Exception) {
            logger.warn("Failed to execute tool call $actionDescription", e)

            return ToolCallResult(
                success = false,
                message = e.message,
                expression = actionDescription.cssFriendlyExpression,
                modelResponse = actionDescription.modelResponse?.content
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

    @Suppress("UNUSED_PARAMETER")
    private suspend fun onDidScrollToBottom(driver: WebDriver, toolCall: ToolCall, evaluate: TcEvaluate) {
        val expression = """
(() => {
    const docEl = document.documentElement, body = document.body;
    if (!docEl || !body) return true;
    const total = Math.min(Math.max(docEl.scrollHeight, body.scrollHeight, docEl.clientHeight), 15000);
    const vh = window.innerHeight || docEl.clientHeight || 800;
    const target = Math.max(0, total - vh);
    return Math.abs(window.scrollY - target) < 2;
})()
"""

        driver.waitUntil(5_000) { (driver.evaluateValue(expression) as? Boolean) == true }
    }
}
