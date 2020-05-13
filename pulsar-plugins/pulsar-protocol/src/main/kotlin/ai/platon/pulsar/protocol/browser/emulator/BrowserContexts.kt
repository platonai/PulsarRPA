package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import com.codahale.metrics.Gauge
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory
import java.lang.Thread.currentThread
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WebDriverContext(
        private val driverManager: WebDriverManager,
        private val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
        private val lock = ReentrantLock()
        private val notBusy = lock.newCondition()
        private val metricRegistry = SharedMetricRegistries.getOrCreate("pulsar")

        init {
            metricRegistry.register(prependReadableClassName(this,"runningTasks"), object: Gauge<Int> {
                override fun getValue(): Int = runningTasks.size
            })
        }
    }

    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = conf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get()

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            driverManager.run(task.priority, task.volatileConfig) { browseFun(task, it) }
        } finally {
            runningTasks.remove(task)
            if (runningTasks.isEmpty()) {
                log.info("No running task now")
                lock.withLock {
                    notBusy.signalAll()
                }
            }
        }
    }

    suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            driverManager.submit(task.priority, task.volatileConfig) { browseFun(task, it) }
        } finally {
            runningTasks.remove(task)
            if (runningTasks.isEmpty()) {
                lock.withLock {
                    log.info("No running task now")
                    notBusy.signalAll()
                }
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching {
                // Mark all working tasks are canceled, so they return as soon as possible,
                // the ready tasks are blocked to wait for driverManager.reset() finish
                runningTasks.forEach { it.cancel() }
                // Mark all drivers are canceled
                driverManager.cancelAll()
                // may wait for cancelling finish?
                // Close all online drivers and delete the browser data
                driverManager.reset(timeToWait = Duration.ofMinutes(2))

                // Wait for all tasks return
                lock.withLock {
                    var i = 0
                    while (i++ < 30 && runningTasks.isNotEmpty() && !currentThread().isInterrupted) {
                        notBusy.await(1, TimeUnit.SECONDS)
                    }
                }

                if (runningTasks.isNotEmpty()) {
                    log.warn("Still {} running tasks after context close | {}",
                            runningTasks.size, runningTasks.joinToString { "${it.id}(${it.state})" })
                }
            }.onFailure { log.warn("Unexpected exception", it) }
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.privacyRetry(task)
        }

        if (++task.nRetries > fetchMaxRetry) {
            return FetchResult.crawlRetry(task).also { log.info("Too many task retries, emit a crawl retry | {}", task.url) }
        }

        return null
    }
}

class ProxyContext(
        private val proxyMonitor: ProxyMonitor,
        private val driverContext: WebDriverContext,
        private val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        val numProxyAbsence = AtomicInteger()
        val numRunningTasks = AtomicInteger()
        private val metricRegistry = SharedMetricRegistries.getOrCreate("pulsar")

        init {
            metricRegistry.register(prependReadableClassName(this,"proxyAbsences"), object: Gauge<Int> {
                override fun getValue(): Int = numProxyAbsence.get()
            })

            metricRegistry.register(prependReadableClassName(this,"runningTasks"), object: Gauge<Int> {
                override fun getValue(): Int = numRunningTasks.get()
            })
        }
    }
    private val log = LoggerFactory.getLogger(ProxyContext::class.java)!!
    /**
     * If the number of success exceeds [maxFetchSuccess], force emit PrivacyRetry result.
     * It's used for test purpose
     * */
    private val maxFetchSuccess = conf.getInt(CapabilityTypes.PROXY_MAX_FETCH_SUCCESS, Int.MAX_VALUE)
    private val maxAllowedProxyAbsence = conf.getInt(CapabilityTypes.PROXY_MAX_ALLOWED_PROXY_ABSENCE, 10)
    private val minTimeToLive = Duration.ofSeconds(10)
    private val closing = AtomicBoolean()
    private val closed = AtomicBoolean()

    /**
     * The proxy for this context
     * */
    var proxyEntry: ProxyEntry? = null
        private set
    val isEnabled get() = proxyMonitor.isEnabled
    val isActive get() = proxyMonitor.isActive && !closing.get() && !closed.get()

    init {
        // warn up if it's idle
        proxyMonitor.warnUp()
    }

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:run0(task, browseFun)?:FetchResult.privacyRetry(task)
    }

    suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:runDeferred0(task, browseFun)?:FetchResult.privacyRetry(task)
    }

    private fun run0(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var success = false
        return try {
            beforeTaskStart(task)
            proxyMonitor.run { driverContext.run(task, browseFun) }.also {
                success = it.response.status.isSuccess
                numProxyAbsence.takeIf { it.get() > 0 }?.decrementAndGet()
            }
        } catch (e: ProxyException) {
            handleProxyException(task, e)
        } finally {
            afterTaskFinished(task, success)
        }
    }

    @Throws(ProxyVendorUntrustedException::class)
    private suspend fun runDeferred0(
            task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var success = false
        var result = try {
            beforeTaskStart(task)
            proxyMonitor.runDeferred { driverContext.runDeferred(task, browseFun) }.also {
                success = it.response.status.isSuccess
                numProxyAbsence.takeIf { it.get() > 0 }?.decrementAndGet()
            }
        } catch (e: ProxyException) {
            handleProxyException(task, e)
        } finally {
            afterTaskFinished(task, success)
        }

        val proxy = proxyEntry
        if (proxy != null) {
            val successPages = proxy.numSuccessPages.get()
            if (successPages > maxFetchSuccess && result.status.isSuccess) {
                log.info("Emit a PRIVACY retry since served $successPages pages | {}", proxy)
                result = FetchResult.privacyRetry(task, ProxyRetiredException("Served too many pages"))
            }
        }

        return result
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.privacyRetry(task)
        }

        checkProxyAbsence()

        return null
    }

    private fun handleProxyException(task: FetchTask, e: ProxyException): FetchResult {
        return when (e) {
            is ProxyRetiredException -> {
                log.warn("{}, context reset will be triggered | {}", e.message, proxyEntry?:"<no proxy>")
                FetchResult.privacyRetry(task, reason = e)
            }
            is NoProxyException -> {
                numProxyAbsence.incrementAndGet()
                checkProxyAbsence()
                log.warn("No proxy available temporary the {}th times, cause: {}", numProxyAbsence, e.message)
                FetchResult.crawlRetry(task)
            }
            else -> {
                log.warn("Task failed with proxy {}, cause: {}", proxyEntry, e.message)
                FetchResult.privacyRetry(task)
            }
        }
    }

    private fun checkProxyAbsence() {
        if (isActive && numProxyAbsence.get() > maxAllowedProxyAbsence) {
            throw ProxyVendorUntrustedException("No proxy available from proxy vendor, the vendor is untrusted")
        }
    }

    private fun beforeTaskStart(task: FetchTask) {
        numRunningTasks.incrementAndGet()
        // initialize the local recorded proxy entry
        if (proxyEntry == null && proxyMonitor.currentProxyEntry != null) {
            proxyEntry = proxyMonitor.currentProxyEntry
        }
        task.proxyEntry = proxyEntry

        // If the proxy is idle, and here comes a new task, reset the context
        if (proxyMonitor.isIdle) {
            if (closing.compareAndSet(false, true)) {
                throw ProxyRetiredException("The proxy is idle")
            }
        }

        // The proxy is about to be unusable, reset the context
        proxyEntry?.also {
            if (it.willExpireAfter(minTimeToLive)) {
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("The proxy expires in $minTimeToLive")
                }
            }
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        numRunningTasks.decrementAndGet()
        if (proxyEntry != null && proxyEntry != proxyMonitor.currentProxyEntry) {
            log.warn("Proxy has been changed | {} -> {} | {} -> {}",
                    proxyEntry?.outIp,
                    proxyMonitor.currentProxyEntry?.outIp,
                    proxyEntry,
                    proxyMonitor.currentProxyEntry
            )
        }

        proxyEntry = proxyMonitor.currentProxyEntry
        task.proxyEntry = proxyEntry
        task.page.proxy = proxyEntry?.outIp

        proxyEntry?.apply {
            if (success) {
                numSuccessPages.incrementAndGet()
                lastTarget = task.url
                servedDomains.add(task.domain)
            } else {
                numFailedPages.incrementAndGet()
            }
        }
    }

    /**
     * Block until the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            proxyEntry?.let { proxyMonitor.takeOff(it, ban = true) }
        }
    }
}

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val driverManager: WebDriverManager,
        val proxyMonitor: ProxyMonitor,
        val conf: ImmutableConfig
): PrivacyContext() {

    private val driverContext = WebDriverContext(driverManager, conf)
    private val proxyContext = ProxyContext(proxyMonitor, driverContext, conf)
    private val closeLatch = CountDownLatch(1)

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) return FetchResult.privacyRetry(task)
        beforeRun(task)
        val result = proxyContext.takeIf { it.isEnabled }
                ?.run(task, browseFun)
                ?:driverContext.run(task, browseFun)
        return result.also { afterRun(it) }
    }

    open suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) return FetchResult.privacyRetry(task)
        beforeRun(task)
        val result = proxyContext.takeIf { it.isEnabled }
                ?.runDeferred(task, browseFun)
                ?:driverContext.runDeferred(task, browseFun)
        return result.also { afterRun(it) }
    }

    override fun report() {
        log.info("Privacy context #{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                id, elapsedTime.readable(),
                numSuccesses, String.format("%.2f", throughput),
                numSmallPages, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(systemNetworkBytesRecv), Strings.readableBytes(networkSpeed),
                numTasks, numTotalRun,
                proxyContext.proxyEntry ?: "<no proxy>"
        )

        if (smallPageRate > 0.5) {
            log.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    id, numSmallPages, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (throughput < 0) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", id)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    /**
     * Block until all the drivers are closed and the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverContext.use { it.close() }
            proxyContext.use { it.close() }
            closeLatch.countDown()

            report()
        }
    }

    private fun beforeRun(task: FetchTask) {
        numTasks.incrementAndGet()
    }

    private fun afterRun(result: FetchResult) {
        numTotalRun.incrementAndGet()
        if (result.status.isSuccess) {
            numSuccesses.incrementAndGet()
        }

        if (result.isSmall) {
            numSmallPages.incrementAndGet()
        }
    }
}
