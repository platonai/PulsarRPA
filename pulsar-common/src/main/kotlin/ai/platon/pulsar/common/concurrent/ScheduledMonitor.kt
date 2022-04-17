package ai.platon.pulsar.common.concurrent

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class ScheduledMonitor(
        var initialDelay: Duration = Duration.ofMinutes(5),
        var watchInterval: Duration = Duration.ofSeconds(30),
        val executor: ScheduledExecutorService = createDefaultExecutor(),
        val autoClose: Boolean = true
): AutoCloseable {
    private val log = LoggerFactory.getLogger(ScheduledMonitor::class.java)

    protected var scheduledFuture: ScheduledFuture<*>? = null

    /**
     * Starts the monitor at the given period with the specific runnable action
     * Visible only for testing
     */
    fun start(initialDelay: Duration, period: Duration, runnable: () -> Unit) {
        start(initialDelay.seconds, period.seconds, TimeUnit.SECONDS) { runnable() }
    }

    fun start(initialDelay: Duration, period: Duration) {
        start(initialDelay.seconds, period.seconds, TimeUnit.SECONDS) { watch() }
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param period the amount of time between polls
     * @param unit   the unit for `period`
     */
    open fun start(period: Long, unit: TimeUnit) {
        start(period, period, unit)
    }

    /**
     * Starts the reporter polling at the given period with the specific runnable action.
     * Visible only for testing.
     */
    @Synchronized
    open fun start(initialDelay: Long, period: Long, unit: TimeUnit, runnable: Runnable) {
        require(scheduledFuture == null) { "Scheduled monitor is already started | ${this.javaClass.simpleName}" }
        scheduledFuture = executor.scheduleAtFixedRate(runnable, initialDelay, period, unit)
        log.info("Scheduled monitor is started | {}", this.javaClass.simpleName)
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param initialDelay the time to delay the first execution
     * @param period       the amount of time between polls
     * @param unit         the unit for `period` and `initialDelay`
     */
    @Synchronized
    open fun start(initialDelay: Long, period: Long, unit: TimeUnit) {
        start(initialDelay, period, unit, Runnable {
            try {
                watch()
            } catch (ex: Throwable) {
                log.error("Exception thrown from {}#report. Exception was suppressed.", javaClass.simpleName, ex)
            }
        })
    }

    fun start() = start(initialDelay, watchInterval) { watch() }

    abstract fun watch()

    override fun close() {
        if (autoClose) {
            stopExecution(executor, scheduledFuture, true)
            log.info("Scheduled monitor is closed | $this")
        }
    }

    companion object {
        private fun createDefaultExecutor(): ScheduledExecutorService {
            val factory = ThreadFactoryBuilder().setNameFormat("em-%d").build()
            return Executors.newSingleThreadScheduledExecutor(factory)
        }
    }
}
