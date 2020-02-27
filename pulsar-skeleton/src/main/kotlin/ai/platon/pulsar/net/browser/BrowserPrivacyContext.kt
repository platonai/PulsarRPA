package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.Freezable
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.proxy.ProxyManager
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * The browser emulator context, the context should be reset if some errors are detected, for example, proxy ips are banned.
 * */
open class BrowserPrivacyContext(
        val driverPool: WebDriverPool,
        val proxyManager: ProxyManager,
        val immutableConfig: ImmutableConfig
): Freezable() {
    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)!!
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)

    private val privacyLeakWarnings = AtomicInteger()
    val isPrivacyLeaked = privacyLeakWarnings.get() > 3

    fun informSuccess() {
        if (privacyLeakWarnings.get() > 0) {
            privacyLeakWarnings.decrementAndGet()
        }
    }

    fun informWarning() {
        privacyLeakWarnings.incrementAndGet()
    }

    open fun reset() {
        freeze {
            log.info("Resetting privacy context ...")

            changeProxy()
            driverPool.reset()
            privacyLeakWarnings.set(0)
        }
    }

    open fun run(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        return whenUnfrozen {
            runWithProxy(task, browseFun)
        }
    }

    open fun runInContext(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult

        var retry = 1
        do {
            if (isPrivacyLeaked) {
                task.reset()
                reset()
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

    private fun changeProxy() {
        driverPool.proxyEntry?.let { proxyManager.changeProxyIfRunning(it) }
    }

    private fun runWithProxy(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        val result = proxyManager.runAnyway { runInDriverPool(task, browseFun) }
        val proxyEntry = proxyManager.proxyEntry
        if (proxyEntry != null) {
            if (result.response.status.isSuccess) {
                proxyEntry.successPageCount.incrementAndGet()
                proxyEntry.servedDomains.add(task.domain)
                proxyEntry.targetHost = task.page.url
                task.page.metadata.set(Name.PROXY, proxyEntry.hostPort)
            } else {
                proxyEntry.failedPageCount.incrementAndGet()
            }
        }
        return result
    }

    private fun runInDriverPool(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> FetchResult): FetchResult {
        var result: FetchResult
        var response: Response
        do {
            result = driverPool.run(task.priority, task.volatileConfig) {
                browseFun(task, it)
            }
            response = result.response

            if (task.retries > 1) {
                log.info("The ${task.retries}th round re-fetching | {}", task.url)
            }
        } while(task.retries <= fetchMaxRetry && response.status.isRetry(RetryScope.WEB_DRIVER))

        return result
    }
}
