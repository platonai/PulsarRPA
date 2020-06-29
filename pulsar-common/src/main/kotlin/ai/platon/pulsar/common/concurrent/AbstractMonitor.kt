package ai.platon.pulsar.common.concurrent

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractMonitor(
        var initialDelay: Duration = Duration.ofMinutes(5),
        var watchInterval: Duration = Duration.ofSeconds(30),
        val executor: ScheduledExecutorService = createDefaultExecutor(),
        val autoClose: Boolean = true
): AutoCloseable {
    private val log = LoggerFactory.getLogger(AbstractMonitor::class.java)

    protected var scheduledFuture: ScheduledFuture<*>? = null

    protected val closed = AtomicBoolean()
    val isActive get() = !closed.get()

    /**
     * Starts the monitor at the given period with the specific runnable action
     * Visible only for testing
     */
    @Synchronized
    fun start(initialDelay: Duration, period: Duration, runnable: () -> Unit) {
        require(scheduledFuture == null) { "Monitor is already started" }
        scheduledFuture = executor.scheduleAtFixedRate(runnable, initialDelay.seconds, period.seconds, TimeUnit.SECONDS)
    }

    fun start() = start(initialDelay, watchInterval) { watch() }

    abstract fun watch()

    override fun close() {
        try {
            if (autoClose) {
                stopExecution(executor, scheduledFuture, true)
            }
        } catch (e: Exception) {
            log.warn("Unexpected exception", e)
        }
    }

    companion object {
        private fun createDefaultExecutor(): ScheduledExecutorService {
            val factory = ThreadFactoryBuilder().setNameFormat("em-%d").build()
            return Executors.newSingleThreadScheduledExecutor(factory)
        }
    }
}
