package ai.platon.pulsar.common

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
    protected val normalTaskTimeout = Duration.ofMinutes(10)
    protected val preemptiveTaskTimeout = Duration.ofMinutes(20)
    private var pollingTimeout = Duration.ofMillis(100)

    val isPreempted get() = numPreemptiveTasks.get() > 0
    val isNormal get() = !isPreempted

    /**
     * The freezer is preemptive, if there is at least one freezer task attempt enter the critical section
     * without obtaining a lock, all task attempt must wait
     * */
    fun <T> preempt(preemptiveTask: () -> T): T {
        return beforePreempt().runCatching { preemptiveTask() }
                .onFailure { afterPreempt() }.onSuccess { afterPreempt() }.getOrThrow()
    }

    fun <T> whenNormal(task: () -> T): T {
        return beforeTask().runCatching { task() }.onFailure { afterTask() }.onSuccess { afterTask() }.getOrThrow()
    }

    suspend fun <T> whenNormalDeferred(task: suspend () -> T): T {
        return beforeTask().runCatching { withTimeout(normalTaskTimeout.toMillis()) { task() } }
                .onFailure { afterTask() }.onSuccess { afterTask() }.getOrThrow()
    }

    private fun beforePreempt() {
        // All workers must NOT pass now
        numPreemptiveTasks.incrementAndGet()

        trace("Preempted with $numPreemptiveTasks preemptive tasks ")

        // Wait until all the normal tasks are finished
        waitUntilNoRunningNormalTasks()

        trace("Performing preemptive action ...")
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
            var nanos = pollingTimeout.toNanos()
            while (numRunningNormalTasks.get() > 0 && nanos > 0) {
                nanos = noRunningNormalTasks.awaitNanos(nanos)
            }

            // must be guarded by lock
            numRunningPreemptiveTasks.incrementAndGet()
        }
    }

    private fun waitUntilNoPreemptiveTask() {
        lock.withLock {
            if (numPreemptiveTasks.get() > 0) {
                trace("There are $numRunningNormalTasks tasks waiting for $numPreemptiveTasks preemptive tasks to finish")
            }

            var nanos = pollingTimeout.toNanos()
            while (numPreemptiveTasks.get() > 0 && nanos > 0) {
                nanos = noPreemptiveTasks.awaitNanos(nanos)
            }

            // must be guarded by lock
            // task channel is open now
            numPendingNormalTasks.decrementAndGet()
            numRunningNormalTasks.incrementAndGet()
        }
    }

    private fun trace(message: String) {
        if (name == "PrivacyManager") {
            val tid = Thread.currentThread().id
            println("[$name] [$tid] $message | " + formatPreemptChannelStatus())
        }
    }
}
