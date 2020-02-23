package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.BatchStat
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.proxy.ProxyManager
import org.apache.commons.lang.IllegalClassException
import org.apache.commons.lang.StringUtils
import org.openqa.selenium.*
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet
import kotlin.random.Random

class FetchTask(
        val batchId: Int,
        val taskId: Int,
        val priority: Int,
        val page: WebPage,
        val volatileConfig: VolatileConfig
): Comparable<FetchTask> {
    var batchSize: Int = 1
    var batchTaskId = 0
    val stat: BatchStat? = null
    var deleteAllCookies: Boolean = false
    var clearLocalStorage: Boolean = false
    var closeBrowsers: Boolean = false
    var incognito
        get() = deleteAllCookies && closeBrowsers && clearLocalStorage
        set(value) {
            if (value) {
                deleteAllCookies = true
                clearLocalStorage = true
                closeBrowsers = true
            }
        }
    var proxyEntry: ProxyEntry? = null
    var retries = 0
    val canceled = AtomicBoolean()

    lateinit var response: Response

    val url get() = page.url
    val domain get() = URLUtil.getDomainName(url)

    override fun compareTo(other: FetchTask): Int {
        return url.compareTo(other.url)
    }

    companion object {
        val NIL = FetchTask(0, 0, 0, WebPage.NIL, VolatileConfig.EMPTY)
    }
}

class FetchResult(
        val task: FetchTask,
        var response: Response
)

class BrowserStatus(
        var status: ProtocolStatus,
        var code: Int = 0
)

class BrowseResult(
        var response: Response? = null,
        var exception: Exception? = null,
        var driver: ManagedWebDriver? = null
)

class NavigateResult(
        var protocolStatus: ProtocolStatus,
        var activeDomMessage: ActiveDomMessage? = null,
        var state: FlowState = FlowState.CONTINUE
)

class EmulateTask(
        val fetchTask: FetchTask,
        val driverConfig: DriverConfig,
        val driver: ManagedWebDriver
) {
    val url get() = fetchTask.url
    val isCanceled get() = fetchTask.canceled.get()
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
open class BrowserEmulator(
        val browserControl: WebDriverControl,
        val driverPool: WebDriverPool,
        protected val proxyManager: ProxyManager,
        protected val fetchTaskTracker: FetchTaskTracker,
        protected val metricsSystem: MetricsSystem,
        protected val immutableConfig: ImmutableConfig
) : Parameterized, AutoCloseable {
    companion object {
        private var instanceCount = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(BrowserEmulator::class.java)!!

    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    private var charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val browserContext = BrowserEmulatorContext(driverPool, proxyManager, immutableConfig)
    private val brokenPages = Collections.synchronizedSet(HashSet<String>())
    private var brokenPageTrackTime = Instant.now()
    private val closed = AtomicBoolean(false)
    private val isClosed get() = closed.get()
    private val fetchMaxRetry = immutableConfig.getInt(HTTP_FETCH_MAX_RETRY, 3)

    init {
        instanceCount.incrementAndGet()
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "instanceCount", instanceCount,
                "charsetPattern", StringUtils.abbreviateMiddle(charsetPattern.toString(), "...", 200),
                "pageLoadTimeout", browserControl.pageLoadTimeout,
                "scriptTimeout", browserControl.scriptTimeout,
                "scrollDownCount", browserControl.scrollDownCount,
                "scrollInterval", browserControl.scrollInterval,
                "driverPoolCapacity", driverPool.capacity
        )
    }

    /**
     * Fetch a page using a real browser
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalStateException Throw if the browser is closed or the program is closed
     * */
    @Throws(IllegalStateException::class)
    fun fetch(task: FetchTask): FetchResult {
        checkState()

        fetchTaskTracker.totalTaskCount.getAndIncrement()
        fetchTaskTracker.batchTaskCounters.computeIfAbsent(task.batchId) { AtomicInteger() }.incrementAndGet()
        val result = browserContext.runWithProxy(task) { _, driver ->
            browseWithDriver(task, driver)
        }

        val response = when {
            result.driver == null -> ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
            result.response != null -> result.response!!
            result.exception != null -> ForwardingResponse(task.url, ProtocolStatus.failed(result.exception))
            else -> ForwardingResponse(task.url, ProtocolStatus.STATUS_FAILED) // unknown failure
        }

        return FetchResult(task, response)
    }

    fun cancel(task: FetchTask) {
        task.canceled.set(true)
        driverPool.cancel(task.url)
    }

    protected open fun browseWithDriver(task: FetchTask, driver: ManagedWebDriver): BrowseResult {
        checkState()

        var response: Response? = null
        var exception: Exception? = null

        if (++task.retries > fetchMaxRetry) {
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE))
            return BrowseResult(response, exception, driver)
        }

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: ProxyException) {
            // TODO: batch should be canceled if NoProxyException is thrown
            log.warn("No proxy, request is canceled | {}", task.url)
            response = ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.warn("Web driver session is closed - {}", StringUtil.simplifyException(e))

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE))
        } catch (e: org.openqa.selenium.WebDriverException) {
            // status = ProtocolStatus.STATUS_RETRY
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                log.warn("Web driver is disconnected - {}", StringUtil.simplifyException(e))
            } else {
                log.warn("Unexpected WebDriver exception", e)
                // The following exceptions are found
                // 1. org.openqa.selenium.WebDriverException: unknown error: Cannot read property 'forEach' of null
            }

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            log.warn("Web driver is disconnected - {}", StringUtil.simplifyException(e))

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
        } finally {
            trackProxy(task, response)
        }

        return BrowseResult(response, exception, driver)
    }

    private fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState()

        val navigateTime = Instant.now()

        val batchId = task.batchId
        val page = task.page

        val driverConfig = getDriverConfig(task.volatileConfig)
        val headers = MultiMetadata(Q_REQUEST_TIME, navigateTime.toEpochMilli().toString())

        var status: ProtocolStatus
        var activeDomMessage: ActiveDomMessage? = null
        var pageSource = ""

        try {
            val result = navigateAndInteract(task, driver, driverConfig)
            status = result.protocolStatus
            activeDomMessage = result.activeDomMessage
            page.activeDomMultiStatus = activeDomMessage?.multiStatus
            pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.ScriptTimeoutException) {
            // ignore script timeout, document might lost data, but it's the page extractor's responsibility
            status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.UnhandledAlertException) {
            // TODO: review the status, what's the proper way to handle this exception?
            log.warn(StringUtil.simplifyException(e))
            status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.TimeoutException) {
            // TODO: which kind of timeout? resource loading timeout? script execution timeout? or web driver connection timeout?
            log.warn("Unexpected web driver timeout - {}", StringUtil.simplifyException(e))
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
        } catch (e: ai.platon.pulsar.net.browser.ContextResetException) {
            // TODO: Do not run context reset automatically for now, re-fetch broken pages it manually
            // status = ProtocolStatus.retry(RetryScope.BROWSER_CONTEXT)
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
        }

        val browserStatus = checkBrowserStatus(page, status)
        status = browserStatus.status
        if (browserStatus.code != 0) {
            pageSource = ""
            val response = ForwardingResponse(page.url, pageSource, status, headers)
            // Eager update required page status for callbacks
            eagerUpdateWebPage(page, response, task.volatileConfig)
            return response
        }

        // Check quality of the page source, throw an exception if content is broken
        val integrity = checkHtmlIntegrity(pageSource, page, status, task)

        // Check browse timeout event, transform status to be success if the page source is good
        if (status.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                status = ProtocolStatus.STATUS_SUCCESS
            }
            handleBrowseTimeout(navigateTime, pageSource, status, page, driverConfig)
        }

        headers.put(CONTENT_LENGTH, pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            pageSource = handlePageSource(pageSource).toString()
        } else {
            // The page seems to be broken, retry it, if there are too many broken pages in a certain period, reset the browse context
            status = handleBrokenPageSource(task, integrity, navigateTime)
        }

        // Update headers, metadata, do the logging stuff
        page.lastBrowser = driver.browserType
        page.htmlIntegrity = integrity
        handleBrowseFinish(page, headers)
        if (status.isSuccess) {
            handleBrowseSuccess(batchId)
        }

        exportIfNecessary(pageSource, status, page, driver)

        // TODO: collect response header of main resource

        val response = ForwardingResponse(page.url, pageSource, status, headers)
        // Eager update required page status for callbacks
        eagerUpdateWebPage(page, response, task.volatileConfig)
        return response
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    private fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: DriverConfig): NavigateResult {
        checkState()

        val taskId = task.taskId
        val url = task.url
        val page = task.page

        if (log.isTraceEnabled) {
            log.trace("Navigate {}/{} in thd{}{}, drivers: {}/{}/{}(w/f/t) | {} | timeouts: {}/{}/{}",
                    taskId, task.batchSize,
                    Thread.currentThread().id,
                    if (task.retries <= 1) "" else "(${task.retries})",
                    driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                    page.configuredUrl,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
            )
        }

        checkContextState(driver)
        if (task.canceled.get()) {
            // TODO: is it better to throw an exception?
            return NavigateResult(ProtocolStatus.STATUS_CANCELED, null)
        }

        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        // TODO: use callbacks instead of blocking
        task.proxyEntry = driver.proxyEntry.get()
        driver.navigate(url)

        return emulate(EmulateTask(task, driverConfig, driver))
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    protected open fun emulate(task: EmulateTask): NavigateResult {
        val result = NavigateResult(ProtocolStatus.STATUS_SUCCESS, null)

        runScriptTask(task, result) { jsCheckDOMState(task, result) }

        if (result.state.isContinue) {
            runScriptTask(task, result) { jsScrollDown(task, result) }
        }

        if (result.state.isContinue) {
            runScriptTask(task, result) { jsComputeFeature(task, result) }
        }

        return result
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    private fun runScriptTask(task: EmulateTask, result: NavigateResult, action: () -> Unit) {
        checkState()
        if (task.isCanceled) {
            // TODO: is it better to throw an exeption?
            result.protocolStatus = ProtocolStatus.STATUS_CANCELED
            result.state = FlowState.BREAK
            return
        }

        var status: ProtocolStatus? = null

        try {
            action()
            result.state = FlowState.CONTINUE
        } catch (e: InterruptedException) {
            log.warn("Interrupted waiting for document | {}", task.url)
            status = ProtocolStatus.STATUS_CANCELED
            result.state = FlowState.BREAK
        } catch (e: NoSuchSessionException) {
            // log.warn("Web driver session is closed, cancel the script task")
            result.state = FlowState.BREAK
            throw e
        } catch (e: WebDriverException) {
            val message = StringUtil.stringifyException(e)
            when {
                e.cause is org.apache.http.conn.HttpHostConnectException -> {
                    // Web driver closed
                    // status = ProtocolStatus.failed(ProtocolStatus.WEB_DRIVER_GONE, e)
                    throw e
                }
                e.cause is InterruptedException -> {
                    // Web driver closed
                    if (message.contains("sleep interrupted")) {
                        log.warn("Interrupted waiting for DOM, sleep interrupted | {}", task.url)
                        status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
                    } else {
                        log.warn("Interrupted waiting for DOM | {} \n>>>\n{}\n<<<", task.url, StringUtil.stringifyException(e))
                    }
                    result.state = FlowState.BREAK
                }
                message.contains("Cannot read property") -> {
                    // An error reported by chrome like this:
                    // unknown error: Cannot read property 'forEach' of null
                    log.warn("Javascript exception error | {} {}", task.url, StringUtil.simplifyException(e))
                    // ignore script errors, document might lost data, but it's the page extractor's responsibility
                    status = ProtocolStatus.STATUS_SUCCESS
                    result.state = FlowState.CONTINUE
                }
                else -> {
                    log.warn("Unexpected WebDriver exception | {} \n>>>\n{}\n<<<", task.url, StringUtil.stringifyException(e))
                    throw e
                }
            }
        }

        if (status != null) {
            result.protocolStatus = status
        }
    }

    protected open fun jsCheckDOMState(emulateTask: EmulateTask, result: NavigateResult) {
        checkState()

        var status = ProtocolStatus.STATUS_SUCCESS
        // wait for the DOM ready, we do not use scriptTimeout here
        val pageLoadTimeout = emulateTask.driverConfig.pageLoadTimeout

        val documentWait = FluentWait<WebDriver>(emulateTask.driver.driver)
                .withTimeout(pageLoadTimeout)
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(InterruptedException::class.java)

        // make sure the document is ready
        val initialScroll = 5
        val maxRound = pageLoadTimeout.seconds - 30 // leave 30 seconds to wait for script finish

        // TODO: wait for expected ni, na, nnum, nst, etc; required element
        val expression = "__utils__.waitForReady($maxRound, $initialScroll)"
        checkContextState(emulateTask.driver)

        try {
            val message = documentWait.until { emulateTask.driver.evaluate(expression) }

            if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", emulateTask.url)
            } else {
                if (message is String && message.contains("chrome-error://")) {
                    // chrome redirected to a special error page chrome-error://

                    val activeDomMessage = ActiveDomMessage.fromJson(message)
                    status = if (activeDomMessage.multiStatus?.status?.ec == "ERR_CONNECTION_TIMED_OUT") {
                        // chrome can not connect to the peer, it might be caused by a bad proxy
                        ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT)
                    } else {
                        // unhandled exception
                        ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERROR)
                    }

                    result.activeDomMessage = activeDomMessage
                    result.state = FlowState.BREAK
                } else {
                    log.trace("DOM is ready {} | {}", message, emulateTask.url)
                }
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            log.trace("DOM is timeout ({}) | {}", pageLoadTimeout, emulateTask.url)
            status = ProtocolStatus.failed(ProtocolStatusCodes.DOM_TIMEOUT)
        }

        // Test context reset automatically
        // Reset the browser context and proxy only when the proxy is not usable
        var simulateError = false
        if (simulateError && emulateTask.fetchTask.batchSize > 0) {
            val rand = Random.nextInt()
            simulateError = rand % 3 == 0
            if (simulateError) {
                log.warn("Simulate connection time out")

                status = ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT)
                result.state = FlowState.BREAK
            }
        }

        result.protocolStatus = status
    }

    protected open fun jsScrollDown(emulateTask: EmulateTask, result: NavigateResult) {
        checkState()

        val random = Random(System.currentTimeMillis())
        val scrollDownCount = emulateTask.driverConfig.scrollDownCount.toLong() + random.nextInt(3) - 1
        val scrollInterval = emulateTask.driverConfig.scrollInterval.plus(Duration.ofMillis(random.nextLong(1000)))
        val scrollTimeout = scrollDownCount * scrollInterval.toMillis() + 3 * 1000
        val scrollWait = FluentWait<WebDriver>(emulateTask.driver.driver)
                .withTimeout(Duration.ofMillis(scrollTimeout))
                .pollingEvery(scrollInterval)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            val expression = "__utils__.scrollDownN($scrollDownCount)"
            checkContextState(emulateTask.driver)
            scrollWait.until { emulateTask.driver.evaluate(expression) }
        } catch (ignored: org.openqa.selenium.TimeoutException) {}
    }

    protected open fun jsComputeFeature(emulateTask: EmulateTask, result: NavigateResult) {
        checkState()

        // TODO: check if the js is injected times, libJs is already injected
        checkContextState(emulateTask.driver)
        val message = emulateTask.driver.evaluate("__utils__.emulate()")

        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (log.isDebugEnabled) {
                log.debug("{} | {}", result.activeDomMessage?.multiStatus, emulateTask.url)
            }
        }
    }

    /**
     * Perform click on the selected element and wait for the new page location
     * */
    protected open fun jsClick(jsExecutor: JavascriptExecutor, selector: String, driver: ManagedWebDriver, driverConfig: DriverConfig): String {
        checkState()

        val scrollWait = FluentWait<WebDriver>(driver.driver)
                .withTimeout(driverConfig.pageLoadTimeout)
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            // TODO: which one is the better? browser side timer or selenium side timer?
            // val js = ";$libJs;return __utils__.navigateTo($selector);"
            val js = "__utils__.click($selector)"
            checkContextState(driver)
            val location = scrollWait.until { (it as? JavascriptExecutor)?.executeScript(js) }
            if (location is String) {
                return location
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }

        return ""
    }

    protected open fun checkBrowserStatus(page: WebPage, status: ProtocolStatus): BrowserStatus {
        val browserStatus = BrowserStatus(status, 0)
        if (status.minorCode == ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT) {
            // The browser can not connect to remote peer, it must be caused by the bad proxy ip
            // It might be fixed by changing the proxy ip
            // buggy
            // log.warn("Connection timed out in browser, resetting the browser context")
            // status = ProtocolStatus.retry(RetryScope.BROWSER_CONTEXT)
            browserStatus.status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
            browserStatus.code = status.minorCode
        } else if (status.minorCode == ProtocolStatusCodes.BROWSER_ERROR) {
            browserStatus.status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
            browserStatus.code = status.minorCode
        }

        return browserStatus
    }

    protected open fun checkHtmlIntegrity(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        return HtmlIntegrity.OK
    }

    protected open fun handleBrowseTimeout(startTime: Instant, pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig) {
        val length = pageSource.length
        val elapsed = Duration.between(startTime, Instant.now())

        if (status.isSuccess) {
            log.info("DOM is good though {} after {} with {} | {}",
                    status.minorName, elapsed, StringUtil.readableByteCount(length.toLong()), page.url)
        } else {
            val link = AppPaths.symbolicLinkFromUri(page.url)
            log.info("DOM is bad {} after {} with {} | file://{}",
                    status.minorName, elapsed, StringUtil.readableByteCount(length.toLong()), link)
        }

        if (log.isDebugEnabled) {
            val link = AppPaths.symbolicLinkFromUri(page.url)
            log.debug("Timeout after {} with {} drivers: {}/{}/{} timeouts: {}/{}/{} | file://{}",
                    elapsed, StringUtil.readableByteCount(pageSource.length.toLong()),
                    driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    link)
        }
    }

    protected open fun handleBrowseFinish(page: WebPage, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8")
        headers.put(Q_TRUSTED_CONTENT_ENCODING, "UTF-8")
        headers.put(Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        val urls = page.activeDomUrls
        if (urls != null) {
            page.location = urls.location
            if (page.url != page.location) {
                // in-browser redirection
                metricsSystem.debugRedirects(page.url, urls)
            }
        }
    }

    protected open fun handleBrowseSuccess(batchId: Int) {
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

    protected open fun handlePageSource(pageSource: String): StringBuilder {
        return replaceHTMLCharset(pageSource, charsetPattern)
    }

    protected open fun handleBrokenPageSource(task: FetchTask, htmlIntegrity: HtmlIntegrity, navigateTime: Instant): ProtocolStatus {
        var status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
        if (task.retries > fetchMaxRetry) {
            return status
        }

        if (htmlIntegrity.isEmpty || htmlIntegrity == HtmlIntegrity.EMPTY_BODY) {
            return ProtocolStatus.retry(RetryScope.WEB_DRIVER, htmlIntegrity)
        }

        if (htmlIntegrity == HtmlIntegrity.ROBOT_CHECK) {
            return ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE, htmlIntegrity)
        }

        brokenPages.add(task.url)

        val resetThreshold = 1
        val elapsed = Duration.between(brokenPageTrackTime, navigateTime)
        val elapsedMinutes = elapsed.toMinutes()
        val resetContext = elapsedMinutes <= 5 && brokenPages.size >= resetThreshold
        if (resetContext) {
            // TODO: browser resetting causes all fetch tasks blocked, it might be a serious problem, trying reset browse context in every time crawl round
            // status = ProtocolStatus.retry(RetryScope.BROWSER_CONTEXT)
            status = ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)
        }

        if (resetContext || elapsedMinutes > 5) {
            brokenPages.clear()
            brokenPageTrackTime = navigateTime
        }

        return status
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver) {
        if (log.isDebugEnabled && pageSource.isNotEmpty()) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.symbolicLinkFromUri(page.url)
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, path)

            if (log.isTraceEnabled) {
                // takeScreenshot(pageSource.length.toLong(), page, driver.driver as RemoteWebDriver)
            }
        }
    }

    /**
     * Eager update web page, the status is partial but required by callbacks
     * */
    private fun eagerUpdateWebPage(page: WebPage, response: Response, conf: ImmutableConfig) {
        page.protocolStatus = response.status
        val bytes = response.content
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        val content = Content(page.url, page.location, bytes, contentType, response.headers, conf)
        FetchComponent.updateContent(page, content)
    }

    private fun getDriverConfig(config: ImmutableConfig): DriverConfig {
        // Page load timeout
        val pageLoadTimeout = config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, browserControl.pageLoadTimeout)
        // Script timeout
        val scriptTimeout = config.getDuration(FETCH_SCRIPT_TIMEOUT, browserControl.scriptTimeout)
        // Scrolling
        var scrollDownCount = config.getInt(FETCH_SCROLL_DOWN_COUNT, browserControl.scrollDownCount)
        if (scrollDownCount > 20) {
            scrollDownCount = 20
        }
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, browserControl.scrollInterval)
        if (scrollDownWait > pageLoadTimeout) {
            scrollDownWait = pageLoadTimeout
        }

        // TODO: handle proxy here

        return DriverConfig(pageLoadTimeout, scriptTimeout, scrollDownCount, scrollDownWait)
    }

    private fun takeScreenshot(contentLength: Long, page: WebPage, driver: RemoteWebDriver) {
        try {
            if (contentLength > 100) {
                val bytes = driver.getScreenshotAs(OutputType.BYTES)
                AppFiles.export(page, bytes, ".png")
            }
        } catch (e: Exception) {
            log.warn("Screenshot failed {} | {}", StringUtil.readableByteCount(contentLength), page.url)
            log.warn(StringUtil.stringifyException(e))
        }
    }

    /**
     * Check if context reset occurs
     * every javascript execution is a checkpoint for context reset
     * */
    @Throws(ContextResetException::class, IllegalStateException::class)
    private fun checkContextState(driver: ManagedWebDriver) {
        if (isClosed) {
            throw IllegalStateException("Browser emulator is closed")
        }

        if (driver.isPaused) {
            throw ContextResetException()
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkState() {
        if (isClosed) {
            throw IllegalStateException("Browser emulator is closed")
        }
    }

    private fun logBrowseDone(retryRound: Int, task: FetchTask, result: BrowseResult) {
        if (log.isInfoEnabled) {
            val r = result.response
            if (retryRound > 1 && r != null && r.status.isSuccess && r.length() > 100_1000) {
                log.info("Retried {} times and obtain a good page with {} | {}",
                        retryRound, StringUtil.readableByteCount(r.length()), task.url)
            }
        }
    }

    private fun trackProxy(task: FetchTask, response: Response?) {
        val proxyEntry = task.proxyEntry
        if (proxyEntry != null && response != null && response.status.isSuccess) {
            task.page.metadata.set(Name.PROXY, proxyEntry.hostPort)
            proxyEntry.servedDomains.add(task.domain)
            proxyEntry.targetHost = task.page.url
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
    }
}
