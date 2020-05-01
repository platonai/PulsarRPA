package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyMonitorFactory
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import org.slf4j.LoggerFactory

class BrowserPrivacyManager(
        proxyMonitorFactory: ProxyMonitorFactory,
        val driverManager: WebDriverManager,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig) {
    private val log = LoggerFactory.getLogger(BrowserPrivacyManager::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }
    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }
    val maxRetry = 2
    val proxyManager = proxyMonitorFactory.get()
    override val autoRefreshContext
        get() = refreshIfNecessary { BrowserPrivacyContext(driverManager, proxyManager, immutableConfig) }
    override var activeContext: PrivacyContext = BrowserPrivacyContext(driverManager, proxyManager, immutableConfig)

    fun run(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.run0(task, fetchFun)?.takeUnless { it.isPrivacyRetry }
                ?:FetchResult.crawlRetry(task)
    }

    suspend fun runDeferred(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.runDeferred0(task, fetchFun)?.takeUnless { it.isPrivacyRetry }
                ?:FetchResult.crawlRetry(task).also {
                    log.warn("{}. Task is still privacy leaked after {} tries | {}",
                    task.page.id, task.nPrivacyRetries, task.url)
                }
    }

    private fun run0(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        do {
            // freezer is not reentrant
            val task0 = if (i == 1) task else task.clone()
            result = FetchResult.retry(task0, RetryScope.CRAWL)
            val context = computePureContextIfAbsent()

            whenUnfrozen {
                try {
                    task0.nPrivacyRetries = i
                    result = context.run(task0) { _, driver -> fetchFun(task0, driver) }
                } finally {
                    updateState(context, result)
                }
            }
        } while (!result.isSuccess && context.isLeaked && i++ <= maxRetry)

        return result
    }

    private suspend fun runDeferred0(task: FetchTask,
            fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        do {
            // freezer is not reentrant
            val task0 = if (i == 1) task else task.clone()

            result = FetchResult.crawlRetry(task0)

            // if the current context is not leaked, return the current context, or block and create a new one
            val context = computePureContextIfAbsent()
            // can leak immediately theoretically, but should not happen and no harm
            // require(context.isActive)

            tracer?.trace("{}. Ready to fetch task the {}th times in context #{} | {}",
                    task0.page.id, i, context.id, task0.url)

            whenUnfrozenDeferred {
                require(numRunningFreezers.get() == 0)

                try {
                    require(!task0.isCanceled)
                    require(task0.proxyEntry == null)

                    task0.nPrivacyRetries = i
                    result = context.runDeferred(task0) { _, driver -> fetchFun(task0, driver) }
                } finally {
                    updateState(context, result)
                }
            }
        } while (!result.isSuccess && context.isLeaked && i++ <= maxRetry)

        return result
    }

    /**
     * If the privacy leak occurs, block until the the context is closed completely
     * */
    private fun computePureContextIfAbsent(): BrowserPrivacyContext {
        val oldContext = activeContext
        return computeIfLeaked {
            require(numWorkers.get() == 0)
            require(numFreezers.get() > 0)
            require(numRunningFreezers.get() > 0)
            require(oldContext.isLeaked)
            require(oldContext.closed.get())

            val newContext = BrowserPrivacyContext(driverManager, proxyManager, immutableConfig)
            if (oldContext != newContext) {
                report()
                log.info("Privacy context is changed #{} -> #{}", oldContext.id, newContext.id)

                // TODO: do not use sleep
                Thread.sleep(5000)

                activeContext = newContext
            }
            newContext
        }
    }

    private fun report() {
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

    /**
     * Handle when after run
     * @return true if privacy is leaked
     * */
    private fun updateState(privacyContext: PrivacyContext, result: FetchResult) {
        val task = result.task
        val status = result.response.status
        val url = task.url
        if (privacyContext.isActive && task.nPrivacyRetries == 1) {
            when {
                status.isRetry(RetryScope.PRIVACY) -> privacyContext.markWarning()
                        .also { log.info("Privacy leak warning {}/#{}", privacyContext.privacyLeakWarnings, privacyContext.id) }
                status.isSuccess -> privacyContext.markSuccess()
            }
        } else {
            tracer?.trace("{}. Context {}/#{} is not active | {} | {}",
                    result.task.page.id, privacyContext.id, privacyContext.privacyLeakWarnings, result.status, url)
        }
    }
}
