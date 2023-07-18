package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.common.DescriptiveResult
import ai.platon.pulsar.common.math.geometric.DimD
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import com.github.kklisura.cdt.protocol.ChromeDevTools
import com.github.kklisura.cdt.protocol.commands.DOM
import com.github.kklisura.cdt.protocol.commands.Page
import com.github.kklisura.cdt.protocol.types.input.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.math3.util.Precision
import java.awt.Robot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class DeviceMetrics(
    val width: Int,
    val height: Int,
    val deviceScaleFactor: Double,
    val mobile: Boolean,
)

data class NodeClip(
    var nodeId: Int = 0,
    var pageX: Int = 0,
    var pageY: Int = 0,
    var rect: RectD? = null,
)

/**
 * ClickableDOM provides a set of methods to help users to click on a specified DOM correctly.
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class ClickableDOM(
    val page: Page,
    val dom: DOM,
    val nodeId: Int,
    val offset: OffsetD? = null
) {
    companion object {
        fun create(page: Page?, dom: DOM?, nodeId: Int?, offset: OffsetD? = null): ClickableDOM? {
            if (nodeId == null || nodeId <= 0) return null
            if (page == null) return null
            if (dom == null) return null
            return ClickableDOM(page, dom, nodeId, offset)
        }
    }

    fun clickablePoint(): DescriptiveResult<PointD> {
        val contentQuads = kotlin.runCatching { dom.getContentQuads(nodeId, null, null) }.getOrNull()
        if (contentQuads == null) {
            // throw new Error('Node is either not clickable or not an HTMLElement');
            // return 'error:notvisible';
            return DescriptiveResult("error:notvisible")
        }

        val layoutMetrics = page.layoutMetrics
        if (layoutMetrics == null) {
            // throw new Error('Node is either not clickable or not an HTMLElement');
            // return 'error:notvisible';
            return DescriptiveResult("error:notvisible")
        }

        val viewport = layoutMetrics.cssLayoutViewport

        val dim = DimD(viewport.clientWidth.toDouble(), viewport.clientHeight.toDouble())
        val quads = contentQuads.filterNotNull()
            .map { fromProtocolQuad(it) }
            .map { intersectQuadWithViewport(it, dim.width, dim.height) }
            .filter { computeQuadArea(it) > 0.99 }
        if (quads.isEmpty()) {
            // throw new Error('Node is either not clickable or not an HTMLElement');
            // return 'error:notinviewport'
            return DescriptiveResult("error:notinviewport")
        }

        val quad = quads[0]

        if (offset != null) {
            // Return the point of the first quad identified by offset.
            val MAX_SAFE_POSITION = 1000000.0
            var minX = MAX_SAFE_POSITION
            var minY = MAX_SAFE_POSITION
            for (point in quad) {
                if (point.x < minX) {
                    minX = point.x
                }
                if (point.y < minY) {
                    minY = point.y
                }
            }

            if (!Precision.equals(minX, MAX_SAFE_POSITION) && !Precision.equals(minY, MAX_SAFE_POSITION)) {
                return DescriptiveResult(PointD(x = minX + offset.x, y = minY + offset.y))
            }
        }

        // Return the middle point of the first quad.
        var x = 0.0
        var y = 0.0
        for (point in quad) {
            x += point.x
            y += point.y
        }

        return DescriptiveResult(PointD(x = x / 4, y = y / 4))
    }

    fun isVisible(): Boolean {
        return clickablePoint().value != null
    }

    fun boundingBox(): RectD? {
        val box = kotlin.runCatching { dom.getBoxModel(nodeId, null, null) }
            .getOrNull() ?: return null

        val quad = box.border
        if (quad.isEmpty()) {
            return null
        }

        val x = arrayOf(quad[0], quad[2], quad[4], quad[6]).minOrNull()!!
        val y = arrayOf(quad[1], quad[3], quad[5], quad[7]).minOrNull()!!
        val width = arrayOf(quad[0], quad[2], quad[4], quad[6]).maxOrNull()!! - x
        val height = arrayOf(quad[1], quad[3], quad[5], quad[7]).maxOrNull()!! - y

        // TODO: handle iframes

        return RectD(x, y, width, height)
    }

    private fun fromProtocolQuad(quad: List<Double>): List<PointD> {
        return listOf(
            PointD(quad[0], quad[1]),
            PointD(quad[2], quad[3]),
            PointD(quad[4], quad[5]),
            PointD(quad[6], quad[7])
        )
    }

    private fun intersectQuadWithViewport(quad: List<PointD>, width: Double, height: Double): List<PointD> {
        return quad.map { point ->
            PointD(x = min(max(point.x, 0.0), width), y = min(max(point.y, 0.0), height))
        }
    }

    private fun computeQuadArea(quad: List<PointD>): Double {
        /* Compute sum of all directed areas of adjacent triangles
          https://en.wikipedia.org/wiki/Polygon#Simple_polygons
        */
        var area = 0.0

        var i = 0
        while (i < quad.size) {
            val p1 = quad[i]
            val p2 = quad[(i + 1) % quad.size]
            area += (p1.x * p2.y - p2.x * p1.y) / 2;

            ++i
        }

        return abs(area)
    }
}

/**
 * The Mouse class operates in main-frame CSS pixels
 * relative to the top-left corner of the viewport.
 *
 * @author Vincent Zhang, ivincent.zhang@gmail.com, platon.ai
 */
class Mouse(private val devTools: ChromeDevTools) {
    private val input get() = devTools.input

    var currentX = 0.0
    var currentY = 0.0

    /**
     * Shortcut for `mouse.move`, `mouse.down` and `mouse.up`.
     * @param x - Horizontal position of the mouse.
     * @param y - Vertical position of the mouse.
     */
    suspend fun click(x: Double, y: Double, clickCount: Int = 1, delayMillis: Long = 500) {
        moveTo(x, y)
        down(x, y, clickCount)

        if (delayMillis > 0) {
            delay(delayMillis)
        }

        up(x, y, clickCount)
    }

    suspend fun moveTo(point: PointD, steps: Int = 5, delayMillis: Long = 50) {
        moveTo(point.x, point.y, steps, delayMillis)
    }

    suspend fun moveTo(x: Double, y: Double, steps: Int = 1, delayMillis: Long = 50) {
        val fromX = currentX
        val fromY = currentY

        currentX = x
        currentY = y

        var i = 0
        while (i < steps) {
            val x1 = fromX + (currentX - fromX) * (i.toDouble() / steps)
            val y1 = fromY + (currentY - fromY) * (i.toDouble() / steps)

            chromeMoveTo(x1, y1)

            if (delayMillis > 0) {
                delay(delayMillis)
            }

            ++i
        }
    }

    /**
     * TODO: input.dispatchMouseEvent(MOUSE_MOVED) not work, the reason is unknown. Robot.mouseMove works.
     * */
    private fun chromeMoveTo(x: Double, y: Double) {
        input.dispatchMouseEvent(
            DispatchMouseEventType.MOUSE_MOVED, x, y,
            null, null,
            null, // button
            null, // buttons
            null,
            null, // force
            null,
            null,
            null,
            null, // twist
            null,
            null,
            null
        )
    }

    private fun awtMoveTo(x: Double, y: Double) {
        val robot = Robot()
        robot.mouseMove(x.toInt(), y.toInt())
    }

    /**
     * Dispatches a `mousedown` event.
     */
    suspend fun down(clickCount: Int = 1) {
        down(currentX, currentY, clickCount)
    }

    /**
     * Dispatches a `mousedown` event.
     */
    suspend fun down(point: PointD, clickCount: Int = 1) {
        down(point.x, point.y, clickCount)
    }

    /**
     * Dispatches a `mousedown` event.
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    suspend fun down(x: Double, y: Double, clickCount: Int = 1) {
        withContext(Dispatchers.IO) {
            input.dispatchMouseEvent(
                DispatchMouseEventType.MOUSE_PRESSED, x, y,
                null, null,
                MouseButton.LEFT,
                null, // buttons
                clickCount,
                0.5, // force
                null,
                null,
                null,
                null, // twist
                null,
                null,
                null
            )
        }
    }

    suspend fun up() {
        up(currentX, currentY)
    }

    suspend fun up(point: PointD) {
        up(point.x, point.y)
    }

    suspend fun up(x: Double, y: Double, clickCount: Int = 1) {
        withContext(Dispatchers.IO) {
            input.dispatchMouseEvent(
                DispatchMouseEventType.MOUSE_RELEASED, x, y,
                null, null,
                MouseButton.LEFT,
                null, // buttons
                clickCount,
                null, // force
                null,
                null, // tiltX
                null, // tiltY
                null, // twist
                null, // deltaX
                null, // deltaY
                null
            )
        }
    }

    suspend fun scroll(deltaX: Double = 0.0, deltaY: Double = 10.0) {
        withContext(Dispatchers.IO) {
            input.dispatchMouseEvent(
                DispatchMouseEventType.MOUSE_WHEEL, currentX, currentY,
                null, null,
                null, // button
                null, // buttons
                null,
                null, // force
                null,
                null, // tiltX
                null, // tiltY
                null, // twist
                deltaX, // deltaX
                deltaY, // deltaY
                null
            )
        }
    }

    /**
     * Dispatches a `mousewheel` event.
     *
     * @example
     * An example of zooming into an element:
     * ```
     * val elem = driver.querySelector('div');
     * val boundingBox = elem.boundingBox();
     * mouse.move(
     *   boundingBox.x + boundingBox.width / 2,
     *   boundingBox.y + boundingBox.height / 2
     * );
     *
     * mouse.wheel({ deltaY: -100 })
     * ```
     */
    suspend fun wheel(deltaX: Double = 10.0, deltaY: Double = 10.0) {
        wheel(currentX, currentY, deltaX, deltaY)
    }

    /**
     * Dispatches a `mousewheel` event.
     *
     * @example
     * An example of zooming into an element:
     * ```
     * val elem = driver.querySelector('div');
     * val boundingBox = elem.boundingBox();
     * mouse.move(
     *   boundingBox.x + boundingBox.width / 2,
     *   boundingBox.y + boundingBox.height / 2
     * );
     *
     * mouse.wheel({ deltaY: -100 })
     * ```
     */
    suspend fun wheel(point: PointD, deltaX: Double = 0.0, deltaY: Double = 10.0) {
        wheel(point.x, point.y, deltaX, deltaY)
    }

    /**
     * Dispatches a `mousewheel` event.
     *
     * @example
     * An example of zooming into an element:
     * ```
     * val elem = driver.querySelector('div');
     * val boundingBox = elem.boundingBox();
     * mouse.move(
     *   boundingBox.x + boundingBox.width / 2,
     *   boundingBox.y + boundingBox.height / 2
     * );
     *
     * mouse.wheel({ deltaY: -100 })
     * ```
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    suspend fun wheel(x: Double, y: Double, deltaX: Double, deltaY: Double) {
        withContext(Dispatchers.IO) {
            input.dispatchMouseEvent(
                DispatchMouseEventType.MOUSE_WHEEL, x, y,
                null, null,
                null, // button
                null, // buttons
                null,
                null, // force
                null,
                null, // tiltX
                null, // tiltY
                null, // twist
                deltaX, // deltaX
                deltaY, // deltaY
                null
            )
        }
    }

    /**
     * Dispatches a `drag` event.
     * @param start - starting point for drag
     * @param target - point to drag to
     */
    suspend fun drag(start: PointD, target: PointD): DragData? {
        var dragData: DragData? = null

        withContext(Dispatchers.IO) {
            input.setInterceptDrags(true)
            input.onDragIntercepted {
                dragData = it.data
            }
        }

        moveTo(start, 5, 100)
        down(currentX, currentY)
        moveTo(target, 3, 500)

        return dragData
    }

    /**
     * Dispatches a `dragenter` event.
     * @param target - point for emitting `dragenter` event
     * @param data - drag data containing items and operations mask
     */
    suspend fun dragEnter(target: PointD, data: DragData) {
        withContext(Dispatchers.IO) {
            input.dispatchDragEvent(
                DispatchDragEventType.DRAG_ENTER, target.x, target.y,
                data
            )
        }
    }

    /**
     * Dispatches a `dragover` event.
     * @param target - point for emitting `dragover` event
     * @param data - drag data containing items and operations mask
     */
    suspend fun dragOver(target: PointD, data: DragData) {
        withContext(Dispatchers.IO) {
            input.dispatchDragEvent(
                DispatchDragEventType.DRAG_OVER, target.x, target.y,
                data
            )
        }
    }

    /**
     * Performs a dragenter, dragover, and drop in sequence.
     * @param target - point to drop on
     * @param data - drag data containing items and operations mask
     */
    suspend fun drop(target: PointD, data: DragData) {
        withContext(Dispatchers.IO) {
            input.dispatchDragEvent(
                DispatchDragEventType.DROP, target.x, target.y,
                data
            )
        }
    }

    /**
     * Performs a drag, dragenter, dragover, and drop in sequence.
     * @param target - point to drag from
     * @param target - point to drop on
     * @param options - An object of options. Accepts delay which,
     * if specified, is the time to wait between `dragover` and `drop` in milliseconds.
     * Defaults to 0.
     */
    suspend fun dragAndDrop(start: PointD, target: PointD, delayMillis: Long = 500) {
        val data = drag(start, target) ?: return
        dragEnter(target, data)
        dragOver(target, data)
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        drop(target, data)
        up()
    }
}

/**
 * Keyboard provides an api for managing a virtual keyboard.
 * */
class Keyboard(private val devTools: ChromeDevTools) {
    private val input get() = devTools.input

    suspend fun type(nodeId: Int, text: String, delayMillis: Long) {
        text.forEach { char ->
            if (Character.isISOControl(char)) {
                // TODO:
            } else {
                input.insertText("$char")
            }
            delay(delayMillis)
        }
    }

    suspend fun press(key: String, delayMillis: Long) {
        down(key)
        delay(delayMillis)
        up(key)
    }

    fun down(key: String) {
        input.dispatchKeyEvent(
            DispatchKeyEventType.KEY_DOWN
        )
    }

    fun up(key: String) {
        input.dispatchKeyEvent(
            DispatchKeyEventType.KEY_UP
        )
    }
}

class Touchscreen() {

}
