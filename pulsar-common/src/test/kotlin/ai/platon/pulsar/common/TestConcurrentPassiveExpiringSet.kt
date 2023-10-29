package ai.platon.pulsar.common

import ai.platon.pulsar.common.concurrent.ConcurrentPassiveExpiringSet
import com.google.common.collect.Iterables
import com.google.common.math.IntMath
import java.time.Duration
import kotlin.test.*

class TestConcurrentPassiveExpiringSet {

    @Test
    fun testPassiveExpiring() {
        val set = ConcurrentPassiveExpiringSet<Int>(Duration.ofSeconds(10))
        IntRange(1, 100).toCollection(set)
        sleepSeconds(11)
        assert(set.isEmpty())
    }

    @Test
    fun testIterator() {
        val set = ConcurrentPassiveExpiringSet<Int>(Duration.ofMinutes(10))
        assertFalse(1 in set)
        
        IntRange(1, 100).toCollection(set)
        set.map { IntMath.pow(it, 2) }.toCollection(set)
        assertTrue { set.isNotEmpty() }
        assertTrue(121 in set)
        assertTrue(64 in set)
        assertTrue(101 !in set)

        val sorted = set.sorted()
        assertTrue { sorted.isNotEmpty() }
        assertEquals(10000, sorted.last())
    }

    @Test
    fun testCycleIterator() {
        val set = mutableSetOf<Int>()
        IntRange(1, 5).toCollection(set)

        val iterator = Iterables.cycle(set).iterator()
        var value = iterator.next()
        assertEquals(1, value)
        var n = set.size
        while (n-- > 0) {
            value = iterator.next()
        }
        assertEquals(1, value)

        value = iterator.next()
        assertEquals(2, value)
        n = set.size
        while (n-- > 0) {
            value = iterator.next()
        }
        assertEquals(2, value)
    }
}
