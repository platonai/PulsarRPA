package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.MetricsCounters
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestMetricsCounters {
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
    fun testCounters() {
        val counter = MetricsCounters()
        val group1 = MetricsCounters.getGroup(Counter1::class.java)
        val group2 = MetricsCounters.getGroup(Counter2::class.java)
        val group3 = MetricsCounters.getGroup(Counter3::class.java)
        val group4 = MetricsCounters.getGroup(Counter3::class.java)
        assertEquals(1, group1.toLong())
        assertEquals(2, group2.toLong())
        assertEquals(3, group3.toLong())
        assertEquals(3, group4.toLong())
        assertEquals(3, MetricsCounters.getGroup(Counter3::class.java).toLong())
        assertEquals(3, MetricsCounters.getGroup(Counter3.stFetched).toLong())
        counter.inc(Counter1.rDepth0)
        counter.setValue(Counter1.rDepth1, 2000)
        counter.inc(Counter2.inlinks)
        counter.inc(Counter2.rDepthN)
        counter.inc(Counter2.rDepth3)
        assertEquals(1, counter[Counter1.rDepth0].toLong())
        assertEquals(1, counter[Counter2.rDepth3].toLong())
        counter.inc(Counter2.rDepth1)
        counter.inc(Counter2.rDepth1)
        counter.inc(Counter2.rDepth1)
        counter.inc(Counter2.rDepth1)
        counter.inc(Counter2.rDepth1)
        counter.inc(Counter2.rDepth1)
        assertEquals(6, counter[Counter2.rDepth1].toLong())
        counter.setValue(Counter2.rDepth1, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE.toLong(), counter[Counter2.rDepth1].toLong())
        counter.inc(group3, Counter3.liveLinks)
        assertEquals(1, counter[Counter3.liveLinks].toLong())
        counter.inc(Counter3.stFetched)
        counter.inc(Counter3.stGone)
        assertEquals(1, counter[Counter3.stGone].toLong())
        assertEquals("1'rDepth0:1, 1'rDepth1:2000, 2'rDepth1:2147483647, 2'rDepth3:1, 2'rDepthN:1, 2'inlinks:1, 3'liveLinks:1, 3'stFetched:1, 3'stGone:1", counter.getStatus(true))
        assertEquals("rDepth0:1, rDepth1:2000, rDepth1:2147483647, rDepth3:1, rDepthN:1, inlinks:1, liveLinks:1, stFetched:1, stGone:1", counter.getStatus(false))
    }

    fun operationOnCounter(counter: MetricsCounters, group3: Int) {
        counter.inc(Counter1.rDepth1)
        counter.inc(Counter3.rDepth1)
        counter.inc(Counter2.rDepth2)
        counter.inc(Counter2.rDepth3)
        counter[Counter1.rDepth1]
        counter[Counter3.stFetched]
        counter.inc(Counter2.rDepth2, -1)
        counter.inc(Counter2.rDepth3, -1)
        counter.setValue(group3, Counter3.rDepth1, 0)
    }

    @Test
    fun testCountersMultipleThreaded() {
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
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        assertEquals(0, counter[Counter2.rDepth2].toLong())
        assertEquals(0, counter[Counter2.rDepth3].toLong())
        assertEquals(maxThreads.toLong(), counter[Counter1.rDepth1].toLong())
        assertEquals(0, counter[Counter3.rDepth1].toLong())
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
