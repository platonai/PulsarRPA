package ai.platon.pulsar.common

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-1-14.
 */
class TestFrequency {

    @Test
    fun testToString() {
        val frequency = Frequency<String>()
        IntRange(1, 10).forEach {  i ->
            repeat(i) { frequency.add("$i") }
        }
//        println(frequency.toString())
//        println(frequency.toPString())
        // Probability string
        assertEquals("1:0.02\t2:0.04\t3:0.05\t4:0.07\t5:0.09\t6:0.11\t7:0.13\t8:0.15\t9:0.16\t10:0.18",
            frequency.toPString())
        // Frequency string
        assertEquals("1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10", frequency.toString())
    }
}
