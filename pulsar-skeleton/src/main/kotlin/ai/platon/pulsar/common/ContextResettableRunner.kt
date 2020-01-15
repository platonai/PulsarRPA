package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.net.browser.BrowseResult
import ai.platon.pulsar.net.browser.BrowserContext
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ContextResettable {
    var reset: Boolean
    fun run(nRedo: Int)
}

/**
 * Test the condition with throw program model, this model is used by [BrowserContext]
 * */
class ContextResettableRunner(val immutableConfig: ImmutableConfig, val contextRestorer: () -> Unit = {}) {
    private val log = LoggerFactory.getLogger(ContextResettableRunner::class.java)!!
    private val pollingTimeout = Duration.ofMillis(100)
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)

    var debug = 0

    /**
     * Total tasks run
     * */
    val nRun = AtomicInteger()
    val nPending = AtomicInteger()
    val nRunning = AtomicInteger()
    val nSignals = AtomicInteger()
    val nWaits = AtomicInteger()
    val sponsorThreadId = AtomicLong()

    private val lock = ReentrantLock()
    private val notMaintain = lock.newCondition()
    private val notBusy = lock.newCondition()

    private var nLogLine = 0

    fun run(action: ContextResettable) {
        var i = 0
        var redo = false
        do {
            nRun.incrementAndGet()

            // wait for the browser context to be ready
            guard()

            nRunning.incrementAndGet()
            try {
                action.run(i)
            } catch (t: Throwable) {
                throw t
            } finally {
                nRunning.decrementAndGet()
                if (nRunning.get() == 0) {
                    lock.withLock {
                        notBusy.signal()
                    }
                }
            }

            if (action.reset) {
                reset()
                redo = true
            }
        } while (redo && ++i < fetchMaxRetry)
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

                log.info(formatMessage("resetting"))

                sponsoredTid = tid
                contextRestorer()

                pr("notMaint-1", 1, "")
                notMaintain.signalAll()
                nSignals.incrementAndGet()
                pr("notMaint-2", 1, "")
            }
        }

        return sponsoredTid
    }

    private fun pr(ident: String, round: Int, msg: String) {
        if (debug < 1) {
            return
        }

        ++nLogLine
        val tid = Thread.currentThread().id
        val s = String.format("%-10s %-4d %10s %15s - running: %-4d pending: %-4d wait:%-4d run:%-4d signal:%-4d",
                "$nLogLine.", round, "[thr $tid/$sponsorThreadId]", ident,
                nRunning.get(), nPending.get(), nWaits.get(), nRun.get(), nSignals.get()
        )

        if (msg.isNotBlank()) println("$s - $msg") else println(s)
    }

    private fun formatMessage(msg: String): String {
        val tid = Thread.currentThread().id
        val s = String.format("%s | running: %-4d pending: %-4d wait:%-4d run:%-4d signal:%-4d",
                "[thr $tid/$sponsorThreadId]",
                nRunning.get(), nPending.get(), nWaits.get(), nRun.get(), nSignals.get()
        )

        return if (msg.isNotBlank()) "$s | $msg" else s
    }
}
