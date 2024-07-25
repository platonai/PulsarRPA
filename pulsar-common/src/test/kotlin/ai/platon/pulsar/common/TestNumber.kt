package ai.platon.pulsar.common

import org.apache.commons.lang3.math.NumberUtils
import org.junit.Assert
import org.junit.Test
import java.text.DecimalFormat

/**
 * Created by vincent on 17-1-14.
 */
class TestNumber {
    @Test
    fun testA() {
        Assert.assertEquals(3.212, NumberUtils.createFloat("3.212").toDouble(), 10e-5)
        Assert.assertTrue(NumberUtils.isNumber("3.122f"))
        Assert.assertTrue(NumberUtils.isDigits("3122"))
        // assertEquals(3.212, NumberUtils.createFloat("a3.212"), 10e-5);
    }
    
    @Test
    fun testDecimalFormat() {
        var df = DecimalFormat("0.0##")
        
        Assert.assertEquals("0.568", df.format(.5678))
        Assert.assertEquals("6.5", df.format(6.5))
        Assert.assertEquals("56.568", df.format(56.5678))
        Assert.assertEquals("123456.568", df.format(123456.5678))
        Assert.assertEquals("0.0", df.format(.0))
        Assert.assertEquals("6.0", df.format(6.00))
        Assert.assertEquals("56.0", df.format(56.0000))
        Assert.assertEquals("123456.0", df.format(123456.00001))
        
        df = DecimalFormat("#.##")
        println(df.format(245.6787))
    }
}
