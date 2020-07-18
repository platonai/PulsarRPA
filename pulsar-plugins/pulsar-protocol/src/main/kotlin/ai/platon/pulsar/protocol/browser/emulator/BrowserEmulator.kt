package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulator(
        privacyManager: PrivacyManager,
        driverControl: WebDriverControl,
        val driverManager: WebDriverPoolManager,
        eventHandlerFactory: BrowserEmulatorEventHandlerFactory,
        messageWriter: MiscMessageWriter,
        immutableConfig: ImmutableConfig
): BrowserEmulatorBase(privacyManager, driverControl, eventHandlerFactory, messageWriter, immutableConfig) {
    private val log = LoggerFactory.getLogger(BrowserEmulator::class.java)!!

    val numDeferredNavigates = metrics.meter(prependReadableClassName(this, "deferredNavigates"))

    init {
        params.withLogger(log).info()
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalApplicationContextStateException Throw if the browser is closed or the program is closed
     * */
    @Throws(IllegalApplicationContextStateException::class)
    open suspend fun fetch(task: FetchTask, driver: AbstractWebDriver): FetchResult {
        return takeIf { isActive }?.browseWithDriver(task, driver) ?: FetchResult.canceled(task)
    }

    open fun cancelNow(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverManager.cancel(task.url)
    }

    open suspend fun cancel(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        withContext(Dispatchers.IO) {
            driverManager.cancel(task.url)
        }
    }

    @Throws(IllegalApplicationContextStateException::class)
    protected open suspend fun browseWithDriver(task: FetchTask, driver: AbstractWebDriver): FetchResult {
        checkState()

        if (task.nRetries > fetchMaxRetry) {
            return FetchResult.crawlRetry(task).also { log.info("Too many task retries, emit crawl retry | {}", task.url) }
        }

        var exception: Exception? = null
        var response: Response?

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: NavigateTaskCancellationException) {
            exception = e
            log.info("{}. Retry canceled task {}/{} in privacy scope later", task.page.id, task.id, task.batchId)
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
            response = ForwardingResponse.crawlRetry(task.page)
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            log.takeIf { isActive }?.warn("Web driver is disconnected", e)
            driver.retire()
            exception = e
            response = ForwardingResponse.crawlRetry(task.page)
        } finally {
        }

        return FetchResult(task, response ?: ForwardingResponse(exception, task.page), exception)
    }

    @Throws(NavigateTaskCancellationException::class)
    private suspend fun browseWithMinorExceptionsHandled(task: FetchTask, driver: AbstractWebDriver): Response {
        checkState(task)
        checkState(driver)

        val navigateTask = NavigateTask(task, driver, driverControl)

        try {
            val interactResult = navigateAndInteract(task, driver, navigateTask.driverConfig)
            checkState(task)
            checkState(driver)
            navigateTask.pageDatum.apply {
                status = interactResult.protocolStatus
                activeDomMultiStatus = interactResult.activeDomMessage?.multiStatus
                activeDomUrls = interactResult.activeDomMessage?.urls
            }
            navigateTask.pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            navigateTask.pageDatum.status = ProtocolStatus.retry(RetryScope.PRIVACY)
        }

        return eventHandler.onAfterNavigate(navigateTask)
    }

    @Throws(NavigateTaskCancellationException::class,
            IllegalApplicationContextStateException::class,
            WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: AbstractWebDriver, driverConfig: BrowserControl): InteractResult {
        eventHandler.logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        withContext(Dispatchers.IO) {
            meterNavigates.mark()
            numDeferredNavigates.mark()

            log.trace("{}. Navigating | {}", task.page.id, task.url)

            driver.navigateTo(task.url)
        }

        val interactTask = InteractTask(task, driverConfig, driver)
        return takeIf { driverConfig.jsInvadingEnabled }?.interact(interactTask)?: interactNoJsInvaded(interactTask)
    }

    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected open suspend fun interactNoJsInvaded(interactTask: InteractTask): InteractResult {
        var pageSource = ""
        var i = 0
        do {
            withContext(Dispatchers.IO) {
                checkState(interactTask.driver)
                checkState(interactTask.fetchTask)
                counterRequests.inc()
                pageSource = interactTask.driver.pageSource

                if (pageSource.length < 20_000) {
                    delay(1000)
                }
            }
        } while (i++ < 45 && pageSource.length < 20_000)

        return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
    }

    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected open suspend fun interact(task: InteractTask): InteractResult {
        val result = InteractResult(ProtocolStatus.STATUS_SUCCESS, null)

        jsCheckDOMState(task, result)

        if (result.state.isContinue) {
            jsScrollDown(task, result)
        }

        if (result.state.isContinue) {
            jsComputeFeature(task, result)
        }

        return result
    }

    @Throws(NavigateTaskCancellationException::class)
    protected open suspend fun jsCheckDOMState(interactTask: InteractTask, result: InteractResult) {
        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = interactTask.driverConfig.scriptTimeout
        val fetchTask = interactTask.fetchTask

        // make sure the document is ready
        val initialScroll = 5
        val maxRound = scriptTimeout.seconds - 5 // leave 5 seconds to wait for script finish

        // TODO: wait for expected data, ni, na, nnum, nst, etc; required element
        val expression = "__utils__.waitForReady($maxRound, $initialScroll)"
        var i = 0
        var message: Any? = null
        try {
            var msg: Any? = null
            while ((msg == null || msg == false) && i++ < maxRound) {
                msg = evaluate(interactTask, expression)

                if (msg == null || msg == false) {
                    delay(500)
                }
            }
            message = msg
        } finally {
            if (message == null) {
                if (!fetchTask.isCanceled && !interactTask.driver.isQuit && isActive) {
                    log.warn("Unexpected script result (null) | {}", interactTask.url)
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
                    result.state = FlowState.BREAK
                }
            } else if (message == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val browserError = eventHandler.handleChromeErrorPage(message)
                status = browserError.status
                result.activeDomMessage = browserError.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                if (log.isTraceEnabled) {
                    val page = interactTask.fetchTask.page
                    val truncatedMessage = message.toString().substringBefore("urls")
                    log.trace("{}. DOM is ready after {} evaluation | {}", page.id, i, truncatedMessage)
                }
            }
        }

        result.protocolStatus = status
    }

    protected open suspend fun jsScrollDown(interactTask: InteractTask, result: InteractResult) {
        val random = ThreadLocalRandom.current().nextInt(3)
        var scrollDownCount = interactTask.driverConfig.scrollDownCount + random - 1
        scrollDownCount = scrollDownCount.coerceAtLeast(3)

        val expression = "__utils__.scrollDownN($scrollDownCount)"
        evaluate(interactTask, expression)
    }

    protected open suspend fun jsComputeFeature(interactTask: InteractTask, result: InteractResult) {
        val expression = "__utils__.compute()"
        val message = evaluate(interactTask, expression)

        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (log.isDebugEnabled) {
                val page = interactTask.fetchTask.page
                log.debug("{}. {} | {}", page.id, result.activeDomMessage?.multiStatus, interactTask.url)
            }
        }
    }

    private suspend fun evaluate(interactTask: InteractTask, expression: String): Any? {
        val scriptTimeout = interactTask.driverConfig.scriptTimeout
        counterRequests.inc()
        return withTimeoutOrNull(scriptTimeout.toMillis()) {
            checkState(interactTask.driver)
            checkState(interactTask.fetchTask)
            interactTask.driver.evaluate(expression)
        }
    }
}
