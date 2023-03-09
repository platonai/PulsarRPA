package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.ServiceTemporaryUnavailableException
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class MultiPrivacyContextManager(
    val driverPoolManager: WebDriverPoolManager,
    val proxyPoolManager: ProxyPoolManager? = null,
    val coreMetrics: CoreMetrics? = null,
    immutableConfig: ImmutableConfig
) : PrivacyManager(immutableConfig) {
    class Metrics {
        private val registry = AppMetrics.reg

        val tasks = registry.multiMetric(this, "tasks")
        val successes = registry.multiMetric(this, "successes")
        val finishes = registry.multiMetric(this, "finishes")
    }

    companion object {
        val VAR_CONTEXT_INFO = "CONTEXT_INFO"
    }

    private val logger = LoggerFactory.getLogger(MultiPrivacyContextManager::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private var numTasksAtLastReportTime = 0L
    private val numPrivacyContexts: Int get() = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)

    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }
    private val lastMaintainTime = Instant.now()
    private val minMaintainInterval = Duration.ofSeconds(10)
    private val tooFrequentMaintenance get() = DateTimes.elapsedTime(lastMaintainTime) < minMaintainInterval

    private val iterator = Iterables.cycle(activeContexts.values).iterator()

    val metrics = Metrics()

    constructor(
        driverPoolManager: WebDriverPoolManager,
        immutableConfig: ImmutableConfig
    ) : this(driverPoolManager, null, null, immutableConfig)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        metrics.tasks.mark()

        if (!isActive) {
            return FetchResult.canceled(task)
        }

        val privacyContext = computeNextContext(task.fingerprint)
        val result = runIfPrivacyContextReady(privacyContext, task, fetchFun).also { metrics.finishes.mark() }

        kotlin.runCatching { maintain() }.onFailure { logger.warn(it.stringify()) }

        return result
    }

    @Throws(ProxyException::class)
    override fun createUnmanagedContext(id: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, id)
        logger.info(
            "Privacy context is created #{}, active: {}, allowed: {}",
            context.display, activeContexts.size, numPrivacyContexts
        )
        return context
    }

    /**
     * If the number of privacy contexts does not exceed the upper limit, create a new one,
     * otherwise return the next active context.
     *
     * If a context is inactive, close it.
     * */
    @Throws(ProxyException::class)
    override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext {
        val context = computeIfNecessary(fingerprint)

        if (context.isActive) {
            return context
        }

        assert(!context.isActive)
        close(context)

        return computeIfAbsent(privacyContextIdGenerator(fingerprint))
    }

    /**
     * If the number of privacy contexts does not exceed the upper limit, create a new one,
     * otherwise return the next one.
     * */
    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        synchronized(activeContexts) {
            if (activeContexts.size < numPrivacyContexts) {
                computeIfAbsent(privacyContextIdGenerator(fingerprint))
            }

//            return iterator.next()
            return nextReadyPrivacyContext()
        }
    }

    @Throws(ProxyException::class)
    override fun computeIfAbsent(id: PrivacyContextId): PrivacyContext {
        synchronized(activeContexts) {
            return activeContexts.computeIfAbsent(id) { createUnmanagedContext(it) }
        }
    }

    /**
     * Maintain all the privacy contexts, usually in a scheduled service.
     * */
    override fun maintain() {
        if (tooFrequentMaintenance) {
            return
        }

        closeDyingContexts()

        // and then check the active context list
        activeContexts.values.forEach { context ->
            context.maintain()
        }
    }

    /**
     * A ready privacy context is:
     * 1. is active
     * 2. not idle
     * 3. the associated driver pool promises to provide an available driver (but can be failed)
     * */
    private fun nextReadyPrivacyContext(): PrivacyContext {
        var n = activeContexts.size

        var pc = iterator.next()
        while (n-- > 0 && !pc.isReady) {
            pc = iterator.next()
        }

        // TODO: subscribe a web driver and no other thread can use it
        return pc
    }

    private fun closeDyingContexts() {
        // weakly consistent, which is OK
        activeContexts.filterValues { !it.isActive }.values.forEach {
            activeContexts.remove(it.id)
            logger.info("Privacy context is dead, closing it | {} | {}", it.elapsedTime.readable(), it.id.display)
            close(it)
        }

        // TODO: check the consistency
        activeContexts.filterValues { it.isIdle }.values.forEach {
            activeContexts.remove(it.id)
            logger.warn(
                "Privacy context hangs unexpectedly, closing it | {} | {}",
                it.elapsedTime.readable(),
                it.id.display
            )
            close(it)
        }

        activeContexts.filterValues { it.isHighFailureRate }.values.forEach {
            activeContexts.remove(it.id)
            logger.warn(
                "Privacy context has too high failure rate: {}, closing it | {} | {}",
                it.failureRate,
                it.elapsedTime.readable(),
                it.id.display
            )
            close(it)
        }
    }

    private suspend fun runIfPrivacyContextReady(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext | ${privacyContext.javaClass}")
        }

        val message = when {
            privacyContext.promisedDriverCount() <= 0 -> {
                "No available driver temporary"
            }
            !privacyContext.isActive -> {
                logger.warn("[Unexpected] Privacy is inactive, inability to perform tasks, closing it now")
                close(privacyContext)
                "Privacy context temporary unavailable - inactive"
            }
            privacyContext.isIdle -> {
                logger.warn("[Unexpected] Privacy is idle, inability to perform tasks, closing it now")
                close(privacyContext)
                "Privacy context temporary unavailable - idle"
            }
            !privacyContext.isReady -> {
                "Privacy context temporary unavailable - not ready"
            }
            else -> ""
        }

        return if (message.isNotBlank()) {
            val delay = Duration.ofSeconds(10)
            FetchResult.crawlRetry(task, delay, ServiceTemporaryUnavailableException(message))
        } else runAndUpdate(privacyContext, task, fetchFun)
    }

    private suspend fun runAndUpdate(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        val result = doRun(privacyContext, task, fetchFun)

        updatePrivacyContext(privacyContext, result)
        // All retries are forced to do in crawl scope
        if (result.isPrivacyRetry) {
            result.status.upgradeRetry(RetryScope.CRAWL)
        }

        return result
    }

    private suspend fun doRun(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        val result: FetchResult = try {
            require(!task.isCanceled)
            require(task.state.get() == FetchTask.State.NOT_READY)
            require(task.proxyEntry == null)

            task.markReady()
            privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables[VAR_CONTEXT_INFO] = formatPrivacyContext(privacyContext)
        }

        return result
    }

    private fun formatPrivacyContext(privacyContext: PrivacyContext): String {
        return String.format("%s(%.2f)", privacyContext.id.display, privacyContext.meterSuccesses.meanRate)
    }

    /**
     * Handle after run
     * */
    private fun updatePrivacyContext(privacyContext: PrivacyContext, result: FetchResult) {
        if (!privacyContext.isActive) {
            tracePrivacyContextInactive(privacyContext, result)
            return
        }

        val numTasks = privacyContext.meterTasks.count
        if (numTasks > numTasksAtLastReportTime && numTasks % 30 == 0L) {
            numTasksAtLastReportTime = numTasks
            privacyContext.report()
        }

        val status = result.response.protocolStatus
        when {
            // TODO: review all retries
//            status.isRetry(RetryScope.PRIVACY) -> logPrivacyLeakWarning(privacyContext, result)
            status.isRetry -> logPrivacyLeakWarning(privacyContext, result)
            status.isSuccess -> metrics.successes.mark()
        }
    }

    private fun logPrivacyLeakWarning(privacyContext: PrivacyContext, result: FetchResult) {
        val warnings = privacyContext.privacyLeakWarnings.get()
        val status = result.status
        if (warnings > 0) {
            val symbol = PopularEmoji.WARNING
            logger.info(
                "$symbol Privacy leak warning {}/{} | {}#{} | {}. {}",
                warnings, privacyContext.maximumWarnings,
                privacyContext.sequence, privacyContext.display,
                result.task.page.id, status
            )
        }

        if (privacyContext.privacyLeakWarnings.get() == 6) {
            privacyContext.report()
        }
    }

    private fun tracePrivacyContextInactive(privacyContext: PrivacyContext, result: FetchResult) {
        tracer?.trace(
            "{}. Context {}/#{} is not active | {} | {}",
            result.task.id, privacyContext.sequence, privacyContext.privacyLeakWarnings,
            result.status, result.task.url
        )
    }
}
