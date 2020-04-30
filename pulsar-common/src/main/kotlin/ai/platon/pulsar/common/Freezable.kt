package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The freezer-worker concurrency pattern, there are two channels: freezer channel and worker channel
 * 1. both channel allows multiple threads
 * 2. new workers have to wait until there is no ready freeze nor running freezer
 * 3. a freezer locks the working channel immediately, but have to wait to run util all workers are finished
 *
 *                  |------ waiting ------------|- ready -|-------------- critical  -------------------|---finished----
 *
 *                                          The entrance gate                                The exit gate
 * Freezer channel: ------------#1--#1--#1------|----#2---|-----------------------------------#3-------|--- #4 --------
 *                                              |         |                                            |
 * Worker channel:  ----*1----*1--*1------*1----|---------|-----------*2--*2------*2--*2---------------|--- *3 --------
 *
 * #1 The waiting freezers
 * #2 The ready freezers
 * #3 The running freezers
 * #4 The finished freezers
 *
 * *1 The waiting workers
 * *2 The running workers
 * *3 The finished workers
 * */
abstract class Freezable(val name: String = "") {
    private val lock = ReentrantLock()
    private val noFreezers = lock.newCondition()
    private val noWorkers = lock.newCondition()
    protected val numFreezers = AtomicInteger()
    protected val numRunningFreezers = AtomicInteger()
    protected val numWorkers = AtomicInteger()
    private var pollingTimeout = Duration.ofMillis(100)

    val isFrozen get() = numFreezers.get() > 0
    val isUnfrozen get() = !isFrozen

    /**
     * The freezer is preemptive, if there is at least one freezer task attempt enter the critical section
     * without obtaining a lock, all task attempt must wait
     * */
    fun <T> freeze(freezer: () -> T): T {
        // All workers must NOT pass now
        numFreezers.incrementAndGet()

        trace("We are now frozen with $numFreezers freezers")

        // Wait until all the workers are finished
        waitUntilWorkerChannelIsEmpty()

        try {
            trace("Perform freeze action ...")
            numRunningFreezers.incrementAndGet()
            return freezer()
        } finally {
            numRunningFreezers.decrementAndGet()
            lock.withLock {
                if (numFreezers.decrementAndGet() == 0) {
                    // all tasks are allowed to pass
                    noFreezers.signalAll()
                }
            }
        }
    }

    fun <T> whenUnfrozen(task: () -> T): T {
        // wait all the freezers are finished
        waitUntilFreezerChannelIsClosed()

        try {
            return task()
        } finally {
            lock.withLock {
                if (numWorkers.decrementAndGet() == 0) {
                    // all freezers are allowed to pass
                    noWorkers.signalAll()
                }
            }
        }
    }

    suspend fun <T> whenUnfrozenDeferred(task: suspend () -> T): T {
        // wait until the freezer channel is closed, meanwhile, the task channel is open
        waitUntilFreezerChannelIsClosed()

        try {
            return task()
        } finally {
            lock.withLock {
                if (numWorkers.decrementAndGet() == 0) {
                    // all freezers are allowed to pass
                    noWorkers.signalAll()
                }
            }
        }
    }

    private fun waitUntilWorkerChannelIsEmpty() {
        lock.withLock {
            var nanos = pollingTimeout.toNanos()
            while (numWorkers.get() > 0 && nanos > 0) {
                nanos = noWorkers.awaitNanos(nanos)
            }
        }
    }

    private fun waitUntilFreezerChannelIsClosed() {
        lock.withLock {
            if (numFreezers.get() > 0) {
                trace("There are $numWorkers workers waiting for $numFreezers freezers finish")
            }

            var nanos = pollingTimeout.toNanos()
            while (numFreezers.get() > 0 && nanos > 0) {
                nanos = noFreezers.awaitNanos(nanos)
            }

            // task channel is open now
            numWorkers.incrementAndGet()
        }
    }

    private fun trace(message: String) {
        if (name == "PrivacyManager") {
            val tid = Thread.currentThread().id
            println("[$name] [$tid] $message | " + formatStatus())
        }
    }

    private fun formatStatus(): String {
        return "freezers: $numFreezers runningFreezers: $numRunningFreezers workers: $numWorkers"
    }
}
