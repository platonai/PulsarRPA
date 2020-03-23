package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.net.browser.BrowserError.Companion.CONNECTION_TIMED_OUT
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.ActiveDomMessage
import org.apache.commons.lang.IllegalClassException
import org.apache.commons.lang.StringUtils
import org.openqa.selenium.*
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Sleeper
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

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
    val isCanceled get() = fetchTask.isCanceled
}

class BrowserStatus(
        var status: ProtocolStatus,
        var code: Int = 0
)

class BrowserError(
        val status: ProtocolStatus,
        val activeDomMessage: ActiveDomMessage
) {
    companion object {
        const val CONNECTION_TIMED_OUT = "ERR_CONNECTION_TIMED_OUT"
        const val NO_SUPPORTED_PROXIES = "ERR_NO_SUPPORTED_PROXIES"
        const val CONNECTION_CLOSED = "ERR_CONNECTION_CLOSED"
        const val EMPTY_RESPONSE = "ERR_EMPTY_RESPONSE"
        const val CONNECTION_RESET = "ERR_CONNECTION_RESET"
    }
}

class CancellableSleeper(val task: FetchTask): Sleeper {
    @Throws(CancellationException::class)
    override fun sleep(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (task.isCanceled) {
            throw CancellationException("Task #${task.batchTaskId}}/${task.batchId} is canceled from sleeper")
        }
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulator(
        val privacyContextManager: PrivacyContextManager,
        val fetchTaskTracker: FetchTaskTracker,
        val metricsSystem: MetricsSystem,
        val immutableConfig: ImmutableConfig
) : Parameterized, AutoCloseable {
    companion object {
        private var sequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(BrowserEmulator::class.java)!!
    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    private var charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val fetchMaxRetry = immutableConfig.getInt(HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean(false)
    private val isClosed get() = closed.get()
    private val driverManager = privacyContextManager.driverManager
    private val driverControl = driverManager.driverControl
    private val driverPool = driverManager.driverPool

    init {
        sequencer.incrementAndGet()
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "instanceSequence", sequencer,
                "charsetPattern", StringUtils.abbreviateMiddle(charsetPattern.toString(), "...", 200),
                "pageLoadTimeout", driverControl.pageLoadTimeout,
                "scriptTimeout", driverControl.scriptTimeout,
                "scrollDownCount", driverControl.scrollDownCount,
                "scrollInterval", driverControl.scrollInterval,
                "driverPoolCapacity", driverPool.capacity
        )
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalContextStateException Throw if the browser is closed or the program is closed
     * */
    @Throws(IllegalContextStateException::class)
    open fun fetch(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        if (isClosed) {
            return FetchResult(task, ForwardingResponse.canceled(task.page))
        }

        fetchTaskTracker.totalTasks.getAndIncrement()
        fetchTaskTracker.batchTaskCounters.computeIfAbsent(task.batchId) { AtomicInteger() }.incrementAndGet()

        val result = browseWithDriver(task, driver)

        if (result.status.isSuccess) {
            handleBrowseSuccess(task.batchId, result)
        }

        return result
    }

    open fun cancel(task: FetchTask) {
        task.cancel()
        driverManager.cancel(task.url)
    }

    protected open fun browseWithDriver(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        checkState()

        var response: Response? = null
        var exception: Exception? = null

        if (++task.nRetries > fetchMaxRetry) {
            response = ForwardingResponse.retry(task.page, RetryScope.CRAWL)
            return FetchResult(task, response, exception)
        }

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: ai.platon.pulsar.net.browser.CancellationException) {
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PRIVACY)
        } catch (e: ProxyException) {
            log.warn("No proxy, will retry task in privacy context | {}", task.url)
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PRIVACY)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            if (!isClosed) {
                log.warn("Web driver session of #{} is closed | {}", driver.id, e.message)
            }
            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.CRAWL)
        } catch (e: org.openqa.selenium.WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                log.warn("Web driver is disconnected - {}", Strings.simplifyException(e))
            } else {
                log.warn("Unexpected WebDriver exception", e)
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PROTOCOL)
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            if (!isClosed) {
                log.warn("Web driver is disconnected - {}", Strings.simplifyException(e))
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PROTOCOL)
        } finally {
        }

        if (response == null) {
            response = ForwardingResponse(task.url, exception, task.page)
        }

        return FetchResult(task, response, exception)
    }

    @Throws(CancellationException::class)
    private fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState(task)
        checkState(driver)

        val navigateTime = Instant.now()

        val batchId = task.batchId
        val url = task.url
        val page = task.page

        val driverConfig = createDriverConfig(task.volatileConfig)
        val headers = MultiMetadata(Q_REQUEST_TIME, navigateTime.toEpochMilli().toString())

        var status: ProtocolStatus
        val activeDomMessage: ActiveDomMessage?
        var pageSource = ""

        try {
            val result = navigateAndInteract(task, driver, driverConfig)
            checkState(task)
            checkState(driver)
            status = result.protocolStatus
            activeDomMessage = result.activeDomMessage
            page.activeDomMultiStatus = activeDomMessage?.multiStatus
            pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.ScriptTimeoutException) {
            // ignore script timeout, document might lost data, but it's the page extractor's responsibility
            status = ProtocolStatus.failed(ProtocolStatusCodes.SCRIPT_TIMEOUT)
        } catch (e: org.openqa.selenium.UnhandledAlertException) {
            // TODO: review the status, what's the proper way to handle this exception?
            log.warn(Strings.simplifyException(e))
            status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.TimeoutException) {
            // TODO: which kind of timeout? resource loading timeout? script execution timeout? or web driver connection timeout?
            log.warn("Unexpected web driver timeout - {}", Strings.simplifyException(e))
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            status = ProtocolStatus.retry(RetryScope.CRAWL)
        }

        val browserStatus = checkErrorPage(page, status)
        status = browserStatus.status
        if (browserStatus.code != ProtocolStatusCodes.SUCCESS_OK) {
            pageSource = ""
            return ForwardingResponse(url, pageSource, status, headers, page)
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
            status = handleBrokenPageSource(task, integrity)
            logBrokenPage(task, pageSource, integrity)
        }

        // Update headers, metadata, do the logging stuff
        page.lastBrowser = driver.browserType
        page.htmlIntegrity = integrity
        handleBrowseFinish(page, headers)

        exportIfNecessary(pageSource, status, page, driver)

        // TODO: collect response headers of main resource

        return ForwardingResponse(page.url, pageSource, status, headers, page)
    }

    @Throws(CancellationException::class,
            IllegalContextStateException::class,
            IllegalClassException::class,
            WebDriverException::class)
    private fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: DriverConfig): NavigateResult {
        checkState(driver)
        checkState(task)

        logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);
        driver.navigateTo(task.url)

        return emulate(EmulateTask(task, driverConfig, driver))
    }

    @Throws(CancellationException::class, IllegalContextStateException::class, IllegalClassException::class, WebDriverException::class)
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

    @Throws(CancellationException::class, IllegalContextStateException::class, IllegalClassException::class, WebDriverException::class)
    private fun runScriptTask(task: EmulateTask, result: NavigateResult, action: () -> Unit) {
        if (task.isCanceled) {
            // TODO: is it better to throw an exception?
            result.protocolStatus = ProtocolStatus.STATUS_CANCELED
            result.state = FlowState.BREAK
            return
        }

        var status: ProtocolStatus? = null

        try {
            checkState(task.driver)
            checkState(task.fetchTask)

            action()
            result.state = FlowState.CONTINUE
        } catch (e: InterruptedException) {
            log.warn("Interrupted waiting for document, cancel it | {}", task.url)
            status = ProtocolStatus.retry(RetryScope.WEB_DRIVER)
            result.state = FlowState.BREAK
        } catch (e: NoSuchSessionException) {
            status = ProtocolStatus.retry(RetryScope.WEB_DRIVER)
            result.state = FlowState.BREAK
            throw e
        } catch (e: WebDriverException) {
            val message = Strings.stringifyException(e)
            when {
                e.cause is org.apache.http.conn.HttpHostConnectException -> {
                    // Web driver is closed
                    // status = ProtocolStatus.failed(ProtocolStatus.WEB_DRIVER_GONE, e)
                    throw e
                }
                e.cause is InterruptedException -> {
                    status = ProtocolStatus.retry(RetryScope.WEB_DRIVER)
                    // Web driver closed
                    if (message.contains("sleep interrupted")) {
                        // throw if we use default sleeper, if we use CancellableSleeper, this must not happen
                        log.warn("Interrupted waiting for document (by cause), cancel it | {}", task.url)
                    } else {
                        log.warn("Interrupted waiting for document | {} \n>>>\n{}\n<<<", task.url, Strings.stringifyException(e))
                    }
                    result.state = FlowState.BREAK
                }
                message.contains("Cannot read property") -> {
                    // An error reported by chrome like this:
                    // unknown error: Cannot read property 'forEach' of null
                    log.warn("Javascript exception error | {} {}", task.url, Strings.simplifyException(e))
                    // ignore script errors, document might lost data, but it's the page extractor's responsibility
                    status = ProtocolStatus.STATUS_SUCCESS
                    result.state = FlowState.CONTINUE
                }
                else -> {
                    log.warn("Unexpected WebDriver exception | {} \n>>>\n{}\n<<<", task.url, Strings.stringifyException(e))
                    throw e
                }
            }
        }

        if (status != null) {
            result.protocolStatus = status
        }

        if (task.isCanceled) {
            result.state = FlowState.BREAK
            result.protocolStatus = ProtocolStatus.STATUS_CANCELED
        }
    }

    @Throws(CancellationException::class)
    protected open fun jsCheckDOMState(emulateTask: EmulateTask, result: NavigateResult) {
        checkState()

        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = emulateTask.driverConfig.scriptTimeout

        val fetchTask = emulateTask.fetchTask
        val driver = emulateTask.driver.driver
        val clock = Clock.systemDefaultZone()
        val sleeper = CancellableSleeper(fetchTask)
        val documentWait = FluentWait<WebDriver>(driver, clock, sleeper)
                .withTimeout(scriptTimeout)
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(NoSuchElementException::class.java)

        // make sure the document is ready
        val initialScroll = 5
        val maxRound = scriptTimeout.seconds - 5 // leave 5 seconds to wait for script finish

        // TODO: wait for expected data, ni, na, nnum, nst, etc; required element
        val expression = "__utils__.waitForReady($maxRound, $initialScroll)"
        checkState(emulateTask.driver)
        checkState(fetchTask)

        var message: Any? = null
        try {
            message = documentWait.until { emulateTask.driver.evaluate(expression) }
        } catch (e: org.openqa.selenium.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.SCRIPT_TIMEOUT)
            result.state = FlowState.BREAK
        } finally {
            checkState(fetchTask)
            if (message == null) {
                if (!fetchTask.isCanceled) {
                    log.warn("Unexpected script result (null) | {}", emulateTask.url)
                }
            } else if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", emulateTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val errorResult = handleChromeError(message)
                status = errorResult.status
                result.activeDomMessage = errorResult.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                log.trace("DOM is ready {} | {}", message.toString().substringBefore("urls"), emulateTask.url)
            }
        }

        // Test context reset automatically
        // Reset the browser context and proxy only when the proxy is not usable
        var simulateError = false
        if (simulateError && emulateTask.fetchTask.batchSize > 0) {
            val rand = Random.nextInt()
            simulateError = rand % 3 == 0
            if (simulateError) {
                log.warn("Simulate chrome error page connection time out")

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

        val driver = emulateTask.driver.driver
        val clock = Clock.systemDefaultZone()
        val sleeper = CancellableSleeper(emulateTask.fetchTask)
        val scrollWait = FluentWait<WebDriver>(driver, clock, sleeper)
                .withTimeout(Duration.ofMillis(scrollTimeout))
                .pollingEvery(scrollInterval)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            val expression = "__utils__.scrollDownN($scrollDownCount)"
            checkState(emulateTask.driver)
            checkState(emulateTask.fetchTask)
            scrollWait.until { emulateTask.driver.evaluate(expression) }
        } catch (ignored: org.openqa.selenium.TimeoutException) {

        }
    }

    protected open fun jsComputeFeature(emulateTask: EmulateTask, result: NavigateResult) {
        checkState(emulateTask.driver)
        checkState(emulateTask.fetchTask)

        val message = emulateTask.driver.evaluate("__utils__.compute()")
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
    protected open fun jsClick(jsExecutor: JavascriptExecutor,
            selector: String, driver: ManagedWebDriver, driverConfig: DriverConfig): String {
        checkState()

        try {
            checkState(driver)
            val location = driver.evaluate("__utils__.click($selector)")
            if (location is String) {
                return location
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }

        return ""
    }

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error
     * */
    private fun handleChromeError(message: String): BrowserError {
        val activeDomMessage = ActiveDomMessage.fromJson(message)
        val status = if (activeDomMessage.multiStatus?.status?.ec == CONNECTION_TIMED_OUT) {
            // chrome can not connect to the peer, it probably be caused by a bad proxy
            // convert to retry in PRIVACY_CONTEXT later
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT)
        } else {
            // unhandled exception
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERROR)
        }
        return BrowserError(status, activeDomMessage)
    }

    protected open fun checkErrorPage(page: WebPage, status: ProtocolStatus): BrowserStatus {
        val browserStatus = BrowserStatus(status, ProtocolStatusCodes.SUCCESS_OK)
        if (status.minorCode == ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT) {
            // The browser can not connect to remote peer, it must be caused by the bad proxy ip
            // It might be fixed by resetting the privacy context
            // log.warn("Connection timed out in browser, resetting the browser context")
            browserStatus.status = ProtocolStatus.retry(RetryScope.PRIVACY, status)
            browserStatus.code = status.minorCode
        } else if (status.minorCode == ProtocolStatusCodes.BROWSER_ERROR) {
            browserStatus.status = ProtocolStatus.retry(RetryScope.CRAWL, status)
            browserStatus.code = status.minorCode
        }

        return browserStatus
    }

    protected open fun checkHtmlIntegrity(pageSource: String,
            page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        return HtmlIntegrity.OK
    }

    protected open fun handleBrowseTimeout(startTime: Instant,
            pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(startTime, Instant.now())
            val length = pageSource.length

            val link = AppPaths.uniqueSymbolicLinkForURI(page.url)
            log.info("Timeout ({}) after {} with {} drivers: {}/{}/{} timeouts: {}/{}/{} | file://{}",
                    status.minorName,
                    elapsed,
                    Strings.readableBytes(length.toLong()),
                    driverPool.numWorking, driverPool.numFree, driverPool.numOnline,
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

    protected open fun handlePageSource(pageSource: String): StringBuilder {
        // The browser has already convert source code to UTF-8
        return replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    protected open fun handleBrokenPageSource(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus {
        return when {
            htmlIntegrity.isBanned -> {
                // should cancel all running tasks and reset the privacy context and then re-fetch them
                ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity)
            }
            task.nRetries > fetchMaxRetry -> {
                // must come after privacy context reset, PRIVACY_CONTEXT reset have the higher priority
                ProtocolStatus.retry(RetryScope.CRAWL)
            }
            htmlIntegrity.isEmpty -> {
                ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity)
            }
            else -> ProtocolStatus.retry(RetryScope.CRAWL)
        }
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver) {
        if (log.isDebugEnabled && pageSource.isNotEmpty()) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.uniqueSymbolicLinkForURI(page.url)
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, path)

            if (log.isTraceEnabled) {
                // takeScreenshot(pageSource.length.toLong(), page, driver.driver as RemoteWebDriver)
            }
        }
    }

    private fun createDriverConfig(config: ImmutableConfig): DriverConfig {
        // Page load timeout
        val pageLoadTimeout = config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, driverControl.pageLoadTimeout)
        // Script timeout
        val scriptTimeout = config.getDuration(FETCH_SCRIPT_TIMEOUT, driverControl.scriptTimeout)
        // Scrolling
        var scrollDownCount = config.getInt(FETCH_SCROLL_DOWN_COUNT, driverControl.scrollDownCount)
        if (scrollDownCount > 20) {
            scrollDownCount = 20
        }
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, driverControl.scrollInterval)
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
            log.warn("Screenshot failed {} | {}", Strings.readableBytes(contentLength), page.url)
            log.warn(Strings.stringifyException(e))
        }
    }

    @Throws(IllegalContextStateException::class)
    private fun checkState() {
        if (isClosed) {
            throw IllegalContextStateException("Context is closed")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(CancellationException::class, IllegalContextStateException::class)
    private fun checkState(driver: ManagedWebDriver) {
        checkState()

        if (driver.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw CancellationException("Task with driver #${driver.id} is canceled | ${driver.url}")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(CancellationException::class, IllegalContextStateException::class)
    private fun checkState(task: FetchTask) {
        checkState()

        if (task.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw CancellationException("Task #${task.batchTaskId}/${task.batchId} is canceled | ${task.url}")
        }
    }

    private fun logBeforeNavigate(task: FetchTask, driverConfig: DriverConfig) {
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

    private fun logBrokenPage(task: FetchTask, pageSource: String, integrity: HtmlIntegrity) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.uniqueSymbolicLinkForURI(task.url)
        val readableLength = Strings.readableBytes(pageSource.length.toLong())

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("Page is broken with {}({}) using proxy {} in {}({}) | file://{} | {}",
                    readableLength, integrity.name,
                    proxyEntry.display, domain, count, link, task.url)
        } else {
            log.warn("Page is broken with {}({}) | file://{} | {}", readableLength, integrity.name, link, task.url)
        }
    }

    protected open fun handleBrowseSuccess(batchId: Int, result: FetchResult) {
        val t = fetchTaskTracker
        t.batchSuccessCounters.computeIfAbsent(batchId) { AtomicInteger() }.incrementAndGet()
        t.totalSuccessTasks.incrementAndGet()

        t.contentBytes.addAndGet(result.response.length())
        val i = t.totalFinishedTasks.incrementAndGet()
        if (i % 5 == 0) {
            t.updateNetworkTraffic()
//            if (log.isInfoEnabled) {
//                log.info(t.formatTraffic())
//            }
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
    }
}
