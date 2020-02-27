package ai.platon.pulsar.common

import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

/**
 * We want to implement a critical-area-1/critical-area-2 functionality where only one thread is allowed
 * in critical-area-1 while multiple threads are allowed in critical-area-2,
 * critical-area-1 are critical-area-2 are exclusive
 * */
abstract class Freezable {
    private val lock = ReentrantLock() // lock for containers
    private val notFrozen = lock.newCondition()
    private val notBusy = lock.newCondition()
    private var numFreezers = 0
    private var numTasks = 0
    private var nanos = Duration.ofMillis(500).toNanos()
    // for debug purpose only
    var enabled = true

    fun <T> freeze(action: () -> T): T {
        if (!enabled) {
            return action()
        }

        lock.withLock {
            while (numTasks > 0 && nanos > 0) {
                nanos = notBusy.awaitNanos(nanos)
            }
            ++numFreezers
        }

        try {
            return action()
        } finally {
            lock.withLock {
                if (--numFreezers == 0) {
                    notFrozen.signalAll()
                }
            }
        }
    }

    fun <T> whenUnfrozen(action: () -> T): T {
        if (!enabled) {
            return action()
        }

        // wait until not frozen
        lock.withLock {
            while (numFreezers > 0 && nanos > 0) {
                nanos = notFrozen.awaitNanos(nanos)
            }
            ++numTasks
        }

        try {
            return action()
        } finally {
            lock.withLock {
                if (--numTasks == 0) {
                    // only one
                    notBusy.signal()
                }
            }
        }
    }
}
