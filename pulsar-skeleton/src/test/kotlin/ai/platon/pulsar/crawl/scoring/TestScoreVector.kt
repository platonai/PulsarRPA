package ai.platon.pulsar.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import com.google.common.math.IntMath
import org.apache.commons.math3.analysis.function.Sigmoid
import org.junit.Assert
import org.junit.Test

class TestScoreVector {
    @Test
    fun testStringFormat() {
        val score = NamedScoreVector()
        score.setValue(3, 10)
        score.setValue(1, 8)
        score.setValue(6, -8)
        score.setValue(7, -2)
        score.setValue(9, 2201)
        Assert.assertEquals("0,8,0,10,0,0,-8,-2,0,2201,0", score.toString())
        Assert.assertEquals(score, ScoreVector.parse("0,8,0,10,0,0,-8,-2,0,2201,0"))
        score.setValue(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        Assert.assertEquals("1,2,3,4,5,6,-7,-8,9,10,11", score.toString())
        Assert.assertEquals(score, ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11"))
    }

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
                ScoreVector("12", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        for (i in 0 until scores.size - 1) {
            Assert.assertTrue(i.toString() + "th", scores[i].compareTo(scores[i + 1]) < 0)
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
        Assert.assertTrue(score.compareTo(score2) < 0)
        Assert.assertTrue(score2.compareTo(score3) < 0)
        Assert.assertTrue(score3.compareTo(score4) < 0)
        Assert.assertTrue(score4.compareTo(score5) < 0)
        Assert.assertTrue(score5.compareTo(score6) < 0)
        Assert.assertTrue(score6.compareTo(score7) < 0)
        Assert.assertTrue(score7.compareTo(score8) < 0)
        Assert.assertTrue(score8.compareTo(score9) < 0)
        Assert.assertTrue(score9.compareTo(score10) < 0)
    }

    @Test
    fun testEquals() {
        val score = ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        val score2 = ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11")
        Assert.assertTrue("$score <-> $score2", score.compareTo(score2) == 0)
        Assert.assertEquals("$score <-> $score2", score, score2)
        Assert.assertEquals(1, 1)
        Assert.assertNotEquals(1, 1)
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