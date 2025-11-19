package ai.platon.pulsar.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InProcessIdGeneratorTest {

    @Test
    fun `ids are strictly increasing in a single thread`() {
        val gen = InProcessIdGenerator(nodeId = 1)
        var prev = gen.nextId()
        repeat(10_000) {
            val cur = gen.nextId()
            assertTrue(cur > prev, "ID not strictly increasing at iteration $it: prev=$prev cur=$cur")
            prev = cur
        }
    }

    @Test
    fun `ids are unique under moderate multi-threaded contention`() {
        val threads = 8
        val perThread = 5_000
        val total = threads * perThread
        val gen = InProcessIdGenerator(nodeId = 2)
        val set = ConcurrentHashMap.newKeySet<Long>()
        val pool = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)
        repeat(threads) {
            pool.submit {
                repeat(perThread) {
                    set.add(gen.nextId())
                }
                latch.countDown()
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        pool.shutdown()
        assertEquals(total, set.size, "Expected $total unique IDs but got ${set.size}")
    }

    @Test
    fun `radix conversions produce expected representations`() {
        val gen = InProcessIdGenerator(nodeId = 3)
        val id = gen.nextId()
        fun manualEncode(v: Long, radix: Int): String {
            require(radix in 2..62)
            if (v == 0L) return "0"
            val digits = charArrayOf(
                '0','1','2','3','4','5','6','7','8','9',
                'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
                'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
            )
            if (radix <= 36) return v.toString(radix)
            var value = v
            val sb = StringBuilder()
            while (value > 0) {
                val r = (value % radix).toInt()
                sb.append(digits[r])
                value /= radix
            }
            return sb.reverse().toString()
        }
        val toUnsigned = InProcessIdGenerator::class.java.getDeclaredMethod("toUnsignedString", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        toUnsigned.isAccessible = true
        val bases = listOf(2, 8, 10, 16, 36, 62)
        bases.forEach { base ->
            val expected = manualEncode(id, base)
            val actual = toUnsigned.invoke(gen, id, base) as String
            assertEquals(expected, actual, "Mismatch for base $base")
        }
    }

    @Test
    fun `sequence rollover results in monotonic ids for small sequenceBits`() {
        val gen = InProcessIdGenerator(nodeId = 0, nodeBits = 5, sequenceBits = 2)
        val ids = LongArray(50) { gen.nextId() }
        for (i in 1 until ids.size) {
            assertTrue(ids[i] > ids[i - 1], "IDs not increasing across rollover boundary at index $i")
        }
    }

    @Test
    fun `invalid parameters throw`() {
        assertThrows<IllegalArgumentException> { InProcessIdGenerator(nodeBits = -1) }
        assertThrows<IllegalArgumentException> { InProcessIdGenerator(sequenceBits = -1) }
        assertThrows<IllegalArgumentException> { InProcessIdGenerator(nodeBits = 40, sequenceBits = 40) }
    }

    @Test
    fun `node id bounds enforced`() {
        val gen = InProcessIdGenerator(nodeId = 0, nodeBits = 1, sequenceBits = 1)
        assertNotNull(gen)
        assertThrows<IllegalArgumentException> { InProcessIdGenerator(nodeId = 2, nodeBits = 1, sequenceBits = 1) }
    }

    @Test
    fun `concurrent ordering not strictly guaranteed but merged sorted list is strictly increasing`() {
        val gen = InProcessIdGenerator(nodeId = 7)
        val threads = 4
        val perThread = 2_000
        val pool = Executors.newFixedThreadPool(threads)
        val results = mutableListOf<List<Long>>()
        val lock = Any()
        val latch = CountDownLatch(threads)
        repeat(threads) {
            pool.submit {
                val local = ArrayList<Long>(perThread)
                repeat(perThread) { local += gen.nextId() }
                synchronized(lock) { results += local }
                latch.countDown()
            }
        }
        latch.await(20, TimeUnit.SECONDS)
        pool.shutdown()
        val merged = results.flatten().sorted()
        for (i in 1 until merged.size) {
            assertTrue(merged[i] > merged[i - 1], "Merged IDs not strictly increasing at $i")
        }
    }
}
