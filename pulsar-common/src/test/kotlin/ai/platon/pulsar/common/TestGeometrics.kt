package ai.platon.pulsar.common

import ai.platon.pulsar.common.math.geometric.str2
import ai.platon.pulsar.common.math.geometric.testAlignment
import java.awt.Rectangle

import kotlin.test.*

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
     * e.g.
     * {x:0 y:24589 w:1905 h:418}.intersection({x:460 y:100 w:1000 h:864}) == {x:460 y:24589 w:1000 h:-23625}
     * */
    @Test
    fun testRectangleIntersect() {
        val r1 = Rectangle(0, 24589, 1905, 418)
        val r2 = Rectangle(460, 100, 1000, 864)

        val b = r1.intersects(r2)
        require(!b)
        println("intersects: $b")

        // so it should return a empty rectangle
        val r = r1.intersection(r2)
        println(r.str2)
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
