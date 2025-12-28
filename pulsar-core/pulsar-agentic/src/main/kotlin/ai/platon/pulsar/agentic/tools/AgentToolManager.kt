package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.BrowserAgentActor
import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.agentic.tools.executors.*
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.agentic.ActionDescription
import ai.platon.pulsar.agentic.TcEvaluate
import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.ToolCallResult
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.nio.file.Path

class AgentToolManager constructor(
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

    /**
     * Custom tool targets registry, mapping domain names to their corresponding target objects.
     * Users can register custom targets here for their custom tool executors.
     */
    private val customTargets = mutableMapOf<String, Any>()

    /**
     * Register a custom target object for a specific domain.
     * The target will be used when executing tool calls for the given domain.
     *
     * @param domain The domain name for the custom tool.
     * @param target The target object to be used by the custom tool executor.
     */
    fun registerCustomTarget(domain: String, target: Any) {
        customTargets[domain] = target
        logger.info("✓ Registered custom target for domain: {}", domain)
    }

    /**
     * Unregister a custom target for a specific domain.
     *
     * @param domain The domain to unregister.
     * @return true if a target was removed, false otherwise.
     */
    fun unregisterCustomTarget(domain: String): Boolean {
        val removed = customTargets.remove(domain)
        if (removed != null) {
            logger.info("✓ Unregistered custom target for domain: {}", domain)
            return true
        }
        return false
    }

    fun help(domain: String, method: String): String {
        // Check built-in executors first
        val builtInHelp = concreteExecutors.firstOrNull { it.domain == domain }?.help(method)
        if (builtInHelp != null) {
            return builtInHelp
        }

        // Check custom executors
        val customExecutor = CustomToolRegistry.instance.get(domain)
        return customExecutor?.help(method) ?: ""
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
                actionDescription = actionDescription,
            )
        }

        try {
            val tc = requireNotNull(actionDescription.toolCall) { "Tool call is required" }

            // First try built-in tool domains
            val evaluate = when (tc.domain) {
                "driver" -> executor.execute(tc, driver)
                "browser" -> executor.execute(tc, driver.browser)
                "fs" -> executor.execute(tc, fs)
                "agent" -> executor.execute(tc, agent)
                "system" -> executor.execute(tc, system)
                else -> {
                    // Check if this is a custom tool domain
                    val customExecutor = CustomToolRegistry.instance.get(tc.domain)
                    if (customExecutor != null) {
                        val target = customTargets[tc.domain]
                            ?: throw UnsupportedOperationException(
                                "❓ Custom domain '${tc.domain}' is registered but no target object is available. " +
                                "Use registerCustomTarget() to provide the target object."
                            )
                        customExecutor.execute(tc, target)
                    } else {
                        throw UnsupportedOperationException("❓ Unsupported domain: ${tc.domain} | $tc")
                    }
                }
            }

            val tcResult = ToolCallResult(
                success = true,
                evaluate = evaluate,
                message = message,
                actionDescription = actionDescription,
            )

            val method = tc.method
            when (method) {
                "switchTab" -> onDidSwitchTab(evaluate)
                "navigateTo" -> onDidNavigateTo(driver, tc, evaluate)
            }

            val timeoutMs = 3_000L
            val oldUrl = actionDescription.agentState?.browserUseState?.browserState?.url
            val pseudoExpression = actionDescription.pseudoExpression
            val maybeNavMethod = method in ToolSpecification.MAY_NAVIGATE_ACTIONS
            if (oldUrl != null && maybeNavMethod) {
                val remainingTime = driver.waitForNavigation(oldUrl, timeoutMs)
                if (remainingTime <= 0) {
                    val navError = "⏳ Navigation timeout after ${timeoutMs}ms for expression: $pseudoExpression"
                    logger.warn(navError)
                    return tcResult
                }
            }

            return tcResult
        } catch (e: Exception) {
            logger.warn("Failed to execute tool call | $actionDescription", e)

            val ad = actionDescription
            val expression = ad.pseudoExpression ?: ad.cssFriendlyExpression ?: ad.expression ?: ""
            return ToolCallResult(
                success = false,
                evaluate = TcEvaluate(expression, e),
                message = e.message,
                actionDescription = actionDescription,
            )
        }
    }

    /**
     * Handle switching to a new tab by binding the target driver to the session.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onDidSwitchTab(evaluate: TcEvaluate) {
        val frontDriver = session.boundBrowser?.frontDriver
        if (frontDriver == null) {
            logger.warn("⚠️ No driver is in front after switchTab")
            return
        }

        val oldBoundDriver = session.boundDriver
        if (frontDriver == oldBoundDriver) {
            logger.warn("⚠️ The bound driver does not change after switchTab")
        }

        // bind the driver which has been brought to front just now
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
