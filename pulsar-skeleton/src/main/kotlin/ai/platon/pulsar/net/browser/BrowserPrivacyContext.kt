package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.PrivacyContext
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.proxy.ProxyManager
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
        val numProxyChanges = AtomicInteger()
    }

    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val closed = AtomicBoolean()
    private val closeLatch = CountDownLatch(1)

    val privacyLeakWarnings = AtomicInteger()
    override val isPrivacyLeaked get() = privacyLeakWarnings.get() > 3
    val nServedTasks = AtomicInteger()
    val servedProxies = ConcurrentLinkedQueue<ProxyEntry>()

    fun informSuccess() {
        informSuccess("")
    }

    fun informSuccess(message: String) {
        if (privacyLeakWarnings.get() > 0) {
            privacyLeakWarnings.decrementAndGet()
        }
        // log.info("success - ${message}privacyLeakWarnings: $privacyLeakWarnings freezers: $numFreezers tasks: $numTasks")
    }

    fun informWarning() {
        informWarning("")
    }

    fun informWarning(message: String) {
        privacyLeakWarnings.incrementAndGet()
        // log.info("warning - ${message}privacyLeakWarnings: $privacyLeakWarnings freezers: $numFreezers tasks: $numTasks")
    }

    open fun <T> run(task: FetchTask, action: () -> T): T {
        return whenUnfrozen {
            action()
        }
    }

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return whenUnfrozen {
            runWithProxy(task, browseFun).also { nServedTasks.incrementAndGet() }
        }
    }

    open fun runInContext(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var retry = 1
        do {
            if (isPrivacyLeaked) {
                task.reset()
                close()
            }

            result = run(task) { _, driver ->
                browseFun(task, driver)
            }

            val response = result.response
            if (response.status.isSuccess) {
                informSuccess()
            } else if (response.status.isRetry(RetryScope.PRIVACY_CONTEXT)) {
                informWarning()
            }
        } while (retry++ <= 2 && isPrivacyLeaked)

        return result
    }

    fun waitUntilClosed() {
        try {
            closeLatch.await()
        } catch (ignored: InterruptedException) {}
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            freeze {
                log.info("Privacy context #{} is closed after {} tasks ...", id, nServedTasks)

                changeProxy()
                driverManager.reset()

                closeLatch.countDown()
            }
        }
    }

    private fun changeProxy() {
        proxyManager.currentProxyEntry?.let { proxyManager.changeProxyIfOnline(it) }
    }

    private fun runWithProxy(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var proxyEntry = proxyManager.currentProxyEntry

        val result: FetchResult
        var success = false
        try {
            result = proxyManager.runAnyway { runInDriverPool(task, browseFun) }
            success = result.response.status.isSuccess
        } finally {
            if (proxyEntry != proxyManager.currentProxyEntry) {
                proxyEntry = proxyManager.currentProxyEntry
                numProxyChanges.incrementAndGet()
            }

            if (proxyEntry != null) {
                task.proxyEntry = proxyEntry
                task.page.metadata.set(Name.PROXY, proxyEntry.hostPort)

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
