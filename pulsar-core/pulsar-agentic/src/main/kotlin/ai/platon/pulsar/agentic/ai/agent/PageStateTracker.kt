package ai.platon.pulsar.agentic.ai.agent

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.browser.driver.chrome.dom.model.BrowserUseState
import ai.platon.pulsar.common.getLogger
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
    private val session: AgenticSession,
    private val config: AgentConfig
) {
    private val logger = getLogger(this)

    private val activeDriver get() = session.boundDriver
    private var lastPageStateHash: Int? = null
    private var sameStateCount = 0

    // Keep a short history to detect small-period oscillations like A<->B toggles
    private val recentHashes: ArrayDeque<Int> = ArrayDeque()
    private val maxRecentHashes = 10

    /**
     * Calculate page state fingerprint for loop detection.
     * Combines URL, DOM structure, and scroll position into a single hash.
     *
     * @param browserUseState The current browser state
     * @return Hash code representing the page state
     */
    suspend fun calculatePageStateHash(browserUseState: BrowserUseState): Int {
        val driver = requireNotNull(activeDriver)

        // Combine URL, DOM structure, and interactive elements for fingerprint
        val urlHash = runCatching { driver.currentUrl() }.getOrNull()?.hashCode() ?: 0
        // Prefer cached microTree JSON from DOMState to avoid repeated serialization cost
        val domJson = browserUseState.domState.nanoTreeLazyJson
        val domHash = domJson.hashCode()
        // Quantize scroll ratio to reduce noise from tiny jitters (bucket to percentage 0..100)
        val scrollBucket = (browserUseState.browserState.scrollState.scrollYRatio * 100).toInt()
        val scrollHash = scrollBucket

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
        // Maintain a ring buffer of recent hashes for loop detection
        recentHashes.addLast(currentStateHash)
        if (recentHashes.size > maxRecentHashes) recentHashes.removeFirst()
        return sameStateCount
    }

    /**
     * Detect simple oscillation loops like A<->B or A->B->C->A.
     * @param periods Periods to check for, e.g., 2,3
     * @param minRepeatsPerPeriod Require at least this many repeats of the period to confirm
     * @return The detected period if a loop is found, or null otherwise
     */
    fun detectLoop(periods: Set<Int> = setOf(2, 3), minRepeatsPerPeriod: Int = 2): Int? {
        if (recentHashes.size < 2) return null
        val list = recentHashes.toList()
        for (p in periods) {
            // Need at least (minRepeatsPerPeriod + 1) * p samples to confirm p-periodicity
            val needed = (minRepeatsPerPeriod + 1) * p
            if (list.size < needed) continue
            var periodic = true
            // Compare last needed window
            val start = list.size - needed
            for (i in start + p until list.size) {
                if (list[i] != list[i - p]) {
                    periodic = false
                    break
                }
            }
            if (periodic) return p
        }
        return null
    }

    /**
     * Reset state tracking counters.
     */
    fun reset() {
        lastPageStateHash = null
        sameStateCount = 0
        recentHashes.clear()
    }

    /**
     * Wait for DOM to stabilize by checking for mutations, using config defaults.
     * This overload uses AgentConfig.domSettleTimeoutMs and domSettleCheckIntervalMs.
     */
    suspend fun waitForDOMSettle() = waitForDOMSettle(
        timeoutMs = config.domSettleTimeoutMs,
        checkIntervalMs = config.domSettleCheckIntervalMs
    )

    /**
     * Wait for DOM to stabilize by checking for mutations.
     *
     * This method repeatedly checks a lightweight DOM stability signature to detect when
     * the page has finished loading and rendering. Requires multiple consecutive
     * stable checks before considering the DOM settled.
     *
     * @param timeoutMs Maximum time to wait for DOM to settle
     * @param checkIntervalMs Interval between stability checks
     */
    suspend fun waitForDOMSettle(timeoutMs: Long, checkIntervalMs: Long) {
        val driver = requireNotNull(activeDriver)

        driver.waitForSelector("body", timeoutMs)

        val startTime = System.currentTimeMillis()
        var lastSignature: String? = null
        var stableCount = 0
        val requiredStableChecks = 3 // Require 3 consecutive stable checks

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Composite, lightweight stability signal: readyState, element count, and body text length
                val signature = driver.evaluateValue(
                    """
                    (() => {
                      const ready = document.readyState;
                      const nodes = document.getElementsByTagName('*').length;
                      const textLen = (document.body?.innerText || '').length;
                      return ready + ':' + nodes + ':' + textLen;
                    })()
                    """.trimIndent()
                ) as? String

                if (signature != null && signature == lastSignature) {
                    stableCount++
                    if (stableCount >= requiredStableChecks) {
                        logger.debug("DOM settled after ${System.currentTimeMillis() - startTime}ms")
                        return
                    }
                } else {
                    stableCount = 0
                }

                lastSignature = signature
                delay(checkIntervalMs)
            } catch (e: Exception) {
                logger.warn("Error checking DOM stability: ${e.message}")
                delay(checkIntervalMs)
            }
        }

        logger.debug("DOM settle timeout after ${timeoutMs}ms")
    }
}
