package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyContextId
import ai.platon.pulsar.crawl.PrivacyContextMonitor
import ai.platon.pulsar.crawl.PrivacyManager
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

class DefaultBrowserPrivacyContextMonitor(
        initialDelay: Long = 300,
        watchInterval: Long = 30
): PrivacyContextMonitor(initialDelay, watchInterval) {
    override fun watch() {
    }
}

class SinglePrivacyContextManager(
        val driverPoolManager: WebDriverPoolManager,
        val proxyPoolManager: ProxyPoolManager,
        immutableConfig: ImmutableConfig
): PrivacyManager(DefaultBrowserPrivacyContextMonitor(), immutableConfig, 1) {

    suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return run(computeNextContext(), task, fetchFun)
    }

    override fun newContext(id: PrivacyContextId): DefaultBrowserPrivacyContext {
        val context = DefaultBrowserPrivacyContext(proxyPoolManager, driverPoolManager, immutableConfig, id)
        log.info("Privacy context is created #{}", context.display)
        return context
    }

    private suspend fun run(
            privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ) = takeIf { isActive }?.run0(privacyContext, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run0(
            privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult
    ): FetchResult {
        if (privacyContext !is DefaultBrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext")
        }

        return try {
            task.markReady()
            privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables["privacyContext"] = formatPrivacyContext(privacyContext)
        }
    }

    private fun formatPrivacyContext(privacyContext: PrivacyContext): String {
        return String.format("%s(%.2f)", privacyContext.id.display, privacyContext.throughput)
    }
}
