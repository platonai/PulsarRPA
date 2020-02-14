package ai.platon.pulsar.net.browser

import ai.platon.pulsar.PulsarContext
import ai.platon.pulsar.PulsarEnv
import ai.platon.pulsar.common.ContextResettableRunner
import ai.platon.pulsar.common.ContextResettable
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals

private class DummyRunnable(val round: Int) : ContextResettable {
    override var reset : Boolean = false

    override fun run(nRedo: Int) {
        val rand = Random(System.currentTimeMillis()).nextInt(0, 100_000)
        TimeUnit.MILLISECONDS.sleep(1)
        reset = nRedo == 0 && rand % 3 == 0
    }
}

/**
 * Test the context reset guard model
 * */
class TestContextResettableRunner {

    val context = PulsarContext.getOrCreate()

    @Test
    fun test() {
        val executor = Executors.newFixedThreadPool(40)
        val guard = ContextResettableRunner(context.unmodifiedConfig)
        guard.debug = 1
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
