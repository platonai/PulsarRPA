package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.PageDatum
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.PageCategory
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import com.codahale.metrics.SharedMetricRegistries
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

open class BrowserEmulateEventHandler(
        private val driverPoolManager: WebDriverPoolManager,
        private val messageWriter: MiscMessageWriter,
        private val immutableConfig: ImmutableConfig
) {
    protected val log = LoggerFactory.getLogger(BrowserEmulateEventHandler::class.java)!!
    protected val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    protected val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    protected val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN

    protected val numNavigates = AtomicInteger()
    protected val jsInvadingEnabled = driverPoolManager.driverControl.jsInvadingEnabled

    protected val metrics = SharedMetricRegistries.getDefault()
    protected val pageSourceBytes = metrics.histogram(prependReadableClassName(this, "pageSourceBytes"))
    protected val totalPageSourceBytes = metrics.meter(prependReadableClassName(this, "totalPageSourceBytes"))
    protected val bannedPages = metrics.meter(prependReadableClassName(this, "bannedPages"))
    protected val smallPages = metrics.meter(prependReadableClassName(this, "smallPages"))
    protected val smallPageRate get() = 100 * smallPages.count / numNavigates.get()
    protected val smallPageRateHistogram = metrics.histogram(prependReadableClassName(this, "smallPageRate"))
    protected val emptyPages = metrics.meter(prependReadableClassName(this, "emptyPages"))

    fun logBeforeNavigate(task: FetchTask, driverConfig: BrowserControl) {
        if (log.isTraceEnabled) {
            log.trace("Navigate {}/{}/{} in [t{}]{} | {} | timeouts: {}/{}/{}",
                    task.batchTaskId, task.batchSize, task.id,
                    Thread.currentThread().id,
                    if (task.nRetries <= 1) "" else "(${task.nRetries})",
                    task.page.configuredUrl,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
            )
        }
    }

    fun onAfterNavigate(task: NavigateTask): Response {
        numNavigates.incrementAndGet()

        val pageDatum = task.pageDatum
        val length = task.pageSource.length
        pageSourceBytes.update(length)
        totalPageSourceBytes.mark(length.toLong())

        pageDatum.pageCategory = sniffPageCategory(task.page)
        pageDatum.status = checkErrorPage(task.page, pageDatum.status)
        if (!pageDatum.status.isSuccess) {
            // The browser shows internal error page, which is no value to store
            task.pageSource = ""
            pageDatum.lastBrowser = task.driver.browserType
            return createResponse(task, pageDatum)
        }

        // Check if the page source is integral
        val integrity = checkHtmlIntegrity(task.pageSource, task.page, pageDatum.status, task.task)
        // Check browse timeout event, transform status to be success if the page source is good
        if (pageDatum.status.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                pageDatum.status = ProtocolStatus.STATUS_SUCCESS
            }
            handleBrowseTimeout(task)
        }

        pageDatum.headers.put(HttpHeaders.CONTENT_LENGTH, task.pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            task.pageSource = normalizePageSource(task.pageSource).toString()
        } else {
            // The page seems to be broken, retry it
            pageDatum.status = handleBrokenPageSource(task.task, integrity)
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

    open fun sniffPageCategory(page: WebPage): PageCategory {
        return PageCategory.UNKNOWN
    }

    open fun checkErrorPage(page: WebPage, status: ProtocolStatus): ProtocolStatus {
        return status
    }

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    open fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        val length = pageSource.length.toLong()

        return when {
            length == 0L -> HtmlIntegrity.EMPTY_0B
            length == 39L -> HtmlIntegrity.EMPTY_39B
            isBlankBody(pageSource) -> HtmlIntegrity.BLANK_BODY
            else -> checkHtmlIntegrity(pageSource)
        }
    }

    open fun normalizePageSource(pageSource: String): StringBuilder {
        // The browser has already convert source code to UTF-8
        return replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    open fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return HtmlIntegrity.OTHER
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.OTHER
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        if (jsInvadingEnabled) {
            // TODO: optimize using region match
            val bodyTag = pageSource.substring(p1, p2)
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
            val r = bodyTag.contains("data-error=\"0\"")
            if (!r) {
                return HtmlIntegrity.NO_JS_OK_FLAG
            }
        }

        return HtmlIntegrity.OK
    }

    open fun handleBrowseTimeout(task: NavigateTask) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(task.startTime, Instant.now())
            val length = task.pageSource.length
            val link = AppPaths.uniqueSymbolicLinkForUri(task.page.url)
            val driverConfig = task.driverConfig
            log.info("Timeout ({}) after {} with {} timeouts: {}/{}/{} | file://{}",
                    task.pageDatum.status.minorName,
                    elapsed,
                    Strings.readableBytes(length.toLong()),
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
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
                messageWriter.debugRedirects(pageDatum.url, urls)
            }
        }

        exportIfNecessary(task)

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
            // must come after privacy context reset, PRIVACY_CONTEXT reset have the higher priority
            task.nRetries > fetchMaxRetry -> ProtocolStatus.retry(RetryScope.CRAWL)
                    .also { log.info("Retry task ${task.id} in the next crawl round") }
            htmlIntegrity.isEmpty -> ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity).also { emptyPages.mark() }
            htmlIntegrity.isSmall -> ProtocolStatus.retry(RetryScope.CRAWL, htmlIntegrity).also {
                smallPages.mark()
                smallPageRateHistogram.update(smallPageRate)
            }
            else -> ProtocolStatus.retry(RetryScope.CRAWL, htmlIntegrity)
        }
    }

    private fun exportIfNecessary(task: NavigateTask) {
        exportIfNecessary(task.pageSource, task.pageDatum.status, task.page)
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage) {
        if (pageSource.isEmpty()) {
            return
        }

        val shouldExport = (log.isInfoEnabled && !status.isSuccess) || log.isDebugEnabled
        if (shouldExport) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.uniqueSymbolicLinkForUri(page.url)
            try {
                Files.deleteIfExists(link)
                Files.createSymbolicLink(link, path)
            } catch (e: IOException) {
                log.warn(e.toString())
            }

            if (log.isTraceEnabled) {
                // takeScreenshot(pageSource.length.toLong(), page, driver.driver as RemoteWebDriver)
            }
        }
    }

    fun takeScreenshot(contentLength: Long, page: WebPage, driver: RemoteWebDriver) {
        try {
            if (contentLength > 100) {
                val bytes = driver.getScreenshotAs(OutputType.BYTES)
                AppFiles.export(page, bytes, ".png")
            }
        } catch (e: Exception) {
            log.warn("Screenshot failed {} | {}", Strings.readableBytes(contentLength), page.url)
            log.warn(Strings.stringifyException(e))
        }
    }

    fun logBrokenPage(task: FetchTask, pageSource: String, integrity: HtmlIntegrity) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.uniqueSymbolicLinkForUri(task.url)
        val readableLength = Strings.readableBytes(pageSource.length.toLong())

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("%3d. Page is {}({}) with {} in {}({}) | file://{}",
                    task.page.id,
                    integrity.name, readableLength,
                    proxyEntry.display, domain, count, link, task.url)
        } else {
            log.warn("%3d. Page is {}({}) | file://{}", task.page.id, integrity.name, readableLength, link)
        }
    }
}
