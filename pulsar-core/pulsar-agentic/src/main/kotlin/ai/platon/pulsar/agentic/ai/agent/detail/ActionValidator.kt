package ai.platon.pulsar.agentic.ai.agent.detail

import ai.platon.pulsar.browser.driver.chrome.dom.FBNLocator
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.support.ToolCall
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Validates actions before execution to ensure safety and correctness.
 *
 * This helper class provides validation for:
 * - URL safety checks
 * - Selector syntax validation
 * - Tool call parameter validation
 * - Security policy enforcement
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class ActionValidator(
    private val config: AgentConfig
) {
    private val logger = getLogger(this)

    // Validation cache to avoid repeated validation of same actions
    private val validationCache = ConcurrentHashMap<String, Boolean>()

    /**
     * Validates tool calls before execution
     * High Priority #5: Deny unknown actions by default for security
     */
    fun validateToolCall(toolCall: ToolCall): Boolean {
        val cacheKey = "${toolCall.name}:${toolCall.args}"
        return validationCache.getOrPut(cacheKey) {
            when (toolCall.name) {
                "navigateTo" -> validateNavigateTo(toolCall.args)
                "click", "fill", "press", "check", "uncheck", "exists", "isVisible", "focus", "scrollTo" -> validateElementAction(
                    toolCall.args
                )

                "waitForNavigation" -> validateWaitForNavigation(toolCall.args)
                "goBack", "goForward", "delay", "scrollToTop", "scrollToBottom" -> true // These don't need validation
                // High Priority #5: Deny unknown actions by default
                else -> {
                    if (config.denyUnknownActions) {
                        logger.warn("Unknown action blocked: ${toolCall.name}")
                        false
                    } else {
                        logger.warn("Unknown action allowed (config): ${toolCall.name}")
                        true
                    }
                }
            }
        }
    }

    /**
     * Validates navigation actions
     */
    private fun validateNavigateTo(args: Map<String, Any?>): Boolean {
        val url = args["url"]?.toString() ?: return false
        return isSafeUrl(url)
    }

    /**
     * Validates element interaction actions
     * Medium Priority #11: Improved validation with selector syntax checking
     */
    private fun validateElementAction(args: Map<String, Any?>): Boolean {
        val selector = args["selector"]?.toString() ?: return false

        // Basic validation
        if (selector.isBlank() || selector.length > config.maxSelectorLength) {
            return false
        }

        val isSimplifiedFBN = selector.matches(FBNLocator.Companion.SIMPLIFIED_REGEX)
        if (isSimplifiedFBN) {
            return true
        }

        // Medium Priority #11: Check for common selector syntax patterns
        val hasValidPrefix = selector.startsWith("xpath:") ||
                selector.startsWith("css:") ||
                selector.startsWith("#") ||
                selector.startsWith(".") ||
                selector.startsWith("//") ||
                selector.startsWith("fbn:") ||
                selector.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*$")) // tag name

        return hasValidPrefix
    }

    /**
     * Validates waitForNavigation actions.
     *
     * @param args Action arguments containing oldUrl and timeout
     * @return true if the parameters are valid
     */
    private fun validateWaitForNavigation(args: Map<String, Any?>): Boolean {
        val oldUrl = args["oldUrl"]?.toString() ?: ""
        val timeout = (args["timeoutMillis"] as? Number)?.toLong() ?: 5000L
        return timeout in 100L..60000L && oldUrl.length < 1000 // Reasonable timeout range and URL length
    }

    /**
     * Enhanced URL validation with comprehensive safety checks.
     * Configurable for localhost and port restrictions.
     *
     * @param url The URL to validate
     * @return true if the URL is safe to use
     */
    fun isSafeUrl(url: String): Boolean {
        if (url.isBlank()) return false

        return runCatching {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()

            // Only allow http and https schemes
            if (scheme !in setOf("http", "https")) {
                logger.warn("Blocked unsafe URL scheme: $scheme for URL: ${url.take(50)}")
                return false
            }

            // Additional safety checks
            val host = uri.host?.lowercase() ?: return false

            // Configurable localhost blocking
            if (!config.allowLocalhost) {
                val dangerousPatterns = listOf("localhost", "127.0.0.1", "0.0.0.0", "::1")
                if (dangerousPatterns.any { host.contains(it) }) {
                    logger.warn("Blocked localhost URL (config): $host")
                    return false
                }
            }

            // Validate port with configurable whitelist
            val port = uri.port
            if (port != -1 && port !in config.allowedPorts) {
                logger.warn("Blocked URL with non-whitelisted port $port: $host")
                return false
            }

            true
        }.getOrDefault(false)
    }

    /**
     * Clear the validation cache.
     * Should be called periodically to prevent memory issues.
     */
    fun clearCache() {
        if (validationCache.size > 1000) {
            validationCache.clear()
            logger.debug("Validation cache cleared")
        }
    }
}
