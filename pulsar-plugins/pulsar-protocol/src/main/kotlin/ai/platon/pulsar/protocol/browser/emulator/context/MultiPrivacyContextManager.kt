package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.chrono.scheduleAtFixedRate
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.emoji.UnicodeEmoji
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyPoolManager
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
import java.util.*
import java.util.concurrent.atomic.AtomicReference

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

    private val logger = LoggerFactory.getLogger(MultiPrivacyContextManager::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private var numTasksAtLastReportTime = 0L
    private val numPrivacyContexts: Int get() = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)
    private val maintainTimer = AtomicReference<Timer>()

    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }

    private val iterator = Iterables.cycle(activeContexts.values).iterator()

    val metrics = Metrics()

    constructor(
        driverPoolManager: WebDriverPoolManager,
        immutableConfig: ImmutableConfig
    ): this(driverPoolManager, null, null, immutableConfig)

    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        metrics.tasks.mark()
        return run(computeNextContext(task.fingerprint), task, fetchFun).also { metrics.finishes.mark() }
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
            // only start the maintaining timer when there is at least one context
            startMaintainTimerIfNecessary()
            return context
        }

        close(context)

        return computeIfAbsent(privacyContextIdGenerator(fingerprint))
    }

    /**
     * If the number of privacy contexts does not exceed the upper limit, create a new one,
     * otherwise return the next one.
     * */
    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        if (activeContexts.size < numPrivacyContexts) {
            synchronized(activeContexts) {
                if (activeContexts.size < numPrivacyContexts) {
                    computeIfAbsent(privacyContextIdGenerator(fingerprint))
                }
            }
        }

        // TODO: is synchronized necessary?
        return synchronized(activeContexts) { iterator.next() }
    }

    @Throws(ProxyException::class)
    override fun computeIfAbsent(id: PrivacyContextId) =
        activeContexts.computeIfAbsent(id) { createUnmanagedContext(it) }

    private suspend fun run(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ) = takeIf { isActive }?.run0(privacyContext, task, fetchFun) ?: FetchResult.crawlRetry(task)

    private suspend fun run0(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext | ${privacyContext.javaClass}")
        }

        var result = FetchResult.crawlRetry(task)

        try {
            require(!task.isCanceled)
            require(task.state.get() == FetchTask.State.NOT_READY)
            require(task.proxyEntry == null)

            task.markReady()
            result = privacyContext.run(task) { _, driver ->
                task.startWork()
                fetchFun(task, driver)
            }
        } finally {
            task.done()
            task.page.variables["CONTEXT_INFO"] = formatPrivacyContext(privacyContext)
            updatePrivacyContext(privacyContext, result)
        }

        // All retry is forced to do in crawl scope
        if (result.isPrivacyRetry) {
            result.status.upgradeRetry(RetryScope.CRAWL)
        }

        return result
    }

    private fun startMaintainTimerIfNecessary() {
        if (maintainTimer.compareAndSet(null, java.util.Timer("PrivacyCMMT", true))) {
            val timer = maintainTimer.get()
            timer?.scheduleAtFixedRate(Duration.ofMinutes(2), Duration.ofSeconds(10)) {
                maintain()
            }
        }
    }

    /**
     * Check if there are hang contexts, close all the hang contexts
     * */
    private fun maintain() {
        activeContexts.filterValues { it.isIdle }.values.forEach {
            logger.warn("Privacy context hangs unexpectedly | {}", it.id.display)
            close(it)
        }
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
            status.isRetry(RetryScope.PRIVACY) -> logPrivacyLeakWarning(privacyContext, result)
            status.isSuccess -> metrics.successes.mark()
        }
    }

    private fun logPrivacyLeakWarning(privacyContext: PrivacyContext, result: FetchResult) {
        val warnings = privacyContext.privacyLeakWarnings.get()
        val status = result.status
        if (warnings > 0) {
            val symbol = UnicodeEmoji.WARNING
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
