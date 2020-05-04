package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import org.apache.commons.lang.IllegalClassException
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.FluentWait
import java.time.Clock
import java.time.Duration
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulator(
        privacyContextManager: BrowserPrivacyManager,
        emulateEventHandler: BrowserEmulateEventHandler,
        messageWriter: MiscMessageWriter,
        immutableConfig: ImmutableConfig
) : BrowserEmulatorBase(privacyContextManager, emulateEventHandler, messageWriter, immutableConfig) {

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalContextStateException Throw if the browser is closed or the program is closed
     * @throws CancellationException Throw if the task is cancelled
     * */
    @Throws(IllegalContextStateException::class, CancellationException::class)
    open fun fetch(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        if (!isActive) {
            return FetchResult.canceled(task)
        }

        return browseWithDriver(task, driver)
    }

    open fun cancel(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverManager.cancel(task.url)
    }

    @Throws(IllegalContextStateException::class)
    protected open fun browseWithDriver(task: FetchTask, driver: ManagedWebDriver): FetchResult {
        checkState()

        var exception: Exception? = null
        var response: Response?

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: CancellationException) {
            exception = e
            log.info("Task #{} is canceled | {}", task.id, task.url)
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.takeIf { isActive }?.warn("Web driver session of #{} is closed | {}", driver.id, Strings.simplifyException(e))
            driver.retire()
            exception = e
            response = ForwardingResponse.privacyRetry(task.page)
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
            log.takeIf { isActive }?.warn("Web driver is disconnected", e)
            driver.retire()
            exception = e
            response = ForwardingResponse.retry(task.page, RetryScope.PROTOCOL)
        } finally {
        }

        return FetchResult(task, response?:ForwardingResponse(exception, task.page), exception)
    }

    private fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState(task)
        checkState(driver)

        val browseTask = NavigateTask(task, driver, driverControl)

        try {
            val result = navigateAndInteract(task, driver, browseTask.driverConfig)
            checkState(task)
            checkState(driver)
            browseTask.status = result.protocolStatus
            browseTask.activeDomMessage = result.activeDomMessage
            browseTask.page.activeDomMultiStatus = browseTask.activeDomMessage?.multiStatus
            // TODO: handle exceptions of pageSource api
            browseTask.pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.ScriptTimeoutException) {
            // ignore script timeout, document might lost data, but it's the page extractor's responsibility
            browseTask.status = ProtocolStatus.failed(ProtocolStatusCodes.SCRIPT_TIMEOUT)
        } catch (e: org.openqa.selenium.UnhandledAlertException) {
            // TODO: review the status, what's the proper way to handle this exception?
            log.warn(Strings.simplifyException(e))
            browseTask.status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.TimeoutException) {
            // TODO: which kind of timeout? resource loading timeout? script execution timeout? or web driver connection timeout?
            log.warn("Unexpected web driver timeout - {}", Strings.simplifyException(e))
            browseTask.status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            browseTask.status = ProtocolStatus.retry(RetryScope.CRAWL)
        }

        return emulateEventHandler.onAfterNavigate(browseTask)
    }

    @Throws(CancellationException::class,
            IllegalContextStateException::class,
            IllegalClassException::class,
            WebDriverException::class)
    private fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: BrowserControl): InteractResult {
        checkState(driver)
        checkState(task)

        emulateEventHandler.logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        // blocking
        meterNavigates.mark()
        driver.navigateTo(task.url)

        // blocking
        return interact(InteractTask(task, driverConfig, driver))
    }

    @Throws(CancellationException::class, IllegalContextStateException::class, IllegalClassException::class, WebDriverException::class)
    protected open fun interact(task: InteractTask): InteractResult {
        val result = InteractResult(ProtocolStatus.STATUS_SUCCESS, null)

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
    private fun runScriptTask(task: InteractTask, result: InteractResult, action: () -> Unit) {
        var status: ProtocolStatus? = null

        try {
            checkState(task.driver)
            checkState(task.fetchTask)
            action()
            result.state = FlowState.CONTINUE
        } catch (e: InterruptedException) {
            log.warn("Interrupted waiting for document, cancel it | {}", task.url)
            status = ProtocolStatus.retry(RetryScope.PRIVACY)
            result.state = FlowState.BREAK
        } catch (e: NoSuchSessionException) {
            log.warn("Session lost waiting for document | {}", task.url)
            status = ProtocolStatus.retry(RetryScope.PRIVACY)
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
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
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
    protected open fun jsCheckDOMState(interactTask: InteractTask, result: InteractResult) {
        checkState()

        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = interactTask.driverConfig.scriptTimeout

        val fetchTask = interactTask.fetchTask
        val driver = interactTask.driver.driver
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

        var message: Any? = null
        try {
            message = documentWait.until {
                checkState(interactTask.driver)
                checkState(fetchTask)
                interactTask.driver.evaluate(expression)
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.SCRIPT_TIMEOUT)
            result.state = FlowState.BREAK
        } finally {
            checkState(fetchTask)
            if (message == null) {
                if (!fetchTask.isCanceled) {
                    log.warn("Unexpected script result (null) | {}", interactTask.url)
                }
            } else if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val errorResult = emulateEventHandler.handleChromeError(message)
                status = errorResult.status
                result.activeDomMessage = errorResult.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                log.trace("DOM is ready {} | {}", message.toString().substringBefore("urls"), interactTask.url)
            }
        }

        result.protocolStatus = status
    }

    protected open fun jsScrollDown(interactTask: InteractTask, result: InteractResult) {
        checkState()

        val random = Random(System.currentTimeMillis())
        val scrollDownCount = interactTask.driverConfig.scrollDownCount.toLong() + random.nextInt(3) - 1
        val scrollInterval = interactTask.driverConfig.scrollInterval.plus(Duration.ofMillis(random.nextLong(1000)))
        val scrollTimeout = scrollDownCount * scrollInterval.toMillis() + 3 * 1000

        val driver = interactTask.driver.driver
        val clock = Clock.systemDefaultZone()
        val sleeper = CancellableSleeper(interactTask.fetchTask)
        val scrollWait = FluentWait<WebDriver>(driver, clock, sleeper)
                .withTimeout(Duration.ofMillis(scrollTimeout))
                .pollingEvery(scrollInterval)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            val expression = "__utils__.scrollDownN($scrollDownCount)"
            checkState(interactTask.driver)
            checkState(interactTask.fetchTask)
            scrollWait.until { interactTask.driver.evaluate(expression) }
        } catch (ignored: org.openqa.selenium.TimeoutException) {

        }
    }

    protected open fun jsComputeFeature(interactTask: InteractTask, result: InteractResult) {
        checkState(interactTask.driver)
        checkState(interactTask.fetchTask)

        val message = interactTask.driver.evaluate("__utils__.compute()")
        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (log.isDebugEnabled) {
                log.debug("{} | {}", result.activeDomMessage?.multiStatus, interactTask.url)
            }
        }
    }

    /**
     * Perform click on the selected element and wait for the new page location
     * */
    protected open fun jsClick(jsExecutor: JavascriptExecutor,
                               selector: String, driver: ManagedWebDriver, driverConfig: BrowserControl): String {
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
}
