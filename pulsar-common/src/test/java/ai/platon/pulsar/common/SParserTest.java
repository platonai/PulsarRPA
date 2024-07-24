package ai.platon.pulsar.common;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SParserTest {

    @InjectMocks
    private SParser sParser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetDuration_ValidIso8601Duration_ShouldReturnCorrectDuration() {
        sParser = new SParser("PT1H30M");
        assertEquals(Duration.ofHours(1).plusMinutes(30), sParser.getDuration());
    }

    @Test
    public void testGetDuration_ValidHadoopDuration_ShouldReturnCorrectDuration() {
        sParser = new SParser("30m");
        assertEquals(Duration.ofMinutes(30), sParser.getDuration());
    }

    @Test
    public void testGetDuration_InvalidHadoopDuration_ShouldThrowNumberFormatException() {
        sParser = new SParser("1h30m");
        try {
            sParser.getDuration();
        } catch (NumberFormatException e) {
            assertEquals("For input string: \"1h30m\"", e.getMessage());
        }
    }

    @Test
    public void testGetDuration_InvalidDuration_ShouldReturnInvalidDuration() {
        sParser = new SParser("invalid");
        assertEquals(SParser.INVALID_DURATION, sParser.getDuration());
    }

    @Test
    public void testGetDuration_EmptyString_ShouldReturnInvalidDuration() {
        sParser = new SParser("");
        assertEquals(SParser.INVALID_DURATION, sParser.getDuration());
    }

    @Test
    public void testGetDuration_NullString_ShouldReturnInvalidDuration() {
        sParser = new SParser(null);
        assertEquals(SParser.INVALID_DURATION, sParser.getDuration());
    }

    @Test
    public void testGetDuration_WhitespaceString_ShouldReturnInvalidDuration() {
        sParser = new SParser("   ");
        assertEquals(SParser.INVALID_DURATION, sParser.getDuration());
    }

    @Test
    public void testGetDuration_DecimalSeconds_ShouldReturnCorrectDuration() {
        sParser = new SParser("PT1H30M10.5S");
        assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(10).plusMillis(500), sParser.getDuration());
    }

    @Test
    public void testGetDuration_NegativeDuration_ShouldReturnCorrectDuration() {
        sParser = new SParser("-PT1H30M");
        assertEquals(Duration.ofHours(-1).plusMinutes(-30), sParser.getDuration());
    }

    @Test
    public void testGetDuration_ZeroDuration_ShouldReturnZeroDuration() {
        sParser = new SParser("PT0S");
        assertEquals(Duration.ZERO, sParser.getDuration());
    }






    @Test
    public void testGetTimeDuration_WithValidDuration_ShouldReturnCorrectDuration() {
        sParser = new SParser("10s");
        assertEquals(10L, sParser.getTimeDuration(0L, TimeUnit.SECONDS));
    }

    @Test
    public void testGetTimeDuration_WithInvalidSuffix_ShouldThrowNumberFormatException() {
        sParser = new SParser("10xx");
        try {
            sParser.getTimeDuration(0L, TimeUnit.SECONDS);
        } catch (NumberFormatException e) {
            assertEquals("For input string: \"10xx\"", e.getMessage());
        }
    }

    @Test
    public void testGetTimeDuration_WithUnknownUnit_ShouldReturnDefaultValue() {
        sParser = new SParser("10ms");
        assertEquals(0L, sParser.getTimeDuration(0L, TimeUnit.HOURS));
    }

    @Test
    public void testGetTimeDuration_WithNullValue_ShouldReturnDefaultValue() {
        sParser = new SParser(null);
        assertEquals(0L, sParser.getTimeDuration(0L, TimeUnit.SECONDS));
    }

    @Test
    public void testGetTimeDuration_WithWhitespace_ShouldTrimAndReturnCorrectDuration() {
        sParser = new SParser(" 10s ");
        assertEquals(10L, sParser.getTimeDuration(0L, TimeUnit.SECONDS));
    }

    @Test
    public void testGetTimeDuration_WithNegativeValue_ShouldReturnNegativeDuration() {
        sParser = new SParser("-5m");
        assertEquals(-5 * 60L, sParser.getTimeDuration(0L, TimeUnit.SECONDS));
    }
}
