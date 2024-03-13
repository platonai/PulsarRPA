package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.metrics.MetricsSystem
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.crawl.fetch.driver.WebDriverUnavailableException
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager.Companion.DRIVER_CLOSE_TIME_OUT
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolException
import ai.platon.pulsar.protocol.browser.emulator.WebDriverPoolExhaustedException
import com.codahale.metrics.Gauge
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The web driver context.
 * Web page fetch tasks run in web driver contexts.
 * */
open class WebDriverContext(
    val browserId: BrowserId,
    private val driverPoolManager: WebDriverPoolManager,
    private val unmodifiedConfig: ImmutableConfig
): AutoCloseable {
    companion object {
        private val numGlobalRunningTasks = AtomicInteger()
        private val globalTasks = MetricsSystem.reg.meter(this, "globalTasks")
        private val globalFinishedTasks = MetricsSystem.reg.meter(this, "globalFinishedTasks")

        init {
            MetricsSystem.reg.register(this,"globalRunningTasks", Gauge { numGlobalRunningTasks.get() })
        }
    }

    private val logger = LoggerFactory.getLogger(WebDriverContext::class.java)!!
    private val runningTasks = ConcurrentLinkedDeque<FetchTask>()
    private val lock = ReentrantLock()
    private val notBusy = lock.newCondition()

    private val closed = AtomicBoolean()
    /**
     * The driver context is active if the following conditions meet:
     * 1. the context is not closed
     * 2. the application is active
     * */
    open val isActive get() = !closed.get() && AppContext.isActive
    /**
     * Check if the driver context is retired.
     * */
    open val isRetired get() = driverPoolManager.isRetiredPool(browserId)
    /**
     * Check if the driver context is ready to serve
     * */
    open val isReady: Boolean
        get() {
            val isDriverPoolReady = driverPoolManager.isReady && driverPoolManager.hasDriverPromise(browserId)
            return isActive && isDriverPoolReady
        }

    suspend fun run(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        globalTasks.mark()
        return checkAbnormalResult(task) ?: try {
            runningTasks.add(task)
            numGlobalRunningTasks.incrementAndGet()
            driverPoolManager.run(browserId, task) {
                browseFun(task, it)
            } ?: FetchResult.crawlRetry(task, "Null response from driver pool manager")
        } catch (e: WebDriverPoolExhaustedException) {
            val message = String.format("%s. Retry task %s in crawl scope | cause by: %s",
                task.page.id, task.id, e.message)
            logger.warn(message)
            FetchResult.crawlRetry(task, WebDriverUnavailableException(message, e))
        } catch (e: WebDriverPoolException) {
            logger.warn("{}. Retry task {} in crawl scope", task.page.id, task.id)
            FetchResult.crawlRetry(task, "Driver pool exception")
        } catch (e: WebDriverException) {
            logger.warn("{}. Retry task {} in crawl scope | caused by: {}", task.page.id, task.id, e.message)
            FetchResult.crawlRetry(task, "Driver exception")
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

    @Throws(Exception::class)
    open fun maintain() {
        // should close dead, valueless, idle driver pools, etc
    }

    /**
     * Closing call stack:
     *
     * PrivacyContextManager.close -> PrivacyContext.close -> WebDriverContext.close -> WebDriverPoolManager.close
     * -> BrowserManager.close -> Browser.close -> WebDriver.close
     * |-> LoadingWebDriverPool.close
     *
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (!AppContext.isActive) {
                runCatching { shutdownUnderlyingLayerImmediately() }.onFailure { warnForClose(this, it) }
            } else {
                runCatching { closeContext() }.onFailure { warnForClose(this, it) }
            }
        }
    }

    private fun closeContext() {
        val asap = !AppContext.isActive || AppSystemInfo.isCriticalResources

        logger.debug("Closing web driver context, asap: $asap")

        // not shutdown, wait longer
        if (asap) {
            closeUnderlyingLayerGracefully()
        } else {
            // TODO: always close the context as soon as possible, just retry the the unfinished tasks.
            waitUntilAllDoneNormally(Duration.ofMinutes(1))
            // close underlying IO based modules asynchronously
            closeUnderlyingLayerGracefully()
        }

        waitUntilNoRunningTasks(Duration.ofSeconds(10))

        val isShutdown = if (AppContext.isActive) "" else " (shutdown)"
        val display = browserId.display
        if (runningTasks.isNotEmpty()) {
            logger.info("Still {} running tasks after context close$isShutdown | {} | {}",
                runningTasks.size, runningTasks.joinToString { "${it.id}(${it.state})" }, display)
        } else {
            logger.info("Web driver context is closed successfully$isShutdown | {} | {}", display, browserId)
        }
    }

    private fun closeUnderlyingLayerGracefully() {
        // Mark all working tasks to be canceled, so they return as soon as possible
        runningTasks.forEach { it.cancel() }
        // Cancel the browser, and all online drivers, and the worker coroutines with the drivers
        driverPoolManager.cancelAll(browserId)

        driverPoolManager.closeDriverPoolGracefully(browserId, DRIVER_CLOSE_TIME_OUT)
    }

    private fun shutdownUnderlyingLayerImmediately() {
        logger.info("Shutdown the underlying layer immediately")

        runningTasks.forEach { it.cancel() }
        driverPoolManager.cancelAll()
        driverPoolManager.close()
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
            while (runningTasks.isNotEmpty() && !AppSystemInfo.isCriticalResources && n-- > 0) {
                notBusy.await(1, TimeUnit.SECONDS)
            }
        } finally {
            lock.unlock()
        }

        val isShutdown = if (AppContext.isActive) "" else " (shutdown)"
        val display = browserId.display
        val message = when {
            AppSystemInfo.isCriticalMemory ->
                String.format("Low memory (%s), close %d retired browsers immediately$isShutdown | $display",
                    AppSystemInfo.formatAvailableMemory(), runningTasks.size)
            n <= 0L -> String.format("Timeout (still %d running tasks)$isShutdown | $display", runningTasks.size)
            n > 0 -> String.format("All tasks return in %d seconds$isShutdown | $display", timeout.seconds - n)
            else -> ""
        }

        if (message.isNotBlank()) {
            logger.info(message)
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        if (!isActive) {
            return FetchResult.canceled(task, "Inactive web driver context")
        }

        if (driverPoolManager.isRetiredPool(browserId)) {
            return FetchResult.canceled(task, "Retired driver pool")
        }

        return null
    }
}
