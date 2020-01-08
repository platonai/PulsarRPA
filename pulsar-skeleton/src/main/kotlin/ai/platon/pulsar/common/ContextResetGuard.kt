package ai.platon.pulsar.common

import ai.platon.pulsar.net.browser.BrowserContext
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ContextResettable {
    val needReset: Boolean
    fun run(nRetry: Int)
}

/**
 * Test the condition with throw program model, this model is used by [BrowserContext]
 * */
class ContextResetGuard {
    val pollingTimeout = Duration.ofMillis(500)
    val maxRetry = 3
    val nSignals = AtomicInteger()
    val nWaits = AtomicInteger()
    val nPending = AtomicInteger()
    val nRunning = AtomicInteger()
    val sponsorThreadId = AtomicLong()

    private val lock = ReentrantLock()
    private val notMaintain = lock.newCondition()
    private val notBusy = lock.newCondition()
    private var nLogLine = 0

    fun run(action: ContextResettable) {
        var i = 0
        var redo = false
        do {
            // wait for the browser context to be ready
            guard()

            nRunning.incrementAndGet()
            action.run(i)
            nRunning.decrementAndGet()

            if (nRunning.get() == 0) {
                lock.withLock {
                    notBusy.signal()
                }
            }

            if (action.needReset) {
                reset()
                redo = true
            }
        } while (redo && ++i < maxRetry)
    }

    private fun guard() {
        val tid = Thread.currentThread().id
        var nanos = pollingTimeout.toNanos()

        nPending.incrementAndGet()
        lock.withLock {
            if (!sponsorThreadId.compareAndSet(tid, 0)) {
                pr("guard-1", 1, "")
                nWaits.incrementAndGet()
                while (sponsorThreadId.get() != 0L && nanos > 0) {
                    nanos = notMaintain.awaitNanos(nanos)
                }
                nWaits.decrementAndGet()
            }
        }
        nPending.decrementAndGet()

        pr("guard-2", 1, "")
    }

    private fun reset(): Long {
        val tid = Thread.currentThread().id
        var nanos = pollingTimeout.toNanos()
        var sponsoredTid = 0L

        lock.withLock {
            if (sponsorThreadId.get() == 0L) {
                sponsorThreadId.set(tid)

                while (nRunning.get() > 0) {
                    pr("notBusy-1", 1, "")
                    nanos = notBusy.awaitNanos(nanos)
                    pr("notBusy-2", 1, "")
                }

                sponsoredTid = tid

                pr("notMaint-1", 1, "")
                notMaintain.signalAll()
                nSignals.incrementAndGet()
                pr("notMaint-2", 1, "")
            }
        }

        return sponsoredTid
    }

    private fun pr(ident: String, round: Int, msg: String) {
        ++nLogLine
        val tid = Thread.currentThread().id
        val s = String.format("$nLogLine.\t %10s %15s %-4d" +
                " running: $nRunning pending: $nPending signals: $nSignals waits: $nWaits",
                "[thr $tid/$sponsorThreadId]", ident, round)
        if (msg.isNotBlank()) println("$s - $msg") else println(s)
    }
}
