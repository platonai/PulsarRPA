package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyContextId
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import com.codahale.metrics.Gauge
import kotlinx.coroutines.withTimeout
import java.time.Duration

class BrowserPrivacyManager(
        val driverManager: WebDriverManager,
        val proxyPoolMonitor: ProxyPoolMonitor,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig) {
    private val tracer = log.takeIf { it.isTraceEnabled }
    val taskTimeout = Duration.ofMinutes(5)
    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }
    val maxRetry = 2

    init {
        mapOf(
                "preemptiveTasks" to Gauge<Int> { numPreemptiveTasks.get() },
                "runningPreemptiveTasks" to Gauge<Int> { numRunningPreemptiveTasks.get() },
                "pendingNormalTasks" to Gauge<Int> { numPendingNormalTasks.get() },
                "runningNormalTasks" to Gauge<Int> { numRunningNormalTasks.get() }
        ).forEach { MetricsManagement.register(this, it.key, it.value) }
    }

    suspend fun run(
            id: PrivacyContextId, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ) = takeIf { isActive }?.run0(id, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run0(
            id: PrivacyContextId, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ): FetchResult {
        var result: FetchResult

        var i = 1
        do {
            val task0 = if (i == 1) task else task.clone()
            result = FetchResult.crawlRetry(task0)

            // if the current context is not leaked, return the current context, or block and create a new one
            val context = computeIfNotActive(id)

            if (i > 1) {
                log.info("{}. Fetching task {} the {}th times in context #{} | {}",
                        task0.page.id, task0.id, i, context.display, task0.url)
            }

            whenNormalDeferred {
                if (numRunningPreemptiveTasks.get() != 0) {
                    log.error("Wrong preempt channel status, it indicates bugs in PreemptChannelSupport | {}",
                            formatPreemptChannelStatus())
                }

                try {
                    require(!task0.isCanceled)
                    require(task0.state.get() == FetchTask.State.NOT_READY)
                    require(task0.proxyEntry == null)

                    task0.markReady()
                    task0.nPrivacyRetries = i
                    withTimeout(taskTimeout.toMillis()) {
                        result = context.run(task0) { _, driver -> task0.startWork(); fetchFun(task0, driver) }
                    }
                } finally {
                    task0.done()
                    updatePrivacyContext(context, result)
                }
            }
        } while (!result.isSuccess && !task0.isCanceled && context.isLeaked && i++ <= maxRetry)

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
    @Synchronized
    override fun computeIfNotActive(id: PrivacyContextId): BrowserPrivacyContext {
        val context = computeIfAbsent(id)
        if (context.isActive) {
            return context
        }

        val context0 = computeIfLeaked(context) {
            require(numPreemptiveTasks.get() > 0) { "Should have at least one active preemptive task" }
            require(numRunningPreemptiveTasks.get() > 0) { "Should have at least one running preemptive task" }
            create().also { activeContexts[id] = it }
        }

        log.info("Privacy context is created #{}", context0.display)
        reportZombieContexts()

        return context0
    }

    override fun computeIfAbsent(id: PrivacyContextId): BrowserPrivacyContext {
        return activeContexts.computeIfAbsent(id) { create() } as BrowserPrivacyContext
    }

    override fun create(): BrowserPrivacyContext {
        return BrowserPrivacyContext(proxyPoolMonitor, driverManager, immutableConfig)
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
            log.info("Privacy leak warning {}/#{} | {}", warnings, privacyContext.sequence, status)
        }

        if (warnings == 6) {
            privacyContext.report()
        }
    }

    private fun tracePrivacyContextInactive(privacyContext: PrivacyContext, result: FetchResult) {
        tracer?.trace("{}. Context {}/#{} is not active | {} | {}",
                result.task.id, privacyContext.sequence, privacyContext.privacyLeakWarnings,
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
