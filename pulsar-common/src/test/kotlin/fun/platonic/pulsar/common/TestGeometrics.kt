package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.geometric.testAlignment
import org.junit.Ignore
import org.junit.Test
import java.awt.Rectangle
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestGeometrics {
    @Test
    fun testRectangle() {
        assertTrue { Rectangle(0, 0, 11, 11).contains(Rectangle(0, 0, 11, 11)) }
        assertTrue { Rectangle(0, 0, 11, 11).contains(Rectangle(0, 0, 11, 10)) }
        assertTrue { Rectangle(0, 0, 11, 11).contains(Rectangle(1, 0, 10, 11)) }
        assertTrue { Rectangle(0, 0, 11, 11).contains(Rectangle(1, 1, 10, 10)) }
        assertFalse { Rectangle(0, 0, 11, 11).contains(Rectangle(1, 1, 0, 0)) }
        assertTrue { Rectangle(0, 0, 11, 11).contains(Rectangle(1, 1, 1, 1)) }
    }

    /**
     * TODO: Test failed
     * */
    @Ignore("TODO: Test failed")
    @Test
    fun testCollinear() {
        val r1 = Rectangle(0, 0, 100, 40)
        for (y in 0 until 40 step 10) {
            val r2 = Rectangle(0, y, 100, 40)
            assertTrue("y: ${r1.y} - $y") { r1.testAlignment(r2).isVertical }
            assertTrue("y: ${r1.y} - $y") { r2.testAlignment(r1).isVertical }
        }

        for (y in 0..40) {
            val r2 = Rectangle(0, y, 100, 40)
            assertTrue("y: ${r1.y} - $y") { r1.testAlignment(r2).isHorizontal }
            assertTrue("y: ${r1.y} - $y") { r2.testAlignment(r1).isHorizontal }
        }

        val r3 = Rectangle(1000000, 20, 100, 40)
        assertTrue { r1.testAlignment(r3, 0.5).isHorizontal }
        assertTrue { r3.testAlignment(r1, 1.0).isHorizontal }

        val r4 = Rectangle(0, 50, 100, 40)
        assertFalse { r1.testAlignment(r4).isHorizontal }
        assertFalse { r4.testAlignment(r1).isHorizontal }
    }
}
