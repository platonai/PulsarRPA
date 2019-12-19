package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.AppConstants.CMD_WEB_DRIVER_CLOSE_ALL
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.BatchStat
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.data.BrowserJsData
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.proxy.InternalProxyServer
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang.StringUtils
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(config: ImmutableConfig): this(
            config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(60)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            config.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60)),
            config.getInt(FETCH_SCROLL_DOWN_COUNT, 5),
            config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
    )
}

class FetchTask(
        val batchId: Int,
        val taskId: Int,
        val priority: Int,
        val page: WebPage,
        val volatileConfig: VolatileConfig,
        var batchSize: Int = 1,
        val stat: BatchStat? = null,
        var deleteAllCookies: Boolean = false,
        var closeBrowsers: Boolean = false
) {
    val url get() = page.url
    val domain get() = URLUtil.getDomainName(url)
    val incognito = deleteAllCookies && closeBrowsers
    lateinit var response: Response

    companion object {
        val NIL = FetchTask(0, 0, 0, WebPage.NIL, VolatileConfig.EMPTY)
    }
}

class FetchResult(
        val task: FetchTask,
        var response: Response
)

private class RetrieveContentResult(
        var response: Response? = null,
        var exception: Exception? = null,
        var driverRetired: Boolean = false
)

private data class VisitResult(
        val protocolStatus: ProtocolStatus,
        val jsData: BrowserJsData? = null
) {
    companion object {
        val canceled = VisitResult(ProtocolStatus.STATUS_CANCELED, BrowserJsData.default)
    }
}

class IncompleteContentException: Exception {
    constructor() : super() {}

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
class SeleniumEngine(
        browserControl: BrowserControl,
        private val driverPool: WebDriverPool,
        private val internalProxyServer: InternalProxyServer,
        private val fetchTaskTracker: FetchTaskTracker,
        private val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(SeleniumEngine::class.java)!!

    private val monthDay = DateTimeUtil.now("MMdd")

    private val libJs = browserControl.parseLibJs(false)
    private val clientJs = browserControl.parseJs(false)
    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    private var charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private var fetchMaxRetry = immutableConfig.getInt(HTTP_FETCH_MAX_RETRY, 3)
    private val defaultDriverConfig = DriverConfig(immutableConfig)
    private val maxCookieView = 40
    private val isISPEnabled get() = internalProxyServer.isEnabled
    private val closed = AtomicBoolean(false)
    private val isClosed get() = closed.get()

    init {
        instanceCount.incrementAndGet()
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "instanceCount", instanceCount,
                "charsetPattern", StringUtils.abbreviateMiddle(charsetPattern.toString(), "...", 200),
                "pageLoadTimeout", defaultDriverConfig.pageLoadTimeout,
                "scriptTimeout", defaultDriverConfig.scriptTimeout,
                "scrollDownCount", defaultDriverConfig.scrollDownCount,
                "scrollInterval", defaultDriverConfig.scrollInterval,
                "clientJsLength", clientJs.length,
                "webDriverCapacity", driverPool.capacity
        )
    }

    internal fun fetchTaskInternal(task: FetchTask): FetchResult {
        val response: Response
        if (isClosed) {
            log.info("System is closed, cancel the task | {}", task.url)
            response = ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED)
        } else {
            fetchTaskTracker.totalTaskCount.getAndIncrement()
            fetchTaskTracker.batchTaskCounters.computeIfAbsent(task.batchId) { AtomicInteger() }.incrementAndGet()
            response = fetchTaskWithRetry(task)
        }

        return FetchResult(task, response)
    }

    private fun fetchTaskWithRetry(task: FetchTask): Response {
        val maxRetry = task.volatileConfig.getInt(HTTP_FETCH_MAX_RETRY, fetchMaxRetry)
        var result = RetrieveContentResult(null, null)

        var i = 0
        while (i++ < maxRetry && result.response == null && !isClosed && !Thread.currentThread().isInterrupted) {
            if (i > 1) {
                log.warn("Round {} retrying another Web driver ... | {}", i, task.url)
            }

            val driver = driverPool.poll(task.priority, task.volatileConfig)?: continue
            driver.incognito = task.incognito
            try {
                result = doRetrieveContent(task, driver)
            } finally {
                afterRetrieveContent(task, driver, result)
            }

            val r = result.response
            if (i > 1 && r != null && r.status.isSuccess && r.length() > 100_1000) {
                log.info("Retried {} times and retrieved a good page with length {} | {}", i, r.length(), task.url)
            }
        }

        val response = result.response
        val exception = result.exception
        return when {
            response != null -> response
            exception != null -> ForwardingResponse(task.url, ProtocolStatus.failed(exception))
            else -> ForwardingResponse(task.url, ProtocolStatus.STATUS_FAILED)
        }
    }

    private fun afterRetrieveContent(task: FetchTask, driver: ManagedWebDriver, result: RetrieveContentResult) {
        if (task.deleteAllCookies) {
            log.info("Deleting all cookies after task {}-{} under {}", task.batchId, task.taskId, task.domain)
            driver.deleteAllCookiesSilently()
            task.deleteAllCookies = false
        }

        if (result.driverRetired) {
            driverPool.retire(driver, result.exception)
        } else {
            driverPool.offer(driver)
        }

        if (RuntimeUtils.hasLocalFileCommand(CMD_WEB_DRIVER_CLOSE_ALL)) {
            log.info("Executing local file command {}", CMD_WEB_DRIVER_CLOSE_ALL)
            driverPool.closeAll()
            task.closeBrowsers = false
        }

        if (task.closeBrowsers) {
            driverPool.closeAll()
            task.closeBrowsers = false
        }
    }

    private fun doRetrieveContent(task: FetchTask, driver: ManagedWebDriver): RetrieveContentResult {
        var driverRetired = false
        var response: Response?
        var exception: Exception? = null

        try {
            if (isISPEnabled) {
                response = internalProxyServer.run {
                    visitPageAndRetrieveContent(driver, task)
                }
                driver.proxyEntry = internalProxyServer.proxyEntry
            } else {
                response = visitPageAndRetrieveContent(driver, task)
            }

            val proxyEntry = driver.proxyEntry
            if (proxyEntry != null) {
                proxyEntry.servedDomains.add(task.domain)
                if (response.status.isSuccess) {
                    proxyEntry.targetHost = task.page.url
                }
            }
        } catch (e: NoProxyException) {
            log.warn("No proxy, request is canceled - {}", task.url)
            response = ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.warn("Web driver is crashed, no session - {}", StringUtil.simplifyException(e))

            response = null
            exception = e
            driverRetired = true
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            log.warn("Web driver is crashed, disconnected - {}", StringUtil.simplifyException(e))

            response = null
            exception = e
            driverRetired = true
        } catch (e: IncompleteContentException) {
            val proxyEntry = driver.proxyEntry
            handleIncompleteContent(task, driver, e)

            response = null
            exception = e
            if (isISPEnabled) {
                log.info("Received an incomplete page, change proxy {} and try again | {}", proxyEntry?.display, task.url)
                internalProxyServer.proxyExpired()
            } else {
                driverRetired = true
            }
        } finally {
            if (isISPEnabled) {
                driver.proxyEntry = internalProxyServer.proxyEntry
            }

            val proxyEntry = driver.proxyEntry
            if (proxyEntry != null) {
                task.page.metadata.set(Name.PROXY, proxyEntry.hostPort)
            }
        }

        return RetrieveContentResult(response, exception, driverRetired)
    }

    private fun handleIncompleteContent(task: FetchTask, driver: ManagedWebDriver, e: IncompleteContentException) {
        val proxyEntry = driver.proxyEntry
        val domain = task.domain
        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("Content incomplete. driver: {} domain: {}({}) proxy: {} - {}",
                    driver, domain, count, proxyEntry.display, StringUtil.simplifyException(e))
        } else {
            log.warn("Content incomplete - {}", StringUtil.simplifyException(e))
        }

        // delete all cookie, change proxy, and retry
        log.info("Deleting all cookies under {}", domain)
        driver.deleteAllCookiesSilently()
    }

    private fun visitPageAndRetrieveContent(driver: ManagedWebDriver, task: FetchTask): Response {
        val batchId = task.batchId
        val page = task.page
        // page.location is the last working address, and page.url is the permanent internal address

        // TODO: redirection (inside browser) is not handled
        val url = page.url
        var location = page.location
        if (location.isBlank()) {
            location = url
        }
        if (url != location) {
            log.warn("Page location does't match url | {} - {}", url, location)
        }

        val driverConfig = getDriverConfig(task.volatileConfig)
        var status: ProtocolStatus
        var jsData: BrowserJsData? = null
        val headers = MultiMetadata()
        val startTime = System.currentTimeMillis()
        headers.put(Q_REQUEST_TIME, startTime.toString())

        var pageSource = ""

        try {
            val result = visit(task, driver.driver, driverConfig)
            status = result.protocolStatus
            jsData = result.jsData
            // TODO: handle with frames
            // driver.switchTo().frame(1);

            pageSource = getPageSourceSilently(driver)
        } catch (e: org.openqa.selenium.TimeoutException) {
            // log.warn(e.toString())
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_RETRY
            log.warn(e.message)
        } catch (e: org.openqa.selenium.UnhandledAlertException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_RETRY
            log.warn(StringUtil.simplifyException(e)?:"UnhandledAlertException")
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_BROWSER_RETRY
            log.warn("Web driver is crashed - {}", StringUtil.simplifyException(e))
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_BROWSER_RETRY
            log.warn("Web driver is crashed - {}", StringUtil.simplifyException(e))
        } catch (e: org.openqa.selenium.WebDriverException) {
            status = ProtocolStatus.STATUS_RETRY
            log.warn("Unexpected WebDriver exception", e)
        } catch (e: Throwable) {
            // must not throw again
            status = ProtocolStatus.STATUS_EXCEPTION
            log.warn("Unexpected exception", e)
        }

        if (pageSource.isNotEmpty()) {
            // throw an exception if content is incomplete
            checkContentIntegrity(pageSource, page, status, task)
        }

        val length = pageSource.length
        if (status.minorCode == ProtocolStatusCodes.WEB_DRIVER_TIMEOUT
                || status.minorCode == ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT) {
            status = handleTimeout(startTime, pageSource, status, page, driverConfig)
        }

        handleFetchFinish(page, driver, headers)
        page.browserJsData = jsData
        pageSource = handlePageSource(pageSource, status, page, driver)
        headers.put(CONTENT_LENGTH, length.toString())
        if (status.isSuccess) {
            handleFetchSuccess(batchId)
        }

        // TODO: handle redirect
        // TODO: collect response header
        // TODO: fetch only the major pages, css, js, etc, ignore the rest resources, ignore external resources
        // TODO: ignore timeout and get the page source

        val response = ForwardingResponse(page.url, pageSource, status, headers)
        eagerUpdateWebPage(page, response, immutableConfig)
        return response
    }

    /**
     * Eager update web page, the status is incomplete but required by callbacks
     * */
    private fun eagerUpdateWebPage(page: WebPage, response: Response, conf: ImmutableConfig) {
        page.protocolStatus = response.status
        val bytes = response.content
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        val content = Content(page.url, page.location, bytes, contentType, response.headers, conf)
        FetchComponent.updateContent(page, content)
    }

    private fun checkContentIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask) {
        val url = page.url
        val length = pageSource.length.toLong()
        val aveLength = page.aveContentBytes.toLong()
        val readableLength = StringUtil.readableByteCount(length)
        val readableAveLength = StringUtil.readableByteCount(aveLength)

        var message = "Retrieved: (only) $readableLength, " +
                "history average: $readableAveLength, " +
                "might be caused by network/proxy | $url"

        if (length < 100 && pageSource.indexOf("<html") != -1 && pageSource.lastIndexOf("</html>") != -1) {
            throw IncompleteContentException(message)
        }

        if (page.fetchCount > 0 && aveLength > 100_1000 && length < 0.1 * aveLength) {
            throw IncompleteContentException(message)
        }

        val stat = task.stat
        if (status.isSuccess && stat != null) {
            val batchAveLength = stat.averagePageSize.roundToLong()
            if (stat.numSuccessTasks > 3 && batchAveLength > 10_000 && length < batchAveLength / 10) {
                val readableBatchAveLength = StringUtil.readableByteCount(batchAveLength)

                message = "Retrieved: (only) $readableLength, " +
                        "history average: $readableAveLength, " +
                        "batch average: $readableBatchAveLength" +
                        "might be caused by network/proxy | $url"

                throw IncompleteContentException(message)
            }
        }
    }

    private fun handleTimeout(startTime: Long,
            pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig): ProtocolStatus {
        val length = pageSource.length
        // The javascript set data-error flag to indicate if the vision information of all DOM nodes are calculated
        var newStatus = status
        val integrity = checkHtmlIntegrity(pageSource)
        if (integrity.first) {
            newStatus = ProtocolStatus.STATUS_SUCCESS
        }

        if (newStatus.isSuccess) {
            log.info("HTML is integral but {} occurs after {} with {} | {}",
                    status.minorName, DateTimeUtil.elapsedTime(startTime),
                    StringUtil.readableByteCount(length.toLong()), page.url)
        } else {
            log.warn("Incomplete page {} {} | {}", status.minorName, integrity.second, page.url)
            handleWebDriverTimeout(page.url, startTime, pageSource, driverConfig)
        }

        return newStatus
    }

    private fun checkHtmlIntegrity(pageSource: String): Pair<Boolean, String> {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return false to "NO_BODY_START"
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return false to "NO_BODY_END"
        // no any link, it's incomplete
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return false to "NO_ANCHOR"

        // TODO: optimization using region match
        val bodyTag = pageSource.substring(p1, p2)
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return false to "NO_JS_OK"
        }

        return true to "OK"
    }

    private fun visit(task: FetchTask, driver: WebDriver, driverConfig: DriverConfig): VisitResult {
        val taskId = task.taskId
        val url = task.url
        val page = task.page

        log.info("Fetching task {}/{} in thd#{}, drivers: {}/{}/{} | {} | timeouts: {}/{}/{}",
                taskId, task.batchSize,
                Thread.currentThread().id,
                driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                page.configuredUrl,
                driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
        )

        val timeouts = driver.manage().timeouts()
        timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
        timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)
        driver.get(url)

        // Block and wait for the document is ready: all css and resources are OK
        if (!JavascriptExecutor::class.java.isAssignableFrom(driver.javaClass)) {
            log.warn("Web driver is not a JavascriptExecutor, cancel the task")
            return VisitResult.canceled
        }

        return executeJs(url, driver, driverConfig)
    }

    @Throws(WebDriverException::class)
    private fun executeJs(url: String, driver: WebDriver, driverConfig: DriverConfig): VisitResult {
        val jsExecutor = driver as? JavascriptExecutor?: return VisitResult.canceled

        var status = ProtocolStatus.STATUS_SUCCESS
        val pageLoadTimeout = driverConfig.pageLoadTimeout.seconds

        try {
            val documentWait = FluentWait<WebDriver>(driver)
                    .withTimeout(pageLoadTimeout, TimeUnit.SECONDS)
                    .pollingEvery(1, TimeUnit.SECONDS)
                    .ignoring(InterruptedException::class.java)

            try {
                // make sure the document is ready
                val initialScroll = 2
                val maxRound = pageLoadTimeout - 10 // leave 10 seconds to wait for script finish
                // TODO: wait for expected ni, na, nnum, nst, etc; required element
                val js = ";$libJs;return __utils__.waitForReady($maxRound, $initialScroll);"
                val r = documentWait.until { (it as? JavascriptExecutor)?.executeScript(js) }

                if (r == "timeout") {
                    log.debug("Hit max round $maxRound to wait for document | {}", url)
                } else {
                    log.trace("Document is ready. {} | {}", r, url)
                }
            } catch (e: org.openqa.selenium.TimeoutException) {
                log.trace("Timeout to wait for document ready, timeout {}s | {}", pageLoadTimeout, url)
                status = ProtocolStatus.failed(ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT)
            }

            performScrollDown(driver, driverConfig)
        } catch (e: InterruptedException) {
            log.warn("Waiting for document interrupted | {}", url)
            Thread.currentThread().interrupt()
            status = ProtocolStatus.STATUS_CANCELED
        } catch (e: WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                // Web driver closed
                // status = ProtocolStatus.failed(ProtocolStatus.WEB_DRIVER_GONE, e)
            } else if (e.cause is InterruptedException) {
                // Web driver closed
            } else {
                // log.warn("Web driver exception | {} \n>>>\n{}\n<<<", url, e.message)
            }
            throw e
        } catch (e: Exception) {
            log.warn("Unexpected exception | {}", url)
            log.warn(StringUtil.stringifyException(e))
            throw e
        }

        // TODO: check if the js is injected twice, libJs is already injected
        val result = jsExecutor.executeScript(clientJs)
        if (result is String) {
            val jsData = BrowserJsData.fromJson(result)
            if (log.isDebugEnabled) {
                log.debug("{} | {}", jsData, url)
            }
            return VisitResult(status, jsData)
        }

        return VisitResult(status)
    }

    private fun performScrollDown(driver: WebDriver, driverConfig: DriverConfig) {
        val scrollDownCount = driverConfig.scrollDownCount.toLong()
        val scrollDownWait = driverConfig.scrollInterval
        val timeout = scrollDownCount * scrollDownWait.toMillis() + 3 * 1000
        val scrollWait = FluentWait<WebDriver>(driver)
                .withTimeout(timeout, TimeUnit.MILLISECONDS)
                .pollingEvery(scrollDownWait.toMillis(), TimeUnit.MILLISECONDS)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            // TODO: which one is the better? browser side timer or selenium side timer?
            val js = ";$libJs;return __utils__.scrollDownN($scrollDownCount);"
            scrollWait.until { (it as? JavascriptExecutor)?.executeScript(js) }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }
    }

    /**
     * Perform click on the selected element and wait for the new page location
     * */
    private fun performJsClick(selector: String, driver: ManagedWebDriver, driverConfig: DriverConfig): String {
        val timeout = driverConfig.pageLoadTimeout
        val scrollWait = FluentWait<WebDriver>(driver.driver)
                .withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .pollingEvery(1000, TimeUnit.MILLISECONDS)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            // TODO: which one is the better? browser side timer or selenium side timer?
            val js = ";$libJs;return __utils__.navigateTo($selector);"
            val location = scrollWait.until { (it as? JavascriptExecutor)?.executeScript(js) }
            if (location is String) {
                return location
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }

        return ""
    }

    private fun getPageSourceSilently(driver: ManagedWebDriver): String {
        return try { driver.driver.pageSource } catch (e: Throwable) { "" }
    }

    private fun handleFetchFinish(page: WebPage, driver: ManagedWebDriver, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8")
        headers.put(Q_TRUSTED_CONTENT_ENCODING, "UTF-8")
        headers.put(Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        when (driver.driver) {
            is ChromeDriver -> page.lastBrowser = BrowserType.CHROME
//            is HtmlUnitDriver -> page.lastBrowser = BrowserType.HTMLUNIT
            else -> {
                log.warn("Actual browser is set to be NATIVE by selenium engine")
                page.lastBrowser = BrowserType.NATIVE
            }
        }
    }

    private fun handleFetchSuccess(batchId: Int) {
        val t = fetchTaskTracker
        t.batchSuccessCounters.computeIfAbsent(batchId) { AtomicInteger() }.incrementAndGet()
        t.totalSuccessCount.incrementAndGet()

        // TODO: A metrics system is required
        if (t.totalTaskCount.get() % 20 == 0) {
            log.debug("Selenium task success: {}/{}, total task success: {}/{}",
                    t.batchSuccessCounters[batchId], t.batchTaskCounters[batchId],
                    t.totalSuccessCount,
                    t.totalTaskCount
            )
        }
    }

    private fun handlePageSource(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver): String {
        val sb = replaceHTMLCharset(pageSource, charsetPattern)
        val content = sb.toString();

        if (log.isDebugEnabled && content.isNotEmpty()) {
            export(sb, status, content, page)

            if (log.isTraceEnabled) {
                takeScreenshot(content.length, page, driver.driver as RemoteWebDriver)
            }
        }

        return content
    }

    private fun export(sb: StringBuilder, status: ProtocolStatus, content: String, page: WebPage) {
        val document = Documents.parse(content, page.baseUrl)
        document.absoluteLinks()
        val prettyHtml = document.prettyHtml

        sb.setLength(0)
        sb.append(status.minorName).append('/').append(monthDay)
        if (prettyHtml.length < 2000) {
            sb.append("/a").append(prettyHtml.length / 500 * 500)
        } else {
            sb.append("/b").append(prettyHtml.length / 20000 * 20000)
        }

        val ident = sb.toString()
        val path = export(page, prettyHtml.toByteArray(), ident)

        page.metadata.set(Name.ORIGINAL_EXPORT_PATH, path.toString())
    }

    private fun takeScreenshot(contentLength: Int, page: WebPage, driver: RemoteWebDriver) {
        if (RemoteWebDriver::class.java.isAssignableFrom(driver.javaClass)) {
            try {
                if (contentLength > 100) {
                    val bytes = driver.getScreenshotAs(OutputType.BYTES)
                    export(page, bytes, ".png")
                }
            } catch (e: Exception) {
                log.warn("Cannot take screenshot, page length {} | {}", contentLength, page.url)
            }
        }
    }

    private fun handleWebDriverTimeout(url: String, startTime: Long, pageSource: String, driverConfig: DriverConfig) {
        val elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime)
        if (log.isDebugEnabled) {
            log.debug("Selenium timeout,  elapsed {} length {} drivers: {}/{}/{} timeouts: {}/{}/{} | {}",
                    elapsed, String.format("%,7d", pageSource.length),
                    driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    url
            )
        } else {
            log.warn("Selenium timeout, elapsed: {} length: {} | {}", elapsed, String.format("%,7d", pageSource.length), url)
        }
    }

    private fun getDriverConfig(config: ImmutableConfig): DriverConfig {
        // Page load timeout
        val pageLoadTimeout = config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, defaultDriverConfig.pageLoadTimeout)
        // Script timeout
        val scriptTimeout = config.getDuration(FETCH_SCRIPT_TIMEOUT, defaultDriverConfig.scriptTimeout)
        // Scrolling
        var scrollDownCount = config.getInt(FETCH_SCROLL_DOWN_COUNT, defaultDriverConfig.scrollDownCount)
        if (scrollDownCount > 20) {
            scrollDownCount = 20
        }
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, defaultDriverConfig.scrollInterval)
        if (scrollDownWait > pageLoadTimeout) {
            scrollDownWait = pageLoadTimeout
        }

        // TODO: handle proxy

        return DriverConfig(pageLoadTimeout, scriptTimeout, scrollDownCount, scrollDownWait)
    }

    private fun export(page: WebPage, content: ByteArray, ident: String = "", suffix: String = ".htm"): Path {
        val browser = page.lastBrowser.name.toLowerCase()

        val u = Urls.getURLOrNull(page.url)?: return AppPaths.TMP_DIR
        val domain = if (StringUtil.isIpPortLike(u.host)) u.host else InternetDomainName.from(u.host).topPrivateDomain().toString()
        val filename = ident + "-" + DigestUtils.md5Hex(page.url) + suffix
        val path = AppPaths.get(AppPaths.WEB_CACHE_DIR.toString(), "original", browser, domain, filename)
        AppFiles.saveTo(content, path, true)
        return path
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private var instanceCount = AtomicInteger()
    }
}
