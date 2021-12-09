package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.FlowState
import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.JsEventHandler
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.openqa.selenium.WebDriverException
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
open class BrowserEmulator(
        val driverManager: WebDriverPoolManager,
        eventHandler: EventHandler,
        immutableConfig: ImmutableConfig
): BrowserEmulatorBase(driverManager.driverFactory.driverSettings, eventHandler, immutableConfig) {
    private val logger = LoggerFactory.getLogger(BrowserEmulator::class.java)!!
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val taskLogger = LoggerFactory.getLogger(BrowserEmulator::class.java.name + ".Task")!!

    val numDeferredNavigates by lazy { AppMetrics.reg.meter(this, "deferredNavigates") }

    init {
        params.withLogger(logger).info(true)
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * @throws IllegalApplicationContextStateException Throw if the browser is closed or the program is closed
     * */
    @Throws(IllegalApplicationContextStateException::class)
    open suspend fun fetch(task: FetchTask, driver: WebDriver): FetchResult {
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
    protected open suspend fun browseWithDriver(task: FetchTask, driver: WebDriver): FetchResult {
        checkState()
        // page.lastBrowser is used by AppFiles.export, so it have to be set before export
        task.page.lastBrowser = driver.browserType

        if (task.nRetries > fetchMaxRetry) {
            return FetchResult.crawlRetry(task).also { logger.info("Too many task retries, emit crawl retry | {}", task.url) }
        }

        if (task.page.options.isDead()) {
            taskLogger.info("Page is dead, cancel the task | {}", task.page.configuredUrl)
            return FetchResult.canceled(task)
        }

        var exception: Exception? = null
        var response: Response?

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: NavigateTaskCancellationException) {
            exception = e
            logger.info("{}. Try canceled task {}/{} again later (privacy scope suggested)", task.page.id, task.id, task.batchId)
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            logger.warn("Web driver session of #{} is closed | {}", driver.id, Strings.simplifyException(e))
            driver.retire()
            exception = e
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: org.openqa.selenium.WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                logger.warn("Web driver is disconnected - {}", Strings.simplifyException(e))
            } else {
                logger.warn("Unexpected WebDriver exception", e)
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.crawlRetry(task.page)
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            logger.takeIf { isActive }?.warn("Web driver is disconnected", e)
            driver.retire()
            exception = e
            response = ForwardingResponse.crawlRetry(task.page)
        } finally {
        }

        return FetchResult(task, response ?: ForwardingResponse(exception, task.page), exception)
    }

    @Throws(NavigateTaskCancellationException::class)
    private suspend fun browseWithMinorExceptionsHandled(task: FetchTask, driver: WebDriver): Response {
        val navigateTask = NavigateTask(task, driver, driverControl)

        try {
            val interactResult = navigateAndInteract(task, driver, navigateTask.driverConfig)
            navigateTask.pageDatum.apply {
                protocolStatus = interactResult.protocolStatus
                activeDomMultiStatus = interactResult.activeDomMessage?.multiStatus
                activeDomUrls = interactResult.activeDomMessage?.urls
            }
            navigateTask.pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            logger.warn(e.message)
            navigateTask.pageDatum.protocolStatus = ProtocolStatus.retry(RetryScope.PRIVACY)
        }

        return eventHandler.onAfterNavigate(navigateTask)
    }

    @Throws(NavigateTaskCancellationException::class,
            IllegalApplicationContextStateException::class,
            WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: WebDriver, driverConfig: BrowserSettings): InteractResult {
        eventHandler.logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        meterNavigates.mark()
        numDeferredNavigates.mark()

        tracer?.trace("{}. Navigating | {}", task.page.id, task.url)

        checkState(driver)
        checkState(task)
        // href has the higher priority to locate a resource
        val location = task.href ?: task.url
        driver.navigateTo(location)

        val interactTask = InteractTask(task, driverConfig, driver)
        return if (driver.supportJavascript) {
            takeIf { driverConfig.jsInvadingEnabled }?.interact(interactTask)?: interactNoJsInvaded(interactTask)
        } else {
            InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
        }
    }

    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected open suspend fun interactNoJsInvaded(interactTask: InteractTask): InteractResult {
        var pageSource = ""
        var i = 0
        do {
            withContext(Dispatchers.IO) {
                counterRequests.inc()

                checkState(interactTask.driver)
                checkState(interactTask.fetchTask)
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
        val volatileConfig = task.fetchTask.page.conf
        val eventHandler = volatileConfig.getBean(JsEventHandler::class)

        tracer?.trace("{}", task.emulateSettings)

        jsCheckDOMState(task, result)

        task.driver.bringToFront()

        if (result.state.isContinue) {
            jsScrollDown(task, result)
        }

        // TODO: move to the session config
        val requiredElements = if (task.url.contains("item.jd.com")) {
            listOf(".itemInfo-wrap .summary-price .p-price .price")
        } else listOf()

        if (result.state.isContinue && requiredElements.isNotEmpty()) {
            jsWaitForElement(task, requiredElements)
        }

        if (result.state.isContinue) {
            eventHandler?.onBeforeComputeFeature(task.fetchTask.page, task.driver)
        }

        if (result.state.isContinue) {
            jsComputeFeature(task, result)
        }

        if (result.state.isContinue) {
            eventHandler?.onAfterComputeFeature(task.fetchTask.page, task.driver)
        }

        return result
    }

    @Throws(NavigateTaskCancellationException::class)
    protected open suspend fun jsCheckDOMState(interactTask: InteractTask, result: InteractResult) {
        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = interactTask.emulateSettings.scriptTimeout
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
                    logger.warn("Script result is null for waitForReady, retry is supposed | {}", interactTask.url)
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
                    result.state = FlowState.BREAK
                }
            } else if (message == "timeout") {
                logger.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val browserError = eventHandler.handleChromeErrorPage(message)
                status = browserError.status
                result.activeDomMessage = browserError.activeDomMessage
                result.state = FlowState.BREAK
            } else {
                if (tracer != null) {
                    val page = interactTask.fetchTask.page
                    val truncatedMessage = message.toString().substringBefore("urls")
                    tracer?.trace("{}. DOM is ready after {} evaluation | {}", page.id, i, truncatedMessage)
                }
            }
        }

        result.protocolStatus = status
    }

    protected open suspend fun jsScrollDown(interactTask: InteractTask, result: InteractResult) {
        val random = ThreadLocalRandom.current().nextInt(3)
        val scrollDownCount = (interactTask.emulateSettings.scrollCount + random - 1).coerceAtLeast(1)
        val scrollInterval = interactTask.emulateSettings.scrollInterval.toMillis()

        val expressions = mutableListOf(
            "__utils__.scrollToMiddle(0.25)",
            "__utils__.scrollToMiddle(0.5)",
            "__utils__.scrollToMiddle(0.75)",
            "__utils__.scrollToMiddle(0.5)"
        )
        repeat(scrollDownCount) {
            expressions.add("__utils__.scrollDown()")
        }
        evaluate(interactTask, expressions, scrollInterval)
    }

    protected open suspend fun jsWaitForElement(
        interactTask: InteractTask, requiredElements: List<String>
    ) {
        val expressions = requiredElements.map { "!!document.querySelector('$it')" }
        var scrollCount = 0

        val delayMillis = interactTask.emulateSettings.scrollInterval.toMillis()
        var exists: Any? = null
        while (scrollCount-- > 0 && (exists == null || exists == false)) {
            counterJsWaits.inc()
            val verbose = false
            exists = expressions.all { expression -> true == evaluate(interactTask, expression, verbose) }
            delay(delayMillis)
        }
    }

    protected open suspend fun jsComputeFeature(interactTask: InteractTask, result: InteractResult) {
        val expression = "__utils__.compute()"
        val message = evaluate(interactTask, expression)

        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (taskLogger.isDebugEnabled) {
                val page = interactTask.fetchTask.page
                taskLogger.debug("{}. {} | {}", page.id, result.activeDomMessage?.multiStatus, interactTask.url)
            }
        }
    }

    private suspend fun evaluate(interactTask: InteractTask,
        expressions: Iterable<String>, delayMillis: Long, verbose: Boolean = false) {
        expressions.asSequence()
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .filterNot { it.startsWith("// ") }
            .filterNot { it.startsWith("# ") }
            .forEach { expression ->
                evaluate(interactTask, expression, verbose)
                delay(delayMillis)
            }
    }

    private suspend fun evaluate(
        interactTask: InteractTask, expression: String, verbose: Boolean
    ): Any? {
        logger.takeIf { verbose }?.info("Evaluate expression >>>$expression<<<")
        val value = evaluate(interactTask, expression)
        if (value is String) {
            val s = Strings.stripNonPrintableChar(value)
            logger.takeIf { verbose }?.info("Result >>>$s<<<")
        } else if (value is Int || value is Long) {
            logger.takeIf { verbose }?.info("Result >>>$value<<<")
        }
        return value
    }

    private suspend fun evaluate(interactTask: InteractTask, expression: String, delayMillis: Long = 0): Any? {
        counterRequests.inc()
        counterJsEvaluates.inc()
        checkState(interactTask.driver)
        checkState(interactTask.fetchTask)
        val result = interactTask.driver.evaluate(expression)
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        return result
    }
}
