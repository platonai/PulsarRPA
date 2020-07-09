package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.BrowserInstanceId
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class DefaultBrowserPrivacyContext(
        val proxyPoolManager: ProxyPoolManager,
        val driverPoolManager: WebDriverPoolManager,
        conf: ImmutableConfig,
        id: PrivacyContextId = PrivacyContextId(generateBaseDir())
): PrivacyContext(id, conf) {

    private val browserInstanceId: BrowserInstanceId
    private var proxyEntry: ProxyEntry? = null
    private val driverContext: WebDriverContext
    private var proxyContext: ProxyContext? = null

    init {
        if (proxyPoolManager.isEnabled) {
            val proxyPool = proxyPoolManager.proxyPool
            proxyEntry = proxyPoolManager.activeProxyEntries.computeIfAbsent(id.dataDir) {
                proxyPool.take() ?: throw NoProxyException("No proxy found in pool ${proxyPool.javaClass.simpleName} | $proxyPool")
            }
            proxyEntry?.startWork()
        }

        browserInstanceId = BrowserInstanceId.resolve(id.dataDir).apply { proxyServer = proxyEntry?.hostPort }
        driverContext = WebDriverContext(browserInstanceId, driverPoolManager, conf)

        if (proxyPoolManager.isEnabled) {
            proxyContext = ProxyContext(proxyEntry, proxyPoolManager, driverContext, conf)
        }
    }

    open suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?: run0(task, browseFun)
    }

    /**
     * Block until all the drivers are closed and the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverContext.shutdown()
            proxyContext?.close()
            report()
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.privacyRetry(task)
        }

        return null
    }

    private suspend fun run0(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        beforeRun(task)
        val result = proxyContext?.run(task, browseFun)?:driverContext.run(task, browseFun)
        afterRun(result)
        return result
    }

    private fun beforeRun(task: FetchTask) {
        numTasks.incrementAndGet()
    }

    private fun afterRun(result: FetchResult) {
        numTotalRun.incrementAndGet()
        if (result.status.isSuccess) {
            numSuccesses.incrementAndGet()
        }

        if (result.isSmall) {
            numSmallPages.incrementAndGet()
        }
    }
}
