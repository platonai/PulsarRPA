package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.ContextResetGuard
import ai.platon.pulsar.common.ContextResettable
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals

private class DummyRunnable(val round: Int) : ContextResettable {
    private var _needReset = false

    override val needReset get() = _needReset

    override fun run(nRetry: Int) {
        val rand = Random(System.currentTimeMillis()).nextInt(0, 100_000)
        TimeUnit.MILLISECONDS.sleep(1)
        _needReset = nRetry == 0 && rand % 3 == 0
    }
}

/**
 * Test the condition with throw program model, this model is used by [BrowserContext]
 * */
class TestContextResetGuard {

    @Test
    fun test() {
        val executor = Executors.newFixedThreadPool(40)
        val guard = ContextResetGuard()
        val tasks = IntRange(1, 100).map { round -> Callable { guard.run(DummyRunnable(round)) } }
        executor.invokeAll(tasks)

        assertEquals(0, guard.nPending.get())
        assertEquals(0, guard.nRunning.get())
        assertEquals(0, guard.nWaits.get())
        assertEquals(0, guard.sponsorThreadId.get())
    }

    // @Ignore("This is a long time test, disabled by default")
    @Test
    fun testNTimes() {
        var round = 0
        while (++round < 100) {
            println("========================================================")
            println("Test round $round")
            test()
        }
    }
}
