package ai.platon.pulsar.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import com.google.common.math.IntMath
import org.apache.commons.math3.analysis.function.Sigmoid
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestScoreVector {
    @Test
    fun testCompare() {
        val scores = arrayOf(
            ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11),
            ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 12),
            ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 11, 11),
            ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -7, 10, 10, 11),
            ScoreVector("11", 1, 2, 3, 4, 5, 6, -6, -8, 10, 10, 11),
            ScoreVector("11", 1, 2, 3, 4, 5, 7, -7, -8, 10, 10, 11),
            ScoreVector("11", 1, 2, 3, 4, 6, 6, -7, -8, 10, 10, 11),
            ScoreVector("11", 1, 2, 3, 5, 5, 6, -7, -8, 10, 10, 11),
            ScoreVector("11", 1, 3, 4, 4, 5, 6, -7, -8, 10, 10, 11),
            ScoreVector("11", 1, 4, 4, 4, 5, 6, -7, -8, 10, 10, 11),
            ScoreVector("11", 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            ScoreVector("12", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        )
        for (i in 0 until scores.size - 1) {
            assertTrue(i.toString() + "th") { scores[i] < scores[i + 1] }
        }
    }

    @Test
    fun testCompare2() {
        val score = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        val score2 = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 12)
        val score3 = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 11, 11)
        val score4 = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -7, 10, 10, 11)
        val score5 = ScoreVector("11", 1, 2, 3, 4, 5, 6, -6, -8, 10, 10, 11)
        val score6 = ScoreVector("11", 1, 2, 3, 4, 5, 7, -7, -8, 10, 10, 11)
        val score7 = ScoreVector("11", 1, 2, 3, 4, 6, 6, -7, -8, 10, 10, 11)
        val score8 = ScoreVector("11", 1, 2, 3, 5, 5, 6, -7, -8, 10, 10, 11)
        val score9 = ScoreVector("11", 1, 3, 4, 4, 5, 6, -7, -8, 10, 10, 11)
        val score10 = ScoreVector("12", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(score < score2)
        assertTrue(score2 < score3)
        assertTrue(score3 < score4)
        assertTrue(score4 < score5)
        assertTrue(score5 < score6)
        assertTrue(score6 < score7)
        assertTrue(score7 < score8)
        assertTrue(score8 < score9)
        assertTrue(score9 < score10)
    }

    @Test
    fun testEquals() {
        val score = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        val score2 = ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11")
        assertTrue("$score <-> $score2") { score.compareTo(score2) == 0 }
        assertEquals(score, score2, "$score <-> $score2")
        assertEquals(1, 1)
    }

    @Test
    fun testDoubleValue() {
        val sig = Sigmoid()
        val score = ScoreVector("16", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11, 12, 13, 14, 15, 16)
        val sz = score.entries.size
        for (i in 0 until sz) {
            var s = score.entries[i].value.toDouble()
            print(String.format("%,f", s))
            print('\t')
            s = IntMath.pow(10, 2 * (sz - i)) * (100 * sig.value(s) - 1)
            print(String.format("%,f", s))
            print('\t')
        }
        println()
        println(String.format("%s", score.toString()))
        println(String.format("%,f", score.toDouble()))
    }

    @Test
    fun testIO() {
        val score = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
    }
}