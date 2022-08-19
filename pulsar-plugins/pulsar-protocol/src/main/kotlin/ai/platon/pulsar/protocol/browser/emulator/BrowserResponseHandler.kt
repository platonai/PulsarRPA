package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.EmulateSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

open class BrowserResponseHandler(
        private val driverPoolManager: WebDriverPoolManager,
        private val messageWriter: MiscMessageWriter? = null,
        private val immutableConfig: ImmutableConfig
) {
    protected val logger = LoggerFactory.getLogger(BrowserResponseHandler::class.java)!!
    protected val tracer = logger.takeIf { it.isTraceEnabled }
    protected val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    protected val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN

    protected val enableStartupScript get() = driverPoolManager.driverFactory.driverSettings.enableStartupScript

    private val registry = AppMetrics.defaultMetricRegistry
    protected val pageSourceBytes by lazy { registry.meter(this, "pageSourceBytes") }
    protected val bannedPages by lazy { registry.meter(this, "bannedPages") }
    protected val notFoundPages by lazy { registry.meter(this, "notFoundPages") }

    protected val numNavigates = AtomicInteger()
    protected val smallPages by lazy { registry.meter(this, "smallPages") }
    protected val smallPageRate get() = 100 * smallPages.count / numNavigates.get()
    protected val smallPageRateHistogram by lazy { registry.histogram(this, "smallPageRate") }
    protected val emptyPages by lazy { registry.meter(this, "emptyPages") }

    val pageCategorySniffer: CombinedPageCategorySniffer = CombinedPageCategorySniffer(immutableConfig).apply {
        sniffers.add(DefaultPageCategorySniffer(immutableConfig))
    }

    var htmlIntegrityChecker: CombinedHtmlIntegrityChecker = CombinedHtmlIntegrityChecker(immutableConfig).apply {
        checkers.add(DefaultHtmlIntegrityChecker(enableStartupScript, immutableConfig))
    }

    open fun onResponseWillBeCreated(task: FetchTask, driver: WebDriver) {
        numNavigates.incrementAndGet()
    }

    open fun onResponseDidCreated(task: FetchTask, driver: WebDriver, response: Response) {

    }

    open fun checkErrorPage(page: WebPage, status: ProtocolStatus): ProtocolStatus {
        return status
    }

    /**
     * Normalize the page source.
     *
     * The browser has already converted source code to be UTF-8, so we replace the charset meta tags to be UTF-8.
     * TODO: or we insert a new metadata to indicate the charset
     */
    open fun normalizePageSource(url: String, pageSource: String): StringBuilder {
        return HtmlUtils.replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    open fun onBrowseTimeout(task: NavigateTask) {
        if (logger.isInfoEnabled) {
            val elapsed = Duration.between(task.startTime, Instant.now())
            val length = task.pageSource.length
            val link = AppPaths.uniqueSymbolicLinkForUri(task.page.url)
            val emulateSettings = EmulateSettings(task.task.volatileConfig)
            logger.info(
                "Timeout ({}) after {} with {} timeouts: {}/{}/{} | file://{}",
                task.pageDatum.protocolStatus.minorName,
                elapsed,
                Strings.readableBytes(length),
                emulateSettings.pageLoadTimeout, emulateSettings.scriptTimeout, emulateSettings.scrollInterval,
                link)
        }
    }

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error.
     * */
    fun onChromeErrorPageReturn(message: String): BrowserError {
        val activeDomMessage = ActiveDomMessage.fromJson(message)
        val ec = activeDomMessage.multiStatus?.status?.ec
        // chrome can not connect to the peer, it probably be caused by a bad proxy
        // convert to retry in PRIVACY_CONTEXT later
        val status = when (ec) {
            BrowserError.CONNECTION_TIMED_OUT -> ProtocolStatus.retry(RetryScope.PRIVACY, ec)
            BrowserError.EMPTY_RESPONSE -> ProtocolStatus.retry(RetryScope.PRIVACY, ec)
            else -> {
                // unexpected exception
                ProtocolStatus.retry(RetryScope.CRAWL, ec)
            }
        }

        return BrowserError(status, activeDomMessage)
    }

    fun onPageSourceIsBroken(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus {
        return when {
            // should cancel all running tasks and reset the privacy context and then re-fetch them
            htmlIntegrity.isRobotCheck -> ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity).also { bannedPages.mark() }
            htmlIntegrity.isForbidden -> ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity).also { bannedPages.mark() }
            htmlIntegrity.isNotFound -> ProtocolStatus.failed(ProtocolStatus.NOT_FOUND).also { notFoundPages.mark() }
            // must come after privacy context reset, PRIVACY_CONTEXT reset have the higher priority
            htmlIntegrity.isEmpty -> ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity).also { emptyPages.mark() }
            htmlIntegrity.isSmall -> ProtocolStatus.retry(RetryScope.CRAWL, htmlIntegrity).also {
                smallPages.mark()
                smallPageRateHistogram.update(smallPageRate)
            }
            else -> ProtocolStatus.retry(RetryScope.CRAWL, htmlIntegrity)
        }
    }

}
