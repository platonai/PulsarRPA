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

    // Install the lightweight DOM stability probe once per page to avoid re-parsing JS in the loop
    private suspend fun ensureDomStabilityProbeInstalled() {
        val driver = requireNotNull(activeDriver)
        driver.evaluateValue(
            """
            (() => {
              try {
                const w = window;
                if (!w.__pulsarDomObserver) {
                  w.__pulsarDomStamp = 0;
                  w.__pulsarDomLastTs = (performance && performance.now) ? performance.now() : Date.now();
                  const obs = new MutationObserver(() => {
                    w.__pulsarDomStamp++;
                    w.__pulsarDomLastTs = (performance && performance.now) ? performance.now() : Date.now();
                  });
                  // Observe subtree text/content/node additions; attributes are OFF to reduce noise
                  const opts = { subtree: true, childList: true, characterData: true };
                  // Intentionally DO NOT observe attributes or set attributeFilter
                  // This avoids counting class/style/aria toggles as instability
                  obs.observe(document, opts);
                  w.__pulsarDomObserver = obs;
                }
                // Bind lifecycle/navigation-ish events once to bump the stamp on non-mutation transitions
                if (!w.__pulsarDomEventsBound) {
                  const bump = () => { try { w.__pulsarDomStamp++; } catch(_) {} };
                  document.addEventListener('readystatechange', bump, { once: false, passive: true });
                  document.addEventListener('DOMContentLoaded', bump, { once: true, passive: true });
                  window.addEventListener('load', bump, { once: false, passive: true });
                  window.addEventListener('pageshow', bump, { once: false, passive: true });
                  window.addEventListener('hashchange', bump, { once: false, passive: true });
                  window.addEventListener('popstate', bump, { once: false, passive: true });
                  document.addEventListener('visibilitychange', bump, { once: false, passive: true });
                  w.__pulsarDomEventsBound = true;
                }
                if (!w.__pulsarGetDomSignature) {
                  w.__pulsarGetDomSignature = function () {
                    const rs = document.readyState;
                    const rsCode = rs === 'complete' ? 2 : (rs === 'interactive' ? 1 : 0);
                    // Pack into a 53-bit safe integer: (stamp << 2) | rsCode
                    return (w.__pulsarDomStamp * 4) + rsCode;
                  }
                }
                return 1;
              } catch (e) {
                return 0;
              }
            })()
            """.trimIndent()
        )
    }

    /**
     * Wait for DOM to stabilize by checking for mutations.
     *
     * Faster implementation details:
     * - Install a single MutationObserver once (no reallocation per check)
     * - Avoid layout-triggering reads (scrollHeight/offsets); rely on a stamp + readyState
     * - Return a compact numeric signature to minimize JS<->JVM marshalling
     * - Require fewer stable checks when document.readyState is 'complete'
     *
     * @param timeoutMs Maximum time to wait for DOM to settle
     * @param checkIntervalMs Interval between stability checks
     */
    suspend fun waitForDOMSettle(timeoutMs: Long, checkIntervalMs: Long) {
        val driver = requireNotNull(activeDriver)

        driver.waitForSelector("body", timeoutMs)
        // Install once to avoid re-parsing the script below on each poll
        ensureDomStabilityProbeInstalled()

        val startTime = System.currentTimeMillis()
        var lastSignature: Long? = null
        var lastReadyStateCode: Int = -1
        var stableCount = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Call the cached function with minimal JS to reduce parser/bridge overhead
                val signatureNum = (driver.evaluateValue(
                    """
                    (() => { try { const f = window.__pulsarGetDomSignature; return typeof f === 'function' ? f() : -1; } catch(e) { return -1; } })()
                    """.trimIndent()
                ) as? Number)?.toLong()

                if (signatureNum != null && signatureNum >= 0) {
                    val rsCode = (signatureNum % 4L).toInt()
                    val isSame = signatureNum == lastSignature
                    if (isSame) {
                        stableCount++
                        val requiredStableChecks = if (rsCode == 2) 2 else 3
                        if (stableCount >= requiredStableChecks) {
                            logger.debug("DOM settled after ${System.currentTimeMillis() - startTime}ms (rsCode=$rsCode, checks=$stableCount)")
                            return
                        }
                    } else {
                        stableCount = 0
                    }
                    lastReadyStateCode = rsCode
                    lastSignature = signatureNum
                } else {
                    // Treat as unstable and continue polling
                    stableCount = 0
                }

                delay(checkIntervalMs)
            } catch (e: Exception) {
                logger.warn("Error checking DOM stability: ${e.message}")
                delay(checkIntervalMs)
            }
        }

        logger.debug("DOM settle timeout after ${timeoutMs}ms (lastReadyStateCode=$lastReadyStateCode)")
    }
}
