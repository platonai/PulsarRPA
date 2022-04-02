package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.EmulateSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.BROWSER_TAKE_SCREENSHOT
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.WebDriverAdapter
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

open class EventHandler(
        private val driverPoolManager: WebDriverPoolManager,
        private val messageWriter: MiscMessageWriter? = null,
        private val immutableConfig: ImmutableConfig
) {
    protected val logger = LoggerFactory.getLogger(EventHandler::class.java)!!
    protected val tracer = logger.takeIf { it.isTraceEnabled }
    protected val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    protected val takeScreenshot get() = immutableConfig.getBoolean(BROWSER_TAKE_SCREENSHOT, false)
    protected val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN

    protected val numNavigates = AtomicInteger()
    protected val jsInvadingEnabled = driverPoolManager.driverFactory.driverSettings.jsInvadingEnabled

    private val registry = AppMetrics.defaultMetricRegistry
    protected val pageSourceBytes by lazy { registry.meter(this, "pageSourceBytes") }
    protected val pageSourceByteHistogram by lazy { registry.histogram(this, "hPageSourceBytes") }
    protected val bannedPages by lazy { registry.meter(this, "bannedPages") }
    protected val notFoundPages by lazy { registry.meter(this, "notFoundPages") }
    protected val smallPages by lazy { registry.meter(this, "smallPages") }
    protected val smallPageRate get() = 100 * smallPages.count / numNavigates.get()
    protected val smallPageRateHistogram by lazy { registry.histogram(this, "smallPageRate") }
    protected val emptyPages by lazy { registry.meter(this, "emptyPages") }

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

    fun onAfterNavigate(task: NavigateTask): Response {
        numNavigates.incrementAndGet()

        val pageDatum = task.pageDatum
        val length = task.pageSource.length
        pageSourceByteHistogram.update(length)
        pageSourceBytes.mark(length.toLong())

        pageDatum.pageCategory = sniffPageCategory(task.page)
        pageDatum.protocolStatus = checkErrorPage(task.page, pageDatum.protocolStatus)
        if (!pageDatum.protocolStatus.isSuccess) {
            // The browser shows internal error page, which is no value to store
            task.pageSource = ""
            pageDatum.lastBrowser = task.driver.browserType
            return createResponse(task, pageDatum)
        }

        // Check if the page source is integral
        val integrity = checkHtmlIntegrity(task.pageSource, task.page, pageDatum.protocolStatus, task)
        // Check browse timeout event, transform status to be success if the page source is good
        if (pageDatum.protocolStatus.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                pageDatum.protocolStatus = ProtocolStatus.STATUS_SUCCESS
            }
            handleBrowseTimeout(task)
        }

        pageDatum.headers.put(HttpHeaders.CONTENT_LENGTH, task.pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            task.pageSource = normalizePageSource(task.url, task.pageSource).toString()
        } else {
            // The page seems to be broken, retry it
            pageDatum.protocolStatus = handleBrokenPageSource(task.task, integrity)
            logBrokenPage(task.task, task.pageSource, integrity)
        }

        pageDatum.apply {
            lastBrowser = task.driver.browserType
            htmlIntegrity = integrity
            content = task.pageSource.toByteArray(StandardCharsets.UTF_8)
        }

        // Update headers, metadata, do the logging stuff
        return createResponse(task, pageDatum)
    }

    open fun sniffPageCategory(page: WebPage): OpenPageCategory {
        return OpenPageCategory(PageCategory.UNKNOWN)
    }

    open fun checkErrorPage(page: WebPage, status: ProtocolStatus): ProtocolStatus {
        return status
    }

    /**
     * Check if the html is integral before field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    open fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: NavigateTask): HtmlIntegrity {
        val length = pageSource.length.toLong()

        return when {
            length == 0L -> HtmlIntegrity.EMPTY_0B
            length == 39L -> HtmlIntegrity.EMPTY_39B
            isBlankBody(pageSource) -> HtmlIntegrity.BLANK_BODY
            else -> checkHtmlIntegrity(pageSource)
        }
    }

    open fun normalizePageSource(url: String, pageSource: String): StringBuilder {
        // The browser has already convert source code to UTF-8
        return replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    open fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val p0 = pageSource.indexOf("</head>")
        val p1 = pageSource.indexOf("<body", p0)
        if (p1 <= 0) return HtmlIntegrity.OTHER
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.OTHER
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        if (jsInvadingEnabled) {
            // TODO: optimize using region match
            val bodyTag = pageSource.substring(p1, p2)
            tracer?.trace("Body tag: $bodyTag")
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
            val r = bodyTag.contains("data-error=\"0\"")
            if (!r) {
                return HtmlIntegrity.NO_JS_OK_FLAG
            }
        }

        return HtmlIntegrity.OK
    }

    open fun handleBrowseTimeout(task: NavigateTask) {
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

    open fun createResponse(task: NavigateTask, pageDatum: PageDatum): ForwardingResponse {
        val headers = pageDatum.headers

        // The page content's encoding is already converted to UTF-8 by Web driver
        val utf8 = StandardCharsets.UTF_8.name()
        require(utf8 == "UTF-8")
        headers.put(HttpHeaders.CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_TRUSTED_CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        val urls = pageDatum.activeDomUrls
        if (urls != null) {
            pageDatum.location = urls.location
            if (pageDatum.url != pageDatum.location) {
                // in-browser redirection
                messageWriter?.debugRedirects(pageDatum.url, urls)
            }
        }

        if (!task.driver.isMockedPageSource) {
            exportIfNecessary(task)
            takeScreenshotIfNecessary(task)
        }

        return ForwardingResponse(task.page, pageDatum)
    }

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error
     * */
    fun handleChromeErrorPage(message: String): BrowserError {
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

    fun handleBrokenPageSource(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus {
        return when {
            // should cancel all running tasks and reset the privacy context and then re-fetch them
            htmlIntegrity.isBanned -> ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity).also { bannedPages.mark() }
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

    private fun exportIfNecessary(task: NavigateTask) {
        exportIfNecessary(task.pageSource, task.pageDatum.protocolStatus, task.page)
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage) {
        if (pageSource.isEmpty()) {
            return
        }

        val test = page.options.test
        val shouldExport = logger.isDebugEnabled || test > 0 || (logger.isInfoEnabled && !status.isSuccess)
        if (shouldExport) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
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

    private fun takeScreenshotIfNecessary(task: NavigateTask) {
        if (takeScreenshot && task.pageDatum.protocolStatus.isSuccess) {
            val driver = task.driver
            if (driver is WebDriverAdapter) {
                // takeScreenshot(task.pageDatum.contentLength, task.page, driver.driver as RemoteWebDriver)
            }
        }
    }

    private fun takeScreenshot(contentLength: Long, page: WebPage, driver: WebDriver) {
//        try {
//            val bytes = driver.getScreenshotAs(OutputType.BYTES)
//            val readableLength = Strings.readableBytes(bytes.size)
//            val filename = AppPaths.fromUri(page.url, "", ".png")
//            val path = ExportPaths.get("screenshot", filename)
//            AppFiles.saveTo(bytes, path, true)
//            page.metadata[Name.SCREENSHOT_EXPORT_PATH] = path.toString()
//            logger.info("{}. Screenshot is exported ({}) | {}", page.id, readableLength, path)
//        } catch (e: ScreenshotException) {
//            logger.warn("{}. Screenshot failed {} | {}", page.id, Strings.readableBytes(contentLength), e.message)
//        } catch (e: Exception) {
//            logger.warn(e.stringify())
//        }
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
