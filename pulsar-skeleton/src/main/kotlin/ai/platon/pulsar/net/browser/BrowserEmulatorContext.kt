package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.ContextResettable
import ai.platon.pulsar.common.ContextResettableRunner
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.proxy.ProxyManager
import org.slf4j.LoggerFactory

private class ContextResettableBrowseTask(
        val task: FetchTask,
        val browseFun: (FetchTask, ManagedWebDriver) -> BrowseResult,
        val emulatorContext: BrowserEmulatorContext
) : ContextResettable {
    var result = BrowseResult()

    override var reset = false

    override fun run(nRedo: Int) {
        result = emulatorContext.runWithProxy(task, browseFun)

        reset = when {
//            task.incognito -> true
            nRedo == 0 -> result.response?.status?.isRetry(RetryScope.BROWSER_CONTEXT)?:false
            else -> false
        }
    }
}

/**
 * The browser emulator context, the context should be reset if some errors are detected, for example, proxy ips are banned.
 * */
class BrowserEmulatorContext(
        val driverPool: WebDriverPool,
        val proxyManager: ProxyManager,
        val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(BrowserEmulatorContext::class.java)!!
    private val contextResettableRunner = ContextResettableRunner(immutableConfig) { driverPool.reset() }
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)

    fun runInResettableContext(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> BrowseResult): BrowseResult {
        val resettable = ContextResettableBrowseTask(task, browseFun, this)
        contextResettableRunner.run(resettable)
        return resettable.result
    }

    fun runWithProxy(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> BrowseResult): BrowseResult {
        return proxyManager.runAnyway { runInDriverPool(task, browseFun) }
    }

    fun runInDriverPool(task: FetchTask, browseFun: (FetchTask, ManagedWebDriver) -> BrowseResult): BrowseResult {
        var result: BrowseResult
        var response: Response = ForwardingResponse(task.url, ProtocolStatus.STATUS_SUCCESS)
        do {
            result = driverPool.run(task.priority, task.volatileConfig) {
                browseFun(task, it)
            }
            result.response?.let { response = it }

            if (task.retries > 1) {
                log.info("The ${task.retries}th round re-fetching | {}", task.url)
            }
        } while(task.retries <= fetchMaxRetry && response.status.isRetry(RetryScope.WEB_DRIVER))

        return result
    }
}
