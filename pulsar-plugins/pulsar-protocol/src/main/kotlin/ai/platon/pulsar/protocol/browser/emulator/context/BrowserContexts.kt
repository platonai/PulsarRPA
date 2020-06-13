package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

class WebDriverContext(
        val dataDir: Path,
        private val driverManager: WebDriverManager,
        private val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
        private val lock = ReentrantLock()
        private val notBusy = lock.newCondition()

        init {
            MetricsManagement.register(this,"runningTasks", Gauge<Int> { runningTasks.size })
        }
    }

    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = conf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val DRIVER_CLOSE_TIME_OUT = Duration.ofSeconds(60)
    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get()
    private val browserDataDir = dataDir.resolve("browser")

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            driverManager.run(browserDataDir, task.priority, task.volatileConfig) {
                task.proxyEntry = it.proxyEntry
                browseFun(task, it)
            }
        } finally {
            runningTasks.remove(task)
            if (runningTasks.isEmpty()) {
                lock.withLock { notBusy.signalAll().also { log.info("No running task now") } }
            }
        }
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

        // Wait for all tasks return
        lock.withLock {
            var i = 0
            try {
                while (i++ < 30 && runningTasks.isNotEmpty()) {
                    notBusy.await(1, TimeUnit.SECONDS)
                }
            } catch (ignored: InterruptedException) {}
        }

        if (runningTasks.isNotEmpty()) {
            log.warn("Still {} running tasks after context close | {}",
                    runningTasks.size, runningTasks.joinToString { "${it.id}(${it.state})" })
        } else {
            log.info("WebDriverContext is closed successfully")
        }
    }

    private fun closeUnderlyingLayer() {
        if (Files.exists(browserDataDir)) {
            Files.writeString(browserDataDir.resolve("CLOSE_INSTANCE"), "", StandardOpenOption.CREATE_NEW)
        } else {
            log.error("Browser data dir {} does not exist", browserDataDir)
        }

        // Mark all working tasks are canceled, so they return as soon as possible,
        // the ready tasks are blocked to wait for driverManager.reset() finish
        runningTasks.forEach { it.cancel() }
        // Mark all drivers are canceled
        driverManager.cancelAll(browserDataDir)
        // may wait for cancelling finish?
        // Close all online drivers and delete the browser data
        driverManager.reset(browserDataDir, DRIVER_CLOSE_TIME_OUT)
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
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
        private val proxyPoolMonitor: ProxyPoolMonitor,
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
    private val maxFetchSuccess = conf.getInt(CapabilityTypes.PROXY_MAX_FETCH_SUCCESS, Int.MAX_VALUE)
    private val maxAllowedProxyAbsence = conf.getInt(CapabilityTypes.PROXY_MAX_ALLOWED_PROXY_ABSENCE, 10)
    private val minTimeToLive = Duration.ofSeconds(10)
    private val closing = AtomicBoolean()
    private val closed = AtomicBoolean()

    var proxyEntry: ProxyEntry? = null
    val isEnabled get() = proxyPoolMonitor.isEnabled
    val isActive get() = proxyPoolMonitor.isActive && !closing.get() && !closed.get()

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return checkAbnormalResult(task) ?:run0(task, browseFun)?:FetchResult.privacyRetry(task)
    }

    @Throws(ProxyVendorUntrustedException::class)
    private suspend fun run0(
            task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var success = false
        return try {
            beforeTaskStart(task)
            proxyPoolMonitor.run(proxyEntry) { driverContext.run(task, browseFun) }.also {
                success = it.response.status.isSuccess
                proxyEntry = task.proxyEntry

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
            if (it.willExpireAfter(minTimeToLive)) {
                if (closing.compareAndSet(false, true)) {
                    throw ProxyRetiredException("The proxy expires in $minTimeToLive")
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
                    throw ProxyRetiredException("Too many pages")
                }
            }
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        numRunningTasks.decrementAndGet()
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
            proxyPoolMonitor.activeProxyEntries.remove(driverContext.dataDir)
        }
    }
}
