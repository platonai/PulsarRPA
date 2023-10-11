package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.PulsarParams.VAR_PRIVACY_AGENT
import ai.platon.pulsar.common.browser.Fingerprint
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_AGENT_GENERATOR_CLASS
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyPoolManager
import ai.platon.pulsar.crawl.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyAgent
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import com.google.common.collect.Iterables
import kotlinx.coroutines.delay
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

class MultiPrivacyContextManager(
    val driverPoolManager: WebDriverPoolManager,
    val proxyPoolManager: ProxyPoolManager? = null,
    val coreMetrics: CoreMetrics? = null,
    immutableConfig: ImmutableConfig
) : PrivacyManager(immutableConfig) {
    class Metrics {
        private val registry = MetricsSystem.reg

        val tasks = registry.multiMetric(this, "tasks")
        val successes = registry.multiMetric(this, "successes")
        val finishes = registry.multiMetric(this, "finishes")

        val illegalDrivers = registry.meter(this, "illegalDrivers")
    }

    companion object {
        val VAR_CONTEXT_INFO = "CONTEXT_INFO"
        val SNAPSHOT_PATH = AppPaths.TMP_DIR.resolve("privacy.context.snapshot.txt")
        var SNAPSHOT_DUMP_INTERVAL = Duration.ofMinutes(1)
    }

    private val logger = getLogger(MultiPrivacyContextManager::class)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private var numTasksAtLastReportTime = 0L
    private val numPrivacyContexts: Int get() = conf.getInt(CapabilityTypes.PRIVACY_CONTEXT_NUMBER, 2)

    val maxAllowedBadContexts = 10
    val numBadContexts get() = zombieContexts.indexOfFirst { it.isGood }

    internal val maintainCount = AtomicInteger()
    private var lastMaintainTime = Instant.now()
    private val minMaintainInterval = Duration.ofSeconds(10)
    private val tooFrequentMaintenance get() = DateTimes.isNotExpired(lastMaintainTime, minMaintainInterval)
    private var lastDumpTime = Instant.now()
    private val snapshotDumpCount = AtomicInteger()
    var snapshotDumpInterval = SNAPSHOT_DUMP_INTERVAL

    private var driverAbsenceReportTime = Instant.EPOCH

    private val iterator = Iterables.cycle(temporaryContexts.values).iterator()

    val metrics = Metrics()

    constructor(
        driverPoolManager: WebDriverPoolManager,
        immutableConfig: ImmutableConfig
    ) : this(driverPoolManager, null, null, immutableConfig)

    /**
     * Run a task in a privacy context.
     *
     * The privacy context is selected from the active privacy context pool,
     * and it is supposed to have at least one ready web driver to run the task.
     *
     * If the privacy context chosen is not ready to serve, especially, it has no any ready web driver,
     * the task will be canceled.
     *
     * @param task the fetch task
     * @param fetchFun the fetch function
     * @return the fetch result
     * */
    override suspend fun run(task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        metrics.tasks.mark()

        if (!isActive) {
            return FetchResult.canceled(task, "Inactive privacy context manager")
        }

        // Try to get a ready privacy context, the privacy context is supposed to be:
        // not closed, not retired, [not idle]?, has promised driver.
        // If the privacy context is inactive, close it and cancel the task.
        val privacyContext = computeNextContext(task.page, task.fingerprint, task)
        val result = runIfPrivacyContextActive(privacyContext, task, fetchFun).also { metrics.finishes.mark() }

        // maintain after run and also start a scheduled monitor,
        // so We can periodically run maintenance processes even when the system is idle.
        maintain()

        return result
    }
    /**
     * Create a privacy context who is not added to the context list yet.
     * */
    @Throws(ProxyException::class)
    override fun createUnmanagedContext(privacyAgent: PrivacyContextId): BrowserPrivacyContext {
        val context = BrowserPrivacyContext(proxyPoolManager, driverPoolManager, coreMetrics, conf, privacyAgent)
        if (privacyAgent.isPermanent) {
            logger.info("Permanent privacy context is created #{} | {}", context.display, context.baseDir)
        } else {
            logger.info(
                "Temporary privacy context is created #{}, active: {}, allowed: {} | {}",
                context.display, temporaryContexts.size, numPrivacyContexts, context.baseDir
            )
        }
        return context
    }
    /**
     * Try to get a ready privacy context.
     *
     * If the total number of active contexts is less than the maximum number allowed,
     * a new privacy context will be created.
     *
     * If the privacy context is inactive, close it and create a new one immediately, and return the new one.
     *
     * This method can return a non-ready privacy context, in which case the task will be canceled.
     *
     * A ready privacy context is:
     * 1. is active
     * 2. [requirement removed] not idle
     * 3. the associated driver pool promises to provide an available driver (but the promise can be failed)
     *
     * @param fingerprint The fingerprint of this privacy context.
     * @return A privacy context which is promised to be ready.
     * */
    @Deprecated(
        "Use computeNextContext(task, fingerprint)",
        replaceWith = ReplaceWith("computeNextContext(task, fingerprint)")
    )
    @Throws(ProxyException::class)
    override fun computeNextContext(fingerprint: Fingerprint): PrivacyContext {
        val context = computeIfNecessary(fingerprint)

        // An active privacy context can be used to serve tasks, and an inactive one should be closed.
        if (context.isActive) {
            return context
        }

        assert(!context.isActive)
        close(context)

        return computeIfAbsent(privacyContextIdGenerator(fingerprint))
    }
    @Throws(ProxyException::class)
    override fun computeNextContext(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext {
        val context = computeIfNecessary(page, fingerprint, task)

        // An active privacy context can be used to serve tasks, and an inactive one should be closed.
        if (context.isActive) {
            return context
        }

        assert(!context.isActive)
        close(context)

        return computeIfAbsent(createPrivacyAgent(task.page, fingerprint))
    }
    @Deprecated(
        "Use computeIfNecessary(task, fingerprint)",
        replaceWith = ReplaceWith("computeIfNecessary(FetchTask, Fingerprint)")
    )
    override fun computeIfNecessary(fingerprint: Fingerprint): PrivacyContext {
        synchronized(contextLifeCycleMonitor) {
            if (temporaryContexts.size < numPrivacyContexts) {
                val generator = privacyAgentGeneratorFactory.generator
                computeIfAbsent(generator(fingerprint))
            }

            return tryNextUnderLoadedPrivacyContext()
        }
    }
    /**
     * Gets an under-loaded privacy context, which can be either active or inactive.
     *
     * If the total number of active contexts is less than the maximum number allowed,
     * a new privacy context will be created.
     *
     * This method can return an inactive privacy context, in which case, the task should be canceled,
     * and the privacy context should be closed.
     *
     * @param fingerprint The fingerprint of this privacy context.
     * @return A privacy context which is promised to be ready.
     * */
    override fun computeIfNecessary(page: WebPage, fingerprint: Fingerprint, task: FetchTask): PrivacyContext {
        synchronized(contextLifeCycleMonitor) {
            val privacyAgent = createPrivacyAgent(page, fingerprint)
            if (privacyAgent.isPermanent) {
                logger.info("Prepare for permanent privacy agent | {}", privacyAgent)
                forceReserveResource()
                return computeIfAbsent(privacyAgent)
            }

            if (activeContexts.size < numPrivacyContexts) {
                computeIfAbsent(privacyAgent)
            }

            return tryNextUnderLoadedPrivacyContext()
        }
    }

    @Throws(ProxyException::class)
    override fun computeIfAbsent(privacyAgent: PrivacyContextId): PrivacyContext {
        synchronized(contextLifeCycleMonitor) {
            return if (privacyAgent.isPermanent) {
                permanentContexts.computeIfAbsent(privacyAgent) { createUnmanagedContext(privacyAgent) }
            } else {
                temporaryContexts.computeIfAbsent(privacyAgent) { createUnmanagedContext(privacyAgent) }
            }
        }
    }

    /**
     * Maintain all the privacy contexts, check and report inconsistency, illness, idleness, etc.,
     * close bad contexts if necessary.
     *
     * If "takePrivacyContextSnapshot" is in file AppPaths.PATH_LOCAL_COMMAND, perform the action.
     *
     * If the tmp dir is the default one, run the following command to take snapshot once:
     * echo takePrivacyContextSnapshot >> /tmp/pulsar/pulsar-commands
     * */
    override fun maintain(force: Boolean) {
        if (!force && tooFrequentMaintenance) {
            return
        }
        lastMaintainTime = Instant.now()

        if (maintainCount.getAndIncrement() == 0) {
            logger.info("Maintaining service is started, minimal maintain interval: {}", minMaintainInterval)
        }

        doMaintain()

        // assign the last maintain time again
        lastMaintainTime = Instant.now()
    }

    private fun doMaintain() {
        closeDyingContexts()

        // and then check the active context list
        activeContexts.values.forEach { context ->
            context.maintain()
        }
//        driverPoolManager.maintain()

        dumpIfNecessary()
    }

    override fun close() {
        super.close()
    }

    private fun createPrivacyAgent(page: WebPage, fingerprint: Fingerprint): PrivacyAgent {
        // Specify the privacy agent by the user code
        // TODO: this is a temporary solution to use a specified privacy agent, try a better and consistent solution
        val specifiedPrivacyAgent = page.getVar(VAR_PRIVACY_AGENT)
        if (specifiedPrivacyAgent is PrivacyAgent) {
            return specifiedPrivacyAgent
        }

        val conf = page.conf
        val privacyAgentClassName = conf[PRIVACY_AGENT_GENERATOR_CLASS]
            ?: conf[PRIVACY_CONTEXT_ID_GENERATOR_CLASS] ?: ""

        val privacyAgentGenerator = privacyAgentGeneratorFactory.create(privacyAgentClassName)
        return privacyAgentGenerator.invoke(fingerprint)
    }

    /**
     * Get the next under loaded privacy context, which can be ether active or inactive.
     *
     * If a privacy context is full capacity, it means the underlying layer is healthy and is running full load, and no
     * resources to serve new tasks.
     *
     * If a privacy context is not full capacity, it means the underlying layer is inactive or has available resources
     * to serve new tasks.
     *
     * @return A privacy context which is promised to be ready to serve a new task.
     * */
    private fun tryNextUnderLoadedPrivacyContext(): PrivacyContext {
        var n = temporaryContexts.size

        var pc = iterator.next()
        while (n-- > 0 && pc.isFullCapacity) {
            pc = iterator.next()
        }

        return pc
    }

    private fun forceReserveResource() {
        doMaintain()

        if (AppSystemInfo.isCriticalResources) {
            logger.info("Critical resource, closing a temporary context | availableMem: {}, memToReserve: {}, shortage: {}",
                AppSystemInfo.formatAvailableMemory(), AppSystemInfo.formatMemoryToReserve(),
                AppSystemInfo.formatMemoryShortage()
            )

            temporaryContexts.entries.firstOrNull()?.let { close(it.value) }
        }
    }

    private fun closeDyingContexts() {
        // weakly consistent, which is OK
        activeContexts.filterValues { !it.isActive }.values.forEach {
            permanentContexts.remove(it.privacyAgent)
            temporaryContexts.remove(it.privacyAgent)
            logger.info("Privacy context is inactive, closing it | {} | {} | {}",
                it.elapsedTime.readable(), it.display, it.readableState)
            close(it)
        }

        temporaryContexts.filterValues { it.isIdle }.values.forEach {
            temporaryContexts.remove(it.privacyAgent)
            logger.warn("Privacy context hangs unexpectedly, closing it | {}/{} | {} | {}",
                it.idelTime.readable(), it.elapsedTime.readable(), it.display, it.readableState)
            close(it)
        }

        permanentContexts.filterValues { it.isIdle }.values.forEach {
            permanentContexts.remove(it.privacyAgent)
            logger.warn("Permanent privacy context is idle, closing it | {}/{} | {} | {}",
                it.idelTime.readable(), it.elapsedTime.readable(), it.display, it.readableState)
            close(it)
        }

        activeContexts.filterValues { it.isHighFailureRate }.values.forEach {
            permanentContexts.remove(it.privacyAgent)
            temporaryContexts.remove(it.privacyAgent)
            logger.warn("Privacy context has too high failure rate: {}, closing it | {} | {} | {}",
                it.failureRate, it.elapsedTime.readable(), it.display, it.readableState)
            close(it)
        }
    }

    /**
     * Try to run the task with the given privacy context, the privacy context is supposed to be:
     * not closed, not retired, [not idle]?, has promised driver.
     * If the privacy context is inactive, close it and cancel the task.
     * */
    private suspend fun runIfPrivacyContextActive(
        privacyContext: PrivacyContext, task: FetchTask, fetchFun: suspend (FetchTask, WebDriver) -> FetchResult
    ): FetchResult {
        if (privacyContext !is BrowserPrivacyContext) {
            throw ClassCastException("The privacy context should be a BrowserPrivacyContext | ${privacyContext.javaClass}")
        }

        val errorMessage = when {
            !privacyContext.hasWebDriverPromise() -> {
                "PRIVACY CX NO DRIVER"
            }
            privacyContext.isIdle -> {
                logger.warn("[Unexpected] Privacy is idle and can not perform tasks, closing it now")
                close(privacyContext)
                "PRIVACY CX IDLE"
            }
            !privacyContext.isActive -> {
                logger.warn("[Unexpected] Privacy is inactive and can not perform tasks, closing it now")
                close(privacyContext)
                "PRIVACY CX NOT INACTIVE"
            }
            else -> null
        }

        return if (errorMessage != null) {
            metrics.illegalDrivers.mark()
            // rate_unit=events/second
            if (metrics.illegalDrivers.oneMinuteRate > 2) {
                handleTooManyDriverAbsence(errorMessage)
            }
            FetchResult.canceled(task, errorMessage)
        } else {
            runAndUpdate(privacyContext, task, fetchFun)
        }
    }

    private suspend fun handleTooManyDriverAbsence(errorMessage: String) {
        val now = Instant.now()
        if (Duration.between(driverAbsenceReportTime, now).seconds > 10) {
            driverAbsenceReportTime = now

            val promisedDrivers = temporaryContexts.values.joinToString { it.promisedWebDriverCount().toString() }
            val states = temporaryContexts.values.joinToString { it.readableState }
            val idleTimes = temporaryContexts.values.joinToString { it.idelTime.readable() }
            logger.warn("Too many driver absence errors, promised drivers: {} | {} | {} | {}",
                promisedDrivers, errorMessage, states, idleTimes)
        }

        delay(2_000)
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
            // TODO: review all retries and cancels
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

    private fun dumpIfNecessary() {
        if (DateTimes.isExpired(lastDumpTime, snapshotDumpInterval)) {
            lastDumpTime = Instant.now()
            dump()
        }
    }

    private fun dump() {
        try {
            if (!Files.exists(SNAPSHOT_PATH)) {
                Files.createDirectories(SNAPSHOT_PATH.parent)
            }

            val count = snapshotDumpCount.incrementAndGet()
            val sb = StringBuilder()
            sb.append("\n$count. Privacy context snapshot: \n")
            sb.appendLine(LocalDateTime.now())
            sb.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            sb.append("\n{}", takeSnapshot())
            activeContexts.values.forEach { sb.appendLine(it.getReport()) }
            sb.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")

            Files.writeString(SNAPSHOT_PATH, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            logger.warn(e.stringify())
        }
    }
}
