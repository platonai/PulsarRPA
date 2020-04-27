package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyInactiveException
import ai.platon.pulsar.common.proxy.ProxyMonitor
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverManager
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebDriverContext(
        private val driverManager: WebDriverManager,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = conf.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)

    suspend fun submit(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        do {
            result = driverManager.submit(task.priority, task.volatileConfig) { browseFun(task, it) }

            if (task.nRetries > 1) {
                log.info("The ${task.nRetries}th retry to fetch | {}", task.url)
            }
        } while(task.nRetries < fetchMaxRetry && !task.isCanceled && result.status.isRetry(RetryScope.WEB_DRIVER))

        return result
    }

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        do {
            result = driverManager.run(task.priority, task.volatileConfig) { browseFun(task, it) }

            if (task.nRetries > 1) {
                log.info("The ${task.nRetries}th retry to fetch | {}", task.url)
            }
        } while(task.nRetries < fetchMaxRetry && !task.isCanceled && result.status.isRetry(RetryScope.WEB_DRIVER))

        return result
    }

    override fun close() {
        driverManager.reset()
    }
}

class ProxyContext(
        private val proxyMonitor: ProxyMonitor,
        private val driverContext: WebDriverContext,
        private val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyContext::class.java)!!
    /**
     * The proxy for this context
     * */
    var proxyEntry: ProxyEntry? = null

    suspend fun submit(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.submit { driverContext.submit(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: ProxyException) {
            log.warn(Strings.simplifyException(e))
            result = FetchResult.retry(task, RetryScope.PRIVACY)
        } finally {
            afterTaskFinished(task, success)
        }

        return result
    }

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var success = false
        try {
            beforeTaskStart()
            result = proxyMonitor.run { driverContext.run(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: ProxyException) {
            log.warn(Strings.simplifyException(e))
            result = FetchResult.retry(task, RetryScope.PRIVACY)
        } finally {
            afterTaskFinished(task, success)
        }

        return result
    }

    private fun beforeTaskStart() {
        if (proxyEntry == null && proxyMonitor.currentProxyEntry != null) {
            proxyEntry = proxyMonitor.currentProxyEntry
        }
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean) {
        if (proxyEntry != proxyMonitor.currentProxyEntry) {
            log.warn("Proxy has been changed unexpected {} -> {}", proxyEntry, proxyMonitor.currentProxyEntry)
        }

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

    override fun close() {
        proxyEntry?.let { proxyMonitor.changeProxyIfOnline(it, ban = true) }
    }
}

/**
 * The privacy context, the context should be dropped if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val driverManager: WebDriverManager,
        val proxyMonitor: ProxyMonitor,
        val conf: ImmutableConfig,
        val id: Int = instanceSequencer.incrementAndGet()
): PrivacyContext() {
    companion object {
        private val instanceSequencer = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val closed = AtomicBoolean()
    private val closeLatch = CountDownLatch(1)
    private val driverContext = WebDriverContext(driverManager, conf)
    private val proxyContext = ProxyContext(proxyMonitor, driverContext, conf)

    val isAlive get() = !closed.get()

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isAlive }?.let {
            numTasks.incrementAndGet()
            proxyContext.run(task, browseFun).also { afterRun(it) }
        }?:FetchResult.retry(task, RetryScope.PRIVACY)
    }

    open suspend fun submit(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return takeIf { isAlive }?.let {
            numTasks.incrementAndGet()
            proxyContext.submit(task, browseFun).also { afterRun(it) }
        }?:FetchResult.retry(task, RetryScope.PRIVACY)
    }

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
                numSuccesses, throughput,
                numTasks, numTotalRun, proxyContext.proxyEntry ?: "no proxy")

        if (throughput < 1) {
            log.info("It is expected at least 120 pages in 120 seconds within a context")
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    private fun afterRun(result: FetchResult) {
        numTotalRun.incrementAndGet()
        if (result.status.isSuccess) {
            numSuccesses.incrementAndGet()
        }
    }
}
