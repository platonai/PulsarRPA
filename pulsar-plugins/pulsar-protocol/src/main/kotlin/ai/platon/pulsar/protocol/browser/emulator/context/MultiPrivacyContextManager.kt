package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.DateTimes
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
        if (!isActive) {
            return FetchResult.crawlRetry(task, "Inactive privacy context")
        }

        metrics.tasks.mark()
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
            return nextReadyContextPrivacy()
        }
    }

    @Throws(ProxyException::class)
    override fun computeIfAbsent(id: PrivacyContextId) =
        activeContexts.computeIfAbsent(id) { createUnmanagedContext(it) }

    /**
     * Maintain the system in a separate thread, usually in a scheduled service.
     * */
    override fun maintain() {
        if (tooFrequentMaintenance) {
            return
        }

        try {
            closeDyingContexts()

            // and then check the active context list
            activeContexts.values.forEach { context ->
                context.maintain()
            }
        } catch (t: Throwable) {
            logger.warn(t.stringify("Unexpected exception when maintain privacy contexts\n"))
        }
    }

    private fun nextReadyContextPrivacy(): PrivacyContext {
        var n = activeContexts.size

        var pc = iterator.next()
        while (n-- > 0 && !isReadyPrivacyContext(pc)) {
            pc = iterator.next()
        }

        return pc
    }

    private fun isReadyPrivacyContext(pc: PrivacyContext): Boolean {
        return pc.availableDriverCount() > 0 && pc.isActive && !pc.isIdle
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

        val failureRateThreshold = 0.6
        activeContexts.filterValues { it.failureRate > failureRateThreshold }.values.forEach {
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

        if (privacyContext.availableDriverCount() <= 0) {
            return FetchResult.crawlRetry(task, "No available driver")
        }

        if (privacyContext.isIdle) {
            logger.warn("[Unexpected] Privacy is idle, not capable to perform tasks")
        }

        return runAndUpdate(privacyContext, task, fetchFun)
    }

    private suspend fun runAndUpdate(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        val result = doRun(privacyContext, task, fetchFun)

        updatePrivacyContext(privacyContext, result)
        // All retry is forced to do in crawl scope
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
     * Handle when after run
     * @return true if privacy is leaked
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
            // TODO: review all retry scope
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
