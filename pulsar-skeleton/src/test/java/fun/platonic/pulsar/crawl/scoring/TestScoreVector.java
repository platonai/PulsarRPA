package fun.platonic.pulsar.crawl.scoring;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestScoreVector {

    @Test
    public void testStringFormat() {
        NamedScoreVector score = new NamedScoreVector();
        score.setValue(3, 10);
        score.setValue(1, 8);
        score.setValue(6, -8);
        score.setValue(7, -2);
        score.setValue(9, 2201);

        assertEquals("0,8,0,10,0,0,-8,-2,0,2201,0", score.toString());
        assertEquals(score, ScoreVector.parse("0,8,0,10,0,0,-8,-2,0,2201,0"));

        score.setValue(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
        assertEquals("1,2,3,4,5,6,-7,-8,9,10,11", score.toString());
        assertEquals(score, ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11"));
    }

    @Test
    public void testCompare() {
        ScoreVector[] scores = {
                new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11),
                new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 12),
                new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 11, 11),
                new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -7, 10, 10, 11),
                new ScoreVector("11", 1, 2, 3, 4, 5, 6, -6, -8, 10, 10, 11),
                new ScoreVector("11", 1, 2, 3, 4, 5, 7, -7, -8, 10, 10, 11),
                new ScoreVector("11", 1, 2, 3, 4, 6, 6, -7, -8, 10, 10, 11),
                new ScoreVector("11", 1, 2, 3, 5, 5, 6, -7, -8, 10, 10, 11),
                new ScoreVector("11", 1, 3, 4, 4, 5, 6, -7, -8, 10, 10, 11),
                new ScoreVector("11", 1, 4, 4, 4, 5, 6, -7, -8, 10, 10, 11),
                new ScoreVector("11", 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new ScoreVector("12", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        };

        for (int i = 0; i < scores.length - 1; ++i) {
            assertTrue(i + "th", scores[i].compareTo(scores[i + 1]) < 0);
        }
    }

    @Test
    public void testCompare2() {
        ScoreVector score = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
        ScoreVector score2 = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 12);
        ScoreVector score3 = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 11, 11);
        ScoreVector score4 = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -7, 10, 10, 11);
        ScoreVector score5 = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -6, -8, 10, 10, 11);
        ScoreVector score6 = new ScoreVector("11", 1, 2, 3, 4, 5, 7, -7, -8, 10, 10, 11);
        ScoreVector score7 = new ScoreVector("11", 1, 2, 3, 4, 6, 6, -7, -8, 10, 10, 11);
        ScoreVector score8 = new ScoreVector("11", 1, 2, 3, 5, 5, 6, -7, -8, 10, 10, 11);

        ScoreVector score9 = new ScoreVector("11", 1, 3, 4, 4, 5, 6, -7, -8, 10, 10, 11);
        ScoreVector score10 = new ScoreVector("12", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertTrue(score.compareTo(score2) < 0);
        assertTrue(score2.compareTo(score3) < 0);
        assertTrue(score3.compareTo(score4) < 0);
        assertTrue(score4.compareTo(score5) < 0);
        assertTrue(score5.compareTo(score6) < 0);
        assertTrue(score6.compareTo(score7) < 0);
        assertTrue(score7.compareTo(score8) < 0);

        assertTrue(score8.compareTo(score9) < 0);
        assertTrue(score9.compareTo(score10) < 0);
    }

    @Test
    public void testEquals() {
        ScoreVector score = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
        ScoreVector score2 = ScoreVector.parse("1,2,3,4,5,6,-7,-8,9,10,11");

        assertTrue(score + " <-> " + score2, score.compareTo(score2) == 0);
        assertEquals(score + " <-> " + score2, score, score2);
        assertEquals(new Integer(1), new Integer(1));
        assertNotEquals(new Integer(1), new Long(1));
    }

//  @Test
//  public void testFloatValue() {
//    MultiValueScore score = new MultiValueScore(1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);
//    System.out.println(score.floatValue());
//  }

    @Test
    public void testIO() {
        ScoreVector score = new ScoreVector("11", 1, 2, 3, 4, 5, 6, -7, -8, 9, 10, 11);

    }
}
