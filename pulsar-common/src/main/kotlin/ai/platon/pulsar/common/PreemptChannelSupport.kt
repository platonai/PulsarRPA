package ai.platon.pulsar.common

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The preemptive channel concurrency pattern, there are two channels: preemptive channel and normal channel
 *
 * TODO: compare with ReadWriteLock
 *
 * 1. both channel allows multiple threads
 * 2. new workers have to wait until there is no ready freeze nor running freezer
 * 3. a freezer locks the working channel immediately, but have to wait to run util all workers are finished
 *
 *                     |------ waiting ------------|- ready -|-------------- critical  -------------------|---finished----
 *
 *                                              The entrance gate                                    The exit gate
 * Preemptive channel: ------------#1--#1--#1------|----#2---|-----------------------------------#3-------|--- #4 --------
 *                                                 |         |                                            |
 * Normal channel:     ----*1----*1--*1------*1----|---------|-----------*2--*2------*2--*2---------------|--- *3 --------
 *
 * #1 The waiting preemptive tasks
 * #2 The ready preemptive tasks
 * #3 The running preemptive tasks
 * #4 The finished preemptive tasks
 *
 * *1 The waiting workers
 * *2 The running workers
 * *3 The finished workers
 * */
abstract class PreemptChannelSupport(val name: String = "") {
    private val lock = ReentrantLock()
    private val noPreemptiveTasks = lock.newCondition()
    private val noRunningNormalTasks = lock.newCondition()
    protected val numPreemptiveTasks = AtomicInteger()
    protected val numRunningPreemptiveTasks = AtomicInteger()
    protected val numPendingNormalTasks = AtomicInteger()
    protected val numRunningNormalTasks = AtomicInteger()
    protected val normalTaskTimeout = Duration.ofMinutes(6)
    protected val preemptiveTaskTimeout = Duration.ofMinutes(20)
    private var pollingInterval = Duration.ofMillis(100)

    val isPreempted get() = numPreemptiveTasks.get() > 0
    val isNormal get() = !isPreempted

    /**
     * The freezer is preemptive, if there is at least one freezer task attempt enter the critical section
     * without obtaining a lock, all task attempt must wait
     * */
    fun <T> preempt(preemptiveTask: () -> T) = beforePreempt().runCatching { preemptiveTask() }
            .also { afterPreempt() }.getOrThrow()

    fun <T> whenNormal(task: () -> T) = beforeTask().runCatching { task() }.also { afterTask() }.getOrThrow()

    @Throws(TimeoutCancellationException::class)
    suspend fun <T> whenNormalDeferred(task: suspend () -> T) =
            beforeTask().runCatching { withTimeout(normalTaskTimeout.toMillis()) { task() } }
                    .also { afterTask() }.getOrThrow()

    fun releaseLocks() {
        if (numRunningNormalTasks.get() == 0) {
            numPreemptiveTasks.set(0)
        }

        numRunningNormalTasks.set(0)
    }

    private fun beforePreempt() {
        // All workers must NOT pass now
        numPreemptiveTasks.incrementAndGet()
        // Wait until all the normal tasks are finished
        waitUntilNoRunningNormalTasks()
    }

    private fun afterPreempt() {
        numRunningPreemptiveTasks.decrementAndGet()
        lock.withLock {
            if (numPreemptiveTasks.decrementAndGet() == 0) {
                // all tasks are allowed to pass
                noPreemptiveTasks.signalAll()
            }
        }
    }

    private fun beforeTask() {
        numPendingNormalTasks.incrementAndGet()
        // wait all the preemptive tasks  are finished
        waitUntilNoPreemptiveTask()
    }

    private fun afterTask() {
        lock.withLock {
            if (numRunningNormalTasks.decrementAndGet() == 0) {
                // all preemptive tasks  are allowed to pass
                noRunningNormalTasks.signalAll()
            }
        }
    }

    fun formatPreemptChannelStatus(): String {
        return "preemptive tasks : $numPreemptiveTasks, " +
                " running preemptive tasks: $numRunningPreemptiveTasks," +
                " pending normal tasks: $numPendingNormalTasks" +
                " running normal tasks: $numRunningNormalTasks"
    }

    private fun waitUntilNoRunningNormalTasks() {
        lock.withLock {
            // TODO: no timeout await() might be just OK
            val nanos = pollingInterval.toNanos()
            while (numRunningNormalTasks.get() > 0) {
                noRunningNormalTasks.awaitNanos(nanos)
            }

            // must be guarded by lock
            numRunningPreemptiveTasks.incrementAndGet()
        }
    }

    private fun waitUntilNoPreemptiveTask() {
        lock.withLock {
            val nanos = pollingInterval.toNanos()
            while (numPreemptiveTasks.get() > 0) {
                noPreemptiveTasks.awaitNanos(nanos)
            }

            // must be guarded by lock
            // task channel is open now
            numPendingNormalTasks.decrementAndGet()
            numRunningNormalTasks.incrementAndGet()
        }
    }
}
