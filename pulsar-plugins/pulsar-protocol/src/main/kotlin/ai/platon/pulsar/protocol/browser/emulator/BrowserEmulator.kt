package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.PulsarEventHandler
import ai.platon.pulsar.crawl.SimulateEventHandler
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.NavigateEntry
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.model.ActiveDomMessage
import ai.platon.pulsar.protocol.browser.driver.SessionLostException
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
open class BrowserEmulator(
    val driverPoolManager: WebDriverPoolManager,
    responseHandler: BrowserResponseHandler,
    immutableConfig: ImmutableConfig
): BrowserEmulatorBase(driverPoolManager.driverFactory.driverSettings, responseHandler, immutableConfig) {
    private val logger = LoggerFactory.getLogger(BrowserEmulator::class.java)!!
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val taskLogger = LoggerFactory.getLogger(BrowserEmulator::class.java.name + ".Task")!!

    val numDeferredNavigates by lazy { AppMetrics.reg.meter(this, "deferredNavigates") }

    var scrollDownPolicy: suspend (InteractTask, InteractResult) -> Unit = { task, result ->
        this.jsScrollDown(task, result)
    }

    init {
        params.withLogger(logger).info(true)
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts.
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * */
    open suspend fun fetch(task: FetchTask, driver: WebDriver): FetchResult {
        return takeIf { isActive }?.browseWithDriver(task, driver) ?: FetchResult.canceled(task)
    }

    open fun cancelNow(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverPoolManager.cancel(task.url)
    }

    open suspend fun cancel(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverPoolManager.cancel(task.url)
    }

    open suspend fun jsScrollDown(interactTask: InteractTask, result: InteractResult) {
        val random = ThreadLocalRandom.current().nextInt(3)
        val scrollDownCount = (interactTask.emulateSettings.scrollCount + random - 1).coerceAtLeast(1)
        val scrollInterval = interactTask.emulateSettings.scrollInterval.toMillis()

        val expressions = listOf(0.2, 0.3, 0.5, 0.75, 0.5, 0.4, 0.5, 0.75)
            .map { "__pulsar_utils__.scrollToMiddle($it)" }
            .toMutableList()
        // some website show lazy content only when the page is in the front.
        repeat(scrollDownCount) { i ->
            val ratio = (0.6 + 0.1 * i).coerceAtMost(0.8)
            expressions.add("__pulsar_utils__.scrollToMiddle($ratio)")
        }

        evaluate(interactTask, expressions, scrollInterval, bringToFront = true)
    }

    protected open suspend fun browseWithDriver(task: FetchTask, driver: WebDriver): FetchResult {
        // page.lastBrowser is used by AppFiles.export, so it has to be set before export
        task.page.lastBrowser = driver.browserType

        if (task.page.options.isDead()) {
            taskLogger.info("Page is dead, cancel the task | {}", task.page.configuredUrl)
            return FetchResult.canceled(task)
        }

        var exception: Exception? = null
        var response: Response?

        try {
            checkState(task, driver)

            response = if (task.page.isResource) {
                loadResourceWithoutRendering(task, driver)
            } else browseWithCancellationHandled(task, driver)
        }  catch (e: NavigateTaskCancellationException) {
            // The task is canceled
            response = ForwardingResponse.canceled(task.page)
        } catch (e: WebDriverCancellationException) {
            // The web driver is canceled
            response = ForwardingResponse.canceled(task.page)
        } catch (e: SessionLostException) {
            logger.warn("Web driver session #{} is lost | {}", e.driver?.id, e.simplify())
            driver.retire()
            exception = e
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                logger.warn("Web driver is disconnected - {}", e.simplify())
            } else {
                logger.warn("[Unexpected]", e)
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.crawlRetry(task.page)
        } catch (e: TimeoutCancellationException) {
            logger.warn("[Timeout] Coroutine was cancelled, thrown by [withTimeout] | {}", e.simplify())
            response = ForwardingResponse.crawlRetry(task.page, e)
        } catch (e: Exception) {
            when {
                e.javaClass.name == "kotlinx.coroutines.JobCancellationException" -> {
                    logger.warn("Coroutine was cancelled | {}", e.message)
                }
                else -> {
                    logger.warn("[Unexpected]", e)
                }
            }
            response = ForwardingResponse.crawlRetry(task.page, e)
        } finally {
        }

        return FetchResult(task, response ?: ForwardingResponse(exception, task.page), exception)
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    private suspend fun loadResourceWithoutRendering(task: FetchTask, driver: WebDriver): Response {
        checkState(task, driver)

        val navigateTask = NavigateTask(task, driver, driverSettings)

        val response = driver.loadResource(task.url)
            ?: return ForwardingResponse.failed(task.page, SessionLostException("null response"))

        navigateTask.pageSource = response.body()
        navigateTask.pageDatum.apply {
            headers.putAll(response.headers())
            contentType = response.contentType()
            content = navigateTask.pageSource.toByteArray(StandardCharsets.UTF_8)
            protocolStatus = ProtocolStatus.STATUS_SUCCESS
        }

        responseHandler.onWillCreateResponse(task, driver)
        return createResponseWithDatum(navigateTask, navigateTask.pageDatum).also {
            responseHandler.onResponseCreated(task, driver, it)
        }
    }

    private suspend fun browseWithCancellationHandled(task: FetchTask, driver: WebDriver): Response? {
        checkState(task, driver)

        var response: Response?

        try {
            response = browseWithWebDriver(task, driver)

            // Do something like a human being
//            interactAfterFetch(task, driver)

            val page = task.page
            val eventHandler = simulateEventHandler(page.conf)
            runSafely("onWillStopTab") { eventHandler?.onWillStopTab?.invokeDeferred(page, driver) }

            // Force the page stop all navigations and releases all resources
            driver.stop()

            runSafely("onTabStopped") { eventHandler?.onTabStopped?.invokeDeferred(page, driver) }
        } catch (e: NavigateTaskCancellationException) {
            logger.info("{}. Try canceled task {}/{} again later (privacy scope suggested)",
                task.page.id, task.id, task.batchId)
            response = ForwardingResponse.canceled(task.page)
        }

        return response
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    private suspend fun browseWithWebDriver(task: FetchTask, driver: WebDriver): Response {
        checkState(task, driver)

        val navigateTask = NavigateTask(task, driver, driverSettings)

        val interactResult = navigateAndInteract(task, driver, navigateTask.driverSettings)
        navigateTask.pageDatum.apply {
            protocolStatus = interactResult.protocolStatus
            activeDomMultiStatus = interactResult.activeDomMessage?.multiStatus
            activeDomUrls = interactResult.activeDomMessage?.urls
        }
        navigateTask.pageSource = driver.pageSource() ?: ""

        responseHandler.onWillCreateResponse(task, driver)
        return createResponse(navigateTask).also {
            responseHandler.onResponseCreated(task, driver, it)
        }
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: WebDriver, driverConfig: BrowserSettings): InteractResult {
        checkState(task, driver)

        logBeforeNavigate(task, driverConfig)
        driver.setTimeouts(driverConfig)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        meterNavigates.mark()
        numDeferredNavigates.mark()

        tracer?.trace("{}. Navigating | {}", task.page.id, task.url)

        checkState(task, driver)

        val page = task.page
        val eventHandler = simulateEventHandler(task.page.conf)
        runSafely("onWillNavigate") { eventHandler?.onWillNavigate?.invokeDeferred(page, driver) }

        // href has the higher priority to locate a resource
        require(task.url == page.url)
        val finalUrl = task.href ?: task.url
        val navigateEntry = NavigateEntry(finalUrl, page.id, task.url, pageReferrer = page.referrer)

        checkState(task, driver)
        try {
            driver.navigateTo(navigateEntry)
        } finally {
            runSafely("onNavigated") { eventHandler?.onNavigated?.invokeDeferred(page, driver) }
        }

        if (!driver.supportJavascript) {
            return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
        }

        val interactTask = InteractTask(task, driverConfig, driver)
        return if (driverConfig.enableStartupScript) {
            runSafely("onWillInteract") { eventHandler?.onWillInteract?.invokeDeferred(page, driver) }

            interact(interactTask).also {
                runSafely("onDidInteract") { eventHandler?.onDidInteract?.invokeDeferred(page, driver) }
            }
        } else {
            interactNoJsInvaded(interactTask)
        }
    }

    protected open suspend fun interactNoJsInvaded(interactTask: InteractTask): InteractResult {
        var pageSource = ""
        var i = 0
        do {
            pageSource = interactTask.driver.pageSource() ?: ""
            if (pageSource.length < 20_000) {
                delay(1000)
            }
        } while (i++ < 45 && pageSource.length < 20_000 && isActive)

        return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    protected open suspend fun interact(task: InteractTask): InteractResult {
        checkState(task.fetchTask, task.driver)

        val result = InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
        val eventHandler = simulateEventHandler(task.fetchTask.page.conf)
        val page = task.fetchTask.page
        val driver = task.driver

        tracer?.trace("{}", task.emulateSettings)

        runSafely("onWillCheckDOMState") { eventHandler?.onWillCheckDOMState?.invokeDeferred(page, driver) }

        jsCheckDOMState(task, result)
        if (result.protocolStatus.isSuccess) {
            task.driver.navigateEntry.documentReadyTime = Instant.now()
        }

        runSafely("onDOMStateChecked") { eventHandler?.onDOMStateChecked?.invokeDeferred(page, driver) }

        if (result.state.isContinue) {
            jsScrollDown(task, result)
        }

        if (result.state.isContinue) {
            runSafely("onWillComputeFeature") { eventHandler?.onWillComputeFeature?.invokeDeferred(page, driver) }

            jsComputeFeature(task, result)

            runSafely("onFeatureComputed") { eventHandler?.onFeatureComputed?.invokeDeferred(page, driver) }
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
        val maxRound = scriptTimeout.seconds - 5 // leave some time to wait for script finish

        // TODO: wait for expected data, ni, na, nnum, nst, etc; required element
        val expression = "__pulsar_utils__.waitForReady($maxRound, $initialScroll)"
        var i = 0
        var message: Any? = null
        try {
            var msg: Any? = null
            while ((msg == null || msg == false) && i++ < maxRound && isActive && !fetchTask.isCanceled) {
                // TODO: do only when working
//                if (fetchTask.isWorking) {
//
//                }

                msg = evaluate(interactTask, expression)

                if (msg == null || msg == false) {
                    delay(500)
                }
            }
            message = msg
        } finally {
            if (message == null) {
                if (!fetchTask.isCanceled && !interactTask.driver.isQuit && isActive) {
                    logger.warn("WaitForReady returns null after $i round, retry is supposed | {}", interactTask.url)
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
                    result.state = FlowState.BREAK
                }
            } else if (message == "timeout") {
                logger.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val browserError = responseHandler.onChromeErrorPageReturn(message)
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

    protected open suspend fun jsWaitForElement(
        interactTask: InteractTask, requiredElements: List<String>
    ) {
        if (requiredElements.isNotEmpty()) {
            return
        }

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
        val expression = "__pulsar_utils__.compute()"
        val message = evaluate(interactTask, expression)

        if (message is String) {
            result.activeDomMessage = ActiveDomMessage.fromJson(message)
            if (taskLogger.isDebugEnabled) {
                val page = interactTask.fetchTask.page
                taskLogger.debug("{}. {} | {}", page.id, result.activeDomMessage?.multiStatus, interactTask.url)
            }
        }
    }

    /**
     * Do something like a human being
     * */
    protected suspend fun interactAfterFetch(task: FetchTask, driver: WebDriver) {
        // must perform the interaction for the 1st and 2nd page
        // for the other pages, there is a change to do the interaction
        if (driver.navigateHistory.size > 1) {
            val rand = Random.nextInt(10)
            if (rand != 0) {
                return
            }
        }

        val anchorCount = task.page.activeDomStats.values.firstOrNull()?.na?: return
        if (anchorCount < 10) {
            return
        }

        val clickN = (0.2 * anchorCount + Random.nextInt((anchorCount * 0.8).toInt())).toInt()
        driver.evaluateSilently("__pulsar_utils__.clickNthAnchor($clickN)")
    }

    private fun simulateEventHandler(conf: VolatileConfig): SimulateEventHandler? {
        return conf.getBeanOrNull(PulsarEventHandler::class)?.simulateEventHandler
    }

    private suspend fun runSafely(name: String, action: suspend () -> Unit) {
        if (!isActive) {
            return
        }

        try {
            action()
        } catch (e: WebDriverCancellationException) {
            logger.info("Web driver is cancelled")
        } catch (e: WebDriverException) {
            logger.warn(e.brief("[Ignored][$name] "))
        } catch (e: Exception) {
            logger.warn(e.stringify("[Ignored][$name] "))
        } catch (e: Throwable) {
            logger.error(e.stringify("[Unexpected][$name] "))
        }
    }

    private suspend fun evaluate(interactTask: InteractTask,
                                 expressions: Iterable<String>, delay: Duration, verbose: Boolean = false) {
        return evaluate(interactTask, expressions, delay.toMillis(), verbose)
    }

    private suspend fun evaluate(interactTask: InteractTask,
        expressions: Iterable<String>, delayMillis: Long, bringToFront: Boolean = false, verbose: Boolean = false) {
        expressions.asSequence()
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .filterNot { it.startsWith("//") }
            .filterNot { it.startsWith("#") }
            .forEachIndexed { i, expression ->
                if (bringToFront && i % 2 == 0) {
                    interactTask.driver.bringToFront()
                }

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

    @Throws(WebDriverCancellationException::class)
    private suspend fun evaluate(interactTask: InteractTask, expression: String, delayMillis: Long = 0): Any? {
        if (!isActive) return null

        counterRequests.inc()
        counterJsEvaluates.inc()
        checkState(interactTask.fetchTask, interactTask.driver)
        val result = interactTask.driver.evaluate(expression)
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        return result
    }
}
