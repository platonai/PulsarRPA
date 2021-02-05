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
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val proxyPoolManager: ProxyPoolManager? = null,
        val driverPoolManager: WebDriverPoolManager,
        val fetchMetrics: FetchMetrics? = null,
        conf: ImmutableConfig,
        id: PrivacyContextId
): PrivacyContext(id, conf) {
    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)
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

    override suspend fun doRun(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:
            proxyContext?.run(task, browseFun) ?:
            driverContext.run(task, browseFun)
    }

    override fun report() {
        val isIdle = proxyContext?.proxyEntry?.isIdle == true
        log.info("Privacy context #{}{}{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, if (isIdle) "(idle)" else "", if (isLeaked) "(leaked)" else "", elapsedTime.readable(),
                meterSuccesses.count, String.format("%.2f", meterSuccesses.meanRate),
                meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(fetchMetrics?.totalNetworkIFsRecvBytes?:0),
                Strings.readableBytes(fetchMetrics?.networkIFsRecvBytesPerSecond?:0),
                meterTasks.count, meterFinishes.count,
                proxyContext?.proxyEntry
        )

        if (smallPageRate > 0.5) {
            log.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    sequence, meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (meterSuccesses.meanRate < 0) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", sequence)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
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

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        return when {
            !isActive -> FetchResult.privacyRetry(task)
            else -> null
        }
    }
}
