package ai.platon.pulsar.common.concurrent

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * We use a GracefulScheduledExecutor because ScheduledExecutorService prevents the process from exiting.
 *
 * The Java Virtual Machine runs until all threads that are not daemon threads have died.
 * And Executors.defaultThreadFactory() creates each new thread as a non-daemon thread.
 * However, there is an overload of Executors.newSingleThreadScheduledExecutor();
 * which takes a ThreadFactory as a parameter, if you care to venture in that direction.
 * */
abstract class GracefulScheduledExecutor(
        var initialDelay: Duration = Duration.ofMinutes(1),
        var watchInterval: Duration = Duration.ofSeconds(10),
        val executor: ScheduledExecutorService = createDefaultExecutor(),
        @Deprecated("Not used")
        val autoClose: Boolean = true
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(GracefulScheduledExecutor::class.java)

    private val closed = AtomicBoolean()
    protected var scheduledFuture: ScheduledFuture<*>? = null

    /**
     * Starts the monitor at the given period with the specific runnable action
     * Visible only for testing
     */
    fun start(initialDelay: Duration, period: Duration, runnable: () -> Unit) {
        start(initialDelay.seconds, period.seconds, TimeUnit.SECONDS) { runnable() }
    }

    fun start(initialDelay: Duration, period: Duration) {
        start(initialDelay.seconds, period.seconds, TimeUnit.SECONDS) { run() }
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
        logger.info("Scheduled monitor is started | {}", this.javaClass.simpleName)
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
        start(initialDelay, period, unit) {
            try {
                run()
            } catch (e: Throwable) {
                logger.error("Exception thrown from {} report. Exception was suppressed.", javaClass.simpleName, e)
            }
        }
    }

    fun start() = start(initialDelay, watchInterval) { run() }

    abstract fun run()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            logger.info("Closing scheduled monitor | $this")
            val name = this.javaClass.simpleName
            stopExecution(name, executor, scheduledFuture, true)
        }
    }

    companion object {
        private fun createDefaultExecutor(): ScheduledExecutorService {
            val factory = ThreadFactoryBuilder().setNameFormat("em-%d")
//                .setDaemon(true)
                .build()
            val service = Executors.newSingleThreadScheduledExecutor(factory)
//            MoreExecutors.addDelayedShutdownHook(service, Duration.ofSeconds(1))
            return service
        }
    }
}
