package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.proxy.ProxyRetiredException
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyContextId
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import java.time.Duration

class BrowserPrivacyContextMonitor(
        initialDelay: Long = 300,
        watchInterval: Long = 30,
        val privacyManager: BrowserPrivacyManager
): ScheduledMonitor(Duration.ofSeconds(initialDelay), Duration.ofSeconds(watchInterval)) {
    override fun watch() {
        privacyManager.activeContexts.values.forEach { context ->
            if (context is BrowserPrivacyContext && context.proxyPoolManager.isIdle) {
                privacyManager.close(context)
            }
        }
    }
}

class BrowserPrivacyManager(
        val driverPoolManager: WebDriverPoolManager,
        val proxyPoolManager: ProxyPoolManager,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig) {
    private val tracer = log.takeIf { it.isTraceEnabled }
    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }

    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return run(computeNextContext(), task, fetchFun)
    }

    override fun newContext(id: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, immutableConfig, id)
        log.info("Privacy context is created #{}", context.display)
        return context
    }

    private suspend fun run(
            privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ) = takeIf { isActive }?.run0(privacyContext, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run0(
            privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext")
        }

        var result = FetchResult.crawlRetry(task)

        try {
            require(!task.isCanceled)
            require(task.state.get() == FetchTask.State.NOT_READY)
            require(task.proxyEntry == null)

            task.markReady()
            result = privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables["privacyContext"] = formatPrivacyContext(privacyContext)
            updatePrivacyContext(privacyContext, result)
        }

        if (result.isPrivacyRetry) {
            result.status.upgradeRetry(RetryScope.CRAWL).also { logCrawlRetry(task) }
        }

        return result
    }

    private fun formatPrivacyContext(privacyContext: PrivacyContext): String {
        return String.format("%s(%.2f)", privacyContext.id.display, privacyContext.throughput)
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
        if (privacyContext.isActive) {
            when {
                status.isRetry(RetryScope.PRIVACY, ProxyRetiredException("")) -> privacyContext.markLeaked()
                status.isRetry(RetryScope.PRIVACY) -> privacyContext.markWarning()
                status.isRetry(RetryScope.CRAWL) -> privacyContext.markMinorWarning()
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
            log.info("Privacy leak warning {}/{} | {}#{} | {}",
                    warnings, privacyContext.maximumWarnings, privacyContext.sequence, privacyContext.display, status)
        }

        if (privacyContext.privacyLeakWarnings.get() == 6) {
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
}
