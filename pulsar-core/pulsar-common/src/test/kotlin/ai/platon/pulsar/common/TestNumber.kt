package ai.platon.pulsar.common

import java.text.DecimalFormat
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-1-14.
 */
class TestNumber {

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
        assertEquals("245.68", df.format(245.6787))
    }
}
