package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.browser.driver.chrome.dom.BrowserUseState
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay

/**
 * Tracks page state changes to detect loops and ensure DOM stability.
 *
 * This helper class encapsulates page state tracking logic, including:
 * - DOM stability detection
 * - Page state fingerprinting
 * - Loop detection
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class PageStateTracker(
    private val driver: WebDriver,
    private val config: AgentConfig
) {
    private val logger = getLogger(this)

    private var lastPageStateHash: Int? = null
    private var sameStateCount = 0

    /**
     * Calculate page state fingerprint for loop detection.
     * Combines URL, DOM structure, and scroll position into a single hash.
     *
     * @param browserUseState The current browser state
     * @return Hash code representing the page state
     */
    suspend fun calculatePageStateHash(browserUseState: BrowserUseState): Int {
        // Combine URL, DOM structure, and interactive elements for fingerprint
        val urlHash = runCatching { driver.currentUrl() }.getOrNull()?.hashCode() ?: 0
        val domHash = DOMSerializer.toJson(browserUseState.domState.microTree).hashCode()
        val scrollHash = browserUseState.browserState.scrollState.hashCode()

        return (urlHash * 31 + domHash) * 31 + scrollHash
    }

    /**
     * Check if page state has changed since last check.
     * Tracks consecutive same-state occurrences for loop detection.
     *
     * @param browserUseState The current browser state
     * @return Number of consecutive times the same state has been observed
     */
    suspend fun checkStateChange(browserUseState: BrowserUseState): Int {
        val currentStateHash = calculatePageStateHash(browserUseState)

        if (currentStateHash == lastPageStateHash) {
            sameStateCount++
        } else {
            sameStateCount = 0
        }

        lastPageStateHash = currentStateHash
        return sameStateCount
    }

    /**
     * Reset state tracking counters.
     */
    fun reset() {
        lastPageStateHash = null
        sameStateCount = 0
    }

    /**
     * Wait for DOM to stabilize by checking for mutations.
     *
     * This method repeatedly checks the DOM structure hash to detect when
     * the page has finished loading and rendering. Requires multiple consecutive
     * stable checks before considering the DOM settled.
     *
     * @param timeoutMs Maximum time to wait for DOM to settle
     * @param checkIntervalMs Interval between stability checks
     */
    suspend fun waitForDOMSettle(timeoutMs: Long, checkIntervalMs: Long) {
        val startTime = System.currentTimeMillis()
        var lastDomHash: Int? = null
        var stableCount = 0
        val requiredStableChecks = 3 // Require 3 consecutive stable checks

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Get a lightweight DOM fingerprint
                val currentHtml = driver.evaluate("document.body?.innerHTML?.length || 0").toString()
                val currentHash = currentHtml.hashCode()

                if (currentHash == lastDomHash) {
                    stableCount++
                    if (stableCount >= requiredStableChecks) {
                        logger.debug("DOM settled after ${System.currentTimeMillis() - startTime}ms")
                        return
                    }
                } else {
                    stableCount = 0
                }

                lastDomHash = currentHash
                delay(checkIntervalMs)
            } catch (e: Exception) {
                logger.warn("Error checking DOM stability: ${e.message}")
                delay(checkIntervalMs)
            }
        }

        logger.debug("DOM settle timeout after ${timeoutMs}ms")
    }
}
