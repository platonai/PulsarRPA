package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The preemptive channel concurrency pattern, there are two channels: preemptive channel and normal channel
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
    private val noTasks = lock.newCondition()
    protected val numReadyPreemptiveTasks = AtomicInteger()
    protected val numRunningPreemptiveTasks = AtomicInteger()
    protected val numTasks = AtomicInteger()
    private var pollingTimeout = Duration.ofMillis(100)

    val isPreempted get() = numReadyPreemptiveTasks.get() > 0
    val isNormal get() = !isPreempted

    /**
     * The freezer is preemptive, if there is at least one freezer task attempt enter the critical section
     * without obtaining a lock, all task attempt must wait
     * */
    fun <T> preempt(preemptiveTask: () -> T): T {
        // All workers must NOT pass now
        numReadyPreemptiveTasks.incrementAndGet()

        trace("Preempted with $numReadyPreemptiveTasks preemptive tasks ")

        // Wait until all the normal tasks are finished
        waitUntilNoNormalTasks()

        try {
            trace("Perform preemptive action ...")
            numRunningPreemptiveTasks.incrementAndGet()
            return preemptiveTask()
        } finally {
            numRunningPreemptiveTasks.decrementAndGet()
            lock.withLock {
                if (numReadyPreemptiveTasks.decrementAndGet() == 0) {
                    // all tasks are allowed to pass
                    noPreemptiveTasks.signalAll()
                }
            }
        }
    }

    fun <T> whenNormal(task: () -> T): T {
        // wait all the preemptive tasks  are finished
        waitUntilNotPreempted()

        try {
            return task()
        } finally {
            lock.withLock {
                if (numTasks.decrementAndGet() == 0) {
                    // all preemptive tasks  are allowed to pass
                    noTasks.signalAll()
                }
            }
        }
    }

    suspend fun <T> whenNormalDeferred(task: suspend () -> T): T {
        // wait until the freezer channel is closed, meanwhile, the task channel is open
        waitUntilNotPreempted()

        try {
            return task()
        } finally {
            lock.withLock {
                if (numTasks.decrementAndGet() == 0) {
                    // all preemptive tasks  are allowed to pass
                    noTasks.signalAll()
                }
            }
        }
    }

    private fun waitUntilNoNormalTasks() {
        lock.withLock {
            var nanos = pollingTimeout.toNanos()
            while (numTasks.get() > 0 && nanos > 0) {
                nanos = noTasks.awaitNanos(nanos)
            }
        }
    }

    private fun waitUntilNotPreempted() {
        lock.withLock {
            if (numReadyPreemptiveTasks.get() > 0) {
                trace("There are $numTasks tasks waiting for $numReadyPreemptiveTasks preemptive tasks to finish")
            }

            var nanos = pollingTimeout.toNanos()
            while (numReadyPreemptiveTasks.get() > 0 && nanos > 0) {
                nanos = noPreemptiveTasks.awaitNanos(nanos)
            }

            // task channel is open now
            numTasks.incrementAndGet()
        }
    }

    private fun trace(message: String) {
        if (name == "PrivacyManager") {
            val tid = Thread.currentThread().id
            println("[$name] [$tid] $message | " + formatStatus())
        }
    }

    private fun formatStatus(): String {
        return "preemptive tasks : $numReadyPreemptiveTasks, " +
                " running preemptive tasks: $numRunningPreemptiveTasks," +
                " normal tasks: $numTasks"
    }
}
