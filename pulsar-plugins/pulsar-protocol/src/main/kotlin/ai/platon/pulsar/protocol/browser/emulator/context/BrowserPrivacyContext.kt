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
import com.google.common.annotations.Beta
import org.slf4j.LoggerFactory

open class BrowserPrivacyContext constructor(
    val proxyPoolManager: ProxyPoolManager? = null,
    val driverPoolManager: WebDriverPoolManager,
    val coreMetrics: CoreMetrics? = null,
    conf: ImmutableConfig,
    id: PrivacyContextId
): PrivacyContext(id, conf) {
    private val logger = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)
    private var proxyEntry: ProxyEntry? = null
    private val browserId = BrowserId(id.contextDir, id.fingerprint)
    private val driverContext = WebDriverContext(browserId, driverPoolManager, conf)
    private var proxyContext: ProxyContext? = null
    /**
     * The privacy context is retired but not closed yet.
     * */
    override val isRetired: Boolean get() {
        // driverContext.isActive
        return proxyContext?.isRetired == true || driverContext.isRetired
    }
    /**
     * A ready privacy context has to meet the following requirements:
     *
     * 1. not closed
     * 2. not leaked
     * 3. not idle
     * 4. if there is a proxy, the proxy has to be ready
     * 5. the associated driver pool promises to provide an available driver, ether one of the following:
     *    1. it has slots to create new drivers
     *    2. it has standby drivers
     *
     * Note: this flag does not guarantee consistency, and can change immediately after it's read
     * */
    override val isReady: Boolean get() {
        // NOTICE:
        // too complex state checking, which is very easy to lead to bugs
        val isProxyContextReady = proxyContext == null || proxyContext?.isReady == true
        val isDriverContextReady = driverContext.isReady
        return isProxyContextReady && isDriverContextReady && super.isReady
    }

    override val isFullCapacity: Boolean get() = driverPoolManager.isFullCapacity(browserId)

    @Throws(ProxyException::class)
    override suspend fun doRun(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        initialize(task)

        return checkAbnormalResult(task) ?:
            proxyContext?.run(task, fetchFun) ?:
            driverContext.run(task, fetchFun)
    }

    override fun maintain() {
        proxyContext?.maintain()
        driverContext.maintain()
    }

    override fun promisedWebDriverCount() = driverPoolManager.promisedDriverCount(browserId)

    @Beta
    override fun subscribeWebDriver() = driverPoolManager.subscribeDriver(browserId)

    override fun report() {
        logger.info("Privacy context #{}{}{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, if (isIdle) "(idle)" else "", if (isLeaked) "(leaked)" else "", elapsedTime.readable(),
                meterSuccesses.count, String.format("%.2f", meterSuccesses.meanRate),
                meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate),
                Strings.compactFormat(coreMetrics?.totalNetworkIFsRecvBytes?:0),
                Strings.compactFormat(coreMetrics?.networkIFsRecvBytesPerSecond?:0),
                meterTasks.count, meterFinishes.count,
                proxyContext?.proxyEntry?.toString()
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
            !isActive -> FetchResult.canceled(task, "PRIVACY CX INACTIVE")
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
