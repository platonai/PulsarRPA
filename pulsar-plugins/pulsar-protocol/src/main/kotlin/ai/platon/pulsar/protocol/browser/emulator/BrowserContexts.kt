package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.RetryScope
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
    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = conf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get()
    private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
    private val lock = ReentrantLock() // lock for containers
    private val notBusy = lock.newCondition()

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) {
            return FetchResult.retry(task, RetryScope.PRIVACY)
        }

        runningTasks.add(task)
        return try {
            driverManager.run(task.priority, task.volatileConfig) { browseFun(task, it) }
        } finally {
            runningTasks.remove(task)
            if (runningTasks.isEmpty()) {
                lock.withLock {
                    notBusy.signalAll()
                }
            }
        }
    }

    suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        if (!isActive) {
            return FetchResult.retry(task, RetryScope.PRIVACY)
        }

        runningTasks.add(task)
        return try {
            driverManager.submit(task.priority, task.volatileConfig) { browseFun(task, it) }
        } finally {
            runningTasks.remove(task)
            if (runningTasks.isEmpty()) {
                lock.withLock {
                    notBusy.signalAll()
                }
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching {
                // Mark all tasks are canceled
                runningTasks.forEach { it.cancel() }
                // Stop loading all pages, so every browser operation return as soon as possible
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
            }.onFailure { log.warn("Unexpected exception", it) }
        }
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
    private val closed = AtomicBoolean()
    /**
     * The proxy for this context
     * */
    var proxyEntry: ProxyEntry? = null
        private set
    val isActive get() = proxyMonitor.isActive && !closed.get()

    suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.let { runDeferred0(task, browseFun) }?:FetchResult.privacyRetry(task)
    }

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isActive }?.let { run0(task, browseFun) }?:FetchResult.privacyRetry(task)
    }

    private fun run0(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.run { driverContext.run(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: NoProxyException) {
            if (numNoProxyException.incrementAndGet() > 10) {
                throw ProxyVendorUnTrustedException("No proxy available from proxy vendor, the vendor is untrusted")
            }

            result = FetchResult.crawlRetry(task)
            log.warn("No proxy available temporary", e.message)
        } catch (e: ProxyException) {
            result = FetchResult.privacyRetry(task)
            log.warn("Task failed with proxy {} | {}", proxyEntry, e.message)
        } finally {
            afterTaskFinished(task, success)
        }

        return result
    }

    private suspend fun runDeferred0(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.runDeferred { driverContext.runDeferred(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: ProxyException) {
            result = FetchResult.privacyRetry(task)
            log.warn("Task failed with proxy {} | {}", proxyEntry, Strings.simplifyException(e))
        } finally {
            afterTaskFinished(task, success)
        }

        val proxy = proxyEntry
        // used for test purpose
        val maximumSuccessPages = 250000
        if (proxy != null) {
            // TEST code, trigger the privacy context reset
            val successPages = proxy.numSuccessPages.get()
            if (successPages > maximumSuccessPages && result.status.isSuccess) {
                log.info("[TEST] Force emit a PRIVACY retry after $successPages pages served | {}", proxy)
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
        return try {
            proxyContext.run(task, browseFun).also { afterRun(it) }
        } catch (e: PrivacyLeakException) {
            FetchResult.privacyRetry(task)
        }
    }

    open suspend fun runDeferred(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        // TODO: return or throw?
        if (!isActive) {
            return FetchResult.privacyRetry(task).also { log.info("Context #{} is closed | {}", id, task.url) }
        }

        beforeRun()
        return try {
            proxyContext.runDeferred(task, browseFun).also { afterRun(it) }
        } catch (e: PrivacyLeakException) {
            FetchResult.privacyRetry(task)
        }
    }

    /**
     * Block until the all drivers are closed and the proxy is offline
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
            log.info("It is expected at least 120 pages in 120 seconds within a context")
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
