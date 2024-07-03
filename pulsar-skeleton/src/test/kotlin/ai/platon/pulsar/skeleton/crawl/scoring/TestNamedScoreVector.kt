package ai.platon.pulsar.skeleton.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import kotlin.test.*

class TestNamedScoreVector {

    private val score = NamedScoreVector()

    @BeforeTest
    fun setup() {
        score.setValue(Name.refIndexErrDensity, 10)
        score.setValue(Name.refParseErrDensity, 8)
        score.setValue(Name.modifyTime, -8)
        score.setValue(Name.modifyTime, -2)
        score.setValue(Name.contentScore, 2201)
    }

    @Test
    fun testStringFormat() {
        assertEquals("0,0,0,2201,0,0,8,0,10,-2,0", score.toString())
        assertEquals(score.toString(), ScoreVector.parse("0,0,0,2201,0,0,8,0,10,-2,0").toString())
        score.setValue(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11)
        assertEquals("1,2,3,4,5,6,-7,-8,9,10,11", score.toString())
        assertEquals(score.toString(), ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11").toString())
    }

    @Test
    @Ignore("TODO: Failed")
    fun testPriority() {
        for (i in 0 until score.size()) {
            assertEquals(i.toLong(), score[i].priority.toLong(), "${i}th")
        }
    }
}
