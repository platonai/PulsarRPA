package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
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
        private val driverManager: WebDriverManager,
        private val messageWriter: MiscMessageWriter,
        private val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(BrowserEmulateEventHandler::class.java)!!
    private val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val metrics = SharedMetricRegistries.getDefault()
    private val pageSourceBytes = metrics.histogram(prependReadableClassName(this, "pageSourceBytes"))
    private val totalPageSourceBytes = metrics.meter(prependReadableClassName(this, "totalPageSourceBytes"))
    private val bannedPages = metrics.meter(prependReadableClassName(this, "bannedPages"))
    private val smallPages = metrics.meter(prependReadableClassName(this, "smallPages"))
    private val smallPageRate = metrics.histogram(prependReadableClassName(this, "smallPageRate"))
    private val emptyPages = metrics.meter(prependReadableClassName(this, "emptyPages"))

    private val numNavigates = AtomicInteger()
    private val driverPool = driverManager.driverPool

    fun logBeforeNavigate(task: FetchTask, driverConfig: BrowserControl) {
        if (log.isTraceEnabled) {
            log.trace("Navigate {}/{}/{} in [t{}]{}, drivers: {}/{}/{}(w/f/o) | {} | timeouts: {}/{}/{}",
                    task.batchTaskId, task.batchSize, task.id,
                    Thread.currentThread().id,
                    if (task.nRetries <= 1) "" else "(${task.nRetries})",
                    driverPool.numWorking, driverPool.numFree, driverPool.numOnline,
                    task.page.configuredUrl,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
            )
        }
    }

    fun onAfterNavigate(task: NavigateTask): Response {
        numNavigates.incrementAndGet()

        val length = task.pageSource.length
        pageSourceBytes.update(length)
        totalPageSourceBytes.mark(length.toLong())

        val browserStatus = checkErrorPage(task.page, task.status)
        task.status = browserStatus.status
        if (browserStatus.code != ProtocolStatusCodes.SUCCESS_OK) {
            // The browser shows it's own error page, which is no value to store
            task.pageSource = ""
            return ForwardingResponse(task.pageSource, task.status, task.headers, task.page)
        }

        // Check if the page source is integral
        val integrity = checkHtmlIntegrity(task.pageSource, task.page, task.status, task.task)

        // Check browse timeout event, transform status to be success if the page source is good
        if (task.status.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                task.status = ProtocolStatus.STATUS_SUCCESS
            }
            handleBrowseTimeout(task)
        }

        task.headers.put(HttpHeaders.CONTENT_LENGTH, task.pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            task.pageSource = handlePageSource(task.pageSource).toString()
        } else {
            // The page seems to be broken, retry it
            task.status = handleBrokenPageSource(task.task, integrity)
            logBrokenPage(task.task, task.pageSource, integrity)
        }

        // Update headers, metadata, do the logging stuff
        task.page.lastBrowser = task.driver.browserType
        task.page.htmlIntegrity = integrity

        // TODO: collect response headers of main resource

        return createResponse(task)
    }

    open fun checkErrorPage(page: WebPage, status: ProtocolStatus): BrowserStatus {
        val browserStatus = BrowserStatus(status, ProtocolStatusCodes.SUCCESS_OK)
        if (status.minorCode == ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT) {
            // The browser can not connect to remote peer, it must be caused by the bad proxy ip
            // It might be fixed by resetting the privacy context
            // log.warn("Connection timed out in browser, resetting the browser context")
            browserStatus.status = ProtocolStatus.retry(RetryScope.PRIVACY, status)
            browserStatus.code = status.minorCode
//            throw PrivacyLeakException()
        } else if (status.minorCode == ProtocolStatusCodes.BROWSER_ERROR) {
            browserStatus.status = ProtocolStatus.retry(RetryScope.CRAWL, status)
            browserStatus.code = status.minorCode
        }

        return browserStatus
    }

    /**
     * Check if the html is integral without field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    open fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        val length = pageSource.length.toLong()
        var integrity = HtmlIntegrity.OK

        if (length == 0L) {
            integrity = HtmlIntegrity.EMPTY_0B
        } else if (length == 39L) {
            integrity = HtmlIntegrity.EMPTY_39B
        }

        // might be caused by web driver exception
        if (integrity.isOK && isBlankBody(pageSource)) {
            integrity = HtmlIntegrity.EMPTY_BODY
        }

        if (integrity.isOK) {
            integrity = checkHtmlIntegrity(pageSource)
        }

        return integrity
    }

    protected fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return HtmlIntegrity.OTHER
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.OTHER
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        // TODO: optimize using region match
        val bodyTag = pageSource.substring(p1, p2)
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return HtmlIntegrity.NO_JS_OK_FLAG
        }

        return HtmlIntegrity.OK
    }

    open fun handleBrowseTimeout(task: NavigateTask) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(task.startTime, Instant.now())
            val length = task.pageSource.length
            val link = AppPaths.uniqueSymbolicLinkForUri(task.page.url)
            val driverConfig = task.driverConfig
            log.info("Timeout ({}) after {} with {} drivers: {}/{}/{} timeouts: {}/{}/{} | file://{}",
                    task.status.minorName,
                    elapsed,
                    Strings.readableBytes(length.toLong()),
                    driverPool.numWorking, driverPool.numFree, driverPool.numOnline,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    link)
        }
    }

    open fun createResponse(task: NavigateTask): ForwardingResponse {
        val response = ForwardingResponse(task.pageSource, task.status, task.headers, task.page)
        val headers = task.headers
        val page = task.page

        // The page content's encoding is already converted to UTF-8 by Web driver
        val utf8 = StandardCharsets.UTF_8.name()
        require(utf8 == "UTF-8")
        headers.put(HttpHeaders.CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_TRUSTED_CONTENT_ENCODING, utf8)
        headers.put(HttpHeaders.Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        // TODO: Update all page data in the same place
        val urls = page.activeDomUrls
        if (urls != null) {
            response.location = urls.location
            if (page.url != response.location) {
                // in-browser redirection
                messageWriter.debugRedirects(page.url, urls)
            }
        }

        exportIfNecessary(task)

        return response
    }

    fun handlePageSource(pageSource: String): StringBuilder {
        // The browser has already convert source code to UTF-8
        return replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error
     * */
    fun handleChromeError(message: String): BrowserError {
        val activeDomMessage = ActiveDomMessage.fromJson(message)
        val status = if (activeDomMessage.multiStatus?.status?.ec == BrowserError.CONNECTION_TIMED_OUT) {
            // chrome can not connect to the peer, it probably be caused by a bad proxy
            // convert to retry in PRIVACY_CONTEXT later
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT)
        } else {
            // unhandled exception
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERROR)
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
                smallPageRate.update(100 * smallPages.count / numNavigates.get())
            }
            else -> ProtocolStatus.retry(RetryScope.CRAWL, htmlIntegrity)
        }
    }

    private fun exportIfNecessary(task: NavigateTask) {
        exportIfNecessary(task.pageSource, task.status, task.page, task.driver)
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver) {
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
            log.warn("Page is {}({}) with {} in {}({}) | file://{} | {}",
                    integrity.name, readableLength,
                    proxyEntry.display, domain, count, link, task.url)
        } else {
            log.warn("Page is {}({}) | file://{} | {}", integrity.name, readableLength, link, task.url)
        }
    }
}
