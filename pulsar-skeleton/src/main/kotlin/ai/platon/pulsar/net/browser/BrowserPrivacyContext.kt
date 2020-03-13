package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyManager
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.metadata.Name
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The privacy context, the context should be dropped if privacy is leaked
 * */
open class BrowserPrivacyContext(
        val driverManager: WebDriverManager,
        val proxyManager: ProxyManager,
        val immutableConfig: ImmutableConfig,
        val id: Int = instanceSequence.incrementAndGet()
): Freezable(), PrivacyContext {
    companion object {
        val instanceSequence = AtomicInteger()
        val numProxies = AtomicInteger()
        /**
         * TODO: use a metrics system
         * */
        @Volatile
        var cumulativePageBytes = 0L
    }

    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean()
    private val closeLatch = CountDownLatch(1)

    val privacyLeakWarnings = AtomicInteger()
    override val isPrivacyLeaked get() = privacyLeakWarnings.get() > 3
    val numServedTasks = AtomicInteger()
    val servedProxies = ConcurrentLinkedQueue<ProxyEntry>()

    fun informSuccess() {
        if (privacyLeakWarnings.get() > 0) {
            privacyLeakWarnings.decrementAndGet()
        }
    }

    fun informWarning() {
        privacyLeakWarnings.incrementAndGet()
    }

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return whenUnfrozen {
            runWithProxy(task, browseFun).also { numServedTasks.incrementAndGet() }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            freeze {
                log.info("Privacy context #{} is closed after {} tasks | {}",
                        id, numServedTasks, proxyManager.currentProxyEntry?:"<no proxy>")

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

    private fun runWithProxy(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var proxyEntry = proxyManager.currentProxyEntry

        val result: FetchResult
        var success = false
        try {
            result = proxyManager.runAnyway { runInDriverPool(task, browseFun) }
            success = result.response.status.isSuccess

            if (success) {
                cumulativePageBytes += result.response.length()
            }
        } finally {
            if (proxyEntry != proxyManager.currentProxyEntry) {
                proxyEntry = proxyManager.currentProxyEntry
                numProxies.incrementAndGet()
            }

            task.proxyEntry = proxyEntry
            task.page.metadata.set(Name.PROXY, proxyEntry?.hostPort)

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

        return result
    }

    private fun runInDriverPool(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
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
