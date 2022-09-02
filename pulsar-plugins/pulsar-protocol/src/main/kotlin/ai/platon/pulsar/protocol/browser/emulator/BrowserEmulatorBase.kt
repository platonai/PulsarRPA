package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.EmulateSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class BrowserEmulatorBase(
    val driverSettings: WebDriverSettings,
    /**
     * Handle the response
     * */
    val responseHandler: BrowserResponseHandler,
    val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val logger = LoggerFactory.getLogger(BrowserEmulatorBase::class.java)!!
    private val tracer = logger.takeIf { it.isTraceEnabled }
    val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    val closed = AtomicBoolean(false)
    val isActive get() = !closed.get() && AppContext.isActive

    protected val pageSourceByteHistogram by lazy { registry.histogram(this, "hPageSourceBytes") }
    private val registry = AppMetrics.reg
    protected val pageSourceBytes by lazy { registry.meter(this, "pageSourceBytes") }

    val meterNavigates by lazy { registry.meter(this,"navigates") }
    val counterRequests by lazy { registry.counter(this,"requests") }
    val counterJsEvaluates by lazy { registry.counter(this,"jsEvaluates") }
    val counterJsWaits by lazy { registry.counter(this,"jsWaits") }
    val counterCancels by lazy { registry.counter(this,"cancels") }

    override fun getParams(): Params {
        val emulateSettings = EmulateSettings(immutableConfig)
        return Params.of(
                "pageLoadTimeout", emulateSettings.pageLoadTimeout,
                "scriptTimeout", emulateSettings.scriptTimeout,
                "scrollDownCount", emulateSettings.scrollCount,
                "scrollInterval", emulateSettings.scrollInterval,
                "enableStartupScript", driverSettings.enableStartupScript
        )
    }

    open fun createResponse(task: NavigateTask): Response {
        val pageDatum = task.pageDatum
        val length = task.pageSource.length
        pageSourceByteHistogram.update(length)
        pageSourceBytes.mark(length.toLong())

        pageDatum.pageCategory = responseHandler.pageCategorySniffer(pageDatum)
        pageDatum.protocolStatus = responseHandler.checkErrorPage(task.page, pageDatum.protocolStatus)
        if (!pageDatum.protocolStatus.isSuccess) {
            // The browser shows internal error page, which is no value to store
            task.pageSource = ""
            pageDatum.lastBrowser = task.driver.browserType
            return createResponseWithDatum(task, pageDatum)
        }

        // Check if the page source is integral
        val integrity = responseHandler.htmlIntegrityChecker(task.pageSource, task.pageDatum)
        // Check browse timeout event, transform status to be success if the page source is good
        if (pageDatum.protocolStatus.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                pageDatum.protocolStatus = ProtocolStatus.STATUS_SUCCESS
            }
            responseHandler.onBrowseTimeout(task)
        }

        pageDatum.headers.put(HttpHeaders.CONTENT_LENGTH, task.pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            task.pageSource = responseHandler.normalizePageSource(task.url, task.pageSource).toString()
        } else {
            // The page seems to be broken, retry it
            pageDatum.protocolStatus = responseHandler.onPageSourceIsBroken(task.task, integrity)
            logBrokenPage(task.task, task.pageSource, integrity)
        }

        pageDatum.apply {
            lastBrowser = task.driver.browserType
            htmlIntegrity = integrity
            content = task.pageSource.toByteArray(StandardCharsets.UTF_8)
        }

        // Update headers, metadata, do the logging stuff
        return createResponseWithDatum(task, pageDatum)
    }

    open fun createResponseWithDatum(task: NavigateTask, pageDatum: PageDatum): ForwardingResponse {
        val headers = pageDatum.headers

        // The page content's encoding is already converted to UTF-8 by Web driver
        val utf8 = StandardCharsets.UTF_8.name()
        require(utf8 == "UTF-8") { "UTF-8 is expected" }

        headers.put(HttpHeaders.CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_TRUSTED_CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        val urls = pageDatum.activeDomUrls
        if (urls != null) {
            pageDatum.location = urls.location
            if (pageDatum.url != pageDatum.location) {
                // in-browser redirection
                // messageWriter?.debugRedirects(pageDatum.url, urls)
            }
        }

        if (!task.driver.isMockedPageSource) {
            exportIfNecessary(task)
        }

        return ForwardingResponse(task.page, pageDatum)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    @Throws(NavigateTaskCancellationException::class)
    protected fun checkState() {
        if (!isActive) {
            throw NavigateTaskCancellationException("Emulator is closed")
        }
    }

    /**
     * Check the task state.
     * */
    @Throws(NavigateTaskCancellationException::class)
    protected fun checkState(driver: WebDriver) {
        checkState()

        if (driver.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw WebDriverCancellationException("Web driver is canceled #${driver.id}", driver)
        }
    }

    /**
     * Check the task state.
     * */
    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    protected fun checkState(task: FetchTask, driver: WebDriver) {
        checkState()

        if (driver.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw WebDriverCancellationException("Web driver is canceled #${driver.id}", driver)
        }

        if (task.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task #${task.batchTaskId}/${task.batchId} is canceled | ${task.url}")
        }
    }

    fun logBeforeNavigate(task: FetchTask, driverSettings: BrowserSettings) {
        if (logger.isTraceEnabled) {
            val emulateSettings = EmulateSettings(task.volatileConfig)
            logger.trace("Navigate {}/{}/{} in [t{}]{} | {} | timeouts: {}/{}/{}",
                task.batchTaskId, task.batchSize, task.id,
                Thread.currentThread().id,
                if (task.nRetries <= 1) "" else "(${task.nRetries})",
                task.page.configuredUrl,
                emulateSettings.pageLoadTimeout, emulateSettings.scriptTimeout, emulateSettings.scrollInterval
            )
        }
    }

    private fun exportIfNecessary(task: NavigateTask) {
        exportIfNecessary(task.pageSource, task.pageDatum.protocolStatus, task.page)
    }

    /**
     * Export the page if one of the following condition triggered:
     * 1. the first 200 pages
     * 2. LoadOptions.test > 0
     * 3. logger level is debug or lower
     * 4. logger level is info and protocol status is failed
     * */
    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage) {
        if (pageSource.isEmpty()) {
            return
        }

        val id = page.id
        val test = page.options.test
        val shouldExport = id < 200 || id % 100 == 0 || test > 0 || logger.isDebugEnabled || (logger.isInfoEnabled && !status.isSuccess)
        if (shouldExport) {
            val path = AppFiles.export(status, pageSource, page)

            // Create a symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.uniqueSymbolicLinkForUri(page.url)
            try {
                Files.deleteIfExists(link)
                Files.createSymbolicLink(link, path)
            } catch (e: IOException) {
                logger.warn(e.toString())
            }
        }
    }

    private fun logBrokenPage(task: FetchTask, pageSource: String, integrity: HtmlIntegrity) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.uniqueSymbolicLinkForUri(task.url)
        val readableLength = Strings.readableBytes(pageSource.length)

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            logger.warn("{}. Page is {}({}) with {} in {}({}) | file://{}",
                task.page.id,
                integrity.name, readableLength,
                proxyEntry.display, domain, count, link, task.url)
        } else {
            logger.warn("{}. Page is {}({}) | file://{} | {}", task.page.id, integrity.name, readableLength, link, task.url)
        }
    }
}
