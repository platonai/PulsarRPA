package ai.platon.pulsar.common.math.geometric

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle

data class GeoIntPoint(var x: Int, var y: Int): Comparable<GeoIntPoint> {
    constructor(point: Point) : this(point.x, point.y)

    fun toPoint(): Point {
        return Point(x, y)
    }

    fun align(gridWidth: Int = 0, gridHeight: Int = 0): GeoIntPoint {
        val x2 = if (gridWidth == 0) x else Math.round(x.toFloat() / gridWidth) * gridWidth
        val y2 = if (gridHeight == 0) y else Math.round(y.toFloat() / gridHeight) * gridHeight
        return GeoIntPoint(x2, y2)
    }

    fun align(grid: Dimension): GeoIntPoint {
        return align(grid.width, grid.height)
    }

    override fun compareTo(other: GeoIntPoint): Int {
        val r = x - other.x
        return if (r == 0) y - other.y else r
    }
}

operator fun Point.component1(): Int {
    return x
}

operator fun Point.component2(): Int {
    return y
}

fun Point.toIntPoint(): GeoIntPoint {
    return GeoIntPoint(x, y)
}

val Point.str
    get() = "[$x $y]"

val Rectangle.x2: Int
    get() {
        return x + width
    }

val Rectangle.y2: Int
    get() {
        return y + height
    }

operator fun Rectangle.component1(): Int {
    return x
}

operator fun Rectangle.component2(): Int {
    return y
}

operator fun Rectangle.component3(): Int {
    return width
}

operator fun Rectangle.component4(): Int {
    return height
}

fun Rectangle.sim(rect: Rectangle): Double {
    val overlap = this.intersection(rect)
    if (overlap.isEmpty) return 0.0
    val a = overlap.area
    val b = Math.max(area, rect.area)
    return 1.0 * a / b
}

val Rectangle.area get() = if (isEmpty) 0 else width * height

val Rectangle.str get() = "[$x $y $width $height]"

val Rectangle.str2 get() = "{x:$x y:$y w:$width h:$height}"

/**
 * The align type to define linearity
 * */
enum class AlignType {
    NONE, LEFT, V_CENTER, RIGHT, TOP, H_CENTER, BOTTOM;

    val isNone: Boolean
        get() = this != NONE

    val isVertical: Boolean
        get() = this in arrayOf(LEFT, V_CENTER, RIGHT)

    val isHorizontal: Boolean
        get() = this in arrayOf(TOP, H_CENTER, BOTTOM)
}

/**
 * Two rectangles are vertical collinear if they are intersects and have a padding, ignore the height
 * TODO: not fully tested
 * */
fun Rectangle.testAlignment(r: Rectangle, bias: Double = 0.2): AlignType {
    val b = Math.min(bias, 1.0)
    return when {
        Math.abs(x - r.x)               <= b * width -> AlignType.LEFT
        Math.abs(centerX - r.centerX)   <= b * width -> AlignType.V_CENTER
        Math.abs(x2 - r.x2)             <= b * width -> AlignType.RIGHT
        Math.abs(y - r.y)               <= b * height -> AlignType.TOP
        Math.abs(centerY - r.centerY)   <= b * height -> AlignType.H_CENTER
        Math.abs(y2 - r.y2)             <= b * height -> AlignType.BOTTOM
        else -> AlignType.NONE
    }
}

data class PointI(
    var x: Int,
    var y: Int
)

data class DimI(
    var width: Int,
    var height: Int
)

data class RectI(
    var x: Int,
    var y: Int,
    var with: Int,
    var height: Int
)

data class OffsetI(
    var x: Int,
    var y: Int,
)

data class PointD(
    var x: Double,
    var y: Double
)

data class DimD(
    var width: Double,
    var height: Double
)

data class RectD(
    var x: Double,
    var y: Double,
    var width: Double,
    var height: Double
)

data class OffsetD(
    var x: Double,
    var y: Double,
)
