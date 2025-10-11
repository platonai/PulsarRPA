package ai.platon.pulsar.common

import org.apache.commons.lang3.math.NumberUtils
import kotlin.test.Test
import java.text.DecimalFormat
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 */
class TestNumber {
    @Test
    fun testA() {
        assertEquals(3.212, NumberUtils.createFloat("3.212").toDouble(), 10e-5)
        assertTrue(NumberUtils.isNumber("3.122f"))
        assertTrue(NumberUtils.isDigits("3122"))
        // assertEquals(3.212, NumberUtils.createFloat("a3.212"), 10e-5);
    }
    
    @Test
    fun testDecimalFormat() {
        var df = DecimalFormat("0.0##")
        
        assertEquals("0.568", df.format(.5678))
        assertEquals("6.5", df.format(6.5))
        assertEquals("56.568", df.format(56.5678))
        assertEquals("123456.568", df.format(123456.5678))
        assertEquals("0.0", df.format(.0))
        assertEquals("6.0", df.format(6.00))
        assertEquals("56.0", df.format(56.0000))
        assertEquals("123456.0", df.format(123456.00001))
        
        df = DecimalFormat("#.##")
        println(df.format(245.6787))
    }
}
