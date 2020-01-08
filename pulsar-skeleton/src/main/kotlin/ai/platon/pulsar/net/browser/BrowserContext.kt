package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.RuntimeUtils
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.proxy.InternalProxyServer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The browse context, the context should be reset if some errors are detected, for example, proxy ips are banned.
 *
 * A ContextResetException can be thrown at any point during browsing, and we must handle the synchronization carefully,
 * [TestConditionWithThrow] is used to test the synchronization model.
 *
 * TODO: the synchronization model works, but seems not strict and has chance to lost page data
 * */
class BrowserContext(
        val driverPool: WebDriverPool,
        val ips: InternalProxyServer,
        val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(BrowserContext::class.java)!!

    private var fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    private val pollingTimeout = Duration.ofMillis(500)

    private var nSignals = 0
    private var nWaits = 0
    private var nWaitsTimeout = 0
    private val nPending = AtomicInteger()
    private val nRunning = AtomicInteger()
    private var sponsorThreadId = 0L
    private val lock = ReentrantLock()
    private val notMaintain = lock.newCondition()
    private val notBusy = lock.newCondition()

    fun run(task: FetchTask, browseAction: (FetchTask, ManagedWebDriver) -> BrowseResult): BrowseResult {
        val maxRetry = task.volatileConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, fetchMaxRetry)
        var result: BrowseResult

        var i = 0
        var noGuard = false
        var retry: Boolean
        do {
            // wait for the browser context to be ready

            guard()

            if (i > 1) {
                log.info("Round {} retrying new browser context ... | {}", i, task.url)
            }

            nRunning.incrementAndGet()
            result = ips.runAnyway { runWithDriverPool(task, browseAction) }
            nRunning.decrementAndGet()
            if (nRunning.get() == 0) {
                notBusy.signal()
            }

            retry = result.response?.status?.isRetry(RetryScope.BROWSER_CONTEXT)?:false
            if (retry) {
                if (i == 0) {
                    reset()
                } else {
                    log.warn("Context reset is not allowed | {}", task.url)
                    retry = false
                    result = BrowseResult(ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.CRAWL_SCHEDULE)))
                }
            }
        } while (retry && i++ < maxRetry && !Thread.currentThread().isInterrupted)

        return result
    }

    private fun guard() {
        val tid = Thread.currentThread().id
        var nanos = pollingTimeout.toNanos()

        nPending.incrementAndGet()
        lock.withLock {
            while (sponsorThreadId != 0L && sponsorThreadId != tid && nanos > 0) {
                nanos = notMaintain.awaitNanos(nanos)
            }
        }
        nPending.decrementAndGet()
    }

    private fun reset() {
        val tid = Thread.currentThread().id

        lock.withLock {
            sponsorThreadId = tid
            notBusy.await()

            // This thread issues the first reset request, it will be the reset sponsor
            log.info("Start resetting browser context - {}", formatStatus())
            driverPool.resetContext()
            sponsorThreadId = 0

            notMaintain.signalAll()
            ++nSignals
            log.info("Finish resetting browser context - {}", formatStatus())
        }
    }

    private fun formatStatus(): String {
        return "pending: $nPending waits: $nWaits | signals: $nSignals waitsTimeout: $nWaitsTimeout"
    }

    private fun runWithDriverPool(task: FetchTask, browseAction: (FetchTask, ManagedWebDriver) -> BrowseResult): BrowseResult {
        val maxRetry = task.volatileConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, fetchMaxRetry)
        var result = BrowseResult()

        // TODO: when and why to retry?
        var i = 0
        while (i++ < maxRetry && result.response == null) {
            if (i > 1) {
                log.warn("Round {} retrying another Web driver ... | {}", i, task.url)
            }

            driverPool.run(task.priority, task.volatileConfig) {
                result = browseAction(task, it)
            }
        }

        return result
    }

    private fun resetBrowser(task: FetchTask, driver: ManagedWebDriver, result: BrowseResult) {
        if (task.deleteAllCookies) {
            log.info("Deleting all cookies after task {}-{} under {}", task.batchId, task.taskId, task.domain)
            driver.deleteAllCookiesSilently()
            task.deleteAllCookies = false
        }

        if (RuntimeUtils.hasLocalFileCommand(AppConstants.CMD_WEB_DRIVER_CLOSE_ALL)) {
            log.info("Executing local file command {}", AppConstants.CMD_WEB_DRIVER_CLOSE_ALL)
            driverPool.closeAll()
            task.closeBrowsers = false
        }

        if (task.closeBrowsers) {
            driverPool.closeAll()
            task.closeBrowsers = false
        }
    }
}
