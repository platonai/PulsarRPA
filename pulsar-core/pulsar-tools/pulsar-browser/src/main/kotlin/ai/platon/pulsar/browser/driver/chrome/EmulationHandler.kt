package ai.platon.pulsar.browser.driver.chrome

import ai.platon.cdt.kt.protocol.ChromeDevTools
import ai.platon.cdt.kt.protocol.commands.DOM
import ai.platon.cdt.kt.protocol.commands.Page
import ai.platon.cdt.kt.protocol.types.input.*
import ai.platon.pulsar.common.DescriptiveResult
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.io.KeyboardModifier
import ai.platon.pulsar.common.io.VirtualKey
import ai.platon.pulsar.common.io.VirtualKeyboard
import ai.platon.pulsar.common.io.VirtualKeyboard.KEYPAD_LOCATION
import ai.platon.pulsar.common.math.geometric.DimD
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.math3.util.Precision
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class NodeClip(
    var node: NodeRef? = null,
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
    val node: NodeRef,
    val offset: OffsetD? = null
) {
    companion object {
        fun create(page: Page?, dom: DOM?, node: NodeRef?, offset: OffsetD? = null): ClickableDOM? {
            if (node == null) return null
            if (page == null) return null
            if (dom == null) return null
            return ClickableDOM(page, dom, node, offset)
        }
    }

    suspend fun clickablePoint(): DescriptiveResult<PointD> {
        val contentQuads = kotlin.runCatching {
            // dom.getContentQuads(node.nodeId, node.backendNodeId, node.objectId)
            dom.getContentQuads(node.nodeId)
        }
            .onFailure { getLogger(this).warn("Failed to get content quads for node ${node.nodeId}", it) }
            .getOrNull()
        if (contentQuads == null) {
            // throw new Error('Node is either not clickable or not an HTMLElement');
            // return 'error:notvisible';
            return DescriptiveResult("error:notvisible")
        }

        val layoutMetrics = page.getLayoutMetrics()

        val viewport = layoutMetrics.cssLayoutViewport

        val dim = DimD(viewport.clientWidth.toDouble(), viewport.clientHeight.toDouble())
        val quads = contentQuads
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

    suspend fun isVisible(): Boolean {
        return clickablePoint().value != null
    }

    suspend fun boundingBox(): RectD? {
        val box = dom.getBoxModel(node.nodeId, node.backendNodeId, node.objectId)

        val quad = box.border.takeIf { it.isNotEmpty() } ?: return null

        val x = arrayOf(quad[0], quad[2], quad[4], quad[6]).minOrNull()
        val y = arrayOf(quad[1], quad[3], quad[5], quad[7]).minOrNull()
        val x2 = arrayOf(quad[0], quad[2], quad[4], quad[6]).maxOrNull()
        val y2 = arrayOf(quad[1], quad[3], quad[5], quad[7]).maxOrNull()

        if (x == null || y == null || x2 == null || y2 == null) {
            return null
        }

        val width = x2 - x
        val height = y2 - y

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
            area += (p1.x * p2.y - p2.x * p1.y) / 2

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
    // Track current pressed buttons bitfield. For left button only, we use 1 as Chromium does.
    private var buttonsState: Int = 0

    /**
     * Shortcut for `mouse.move`, `mouse.down` and `mouse.up`.
     * @param x - Horizontal position of the mouse.
     * @param y - Vertical position of the mouse.
     */
    suspend fun click(x: Double, y: Double, clickCount: Int = 1, modifiers: Int? = null, delayMillis: Long = 500) {
        moveTo(x, y)


        // Proper multi-click semantics: for each click i, use clickCount=i on press/release
        for (cc in 1..max(1, clickCount)) {
            down(x, y, cc, modifiers)
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            up(x, y, cc, modifiers)
            if (cc < clickCount && delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }

    suspend fun moveTo(point: PointD, steps: Int = 5, delayMillis: Long = 50) {
        moveTo(point.x, point.y, steps, delayMillis)
    }

    suspend fun moveTo(x: Double, y: Double, steps: Int = 1, delayMillis: Long = 50) {
        val fromX = currentX
        val fromY = currentY

        currentX = x
        currentY = y

        // Ensure we always emit a final move to the target, even when steps == 1
        val s = if (steps < 1) 1 else steps
        var i = 1
        while (i <= s) {
            val t = i.toDouble() / s
            val x1 = fromX + (currentX - fromX) * t
            val y1 = fromY + (currentY - fromY) * t

            cdpMoveTo(x1, y1)

            if (delayMillis > 0) {
                delay(delayMillis)
            }

            ++i
        }
    }

    private suspend fun cdpMoveTo(x: Double, y: Double) {
        input.dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_MOVED, x = x, y = y,
            modifiers = null, timestamp = null,
            button = null, // button
            buttons = buttonsState, // buttons
            clickCount = null,
            force = null, // force
            tangentialPressure = null,
            tiltX = null,
            tiltY = null,
            twist = null, // twist
            deltaX = null,
            deltaY = null,
            pointerType = null
        )
    }

    /**
     * Dispatches a `mousedown` event.
     */
    suspend fun down(clickCount: Int = 1, modifiers: Int? = null) {
        down(currentX, currentY, clickCount, modifiers)
    }

    /**
     * Dispatches a `mousedown` event.
     */
    suspend fun down(point: PointD, clickCount: Int = 1, modifiers: Int? = null) {
        down(point.x, point.y, clickCount, modifiers)
    }

    /**
     * Dispatches a `mousedown` event.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param modifiers Bit field representing pressed modifier keys. Alt=1, Ctrl=2, Meta/Command=4, Shift=8
     * * (default: 0).
     */
    suspend fun down(x: Double, y: Double, clickCount: Int = 1, modifiers: Int? = null) {
        // Update buttons bitfield to include left button (1)
        buttonsState = buttonsState or 1
        input.dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_PRESSED, x = x, y = y,
            button = MouseButton.LEFT,
            modifiers = modifiers, timestamp = null,
            buttons = buttonsState, // buttons after press
            clickCount = clickCount,
            force = 0.5, // force
            tangentialPressure = null,
            tiltX = null,
            tiltY = null,
            twist = null, // twist
            deltaX = null,
            deltaY = null,
            pointerType = null
        )
    }

    suspend fun up() {
        up(currentX, currentY)
    }

    suspend fun up(point: PointD) {
        up(point.x, point.y)
    }

    suspend fun up(x: Double, y: Double, clickCount: Int = 1, modifiers: Int? = null) {
        // Update buttons bitfield to reflect release of left button
        buttonsState = buttonsState and 1.inv()
        input.dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_RELEASED, x = x, y = y,
            button = MouseButton.LEFT,
            clickCount = clickCount,
            modifiers = modifiers,
            buttons = buttonsState
        )
    }

    suspend fun scroll(deltaX: Double = 0.0, deltaY: Double = 10.0) {
        input.dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_WHEEL, x = currentX, y = currentY,
            modifiers = null, timestamp = null,
            button = null, // button
            buttons = null, // buttons
            clickCount = null,
            force = null, // force
            tangentialPressure = null,
            tiltX = null, // tiltX
            tiltY = null, // tiltY
            twist = null, // twist
            deltaX = deltaX, // deltaX
            deltaY = deltaY, // deltaY
            pointerType = null
        )
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
        input.dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_WHEEL, x = x, y = y,
            modifiers = null, timestamp = null,
            button = null, // button
            buttons = null, // buttons
            clickCount = null,
            force = null, // force
            tangentialPressure = null,
            tiltX = null, // tiltX
            tiltY = null, // tiltY
            twist = null, // twist
            deltaX = deltaX, // deltaX
            deltaY = deltaY, // deltaY
            pointerType = null
        )
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

        try {
            moveTo(start, 5, 100)
            down(currentX, currentY)
            moveTo(target, 3, 500)
        } finally {
            // Always release button and disable interception
            runCatching { up() }
            runCatching { input.setInterceptDrags(false) }
        }

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
     * @param start - point to drag from
     * @param target - point to drop on
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
    private val pressedModifiers = mutableSetOf<String>()
    private val pressedKeys = mutableSetOf<String>()

    suspend fun type(text: String, delayMillis: Long) {
        text.forEach { char ->
            if (Character.isISOControl(char)) {
                press("$char", delayMillis)
            } else {
                input.insertText("$char")
            }

            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }

    suspend fun delete(n: Int, delayMillis: Long) {
        repeat(n) {
            press("Backspace", delayMillis)
        }
    }

    /**
     * Presses a key.
     * The key is specified as a string, which can be a single character, a key name, or a combination of both.
     * For example, 'a', 'A', 'KeyA', 'Enter', 'Shift+A', and 'Control+Shift+Tab' are all valid keys.
     * */
    suspend fun press(keyString: String, delayMillis: Long) {
        if (keyString.isEmpty()) {
            return
        }

        val tokens = splitKeyString(keyString).ifEmpty { return@press }

        val key = tokens.last()

        for (i in 0 until tokens.size - 1) {
            down(tokens[i])
        }

        try {
            down(key)
            delay(delayMillis)
        } finally {
            up(key)
        }

        for (i in tokens.size - 2 downTo 0) {
            up(tokens[i])
        }
    }

    suspend fun press(key: VirtualKey, delayMillis: Long) {
        try {
            down(key)
            delay(delayMillis.coerceAtLeast(20))
        } finally {
            up(key)
        }
    }

    suspend fun down(singleKey: String) {
        if (singleKey.isEmpty()) {
            return
        }

        val virtualKey = createVirtualKeyForSingleKeyString(singleKey)
        down(virtualKey)
    }

    suspend fun up(singleKey: String) {
        if (singleKey.isEmpty()) {
            return
        }

        val virtualKey = createVirtualKeyForSingleKeyString(singleKey)
        up(virtualKey)
    }

    suspend fun down(key: VirtualKey) {
        // From playwright:
        // {"type":"keyDown","modifiers":0,"windowsVirtualKeyCode":13,"code":"Enter","commands":[],"key":"Enter","text":"\r","unmodifiedText":"\r","autoRepeat":false,"location":0,"isKeypad":false},"sessionId":"45E0A2ABC64CE5ACDC8A98061CC4667B"}

        down(pressedModifiers, key)
    }

    suspend fun up(key: VirtualKey) {
        // {"type":"keyUp","modifiers":0,"key":"Enter","windowsVirtualKeyCode":13,"code":"Enter","location":0}

        up(pressedModifiers, key)
    }

    /**
     * Splits a key string into its components.
     * The key string can be a single character, a key name, or a combination of both.
     * For example, 'a', 'A', 'KeyA', 'Enter', 'Shift+A', and 'Control+Shift+Tab' are all valid key strings.
     * */
    fun splitKeyString(keyString: String): List<String> {
        val keys = mutableListOf<String>()
        val token = StringBuilder()

        keyString.forEach { char ->
            if (char == '+' && token.isNotEmpty()) {
                keys.add(token.toString().trim())
                token.clear()
            } else {
                token.append(char)
            }
        }

        if (token.isNotEmpty()) {
            keys.add(token.toString().trim())
        }

        return keys
    }

    fun createVirtualKeyForSingleKeyString(singleKey: String): VirtualKey {
        var virtualKey =
            VirtualKeyboard.KEYBOARD_LAYOUT[singleKey] ?: throw IllegalArgumentException("Unknown key: >$singleKey<")

        val shift = isShifted(virtualKey)
        val shifted = virtualKey.shifted
        virtualKey = if (shift && shifted != null) shifted else virtualKey

        return when {
            pressedModifiers.size > 1 -> virtualKey.copy(text = "")
            shift && pressedModifiers.size == 1 -> virtualKey.copy(text = "")
            else -> virtualKey
        }
    }

    private fun isShifted(key: VirtualKey): Boolean {
        return pressedModifiers.contains("Shift") && key.shifted != null
    }

    private fun toModifiersMask(modifiers: Set<String>): Int {
        var mask = 0
        if (modifiers.contains("Alt")) mask = mask or 1
        if (modifiers.contains("Control")) mask = mask or 2
        if (modifiers.contains("Meta")) mask = mask or 4
        if (modifiers.contains("Shift")) mask = mask or 8
        return mask
    }

    private suspend fun down(modifiers: Set<String>, key: VirtualKey) {
        // playwright format:
        // {"type":"keyDown","modifiers":0,"windowsVirtualKeyCode":13,"code":"Enter","commands":[],"key":"Enter","text":"\r","unmodifiedText":"\r","autoRepeat":false,"location":0,"isKeypad":false},"sessionId":"45E0A2ABC64CE5ACDC8A98061CC4667B"}

        val autoRepeat = pressedKeys.contains(key.code)
        pressedKeys.add(key.code)
        if (key.isModifier) {
            pressedModifiers.add(key.key)
        }

        val type = if (key.text.isEmpty()) DispatchKeyEventType.RAW_KEY_DOWN else DispatchKeyEventType.KEY_DOWN
        val commands = emptyList<String>()
        input.dispatchKeyEvent(
            type,
            modifiers = toModifiersMask(modifiers),
            windowsVirtualKeyCode = key.keyCodeWithoutLocation,
            code = key.code,
            commands = commands,
            key = key.key,
            text = key.text,
            unmodifiedText = key.text,
            location = key.location,
            isKeypad = key.location == KEYPAD_LOCATION,
            autoRepeat = autoRepeat,
        )
    }

    private suspend fun up(modifiers: Set<String>, key: VirtualKey) {
        // playwright format:
        // {"type":"keyUp","modifiers":0,"key":"Enter","windowsVirtualKeyCode":13,"code":"Enter","location":0}

        if (key.isModifier) {
            pressedModifiers.remove(key.key)
        }
        pressedKeys.remove(key.code)

        input.dispatchKeyEvent(
            type = DispatchKeyEventType.KEY_UP,
            modifiers = toModifiersMask(modifiers),
            key = key.key,
            windowsVirtualKeyCode = key.keyCodeWithoutLocation,
            code = key.code,
            location = key.location,
        )
    }
}

class EmulationHandler(
    private val pageAPI: Page?,
    private val domAPI: DOM?,
    private val keyboard: Keyboard?,
    private val mouse: Mouse?
) {
    private fun modifierMaskForKeyString(key: String): Int {
        return when (key.trim().lowercase()) {
            "alt" -> 1
            "control", "ctrl" -> 2
            "meta", "command", "cmd", "win", "super" -> 4
            "shift" -> 8
            else -> 0
        }
    }

    suspend fun click(
        node: NodeRef, count: Int, position: String = "center", modifier: String? = null,
        delayMillis: Long = 100
    ) {
        val deltaX = 4.0 + Random.nextInt(4)
        val deltaY = 4.0
        val offset = OffsetD(deltaX, deltaY)
        val minDeltaX = 2.0

        val p = pageAPI
        val d = domAPI
        if (p == null || d == null) {
            return
        }

        val clickableDOM = ClickableDOM(p, d, node, offset)
        val point = clickableDOM.clickablePoint().value ?: return
        val box = runCatching { clickableDOM.boundingBox() }
            .onFailure { getLogger(this).warn("clickable bounding box failed", it) }
            .getOrNull() ?: return

        val width = box.width
        // if it's an input element, we should click on the right side of the element to activate the input box,
        // so the cursor is at the tail of the text
        if (width > 0.0) {
            var offsetX = when (position) {
                "left" -> 0.0 + deltaX
                "right" -> width - deltaX
                else -> width / 2 + deltaX
            }
            offsetX = offsetX.coerceAtMost((width - minDeltaX).coerceAtLeast(0.0)).coerceAtLeast(minDeltaX)
            // Base X on the element's left edge to avoid overshooting from a center-based point
            point.x = box.x + offsetX
        }

        var modifiers = 0
        var normModifier: KeyboardModifier? = null
        var virtualKey: VirtualKey? = null
        if (modifier != null) {
            normModifier = KeyboardModifier.entries.find { it.name.equals(modifier, ignoreCase = true) }
            if (normModifier != null) {
                virtualKey = keyboard?.createVirtualKeyForSingleKeyString(normModifier.name)
                if (virtualKey != null) {
                    if (virtualKey.isModifier) {
                        // Use CDP-compliant modifier bitmask for mouse events
                        modifiers = modifierMaskForKeyString(normModifier.name)
                    }
                    keyboard?.down(virtualKey)
                }
            }
        }

        mouse?.click(point.x, point.y, count, modifiers = modifiers, delayMillis = delayMillis)

        if (virtualKey != null) {
            keyboard?.up(virtualKey)
        }
    }
}
