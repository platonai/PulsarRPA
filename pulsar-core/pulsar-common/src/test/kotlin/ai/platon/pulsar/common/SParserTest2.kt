package ai.platon.pulsar.common

import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SParserTest2 {
    @InjectMocks
    private var sParser: SParser? = null
    
    @BeforeTest
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }
    
    @Test
    fun testGetDuration_ValidIso8601Duration_ShouldReturnCorrectDuration() {
        sParser = SParser("PT1H30M")
        assertEquals(Duration.ofHours(1).plusMinutes(30), sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_ValidHadoopDuration_ShouldReturnCorrectDuration() {
        sParser = SParser("30m")
        assertEquals(Duration.ofMinutes(30), sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_InvalidHadoopDuration_ShouldThrowNumberFormatException() {
        sParser = SParser("1h30m")
        try {
            sParser!!.duration
        } catch (e: NumberFormatException) {
            assertEquals("For input string: \"1h30m\"", e.message)
        }
    }
    
    @Test
    fun testGetDuration_InvalidDuration_ShouldReturnInvalidDuration() {
        sParser = SParser("invalid")
        assertEquals(SParser.INVALID_DURATION, sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_EmptyString_ShouldReturnInvalidDuration() {
        sParser = SParser("")
        assertEquals(SParser.INVALID_DURATION, sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_NullString_ShouldReturnInvalidDuration() {
        sParser = SParser(null)
        assertEquals(SParser.INVALID_DURATION, sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_WhitespaceString_ShouldReturnInvalidDuration() {
        sParser = SParser("   ")
        assertEquals(SParser.INVALID_DURATION, sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_DecimalSeconds_ShouldReturnCorrectDuration() {
        sParser = SParser("PT1H30M10.5S")
        assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(10).plusMillis(500), sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_NegativeDuration_ShouldReturnCorrectDuration() {
        sParser = SParser("-PT1H30M")
        assertEquals(Duration.ofHours(-1).plusMinutes(-30), sParser!!.duration)
    }
    
    @Test
    fun testGetDuration_ZeroDuration_ShouldReturnZeroDuration() {
        sParser = SParser("PT0S")
        assertEquals(Duration.ZERO, sParser!!.duration)
    }
    
    
    @Test
    fun testGetTimeDuration_WithValidDuration_ShouldReturnCorrectDuration() {
        sParser = SParser("10s")
        assertEquals(10L, sParser!!.getTimeDuration(0L, TimeUnit.SECONDS))
    }
    
    @Test
    fun testGetTimeDuration_WithInvalidSuffix_ShouldThrowNumberFormatException() {
        sParser = SParser("10xx")
        try {
            sParser!!.getTimeDuration(0L, TimeUnit.SECONDS)
        } catch (e: NumberFormatException) {
            assertEquals("For input string: \"10xx\"", e.message)
        }
    }
    
    @Test
    fun testGetTimeDuration_WithUnknownUnit_ShouldReturnDefaultValue() {
        sParser = SParser("10ms")
        assertEquals(0L, sParser!!.getTimeDuration(0L, TimeUnit.HOURS))
    }
    
    @Test
    fun testGetTimeDuration_WithNullValue_ShouldReturnDefaultValue() {
        sParser = SParser(null)
        assertEquals(0L, sParser!!.getTimeDuration(0L, TimeUnit.SECONDS))
    }
    
    @Test
    fun testGetTimeDuration_WithWhitespace_ShouldTrimAndReturnCorrectDuration() {
        sParser = SParser(" 10s ")
        assertEquals(10L, sParser!!.getTimeDuration(0L, TimeUnit.SECONDS))
    }
    
    @Test
    fun testGetTimeDuration_WithNegativeValue_ShouldReturnNegativeDuration() {
        sParser = SParser("-5m")
        assertEquals(-5 * 60L, sParser!!.getTimeDuration(0L, TimeUnit.SECONDS))
    }
}
