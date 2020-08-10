package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.FetchMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import java.time.Instant

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val proxyPoolManager: ProxyPoolManager? = null,
        val driverPoolManager: WebDriverPoolManager,
        val fetchMetrics: FetchMetrics? = null,
        conf: ImmutableConfig,
        id: PrivacyContextId = PrivacyContextId(generateBaseDir())
): PrivacyContext(id, conf) {

    private val browserInstanceId: BrowserInstanceId
    private val driverContext: WebDriverContext
    private var proxyContext: ProxyContext? = null
    var proxyEntry: ProxyEntry? = null
    val numFreeDrivers get() = driverPoolManager.numFreeDrivers
    val numWorkingDrivers get() = driverPoolManager.numWorkingDrivers
    val numAvailableDrivers get() = driverPoolManager.numAvailableDrivers

    init {
        if (proxyPoolManager != null && proxyPoolManager.isEnabled) {
            val proxyPool = proxyPoolManager.proxyPool
            proxyEntry = proxyPoolManager.activeProxyEntries.computeIfAbsent(id.dataDir) {
                proxyPool.take() ?: throw NoProxyException("No proxy found in pool ${proxyPool.javaClass.simpleName} | $proxyPool")
            }
            proxyEntry?.startWork()
        }

        browserInstanceId = BrowserInstanceId.resolve(id.dataDir).apply { proxyServer = proxyEntry?.hostPort }
        driverContext = WebDriverContext(browserInstanceId, driverPoolManager, conf)

        if (proxyPoolManager != null && proxyPoolManager.isEnabled) {
            proxyContext = ProxyContext(proxyEntry, proxyPoolManager, driverContext, conf)
        }
    }

    open suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?: run0(task, browseFun)
    }

    /**
     * Block until all the drivers are closed and the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            report()
            driverContext.shutdown()
            proxyContext?.close()
        }
    }

    override fun report() {
        val isIdle = proxyContext?.proxyEntry?.isIdle == true
        log.info("Privacy context #{}{}{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, if (isIdle) "(idle)" else "", if (isLeaked) "(leaked)" else "", elapsedTime.readable(),
                numSuccesses, String.format("%.2f", numSuccesses.meanRate),
                numSmallPages, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(fetchMetrics?.systemNetworkBytesRecv?:0), Strings.readableBytes(fetchMetrics?.networkBytesRecvPerSecond?:0),
                numTasks, numFinished,
                proxyContext?.proxyEntry
        )

        if (smallPageRate > 0.5) {
            log.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    sequence, numSmallPages, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (numSuccesses.meanRate < 0) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", sequence)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.privacyRetry(task)
        }

        return null
    }

    private suspend fun run0(task: FetchTask, browseFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        beforeRun(task)
        val result = proxyContext?.run(task, browseFun)?:driverContext.run(task, browseFun)
        afterRun(result)
        return result
    }

    private fun beforeRun(task: FetchTask) {
        lastActiveTime = Instant.now()
        numTasks.mark()
        numRunningTasks.incrementAndGet()
    }

    private fun afterRun(result: FetchResult) {
        lastActiveTime = Instant.now()
        numRunningTasks.decrementAndGet()
        numFinished.mark()
        if (result.status.isSuccess) {
            numSuccesses.mark()
        }

        if (result.isSmall) {
            numSmallPages.mark()
        }
    }
}
