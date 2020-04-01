package ai.platon.pulsar.protocol.browser.driver.async

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.DEFAULT_CHARSET_PATTERN
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.SYSTEM_AVAILABLE_CHARSET_PATTERN
import ai.platon.pulsar.common.config.CapabilityTypes.HTTP_FETCH_MAX_RETRY
import ai.platon.pulsar.common.config.CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.*
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import io.netty.util.concurrent.FastThreadLocalThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.lang.IllegalClassException
import org.apache.commons.lang.StringUtils
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class AsyncBrowserEmulator(
        val privacyContextManager: PrivacyContextManager,
        val messageWriter: MessageWriter,
        val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulator::class.java)!!
    private val tracer = log.takeIf { it.isTraceEnabled }
    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    private var charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val fetchMaxRetry = immutableConfig.getInt(HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean(false)
    private val isClosed get() = closed.get()
    private val driverManager = privacyContextManager.driverManager
    private val driverControl = driverManager.driverControl
    private val driverPool = driverManager.driverPool
    private val handlers = BrowserEmulatorHandlers(driverPool, messageWriter, immutableConfig)
    private val metrics = SharedMetricRegistries.getDefault()
    private val numNavigates = metrics.meter("navigates")

    init {
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
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
    open suspend fun fetch(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        if (isClosed) {
            return FetchResult(task, ForwardingResponse.canceled(task.page))
        }

        return browseWithDriver(task, driver)
    }

    open suspend fun cancel(task: FetchTask) {
        task.cancel()
        withContext(Dispatchers.IO) {
            driverManager.cancel(task.url)
        }
    }

    protected suspend open fun browseWithDriver(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        checkState()

        var response: Response? = null
        var exception: Exception? = null

        if (++task.nRetries > fetchMaxRetry) {
            response = ForwardingResponse.retry(task.page, RetryScope.CRAWL)
            return FetchResult(task, response, exception)
        }

        response = browseWithMinorExceptionsHandled(task, driver)

        return FetchResult(task, response, exception)
    }

    @Throws(CancellationException::class)
    private suspend fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState(task)
        checkState(driver)

        val browseTask = BrowseTask(task, driver, driverControl)

        try {
            val result = navigateAndInteract(task, driver, browseTask.driverConfig)
            checkState(task)
            checkState(driver)
            browseTask.status = result.protocolStatus
            browseTask.activeDomMessage = result.activeDomMessage
            browseTask.page.activeDomMultiStatus = browseTask.activeDomMessage?.multiStatus
            browseTask.pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            browseTask.status = ProtocolStatus.retry(RetryScope.CRAWL)
        }

        handlers.onAfterBrowse(browseTask)

        return ForwardingResponse(browseTask.pageSource, browseTask.status, browseTask.headers, browseTask.page)
    }

    @Throws(CancellationException::class,
            IllegalContextStateException::class,
            IllegalClassException::class,
            WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: BrowserControl): NavigateResult {
        checkState(driver)
        checkState(task)

        handlers.logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        withContext(Dispatchers.IO) {
            numNavigates.mark()
            // tracer?.trace("About to navigate to #{} in {}", task.id, Thread.currentThread().name)
            driver.navigateTo(task.url)
        }

        return emulate(EmulateTask(task, driverConfig, driver))
    }

    @Throws(CancellationException::class, IllegalContextStateException::class)
    protected open suspend fun emulate(task: EmulateTask): NavigateResult {
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
    private suspend fun runScriptTask(task: EmulateTask, result: NavigateResult, action: suspend () -> Unit) {
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
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            status = ProtocolStatus.retry(RetryScope.CRAWL)
            result.state = FlowState.BREAK
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
    protected open suspend fun jsCheckDOMState(emulateTask: EmulateTask, result: NavigateResult) {
        checkState()

        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = emulateTask.driverConfig.scriptTimeout

        val fetchTask = emulateTask.fetchTask

        // make sure the document is ready
        val initialScroll = 5
        val maxRound = scriptTimeout.seconds - 5 // leave 5 seconds to wait for script finish

        // TODO: wait for expected data, ni, na, nnum, nst, etc; required element
        val expression = "__utils__.waitForReady($maxRound, $initialScroll)"
        var message: Any? = null
        try {
            var msg: Any? = null
            var i = 0
            while ((msg == null || msg == false) && i++ < 60) {
                withContext(Dispatchers.IO) {
                    checkState(emulateTask.driver)
                    checkState(fetchTask)
                    msg = emulateTask.driver.evaluate(expression)
                    if (msg == null || msg == false) {
                        delay(1000)
                    }
                }
            }
            message = msg
        } finally {
            if (message == null) {
                if (!fetchTask.isCanceled) {
                    log.warn("Unexpected script result (null) | {}", emulateTask.url)
                }
            } else if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", emulateTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val errorResult = handlers.handleChromeError(message)
                status = errorResult.status
                result.activeDomMessage = errorResult.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                log.trace("DOM is ready {} | {}", message.toString().substringBefore("urls"), emulateTask.url)
            }
        }

        result.protocolStatus = status
    }

    protected open suspend fun jsScrollDown(emulateTask: EmulateTask, result: NavigateResult) {
        val random = ThreadLocalRandom.current().nextInt(3)
        val scrollDownCount = emulateTask.driverConfig.scrollDownCount + random - 1

        val expression = "__utils__.scrollDownN($scrollDownCount)"
        withContext(Dispatchers.IO) {
            checkState(emulateTask.driver)
            checkState(emulateTask.fetchTask)
            // tracer?.trace("About to scrollDownN #{} in {}", emulateTask.fetchTask.id, Thread.currentThread().name)
            emulateTask.driver.evaluate(expression)
        }
    }

    protected open suspend fun jsComputeFeature(emulateTask: EmulateTask, result: NavigateResult) {
        val message = withContext(Dispatchers.IO) {
            checkState(emulateTask.driver)
            checkState(emulateTask.fetchTask)
            // tracer?.trace("About to compute #{} in {}", emulateTask.fetchTask.id, Thread.currentThread().name)
            emulateTask.driver.evaluate("__utils__.compute()")
        }
        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (log.isDebugEnabled) {
                log.debug("{} | {}", result.activeDomMessage?.multiStatus, emulateTask.url)
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

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
}
