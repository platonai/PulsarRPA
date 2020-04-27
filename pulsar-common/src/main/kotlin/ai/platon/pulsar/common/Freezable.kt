package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implement the two-channel concurrency pattern, there are two channels: freezer channel and task channel
 * 1. both channel allows multiple threads
 * 2. once a channel is open, the other one must be closed
 * */
abstract class Freezable {
    private val lock = ReentrantLock() // lock for containers
    private val freezerChannelIsClosed = lock.newCondition()
    private val workerChannelIsClosed = lock.newCondition()
    private val numFreezers = AtomicInteger()
    private val numTasks = AtomicInteger()
    private var pollingTimeout = Duration.ofMillis(100)

    val isFrozen get() = numFreezers.get() > 0
    val isUnfrozen get() = !isFrozen

    fun <T> freeze(freezer: () -> T): T {
        // wait until the task channel is closed, meanwhile, the freezer channel is open
        waitUntilWorkerChannelIsClosed()

        try {
            return freezer()
        } finally {
            lock.withLock {
                if (numFreezers.decrementAndGet() == 0) {
                    // all tasks are allowed to pass
                    freezerChannelIsClosed.signalAll()
                }
            }
        }
    }

    fun <T> whenUnfrozen(task: () -> T): T {
        // wait until the freezer channel is closed, meanwhile, the task channel is open
        waitUntilFreezerChannelIsClosed()

        try {
            return task()
        } finally {
            lock.withLock {
                if (numTasks.decrementAndGet() == 0) {
                    // all freezers are allowed to pass
                    workerChannelIsClosed.signalAll()
                }
            }
        }
    }

    private fun waitUntilWorkerChannelIsClosed() {
        lock.withLock {
            var nanos = pollingTimeout.toNanos()
            while (numTasks.get() > 0 && nanos > 0) {
                nanos = workerChannelIsClosed.awaitNanos(nanos)
            }
            // freezer channel is open now
            numFreezers.incrementAndGet()
        }
    }

    private fun waitUntilFreezerChannelIsClosed() {
        lock.withLock {
            var nanos = pollingTimeout.toNanos()
            while (numFreezers.get() > 0 && nanos > 0) {
                nanos = freezerChannelIsClosed.awaitNanos(nanos)
            }
            // task channel is open now
            numTasks.incrementAndGet()
        }
    }
}
