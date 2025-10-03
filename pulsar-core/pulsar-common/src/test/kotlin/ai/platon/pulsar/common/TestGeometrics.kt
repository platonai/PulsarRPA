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
}
