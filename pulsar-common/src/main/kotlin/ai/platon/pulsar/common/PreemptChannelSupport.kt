package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * The preemptive channel concurrency pattern, there are two channels: preemptive channel and normal channel
 *
 * TODO: compare with ReadWriteLock
 *
 * 1. both channel allows multiple threads
 * 2. new workers have to wait until there is no ready preemptive tasks nor running preemptive tasks
 * 3. a preemptive task locks the working channel immediately, but have to wait to run util all workers are finished
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
    private var pollingTimeout = Duration.ofMillis(100)

    protected val numPreemptiveTasks = AtomicInteger()
    protected val numRunningPreemptiveTasks = AtomicInteger()
    protected val numPendingNormalTasks = AtomicInteger()
    protected val numRunningNormalTasks = AtomicInteger()

    val isPreempted get() = numPreemptiveTasks.get() > 0
    val isNormal get() = !isPreempted
    val hasEvent get() = arrayOf(numRunningPreemptiveTasks,
            numRunningPreemptiveTasks, numPendingNormalTasks, numRunningNormalTasks).sumBy { it.get() } > 0

    /**
     * If there is at least one preemptive task in the critical section, all normal tasks must wait.
     * */
    @Throws(InterruptedException::class)
    fun <T> preempt(preemptiveTask: () -> T) {
        beforePreempt().runCatching { preemptiveTask() }.also { afterPreempt() }.getOrThrow()
    }
    
    @Throws(InterruptedException::class)
    fun <T> whenNormal(task: () -> T) {
        beforeTask().runCatching { task() }.also { afterTask() }.getOrThrow()
    }
    
    @Throws(InterruptedException::class)
    suspend fun <T> whenNormalDeferred(task: suspend () -> T) {
        beforeTask().runCatching { task() }.also { afterTask() }.getOrThrow()
    }

    fun releaseLocks() {
        if (numRunningNormalTasks.get() == 0) {
            System.err.println("Force release preemptive locks | ${formatPreemptChannelStatus()}")
            numPreemptiveTasks.set(0)
            numRunningPreemptiveTasks.set(0)
        }

        numRunningNormalTasks.set(0)
    }

    fun formatPreemptChannelStatus(): String {
        return "pTasks: $numPreemptiveTasks," +
                " rPTasks: $numRunningPreemptiveTasks," +
                " pNTasks: $numPendingNormalTasks," +
                " rNTasks: $numRunningNormalTasks"
    }
    
    @Throws(InterruptedException::class)
    private fun beforePreempt() {
        // All workers must NOT pass now
        numPreemptiveTasks.incrementAndGet()
        // Wait until all the normal tasks are finished
        waitUntilNoRunningNormalTasks()
    }
    
    @Throws(InterruptedException::class)
    private fun afterPreempt() {
        numRunningPreemptiveTasks.decrementAndGet()
        lock.withLock {
            if (numPreemptiveTasks.decrementAndGet() == 0) {
                // all tasks are allowed to pass
                noPreemptiveTasks.signalAll()
            }
        }
    }
    
    @Throws(InterruptedException::class)
    private fun beforeTask() {
        numPendingNormalTasks.incrementAndGet()
        // wait all the preemptive tasks  are finished
        waitUntilNoPreemptiveTask()
    }
    
    @Throws(InterruptedException::class)
    private fun afterTask() {
        lock.withLock {
            if (numRunningNormalTasks.decrementAndGet() == 0) {
                // all preemptive tasks  are allowed to pass
                noRunningNormalTasks.signalAll()
            }
        }
    }
    
    @Throws(InterruptedException::class)
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

    @Throws(InterruptedException::class)
    private fun waitUntilNoPreemptiveTask() {
        lock.withLock {
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
}
