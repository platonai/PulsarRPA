package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import org.slf4j.LoggerFactory

class BrowserPrivacyManager(
        val driverManager: WebDriverManager,
        val proxyPoolMonitor: ProxyPoolMonitor,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig) {
    private val log = LoggerFactory.getLogger(PrivacyManager::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }
    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }
    val maxRetry = 2
    override val autoRefreshContext
        get() = refreshIfNecessary { BrowserPrivacyContext(driverManager, proxyPoolMonitor, immutableConfig) }
    override var activeContext: PrivacyContext = BrowserPrivacyContext(driverManager, proxyPoolMonitor, immutableConfig)

    fun run(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.run0(task, fetchFun)?:FetchResult.crawlRetry(task)
    }

    suspend fun runDeferred(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.runDeferred0(task, fetchFun)?:FetchResult.crawlRetry(task)
    }

    private fun run0(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        do {
            val task0 = if (i == 1) task else task.clone()
            result = FetchResult.crawlRetry(task0)

            // if the current context is not leaked, return the current context, or block and create a new one
            val context = computeContextIfLeaked()
            // can leak immediately theoretically, but should not happen and no harm
            // require(context.isActive)

            tracer?.trace("{}. Ready to fetch task the {}th times in context #{} | {}",
                    task0.id, i, context.id, task0.url)

            whenNormal {
                require(numRunningPreemptiveTasks.get() == 0)

                try {
                    require(!task0.isCanceled)
                    require(task0.expectedProxyEntry == null)

                    task0.markReady()
                    task0.nPrivacyRetries = i
                    result = context.run(task0) { _, driver -> task0.startWork(); fetchFun(task0, driver) }
                } finally {
                    task0.done()
                    updatePrivacyContext(context, result)
                }
            }
        } while (!result.isSuccess && context.isLeaked && i++ <= maxRetry)

        if (result.isPrivacyRetry) {
            result.status.upgradeRetry(RetryScope.CRAWL).also { logCrawlRetry(task) }
        }

        return result
    }

    private suspend fun runDeferred0(
            task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        do {
            val task0 = if (i == 1) task else task.clone()
            result = FetchResult.crawlRetry(task0)

            // if the current context is not leaked, return the current context, or block and create a new one
            val context = computeContextIfLeaked()
            // can leak immediately theoretically, but should not happen and no harm
            // require(context.isActive)

            tracer?.trace("{}. Ready to fetch task the {}th times in context #{} | {}",
                    task0.id, i, context.id, task0.url)

            whenNormalDeferred {
                require(numRunningPreemptiveTasks.get() == 0)

                try {
                    require(!task0.isCanceled)
                    require(task0.state.get() == FetchTask.State.NOT_READY)
                    require(task0.expectedProxyEntry == null)

                    task0.markReady()
                    task0.nPrivacyRetries = i
                    result = context.runDeferred(task0) { _, driver -> task0.startWork(); fetchFun(task0, driver) }
                } finally {
                    task0.done()
                    updatePrivacyContext(context, result)
                }
            }
        } while (!result.isSuccess && context.isLeaked && i++ <= maxRetry)

        if (result.isPrivacyRetry) {
            result.status.upgradeRetry(RetryScope.CRAWL).also { logCrawlRetry(task) }
        }

        return result
    }

    /**
     * Get the current privacy context if it's not leaked, or create a new pure privacy context
     *
     * If the current privacy is not leaked, return the current privacy context
     * If the privacy leak occurs, block until the the context is closed completely, and create a new privacy context
     * */
    private fun computeContextIfLeaked(): BrowserPrivacyContext {
        if (proxyPoolMonitor.isIdle && proxyPoolMonitor.currentProxyEntry == null) {
            activeContext.markLeaked()
        }

        return computeIfLeaked {
            val oldContext = activeContext

            require(oldContext.isLeaked) { "Privacy context #${oldContext.id} should be leaked" }
            // TODO: check why there are still workers
            // require(numWorkers.get() == 0) { "Should have no workers, actual $numWorkers" }
            require(numReadyPreemptiveTasks.get() > 0) { "Should have at least one active preemptive task" }
            require(numRunningPreemptiveTasks.get() > 0) { "Should have at least one running preemptive task" }
            reportZombieContexts()

            val newContext = BrowserPrivacyContext(driverManager, proxyPoolMonitor, immutableConfig)
            log.info("Privacy context is changed #{} -> #{} <<<<<<<Bye", oldContext.id, newContext.id)

            newContext
        }
    }

    /**
     * Handle when after run
     * @return true if privacy is leaked
     * */
    private fun updatePrivacyContext(privacyContext: PrivacyContext, result: FetchResult) {
        if (privacyContext.numTasks.get() % 30 == 0) {
            privacyContext.report()
        }

        val status = result.response.status
        if (privacyContext.isActive && result.task.nPrivacyRetries == 1) {
            when {
                status.isRetry(RetryScope.PRIVACY, ProxyRetiredException("")) -> privacyContext.markLeaked()
                status.isRetry(RetryScope.PRIVACY) -> privacyContext.markWarning()
                status.isSuccess -> privacyContext.markSuccess()
            }

            takeIf { status.isRetry(RetryScope.PRIVACY) }?.also { logPrivacyLeakWarning(privacyContext, status) }
        } else {
            tracePrivacyContextInactive(privacyContext, result)
        }
    }

    private fun logPrivacyLeakWarning(privacyContext: PrivacyContext, status: ProtocolStatus) {
        val warnings = privacyContext.privacyLeakWarnings.get()
        if (warnings > 0) {
            log.info("Privacy leak warning {}/#{} | {}", warnings, privacyContext.id, status)
        }

        if (warnings == 6) {
            privacyContext.report()
        }
    }

    private fun tracePrivacyContextInactive(privacyContext: PrivacyContext, result: FetchResult) {
        tracer?.trace("{}. Context {}/#{} is not active | {} | {}",
                result.task.id, privacyContext.id, privacyContext.privacyLeakWarnings,
                result.status, result.task.url)
    }

    private fun logCrawlRetry(task: FetchTask) {
        if (task.nPrivacyRetries > 1) {
            log.takeIf { task.nPrivacyRetries > 1 }?.warn("{}. Task is still privacy leaked after {}/{} tries | {}",
                    task.id, task.nPrivacyRetries, task.nRetries, task.url)
        }
    }

    private fun reportZombieContexts() {
        if (zombieContexts.isNotEmpty()) {
            val prefix = "The latest context throughput: "
            val postfix = " (success/sec)"
            zombieContexts.take(15)
                    .joinToString(", ", prefix, postfix) { String.format("%.2f", it.throughput) }
                    .let { log.info(it) }
        }

        if (numBadContexts > maxAllowedBadContexts) {
            log.warn("Warning!!! There latest $numBadContexts contexts are bad, the proxy vendor is untrusted")

            // We may want to exit the process
            // throw FatalPrivacyContextException("Too many bad contexts, the proxy vendor might untrusted")
        }
    }
}
