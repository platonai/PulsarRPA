package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.MetricsCounters
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarCounter {
    companion object {
        init {
            MetricsCounters.register(Counter1::class.java)
            MetricsCounters.register(Counter2::class.java)
            MetricsCounters.register(Counter3::class.java)
            // It's OK to re-register
            MetricsCounters.register(Counter3::class.java)
            MetricsCounters.register(Counter3::class.java)
        }
    }

    @Test
    fun testPulsarCounter() {
        val counter = MetricsCounters()
        val group1 = MetricsCounters.getGroup(Counter1::class.java)
        val group2 = MetricsCounters.getGroup(Counter2::class.java)
        val group3 = MetricsCounters.getGroup(Counter3::class.java)
        val group4 = MetricsCounters.getGroup(Counter3::class.java)
        Assert.assertEquals(1, group1.toLong())
        Assert.assertEquals(2, group2.toLong())
        Assert.assertEquals(3, group3.toLong())
        Assert.assertEquals(3, group4.toLong())
        Assert.assertEquals(3, MetricsCounters.getGroup(Counter3::class.java).toLong())
        Assert.assertEquals(3, MetricsCounters.getGroup(Counter3.stFetched).toLong())
        counter.increase(Counter1.rDepth0)
        counter.increase(Counter2.inlinks)
        counter.increase(Counter2.rDepthN)
        counter.increase(Counter2.rDepth3)
        Assert.assertEquals(1, counter[Counter1.rDepth0].toLong())
        Assert.assertEquals(1, counter[Counter2.rDepth3].toLong())
        counter.increase(Counter2.rDepth1)
        counter.increase(Counter2.rDepth1)
        counter.increase(Counter2.rDepth1)
        counter.increase(Counter2.rDepth1)
        counter.increase(Counter2.rDepth1)
        counter.increase(Counter2.rDepth1)
        Assert.assertEquals(6, counter[Counter2.rDepth1].toLong())
        counter.setValue(Counter2.rDepth1, Int.MAX_VALUE)
        Assert.assertEquals(Int.MAX_VALUE.toLong(), counter[Counter2.rDepth1].toLong())
        counter.increase(group3, Counter3.liveLinks)
        Assert.assertEquals(1, counter[Counter3.liveLinks].toLong())
        counter.increase(Counter3.stFetched)
        counter.increase(Counter3.stGone)
        Assert.assertEquals(1, counter[Counter3.stGone].toLong())
        Assert.assertEquals("1'rDepth0:1, 2'rDepth1:2147483647, 2'rDepth3:1, 2'rDepthN:1, 2'inlinks:1, 3'liveLinks:1, 3'stFetched:1, 3'stGone:1", counter.getStatus(true))
        Assert.assertEquals("rDepth0:1, rDepth1:2147483647, rDepth3:1, rDepthN:1, inlinks:1, liveLinks:1, stFetched:1, stGone:1", counter.getStatus(false))
    }

    fun operationOnCounter(counter: MetricsCounters, group3: Int) {
        counter.increase(Counter1.rDepth1)
        counter.increase(Counter3.rDepth1)
        counter.increase(Counter2.rDepth2)
        counter.increase(Counter2.rDepth3)
        counter[Counter1.rDepth1]
        counter[Counter3.stFetched]
        counter.increase(Counter2.rDepth2, -1)
        counter.increase(Counter2.rDepth3, -1)
        counter.setValue(group3, Counter3.rDepth1, 0)
    }

    @Test
    @Ignore
    fun testPulsarCounterMultipleThreaded() {
        val counter = MetricsCounters()
        val group3 = MetricsCounters.getGroup(Counter3::class.java)
        val maxThreads = 2000
        var countDown = maxThreads
        val taskExecutor = Executors.newFixedThreadPool(maxThreads)
        while (countDown-- > 0) {
            taskExecutor.execute { operationOnCounter(counter, group3) }
        }
        taskExecutor.shutdown()
        try {
            taskExecutor.awaitTermination(2, TimeUnit.MINUTES)
        } catch (ignored: InterruptedException) {
        }
        Assert.assertEquals(0, counter[Counter2.rDepth2].toLong())
        Assert.assertEquals(0, counter[Counter2.rDepth3].toLong())
        Assert.assertEquals(maxThreads.toLong(), counter[Counter1.rDepth1].toLong())
        Assert.assertEquals(0, counter[Counter3.rDepth1].toLong())
    }

    private enum class Counter1 {
        rDepth0, rDepth1, rDepth2, rDepth3, rDepthN
    }

    private enum class Counter2 {
        rDepth0, rDepth1, rDepth2, rDepth3, rDepthN, inlinks, liveLinks
    }

    private enum class Counter3 {
        rDepth0, rDepth1, rDepth2, rDepth3, rDepthN, inlinks, liveLinks, stFetched, stRedirTemp, stRedirPerm, stNotModified, stRetry, stUnfetched, stGone
    }
}