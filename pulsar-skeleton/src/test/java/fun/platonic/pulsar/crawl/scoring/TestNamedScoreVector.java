package fun.platonic.pulsar.crawl.scoring;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestNamedScoreVector {

    private NamedScoreVector score = new NamedScoreVector();

    @Before
    public void setup() {
        score.setValue(NamedScoreVector.Name.refIndexErrDensity, 10);
        score.setValue(NamedScoreVector.Name.refParseErrDensity, 8);
        score.setValue(NamedScoreVector.Name.modifyTime, -8);
        score.setValue(NamedScoreVector.Name.modifyTime, -2);
        score.setValue(NamedScoreVector.Name.contentScore, 2201);
    }

    @Test
    public void testStringFormat() {
        assertEquals("0,0,0,2201,0,0,8,0,10,-2,0", score.toString());
        assertEquals(score, ScoreVector.parse("0,0,0,2201,0,0,8,0,10,-2,0"));

        score.setValue(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
        assertEquals("1,2,3,4,5,6,-7,-8,9,10,11", score.toString());
        assertEquals(score, ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11"));
    }

    @Test
    public void testPriority() {
        for (int i = 0; i < score.size(); ++i) {
            assertEquals(i + "th", i, score.get(i).getPriority());
        }
    }
}
