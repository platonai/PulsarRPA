package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.common.DescriptiveResult
import ai.platon.pulsar.common.io.VirtualKey
import ai.platon.pulsar.common.io.VirtualKeyboard
import ai.platon.pulsar.common.io.VirtualKeyboard.KEYPAD_LOCATION
import ai.platon.pulsar.common.math.geometric.DimD
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.common.math.geometric.PointD
import ai.platon.pulsar.common.math.geometric.RectD
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import com.github.kklisura.cdt.protocol.v2023.commands.DOM
import com.github.kklisura.cdt.protocol.v2023.commands.Page
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName
import com.github.kklisura.cdt.protocol.v2023.types.input.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.math3.util.Precision
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
//        val box = dom.getBoxModel(nodeId, null, null)

        val box = dom.runCatching { getBoxModel(nodeId, null, null) }.getOrNull() ?: return null

        val quad = box.border.takeIf { it.isNotEmpty() } ?: return null

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

//        down(x, y, clickCount)
//        if (delayMillis > 0) {
//            delay(delayMillis)
//        }
//        up(x, y, clickCount)

        for (cc in 1..clickCount) {
            down(x, y, cc)
            delay(delayMillis)
            up(x, y, cc)
            if (cc < clickCount) {
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
     * if input.dispatchMouseEvent(MOUSE_MOVED) not work, try Robot.mouseMove.
     * */
    private fun chromeMoveTo(x: Double, y: Double) {
        dispatchMouseEvent(
            type = DispatchMouseEventType.MOUSE_MOVED, x = x, y = y,
            modifiers = null, timestamp = null,
            button = null, // button
            buttons = null, // buttons
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
        // TODO: handle modifiers
        withContext(Dispatchers.IO) {
            dispatchMouseEvent(
                type = DispatchMouseEventType.MOUSE_PRESSED, x = x, y = y,
                modifiers = null, timestamp = null,
                button = MouseButton.LEFT,
                buttons = null, // buttons
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
    }

    suspend fun up() {
        up(currentX, currentY)
    }

    suspend fun up(point: PointD) {
        up(point.x, point.y)
    }

    suspend fun up(x: Double, y: Double, clickCount: Int = 1) {
        // TODO: handle modifiers
        withContext(Dispatchers.IO) {
            dispatchMouseEvent(
                type = DispatchMouseEventType.MOUSE_RELEASED, x = x, y = y,
                button = MouseButton.LEFT,
                clickCount = clickCount
            )
        }
    }

    suspend fun scroll(deltaX: Double = 0.0, deltaY: Double = 10.0) {
        withContext(Dispatchers.IO) {
            dispatchMouseEvent(
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
            dispatchMouseEvent(
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

    private fun dispatchMouseEvent(
        @ParamName("type") type: DispatchMouseEventType,
        @ParamName("x") x: Double? = null,
        @ParamName("y") y: Double? = null,
        @Optional @ParamName("modifiers") modifiers: Int? = null,
        @Optional @ParamName("timestamp") timestamp: Double? = null,
        @Optional @ParamName("button") button: MouseButton? = null,
        @Optional @ParamName("buttons") buttons: Int? = null,
        @Optional @ParamName("clickCount") clickCount: Int? = null,
        @Experimental @Optional @ParamName("force") force: Double? = null,
        @Experimental @Optional @ParamName("tangentialPressure") tangentialPressure: Double? = null,
        @Experimental @Optional @ParamName("tiltX") tiltX: Int? = null,
        @Experimental @Optional @ParamName("tiltY") tiltY: Int? = null,
        @Experimental @Optional @ParamName("twist") twist: Int? = null,
        @Optional @ParamName("deltaX") deltaX: Double? = null,
        @Optional @ParamName("deltaY") deltaY: Double? = null,
        @Optional @ParamName("pointerType") pointerType: DispatchMouseEventPointerType? = null
    ) {
        input.dispatchMouseEvent(
            type,
            x,
            y,
            modifiers,
            timestamp,
            button,
            buttons,
            clickCount,
            force,
            tangentialPressure,
            tiltX,
            tiltY,
            twist,
            deltaX,
            deltaY,
            pointerType
        )
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

        down(key)
        delay(delayMillis)
        up(key)

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
    private fun splitKeyString(keyString: String): List<String> {
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

    private fun createVirtualKeyForSingleKeyString(singleKey: String): VirtualKey {
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
        withContext(Dispatchers.IO) {
            dispatchKeyEvent1(
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
    }

    private suspend fun up(modifiers: Set<String>, key: VirtualKey) {
        // playwright format:
        // {"type":"keyUp","modifiers":0,"key":"Enter","windowsVirtualKeyCode":13,"code":"Enter","location":0}

        if (key.isModifier) {
            pressedModifiers.remove(key.key)
        }
        pressedKeys.remove(key.code)

        withContext(Dispatchers.IO) {
            dispatchKeyEvent1(
                DispatchKeyEventType.KEY_UP,
                modifiers = toModifiersMask(modifiers),
                key = key.key,
                windowsVirtualKeyCode = key.keyCodeWithoutLocation,
                code = key.code,
                location = key.location,
            )
        }
    }

    /**
     * Dispatches a key event to the page.
     *
     * @param type Type of the key event.
     * @param modifiers Bit field representing pressed modifier keys. Alt=1, Ctrl=2, Meta/Command=4,
     *     Shift=8 (default: 0).
     * @param timestamp Time at which the event occurred.
     * @param text Text as generated by processing a virtual key code with a keyboard layout. Not
     *     needed for `keyUp` and `rawKeyDown` events (default: "")
     * @param unmodifiedText Text that would have been generated by the keyboard if no modifiers were
     *     pressed (except for shift). Useful for shortcut (accelerator) key handling (default: "").
     * @param keyIdentifier Unique key identifier (e.g., 'U+0041') (default: "").
     * @param code Unique DOM defined string value for each physical key (e.g., 'KeyA') (default: "").
     * @param key Unique DOM defined string value describing the meaning of the key in the context of
     *     active modifiers, keyboard layout, etc. (e.g., 'AltGr') (default: "").
     * @param windowsVirtualKeyCode Windows virtual key code (default: 0).
     * @param nativeVirtualKeyCode Native virtual key code (default: 0).
     * @param autoRepeat Whether the event was generated from auto repeat (default: false).
     * @param isKeypad Whether the event was generated from the keypad (default: false).
     * @param isSystemKey Whether the event was a system key event (default: false).
     * @param location Whether the event was from the left or right side of the keyboard. 1=Left,
     *     2=Right (default: 0).
     * @param commands Editing commands to send with the key event (e.g., 'selectAll') (default: []).
     *     These are related to but not equal the command names used in `document.execCommand` and
     *     NSStandardKeyBindingResponding. See
     *     https://source.chromium.org/chromium/chromium/src/+/main:third_party/blink/renderer/core/editing/commands/editor_command_names.h
     *     for valid command names.
     */
    private fun dispatchKeyEvent1(
        @ParamName("type") type: DispatchKeyEventType? = null,
        @Optional @ParamName("modifiers") modifiers: Int? = null,
        @Optional @ParamName("timestamp") timestamp: Double? = null,
        @Optional @ParamName("text") text: String? = null,
        @Optional @ParamName("unmodifiedText") unmodifiedText: String? = null,
        @Optional @ParamName("keyIdentifier") keyIdentifier: String? = null,
        @Optional @ParamName("code") code: String? = null,
        @Optional @ParamName("key") key: String? = null,
        @Optional @ParamName("windowsVirtualKeyCode") windowsVirtualKeyCode: Int? = null,
        @Optional @ParamName("nativeVirtualKeyCode") nativeVirtualKeyCode: Int? = null,
        @Optional @ParamName("autoRepeat") autoRepeat: Boolean? = null,
        @Optional @ParamName("isKeypad") isKeypad: Boolean? = null,
        @Optional @ParamName("isSystemKey") isSystemKey: Boolean? = null,
        @Optional @ParamName("location") location: Int? = null,
        @Experimental @Optional @ParamName("commands") commands: List<String?>? = null
    ) {
        input.dispatchKeyEvent(
            type, modifiers, timestamp, text, unmodifiedText, keyIdentifier, code, key, windowsVirtualKeyCode,
            nativeVirtualKeyCode, autoRepeat, isKeypad, isSystemKey, location, commands
        )
    }
}
