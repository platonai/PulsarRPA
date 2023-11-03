package ai.platon.pulsar.common

import kotlin.test.*

/**
 * Created by vincent on 17-1-14.
 */
class TestPriority {
    @Test
    fun testUpperPriority() {
        assertEquals(Priority13.HIGHEST, Priority13.upperPriority(Int.MIN_VALUE))
        assertEquals(Priority13.HIGHER.value, -1000)
        assertEquals(Priority13.HIGHER, Priority13.upperPriority(-999))
        assertEquals(Priority13.HIGHER, Priority13.upperPriority(-1))
        assertEquals(Priority13.NORMAL.value, 0)
        assertEquals(Priority13.NORMAL, Priority13.upperPriority(0))
        assertEquals(Priority13.NORMAL, Priority13.upperPriority(1))
        assertEquals(Priority13.NORMAL, Priority13.upperPriority(998))
        assertEquals(Priority13.LOWER.value, 1000)
        assertEquals(Priority13.LOWER, Priority13.upperPriority(1000))
        assertEquals(Priority13.LOWER, Priority13.upperPriority(1001))
        assertEquals(Priority13.LOWER, Priority13.upperPriority(1998))
        assertEquals(Priority13.LOWEST, Priority13.upperPriority(Int.MAX_VALUE))
    }

    @Test
    fun testLowerPriority() {
        assertEquals(Priority13.HIGHEST, Priority13.lowerPriority(Int.MIN_VALUE))
        assertEquals(Priority13.HIGHER.value, -1000)
        assertEquals(Priority13.NORMAL, Priority13.lowerPriority(-999))
        assertEquals(Priority13.NORMAL, Priority13.lowerPriority(-1))
        assertEquals(Priority13.NORMAL, Priority13.lowerPriority(0))
        assertEquals(Priority13.NORMAL.value, 0)
        assertEquals(Priority13.LOWER, Priority13.lowerPriority(1))
        assertEquals(Priority13.LOWER, Priority13.lowerPriority(998))
        assertEquals(Priority13.LOWER, Priority13.lowerPriority(1000))
        assertEquals(Priority13.LOWER.value, 1000)
        assertEquals(Priority13.LOWER2, Priority13.lowerPriority(1001))
        assertEquals(Priority13.LOWER2, Priority13.lowerPriority(1998))
        assertEquals(Priority13.LOWEST, Priority13.lowerPriority(Int.MAX_VALUE))
    }

    @Test
    fun testLowerUpperPriority() {
        assertEquals(Priority13.HIGHEST, Priority13.upperPriority(Int.MIN_VALUE))
        assertEquals(Priority13.HIGHEST, Priority13.lowerPriority(Int.MIN_VALUE))

        assertEquals(Priority13.HIGHER, Priority13.upperPriority(-1000))
        assertEquals(Priority13.HIGHER, Priority13.lowerPriority(-1000))
        assertEquals(Priority13.HIGHER.value, -1000)

        assertEquals(Priority13.NORMAL, Priority13.upperPriority(0))
        assertEquals(Priority13.NORMAL, Priority13.lowerPriority(0))
        assertEquals(Priority13.NORMAL.value, 0)

        assertEquals(Priority13.LOWER, Priority13.upperPriority(1000))
        assertEquals(Priority13.LOWER, Priority13.lowerPriority(1000))
        assertEquals(Priority13.LOWER.value, 1000)

        assertEquals(Priority13.LOWER, Priority13.upperPriority(1000))
        assertEquals(Priority13.LOWER, Priority13.lowerPriority(1000))

        assertEquals(Priority13.LOWEST, Priority13.upperPriority(Int.MAX_VALUE))
        assertEquals(Priority13.LOWEST, Priority13.lowerPriority(Int.MAX_VALUE))
    }
}
