package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import org.slf4j.LoggerFactory
import java.lang.Thread.currentThread
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
                // Mark all tasks are canceled, so they return as soon as possible
                runningTasks.forEach { it.cancel() }
                // Mark all drivers are canceled
                driverManager.cancelAll()
                // may wait for cancelling finish?
                // Close all online drivers and delete the browser data
                driverManager.reset()
                // Wait for all tasks return
                lock.withLock {
                    var i = 0
                    while (i++ < 30 && runningTasks.isNotEmpty() && !currentThread().isInterrupted) {
                        notBusy.await(1, TimeUnit.SECONDS)
                    }
                }

                if (runningTasks.isNotEmpty()) {
                    log.warn("Still {} running tasks after context close", runningTasks.size)
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
        val numNoProxyException = AtomicInteger()
    }
    private val log = LoggerFactory.getLogger(ProxyContext::class.java)!!
    /**
     * If the number of success exceeds [maxFetchSuccess], force emit PrivacyRetry result.
     * It's used for test purpose
     * */
    private val maxFetchSuccess = conf.getInt(CapabilityTypes.PROXY_MAX_FETCH_SUCCESS, Int.MAX_VALUE)
    private val closed = AtomicBoolean()
    /**
     * The proxy for this context
     * */
    var proxyEntry: ProxyEntry? = null
        private set
    val isActive get() = proxyMonitor.isActive && !closed.get()

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.run0(task, browseFun)?:FetchResult.privacyRetry(task)
    }

    suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.runDeferred0(task, browseFun)?:FetchResult.privacyRetry(task)
    }

    private fun run0(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.run { driverContext.run(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: NoProxyException) {
            val count = numNoProxyException.incrementAndGet()

            if (count > 10) {
                throw ProxyVendorUntrustedException("No proxy available from proxy vendor, the vendor is untrusted")
            }

            result = FetchResult.crawlRetry(task)
            log.warn("No proxy available temporary the {}th times, cause: {}", count, e.message)
        } catch (e: ProxyException) {
            result = FetchResult.privacyRetry(task)
            log.warn("Task failed with proxy {}, cause: {}", proxyEntry, e.message)
        } finally {
            afterTaskFinished(task, success)
        }

        return result
    }

    @Throws(ProxyVendorUntrustedException::class)
    private suspend fun runDeferred0(
            task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.runDeferred { driverContext.runDeferred(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: NoProxyException) {
            if (numNoProxyException.incrementAndGet() > 10) {
                throw ProxyVendorUntrustedException("No proxy available from proxy vendor, the vendor is untrusted")
            }

            result = FetchResult.crawlRetry(task)
            log.warn("No proxy available temporary the {}th times, cause: {}", numNoProxyException, e.message)
        } catch (e: ProxyException) {
            result = FetchResult.privacyRetry(task)
            log.warn("Task failed with proxy {}, cause: {}", proxyEntry, e.message)
        } finally {
            afterTaskFinished(task, success)
        }

        val proxy = proxyEntry
        // used for test purpose
        if (proxy != null) {
            // TEST code, trigger the privacy context reset
            val successPages = proxy.numSuccessPages.get()
            if (successPages > maxFetchSuccess && result.status.isSuccess) {
                log.info("[TEST] Emit a PRIVACY retry after $successPages pages served | {}", proxy)
                result = FetchResult.privacyRetry(task)
            }
        }

        return result
    }

    private fun beforeTaskStart() {
        // initialize the local recorded proxy entry
        if (proxyEntry == null && proxyMonitor.currentProxyEntry != null) {
            proxyEntry = proxyMonitor.currentProxyEntry
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        if (proxyEntry != null && proxyEntry != proxyMonitor.currentProxyEntry) {
            log.warn("Proxy has been changed unexpected | {} -> {} | {} -> {}",
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

    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val closeLatch = CountDownLatch(1)
    private val driverContext = WebDriverContext(driverManager, conf)
    private val proxyContext = ProxyContext(proxyMonitor, driverContext, conf)

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) return FetchResult.privacyRetry(task)
        beforeRun()
        return proxyContext.run(task, browseFun).also { afterRun(it) }
    }

    open suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) return FetchResult.privacyRetry(task)
        beforeRun()
        return proxyContext.runDeferred(task, browseFun).also { afterRun(it) }
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

    private fun report() {
        log.info("Privacy context #{} has lived for {} | success: {}({} pages/s) | tasks: {} run: {} with {}",
                id, elapsedTime.readable(),
                numSuccesses, String.format("%.2f", throughput),
                numTasks, numTotalRun, proxyContext.proxyEntry ?: "no proxy")

        if (throughput < 1) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", id)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    private fun beforeRun() {
        numTasks.incrementAndGet()
    }

    private fun afterRun(result: FetchResult) {
        numTotalRun.incrementAndGet()
        if (result.status.isSuccess) {
            numSuccesses.incrementAndGet()
        }
    }
}
