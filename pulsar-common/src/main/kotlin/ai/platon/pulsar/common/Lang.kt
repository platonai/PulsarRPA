package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

/**
 * Implement a freezer/task functionality
 * 1. freezer and task can not run at the same time
 * 2. freezers can run at the same time
 * 3. task can run at the same time
 * */
abstract class Freezable {
    private val lock = ReentrantLock() // lock for containers
    private val notFrozen = lock.newCondition()
    private val notBusy = lock.newCondition()
    private var pollingNanos = Duration.ofMillis(500).toNanos()
    val numFreezers = AtomicInteger()
    val numTasks = AtomicInteger()
    // for debug purpose
    var enabled = true
    val disabled get() = !enabled

    fun <T> freeze(action: () -> T): T {
        if (disabled) {
            return action()
        }

        lock.withLock {
            while (numTasks.get() > 0 && pollingNanos > 0) {
                pollingNanos = notBusy.awaitNanos(pollingNanos)
            }
            numFreezers.incrementAndGet()
        }

        try {
            return action()
        } finally {
            lock.withLock {
                if (numFreezers.decrementAndGet() == 0) {
                    notFrozen.signalAll()
                }
            }
        }
    }

    fun <T> whenUnfrozen(action: () -> T): T {
        if (disabled) {
            return action()
        }

        // wait until not frozen
        lock.withLock {
            while (numFreezers.get() > 0 && pollingNanos > 0) {
                pollingNanos = notFrozen.awaitNanos(pollingNanos)
            }
            numTasks.incrementAndGet()
        }

        try {
            return action()
        } finally {
            lock.withLock {
                if (numTasks.decrementAndGet() == 0) {
                    // only one
                    notBusy.signal()
                }
            }
        }
    }
}
