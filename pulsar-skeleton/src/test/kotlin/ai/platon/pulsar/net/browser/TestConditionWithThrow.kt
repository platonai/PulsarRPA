package ai.platon.pulsar.net.browser

import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random
import kotlin.test.assertEquals

/**
 * Test the condition with throw program model, this model is used by [BrowserContext]
 * */
class TestConditionWithThrow {
    private var logLine = 0
    private var nSignals = 0
    private var nWaits = 0
    private var nWaitsTimeout = 0
    private val nPending = AtomicInteger()
    private val sponsorThreadId = AtomicLong()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    @Test
    fun test() {
        val executor = Executors.newFixedThreadPool(40)
        val tasks = IntRange(1, 100).map { Callable { run() } }
        executor.invokeAll(tasks)

        assertEquals(0, nPending.get())
        assertEquals(0, sponsorThreadId.get())
    }

    @Ignore("This is a long time test, disable by default")
    @Test
    fun testNTimes() {
        var i = 0
        while (i++ < 100) {
            println("========================================================")
            println("Test round $i")
            val nThreads = 20 + i
            val executor = Executors.newFixedThreadPool(nThreads)
            val tasks = IntRange(1, 100).map { Callable { run() } }
            executor.invokeAll(tasks)

            assertEquals(0, nPending.get())
            assertEquals(0, sponsorThreadId.get())
        }
    }

    private fun run() {
        val tid = Thread.currentThread().id
        var i = 0
        var message = ""
        while (i++ < 3) {
            // wait for the browser context to be ready
            nPending.incrementAndGet()
            lock.withLock {
                pr("lock", i, message)
                if (!sponsorThreadId.compareAndSet(tid, 0)) {
                    pr("await", i, message)
                    // TODO: sometimes no one issue the signal
                    val b = condition.await(10, TimeUnit.SECONDS)
                    ++nWaits
                    if (!b) {
                        ++nWaitsTimeout
                        pr("timeout", i, message)
                    }
                }
            }
            nPending.decrementAndGet()

            try {
                doSomethingWithException(i)
            } catch (e: Exception) {
                message = e.message?:""

                lock.withLock {
                    if (sponsorThreadId.compareAndSet(0, tid)) {
                        // do something
                        pr("sponsor", i, message)
                        // Thread.sleep(1000)
                        condition.signalAll()
                        ++nSignals
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun doSomethingWithException(round: Int) {
        if (round >= 3) {
            pr("worker", round, "")
            return
        }

        val rand = Random(System.currentTimeMillis()).nextInt(0, 100_000)
        pr("worker", round,"$rand")

        if (rand % 3 == 0) {
            throw Exception("$rand")
        }
    }

    private fun pr(ident: String, round: Int, msg: String) {
        ++logLine
        val tid = Thread.currentThread().id
        val s = String.format("$logLine.\t %15s %10s [$round]" +
                " - pending: $nPending signals: $nSignals waits: $nWaits waitsTimeout: $nWaitsTimeout" +
                " - $msg",
                "[thr#$tid-$sponsorThreadId]", ident)
        println(s)
    }
}
