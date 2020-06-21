package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.BrowserInstanceId
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val proxyPoolMonitor: ProxyPoolMonitor,
        val driverManager: WebDriverManager,
        val conf: ImmutableConfig,
        id: PrivacyContextId = PrivacyContextId(generateBaseDir())
): PrivacyContext(id) {

    private val browserInstanceId: BrowserInstanceId
    private var proxyEntry: ProxyEntry? = null
    private val driverContext: WebDriverContext
    private val proxyContext: ProxyContext

    init {
        if (proxyPoolMonitor.isEnabled) {
            val proxyPool = proxyPoolMonitor.proxyPool
            proxyEntry = proxyPoolMonitor.activeProxyEntries.computeIfAbsent(id.dataDir) {
                proxyPool.take() ?: throw NoProxyException("No proxy found in pool ${proxyPool.javaClass.simpleName} | $proxyPool")
            }
            proxyEntry?.startWork()
        }

        browserInstanceId = BrowserInstanceId.resolve(id.dataDir).apply { proxyServer = proxyEntry?.hostPort }
        driverContext = WebDriverContext(browserInstanceId, driverManager, conf)
        proxyContext = ProxyContext(proxyEntry, proxyPoolMonitor, driverContext, conf)
    }

    open suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) return FetchResult.privacyRetry(task)
        beforeRun(task)
        return proxyContext.run(task, browseFun).also { afterRun(it) }
    }

    /**
     * Block until all the drivers are closed and the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverContext.close()
            proxyContext.close()
            report()
        }
    }

    override fun report() {
        log.info("Privacy context #{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, elapsedTime.readable(),
                numSuccesses, String.format("%.2f", throughput),
                numSmallPages, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(systemNetworkBytesRecv), Strings.readableBytes(networkSpeed),
                numTasks, numTotalRun,
                proxyContext.proxyEntry
        )

        if (smallPageRate > 0.5) {
            log.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    sequence, numSmallPages, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (throughput < 0) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", sequence)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
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
