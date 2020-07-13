package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager.Companion.DRIVER_CLOSE_TIME_OUT
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

class WebDriverContext(
        val browserId: BrowserInstanceId,
        private val driverPoolManager: WebDriverPoolManager,
        private val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        private val numGlobalRunningTasks = AtomicInteger()
        private val numGlobalTasks = AtomicInteger()
        private val numGlobalFinishedTasks = AtomicInteger()
        private val lock = ReentrantLock()
        private val notBusy = lock.newCondition()

        init {
            MetricsManagement.register(this,"globalRunningTasks", Gauge<Int> { numGlobalRunningTasks.get() })
            MetricsManagement.register(this,"globalTasks", Gauge<Int> { numGlobalTasks.get() })
            MetricsManagement.register(this,"globalFinishedTasks", Gauge<Int> { numGlobalFinishedTasks.get() })
        }
    }

    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = conf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get()
    private val isShutdown = AtomicBoolean()

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        numGlobalTasks.incrementAndGet()
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            numGlobalRunningTasks.incrementAndGet()
            driverPoolManager.run(browserId, task.priority, task.volatileConfig) {
                browseFun(task, it)
            }?:FetchResult.crawlRetry(task)
        } catch (e: WebDriverPoolExhaustedException) {
            log.warn("{}. Retry task {} in crawl scope | cause by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task)
        } catch (e: WebDriverPoolException) {
            log.warn("{}. Retry task {} in crawl scope | caused by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task)
        } finally {
            runningTasks.remove(task)
            numGlobalRunningTasks.decrementAndGet()
            numGlobalFinishedTasks.incrementAndGet()

            if (runningTasks.isEmpty()) {
                lock.withLock { notBusy.signalAll().also { log.info("No running task in driver context now | {}", browserId) } }
            }

            if (numGlobalRunningTasks.get() == 0) {
                log.info("No running task now | $numGlobalFinishedTasks/$numGlobalTasks (finished/all)")
            }
        }
    }

    fun shutdown() {
        isShutdown.set(true)
        close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                doClose()
            } catch (t: Throwable) {
                log.error("Unexpected exception", t)
            }
        }
    }

    private fun doClose() {
        // close underlying IO based modules asynchronously
        closeUnderlyingLayer()

        if (!isShutdown.get()) {
            waitUntilNoRunningTasks()
        }

        if (runningTasks.isNotEmpty()) {
            log.warn("Still {} running tasks after context close | {}",
                    runningTasks.size, runningTasks.joinToString { "${it.id}(${it.state})" })
        } else {
            log.info("Web driver context is closed successfully | {}", browserId)
        }
    }

    private fun closeUnderlyingLayer() {
        // Mark all working tasks are canceled, so they return as soon as possible,
        // the ready tasks are blocked to wait for driverManager.reset() finish
        // TODO: why the caneled tasks do not return in time?
        runningTasks.forEach { it.cancel() }
        // may wait for cancelling finish?
        // Close all online drivers and delete the browser data
        driverPoolManager.closeDriverPool(browserId, DRIVER_CLOSE_TIME_OUT)
    }

    private fun waitUntilNoRunningTasks() {
        // Wait for all tasks return
        lock.withLock {
            var i = 0
            try {
                while (i++ < 20 && runningTasks.isNotEmpty()) {
                    notBusy.await(1, TimeUnit.SECONDS)
                }
            } catch (ignored: InterruptedException) {}
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.privacyRetry(task)
        }

        if (driverPoolManager.isRetiredPool(browserId)) {
            return FetchResult.privacyRetry(task)
        }

        if (++task.nRetries > fetchMaxRetry) {
            return FetchResult.crawlRetry(task).also {
                log.info("Too many task retries, upgrade to crawl retry | {}", task.url)
            }
        }

        return null
    }
}

class ProxyContext(
        var proxyEntry: ProxyEntry? = null,
        private val proxyPoolManager: ProxyPoolManager,
        private val driverContext: WebDriverContext,
        private val conf: ImmutableConfig
): AutoCloseable {

    companion object {
        val numProxyAbsence = AtomicInteger()
        val numRunningTasks = AtomicInteger()

        init {
            mapOf(
                    "proxyAbsences" to Gauge<Int> { numProxyAbsence.get() },
                    "runningTasks" to Gauge<Int> { numRunningTasks.get() }
            ).forEach { MetricsManagement.register(this, it.key, it.value) }
        }
    }

    private val log = LoggerFactory.getLogger(ProxyContext::class.java)!!
    /**
     * If the number of success exceeds [maxFetchSuccess], emit a PrivacyRetry result
     * */
    private val maxFetchSuccess = conf.getInt(CapabilityTypes.PROXY_MAX_FETCH_SUCCESS, Int.MAX_VALUE / 10)
    private val maxAllowedProxyAbsence = conf.getInt(CapabilityTypes.PROXY_MAX_ALLOWED_PROXY_ABSENCE, 10)
    private val minTimeToLive = Duration.ofSeconds(30)
    private val closing = AtomicBoolean()
    private val closed = AtomicBoolean()

    val isEnabled get() = proxyPoolManager.isEnabled
    val isActive get() = proxyPoolManager.isActive && !closing.get() && !closed.get()

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:run0(task, browseFun)
    }

    @Throws(ProxyVendorUntrustedException::class)
    private suspend fun run0(
            task: FetchTask, browseFun: suspend (FetchTask, AbstractWebDriver) -> FetchResult): FetchResult {
        var success = false
        return try {
            beforeTaskStart(task)
            proxyPoolManager.runWith(proxyEntry) { driverContext.run(task, browseFun) }.also {
                success = it.response.status.isSuccess
                it.response.pageDatum.proxyEntry = proxyEntry
                numProxyAbsence.takeIf { it.get() > 0 }?.decrementAndGet()
            }
        } catch (e: ProxyException) {
            handleProxyException(task, e)
        } finally {
            afterTaskFinished(task, success)
        }
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
                log.warn("{}, context reset will be triggered | {}", e.message, task.proxyEntry?:"<no proxy>")
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
        // If the proxy is idle, and here comes a new task, reset the context
        // The proxy is about to be unusable, reset the context
        proxyEntry?.also {
            task.proxyEntry = it
            it.lastActiveTime = Instant.now()

            if (it.willExpireAfter(minTimeToLive)) {
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("The proxy is expired ($minTimeToLive)")
                }
            }

            val successPages = it.numSuccessPages.get()
            // Add a random number to disturb the anti-spider
            val delta = (0.25 * maxFetchSuccess).toInt()
            val limit = maxFetchSuccess + Random(System.currentTimeMillis()).nextInt(-delta, delta)
            if (successPages > limit) {
                // If a proxy served to many pages, the target site may track the finger print of the crawler
                // and also maxFetchSuccess can be used for test purpose
                log.info("Served too many pages ($successPages/$maxFetchSuccess) | {}", it)
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("Served too many pages")
                }
            }
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        numRunningTasks.decrementAndGet()
        proxyEntry?.apply {
            if (success) {
                refresh()
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
            proxyPoolManager.activeProxyEntries.remove(driverContext.browserId.dataDir)
        }
    }
}