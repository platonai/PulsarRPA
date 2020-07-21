package ai.platon.pulsar.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TestNamedScoreVector {

    private val score = NamedScoreVector()

    @Before
    fun setup() {
        score.setValue(Name.refIndexErrDensity, 10)
        score.setValue(Name.refParseErrDensity, 8)
        score.setValue(Name.modifyTime, -8)
        score.setValue(Name.modifyTime, -2)
        score.setValue(Name.contentScore, 2201)
    }

    @Test
    fun testStringFormat() {
        Assert.assertEquals("0,0,0,2201,0,0,8,0,10,-2,0", score.toString())
        Assert.assertEquals(score, ScoreVector.parse("0,0,0,2201,0,0,8,0,10,-2,0"))
        score.setValue(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        Assert.assertEquals("1,2,3,4,5,6,-7,-8,9,10,11", score.toString())
        Assert.assertEquals(score, ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11"))
    }

    @Test
    fun testPriority() {
        for (i in 0..score.size().dec()) {
            Assert.assertEquals(i.toString() + "th", i.toLong(), score[i].priority.toLong())
        }
    }
}
