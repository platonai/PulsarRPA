package ai.platon.pulsar.common

import junit.framework.TestCase
import org.junit.Test
import java.util.*

/**
 * Created by vincent on 17-1-14.
 */
class TestMath {
    @Test
    fun testFloatHash() {
        val a = 0f
        val b = 1000f
        val c = 200000f
        val d = 300000f
        val r = Random()
        
        for (i in 0..999) {
            val x = a + r.nextInt((b - a).toInt())
            val y = MathUtils.hashFloat(x, a, b, c, d)
            TestCase.assertTrue(y.toString(), y >= c && y <= d)
        }
        
        for (i in 0..999) {
            val x = c + r.nextInt(1000)
            val y = MathUtils.hashFloat(x, a, b, c, d)
            TestCase.assertFalse(y >= c && y <= d)
        }
    }
    
    @Test
    fun testIntHash() {
        val a = 0
        val b = 1000
        val c = 200000
        val d = 300000
        val r = Random()
        
        for (i in 0..999) {
            val x = a + r.nextInt(b - a)
            val y = MathUtils.hashInt(x, a, b, c, d)
            TestCase.assertTrue(y.toString(), y >= c && y <= d)
        }
        
        for (i in 0..999) {
            val x = c + r.nextInt(1000)
            val y = MathUtils.hashInt(x, a, b, c, d)
            TestCase.assertFalse(y.toString(), y >= c && y <= d)
        }
    }
    
    @Test
    fun testRound() {
        var a = 10f
        var b = 3f
        
        TestCase.assertEquals(3, Math.round(a / b))
        TestCase.assertEquals(3.33, Math.round(100 * a / b) / 100.0)
        
        a = 17f
        b = 3f
        // System.out.println(17f/3f);
        TestCase.assertEquals(6, Math.round(a / b))
        TestCase.assertEquals(5.67, Math.round(100 * a / b) / 100.0)
    }
}
