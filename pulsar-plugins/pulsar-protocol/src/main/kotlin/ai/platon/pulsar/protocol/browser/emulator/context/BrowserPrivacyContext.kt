package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.PulsarParams.VAR_PRIVACY_CONTEXT_NAME
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext constructor(
    val proxyPoolManager: ProxyPoolManager? = null,
    val driverPoolManager: WebDriverPoolManager,
    val coreMetrics: CoreMetrics? = null,
    conf: ImmutableConfig,
    id: PrivacyContextId
): PrivacyContext(id, conf) {
    private val logger = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)
    var proxyEntry: ProxyEntry? = null
    private val browserId = BrowserId(id.contextDir, id.fingerprint)
    private val driverContext = WebDriverContext(browserId, driverPoolManager, conf)
    private var proxyContext: ProxyContext? = null

    @Throws(NoProxyException::class, ProxyVendorUntrustedException::class)
    override suspend fun doRun(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        initialize(task)

        return checkAbnormalResult(task) ?:
            proxyContext?.run(task, browseFun) ?:
            driverContext.run(task, browseFun)
    }

    override fun maintain() {
        proxyContext?.maintain()
        driverContext.maintain()
    }

    override fun standByDriverCount() = driverPoolManager.standByDriverCount(browserId)

    override fun report() {
        val isIdle = proxyContext?.proxyEntry?.isIdle == true
        logger.info("Privacy context #{}{}{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, if (isIdle) "(idle)" else "", if (isLeaked) "(leaked)" else "", elapsedTime.readable(),
                meterSuccesses.count, String.format("%.2f", meterSuccesses.meanRate),
                meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate),
                Strings.compactFormat(coreMetrics?.totalNetworkIFsRecvBytes?:0),
                Strings.compactFormat(coreMetrics?.networkIFsRecvBytesPerSecond?:0),
                meterTasks.count, meterFinishes.count,
                proxyContext?.proxyEntry
        )

        if (smallPageRate > 0.5) {
            logger.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    sequence, meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (meterSuccesses.meanRate < 0) {
            logger.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", sequence)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    /**
     * Block until all the drivers are closed and the proxy is offline.
     *
     * Closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     *
     * */
    override fun close() {
        logger.debug("Closing browser privacy context ...")
        if (closed.compareAndSet(false, true)) {
            try {
                report()
                driverContext.close()
                proxyContext?.close()
            } catch (e: Exception) {
                logger.warn(e.stringify())
            }
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        return when {
            !isActive -> FetchResult.canceled(task)
            else -> null
        }
    }

    @Throws(ProxyException::class)
    @Synchronized
    private fun initialize(task: FetchTask) {
        createProxyContextIfEnabled()
        task.page.variables[VAR_PRIVACY_CONTEXT_NAME] = display
    }

    private fun createProxyContextIfEnabled() {
        if (proxyEntry == null && proxyPoolManager != null && proxyPoolManager.isEnabled) {
            createProxyContext(proxyPoolManager)
        }
    }

    private fun createProxyContext(proxyPoolManager: ProxyPoolManager) {
        try {
            val pc = ProxyContext.create(id, driverContext, proxyPoolManager, conf)
            proxyEntry = pc.proxyEntry
            // TODO: better initialize fingerprint.proxyServer
            browserId.fingerprint.proxyServer = proxyEntry?.hostPort
            proxyContext = pc
            coreMetrics?.proxies?.mark()
        } catch (e: ProxyException) {
            logger.warn(e.brief("Failed to create proxy context - "))
        }
    }
}
