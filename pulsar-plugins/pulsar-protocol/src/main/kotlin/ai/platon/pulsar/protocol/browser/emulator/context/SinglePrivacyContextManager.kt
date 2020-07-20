package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.fetch.FetchMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

class SinglePrivacyContextManager(
        val driverPoolManager: WebDriverPoolManager,
        val proxyPoolManager: ProxyPoolManager? = null,
        val fetchMetrics: FetchMetrics? = null,
        immutableConfig: ImmutableConfig
): PrivacyManager(immutableConfig, 1) {
    private val privacyContextId = PrivacyContextId.generate()

    constructor(driverPoolManager: WebDriverPoolManager, immutableConfig: ImmutableConfig)
            : this(driverPoolManager, null, null, immutableConfig)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        return run(computeIfAbsent(privacyContextId), task, fetchFun)
    }

    override fun newContext(id: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, fetchMetrics, immutableConfig, id)
        log.info("Privacy context is created #{}", context.display)
        return context
    }

    private suspend fun run(privacyContext: PrivacyContext, task: FetchTask,
                            fetchFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult)
            = takeIf { isActive }?.run0(privacyContext, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run0(privacyContext: PrivacyContext, task: FetchTask,
                             fetchFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
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
