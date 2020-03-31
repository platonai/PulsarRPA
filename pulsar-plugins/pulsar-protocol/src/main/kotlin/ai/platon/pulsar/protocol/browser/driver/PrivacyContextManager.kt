package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyManagerFactory
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.RetryScope
import org.jsoup.Connection
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: multiple context support
 * */
class PrivacyContextManager(
        val proxyManagerFactory: ProxyManagerFactory,
        val driverManager: WebDriverManager,
        val immutableConfig: ImmutableConfig
): Freezable() {
    companion object {
        private val globalActiveContext = AtomicReference<BrowserPrivacyContext>()
        val zombieContexts = ConcurrentLinkedQueue<BrowserPrivacyContext>()
    }

    val proxyManager = proxyManagerFactory.get()
    val maxRetry = 2
//    val retryingTasks = LinkedBlockingQueue<FetchTask>()

    val activeContext get() = getOrCreate()

    suspend fun submit(task: FetchTask, fetchFun: suspend (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return activeContext.submit(task) { _, driver ->
            fetchFun(task, driver)
        }
    }

    fun run(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return whenUnfrozen { run0(task, fetchFun) }
    }

    fun reset() {
        freeze {
            if (activeContext.isPrivacyLeaked) {
                synchronized(globalActiveContext) {
                    if (activeContext.isPrivacyLeaked) {
                        // we need to freeze all running tasks and reset driver pool and proxy
                        globalActiveContext.get()?.use { it.close() }
                        globalActiveContext.getAndSet(null)?.let { zombieContexts.add(it) }
                    }
                }
            }
        }
    }

    private fun run0(task: FetchTask, fetchFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var i = 1
        var retry = false
        do {
            if (activeContext.isPrivacyLeaked) {
                // hung up all other tasks
                reset()
                task.reset()
            }

            result = activeContext.run(task) { _, driver ->
                fetchFun(task, driver)
            }

            val response = result.response
            if (response.status.isSuccess) {
                activeContext.informSuccess()
                retry = false
            } else if (response.status.isRetry(RetryScope.PRIVACY)) {
                activeContext.informWarning()
                retry = activeContext.isPrivacyLeaked
            }
        } while (retry && i++ <= maxRetry)

        return result
    }

    private fun getOrCreate(): BrowserPrivacyContext {
        synchronized(globalActiveContext) {
            if (globalActiveContext.get() == null) {
                globalActiveContext.set(BrowserPrivacyContext(driverManager, proxyManager, immutableConfig))
            }
            return globalActiveContext.get()
        }
    }
}
