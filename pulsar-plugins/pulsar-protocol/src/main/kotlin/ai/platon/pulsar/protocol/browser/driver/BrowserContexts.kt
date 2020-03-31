package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyException
import ai.platon.pulsar.common.proxy.ProxyManager
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.RetryScope
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebDriverContext(
        val driverManager: WebDriverManager,
        val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)

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
}

class ProxyContext(
        val driverManager: WebDriverManager,
        val proxyManager: ProxyManager,
        val conf: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(ProxyContext::class.java)!!
    val driverContext = WebDriverContext(driverManager, conf)
    val servedProxies = ConcurrentLinkedQueue<ProxyEntry>()

    suspend fun submit(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        val proxyEntry = proxyManager.currentProxyEntry
        var result: FetchResult
        var success = false
        try {
            result = proxyManager.submitAnyway {
                driverContext.submit(task, browseFun)
            }
            success = result.response.status.isSuccess
        } catch (e: ProxyException) {
            log.warn(Strings.simplifyException(e))
            result = FetchResult(task, ForwardingResponse.retry(task.page, RetryScope.CRAWL))
        } finally {
            afterTaskFinished(task, success, proxyEntry)
        }

        return result
    }

    fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        val proxyEntry = proxyManager.currentProxyEntry
        var result: FetchResult
        var success = false
        try {
            result = proxyManager.runAnyway { driverContext.run(task, browseFun) }
            success = result.response.status.isSuccess
        } catch (e: ProxyException) {
            log.warn(Strings.simplifyException(e))
            result = FetchResult(task, ForwardingResponse.retry(task.page, RetryScope.CRAWL))
        } finally {
            afterTaskFinished(task, success, proxyEntry)
        }

        return result
    }

    private fun afterTaskFinished(task: FetchTask, success: Boolean, proxyEntry0: ProxyEntry?) {
        var proxyEntry = proxyEntry0
        if (proxyEntry != proxyManager.currentProxyEntry) {
            proxyEntry = proxyManager.currentProxyEntry
            BrowserPrivacyContext.numProxies.incrementAndGet()
        }

        task.proxyEntry = proxyEntry
        task.page.proxy = proxyEntry?.outIp

        if (proxyEntry != null) {
            if (success) {
                proxyEntry.numSuccessPages.incrementAndGet()
                proxyEntry.lastTarget = task.url
                proxyEntry.servedDomains.add(task.domain)

                servedProxies.add(proxyEntry)
            } else {
                proxyEntry.numFailedPages.incrementAndGet()
            }
        }
    }
}

/**
 * The privacy context, the context should be dropped if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val driverManager: WebDriverManager,
        val proxyManager: ProxyManager,
        val conf: ImmutableConfig,
        val id: Int = instanceSequence.incrementAndGet()
): PrivacyContext() {
    companion object {
        val instanceSequence = AtomicInteger()
        val numProxies = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val closed = AtomicBoolean()
    private val closeLatch = CountDownLatch(1)
    val numServedTasks = AtomicInteger()
    val proxyContext = ProxyContext(driverManager, proxyManager, conf)

    suspend fun submit(task: FetchTask, browseFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return proxyContext.submit(task) { _, driver -> browseFun(task, driver) }
    }

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return whenUnfrozen {
            if (closed.get()) {
                FetchResult(task, ForwardingResponse.retry(task.page, RetryScope.PRIVACY))
            } else {
                proxyContext.run(task, browseFun).also { numServedTasks.incrementAndGet() }
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            freeze {
                log.info("Privacy context #{} is closed after {} tasks | {}",
                        id, numServedTasks, proxyManager.currentProxyEntry ?: "<no proxy>")

                changeProxy()
                driverManager.reset()

                closeLatch.countDown()
            }
        }
    }

    private fun changeProxy() {
        proxyManager.currentProxyEntry?.let {
            proxyManager.changeProxyIfOnline(it, ban = true)
        }
    }
}
