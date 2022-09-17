package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.measure.ByteUnit
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
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
    val browserId: BrowserId,
    private val driverPoolManager: WebDriverPoolManager,
    private val unmodifiedConfig: ImmutableConfig
): AutoCloseable {
    companion object {
        private val numGlobalRunningTasks = AtomicInteger()
        private val globalTasks = AppMetrics.reg.meter(this, "globalTasks")
        private val globalFinishedTasks = AppMetrics.reg.meter(this, "globalFinishedTasks")
        private val availableMemory get() = AppMetrics.availableMemory
        private val memoryToReserve = ByteUnit.GIB.toBytes(2.0)

        init {
            AppMetrics.reg.register(this,"globalRunningTasks", Gauge { numGlobalRunningTasks.get() })
        }
    }

    private val logger = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val closed = AtomicBoolean()
    private val isActive get() = !closed.get() && AppContext.isActive

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        globalTasks.mark()
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            numGlobalRunningTasks.incrementAndGet()
            driverPoolManager.run(browserId, task) {
                browseFun(task, it)
            }?:FetchResult.crawlRetry(task)
        } catch (e: WebDriverPoolExhaustedException) {
            logger.warn("{}. Retry task {} in crawl scope | cause by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task, Duration.ofSeconds(20))
        } catch (e: WebDriverPoolException) {
            logger.warn("{}. Retry task {} in crawl scope | caused by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task)
        } catch (e: WebDriverException) {
            logger.warn("{}. Retry task {} in crawl scope | caused by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task)
        } finally {
            runningTasks.remove(task)
            numGlobalRunningTasks.decrementAndGet()
            globalFinishedTasks.mark()

            if (runningTasks.isEmpty()) {
                lock.withLock { notBusy.signalAll() }
            }

            if (numGlobalRunningTasks.get() == 0 && globalFinishedTasks.fiveMinuteRate > 0.1) {
                logger.debug("No running task now | ${globalFinishedTasks.count}/${globalTasks.count} (finished/all)")
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            kotlin.runCatching { doClose() }.onFailure { logger.warn(it.stringify("[Unexpected][Ignored]")) }
        }
    }

    private fun doClose() {
        // not shutdown, wait longer
        if (AppContext.isActive) {
            waitUntilAllDoneNormally(Duration.ofMinutes(3))
        }

        // close underlying IO based modules asynchronously
        cancelAllAndCloseUnderlyingLayer()

        waitUntilNoRunningTasks(Duration.ofSeconds(20))

        if (runningTasks.isNotEmpty()) {
            logger.info("Still {} running tasks after context close | {}",
                runningTasks.size, runningTasks.joinToString { "${it.id}(${it.state})" })
        } else {
            logger.info("Web driver context is closed successfully | {}", browserId)
        }
    }

    private fun cancelAllAndCloseUnderlyingLayer() {
        // Mark all working tasks are canceled, so they return as soon as possible,
        // the ready tasks are blocked to wait for driverManager.reset() finish
        // TODO: why the canceled tasks do not return in time?
        runningTasks.forEach { it.cancel() }
        // may wait for cancelling finish?
        // Close all online drivers and delete the browser data
        driverPoolManager.cancelAll(browserId)
        driverPoolManager.closeDriverPool(browserId, DRIVER_CLOSE_TIME_OUT)
    }

    private fun waitUntilAllDoneNormally(timeout: Duration) {
        waitUntilIdle(timeout)
    }

    private fun waitUntilNoRunningTasks(timeout: Duration) {
        waitUntilIdle(timeout)
    }

    /**
     * Wait until there is no running tasks.
     * @see [ArrayBlockingQueue#take]
     * @throws InterruptedException if the current thread is interrupted
     * */
    @Throws(InterruptedException::class)
    private fun waitUntilIdle(timeout: Duration) {
        var n = timeout.seconds
        lock.lockInterruptibly()

        try {
            while (runningTasks.isNotEmpty() && availableMemory > memoryToReserve && n-- > 0) {
                notBusy.await(1, TimeUnit.SECONDS)
            }
        } finally {
            lock.unlock()
        }

        val message = when {
            availableMemory < memoryToReserve ->
                String.format("Low memory (%.2fGiB), close %d retired browsers immediately",
                    ByteUnit.BYTE.toGiB(availableMemory.toDouble()), runningTasks.size)
            n == 0L -> String.format("Timeout (still %d running tasks)", runningTasks.size)
            n < timeout.seconds -> String.format("All finished in %d seconds", timeout.seconds - n)
            else -> ""
        }

        if (message.isNotBlank()) {
            logger.info(message)
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.canceled(task)
        }

        if (driverPoolManager.isRetiredPool(browserId)) {
            return FetchResult.canceled(task)
        }

        return null
    }
}
