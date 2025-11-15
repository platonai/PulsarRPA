package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.BrowserAgentActor
import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.tools.executors.*
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ActionDescription
import ai.platon.pulsar.skeleton.ai.TcEvaluate
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.ai.ToolCallResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.nio.file.Path

class AgentToolManager(
    val baseDir: Path,
    val agent: BrowserAgentActor,
) {
    private val logger = getLogger(AgentToolManager::class)

    val fs: AgentFileSystem = AgentFileSystem(baseDir)
    val system: SystemToolExecutor = SystemToolExecutor(this)

    val session: AgenticSession get() = agent.session
    val driver: WebDriver get() = session.getOrCreateBoundDriver()

    val concreteExecutors: List<ToolExecutor> = listOf(
        WebDriverToolExecutor(),
        BrowserToolExecutor(),
        FileSystemToolExecutor(),
        AgentToolExecutor(),
        system
    )

    val executor = BasicToolCallExecutor(concreteExecutors)

    fun help(domain: String, method: String): String {
        return concreteExecutors.firstOrNull { it.domain == domain }?.help(method) ?: ""
    }

    @Throws(UnsupportedOperationException::class)
    suspend fun execute(actionDescription: ActionDescription, message: String? = null): ToolCallResult {
        // Fast path: respect user interruption immediately
        val cancelled = runCatching { !currentCoroutineContext().isActive }.getOrDefault(false)
        if (cancelled) {
            return ToolCallResult(
                success = false,
                evaluate = null,
                message = "USER interrupted",
                expression = actionDescription.cssFriendlyExpression,
                modelResponse = actionDescription.modelResponse?.content
            )
        }

        try {
            val tc = requireNotNull(actionDescription.toolCall) { "Tool call is required" }

            val evaluate = when (tc.domain) {
                "driver" -> executor.execute(tc, driver)
                "browser" -> executor.execute(tc, driver.browser)
                "fs" -> executor.execute(tc, fs)
                "agent" -> executor.execute(tc, agent)
                "system" -> executor.execute(tc, system)
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

            val timeoutMs = 3_000L
            val oldUrl = actionDescription.agentState?.browserUseState?.browserState?.url
            val expression = actionDescription.cssFriendlyExpression
            val maybeNavMethod = method in ToolSpecification.MAY_NAVIGATE_ACTIONS
            if (oldUrl != null && maybeNavMethod) {
                val remainingTime = driver.waitForNavigation(oldUrl, timeoutMs)
                if (remainingTime <= 0) {
                    val navError = "⏳ Navigation timeout after ${timeoutMs}ms for expression: $expression"
                    logger.warn(navError)
                    return tcResult
                }
            }

            return tcResult
        } catch (e: Exception) {
            logger.warn("Failed to execute tool call | $actionDescription", e)

            val expression = actionDescription.cssFriendlyExpression ?: actionDescription.expression ?: ""
            return ToolCallResult(
                success = false,
                evaluate = TcEvaluate(expression, e),
                message = e.message,
                expression = expression,
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
