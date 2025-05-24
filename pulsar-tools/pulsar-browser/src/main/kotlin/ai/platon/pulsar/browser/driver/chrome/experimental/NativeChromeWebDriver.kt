package ai.platon.pulsar.browser.driver.chrome.experimental

import ai.platon.pulsar.common.urls.Hyperlink
import com.github.kklisura.cdt.protocol.v2023.ChromeDevTools
import com.github.kklisura.cdt.protocol.v2023.commands.DOM
import com.github.kklisura.cdt.protocol.v2023.commands.Page
import com.github.kklisura.cdt.protocol.v2023.commands.Runtime
import com.google.common.annotations.Beta
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Beta
class NativeChromeWebDriver(
    private val devTools: ChromeDevTools
) : NativeWebDriver {

    private val mutex = Mutex()
    private val domAPI: DOM = devTools.getDOM()
    private val pageAPI: Page = devTools.getPage()
    private val runtimeAPI: Runtime = devTools.getRuntime()

    init {
        domAPI.enable()
        pageAPI.enable()
    }

    override suspend fun waitForNodeId(nodeId: Int, timeoutMillis: Long): Long {
        val startTime = System.currentTimeMillis()
        val deadline = startTime + timeoutMillis

        while (System.currentTimeMillis() < deadline) {
            if (exists(nodeId)) {
                return deadline - System.currentTimeMillis()
            }
            delay(100) // Check every 100ms
        }

        return 0
    }

    override suspend fun exists(nodeId: Int): Boolean {
        return try {
            domAPI.describeNode(nodeId, null, null, null, null) != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isHidden(nodeId: Int): Boolean {
        return !isVisible(nodeId)
    }

    override suspend fun isVisible(nodeId: Int): Boolean {
        val script = """
            function isVisible(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                const style = window.getComputedStyle(node);
                if (style.visibility === 'hidden' || style.display === 'none' || style.opacity === '0') {
                    return false;
                }
                
                const rect = node.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0;
            }
            isVisible(${nodeId});
        """.trimIndent()

        val result = evaluateScript(script)
        return result?.toString() == "true"
    }

    override suspend fun visible(nodeId: Int): Boolean = isVisible(nodeId)

    override suspend fun isChecked(nodeId: Int): Boolean {
        val script = """
            function isChecked(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                return node ? node.checked : false;
            }
            isChecked(${nodeId});
        """.trimIndent()

        val result = evaluateScript(script)
        return result?.toString() == "true"
    }

    override suspend fun bringToFront() {
        pageAPI.bringToFront()
    }

    override suspend fun focus(nodeId: Int) {
        domAPI.focus(nodeId, null, null)
    }

    override suspend fun type(nodeId: Int, text: String) {
        focus(nodeId)

        // Break text into individual characters and send as keystrokes
        text.forEach { char ->
            val script = """
                function typeChar(nodeId, char) {
                    const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                    if (!node) return false;
                    
                    const event = new KeyboardEvent('keydown', { key: '${char}' });
                    node.dispatchEvent(event);
                    node.value += '${char}';
                    const inputEvent = new Event('input', { bubbles: true });
                    node.dispatchEvent(inputEvent);
                    return true;
                }
                typeChar(${nodeId}, '${char}');
            """.trimIndent()

            evaluateScript(script)
            delay(20) // Small delay between characters
        }
    }

    override suspend fun fill(nodeId: Int, text: String) {
        val script = """
            function fillText(nodeId, text) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                node.value = '${text.replace("'", "\\'")}';
                node.dispatchEvent(new Event('input', { bubbles: true }));
                node.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
            }
            fillText(${nodeId}, '${text.replace("'", "\\'")}');
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun press(nodeId: Int, key: String) {
        focus(nodeId)

        val script = """
            function pressKey(nodeId, key) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                const keyEvent = new KeyboardEvent('keydown', { key: '${key}' });
                node.dispatchEvent(keyEvent);
                return true;
            }
            pressKey(${nodeId}, '${key}');
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun click(nodeId: Int, count: Int) {
        for (i in 0 until count) {
            domAPI.focus(nodeId, null, null)
            val script = """
                function clickElement(nodeId) {
                    const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                    if (!node) return false;
                    
                    node.click();
                    return true;
                }
                clickElement(${nodeId});
            """.trimIndent()

            evaluateScript(script)

            if (i < count - 1) {
                delay(100) // Delay between clicks
            }
        }
    }

    override suspend fun clickTextMatches(nodeId: Int, pattern: String, count: Int) {
        val script = """
            function clickTextMatches(nodeId, pattern) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                const regex = new RegExp('${pattern.replace("'", "\\'")}');
                
                for (const node of nodes) {
                    if (regex.test(node.textContent)) {
                        node.click();
                        return true;
                    }
                }
                return false;
            }
            clickTextMatches(${nodeId}, '${pattern.replace("'", "\\'")}');
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            if (i < count - 1) {
                delay(100)
            }
        }
    }

    override suspend fun clickMatches(nodeId: Int, attrName: String, pattern: String, count: Int) {
        val script = """
            function clickMatches(nodeId, attrName, pattern) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                const regex = new RegExp('${pattern.replace("'", "\\'")}');
                
                for (const node of nodes) {
                    if (regex.test(node.getAttribute('${attrName}'))) {
                        node.click();
                        return true;
                    }
                }
                return false;
            }
            clickMatches(${nodeId}, '${attrName}', '${pattern.replace("'", "\\'")}');
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            if (i < count - 1) {
                delay(100)
            }
        }
    }

    override suspend fun clickNthAnchor(n: Int, rootNodeId: Int): String? {
        val script = """
            function clickNthAnchor(n, rootNodeId) {
                const root = document.querySelector(`[data-pulsar-node-id="${rootNodeId}"]`) || document.body;
                const anchors = root.querySelectorAll('a');
                
                if (n >= 0 && n < anchors.length) {
                    const href = anchors[n].href;
                    anchors[n].click();
                    return href;
                }
                return null;
            }
            clickNthAnchor(${n}, ${rootNodeId});
        """.trimIndent()

        return evaluateScript(script)?.toString()
    }

    override suspend fun check(nodeId: Int) {
        val script = """
            function checkElement(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node || !('checked' in node)) return false;
                
                if (!node.checked) {
                    node.checked = true;
                    node.dispatchEvent(new Event('change', { bubbles: true }));
                }
                return true;
            }
            checkElement(${nodeId});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun uncheck(nodeId: Int) {
        val script = """
            function uncheckElement(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node || !('checked' in node)) return false;
                
                if (node.checked) {
                    node.checked = false;
                    node.dispatchEvent(new Event('change', { bubbles: true }));
                }
                return true;
            }
            uncheckElement(${nodeId});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun scrollTo(nodeId: Int) {
        val script = """
            function scrollToElement(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                node.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return true;
            }
            scrollToElement(${nodeId});
        """.trimIndent()

        evaluateScript(script)
        delay(500) // Give time for smooth scrolling
    }

    override suspend fun scrollDown(count: Int) {
        val script = """
            function scrollDownPage() {
                window.scrollBy(0, window.innerHeight * 0.8);
                return true;
            }
            scrollDownPage();
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            delay(300) // Give time between scrolls
        }
    }

    override suspend fun scrollUp(count: Int) {
        val script = """
            function scrollUpPage() {
                window.scrollBy(0, -window.innerHeight * 0.8);
                return true;
            }
            scrollUpPage();
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            delay(300) // Give time between scrolls
        }
    }

    override suspend fun scrollToTop() {
        val script = """
            function scrollToTop() {
                window.scrollTo(0, 0);
                return true;
            }
            scrollToTop();
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun scrollToBottom() {
        val script = """
            function scrollToBottom() {
                window.scrollTo(0, document.body.scrollHeight);
                return true;
            }
            scrollToBottom();
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun scrollToMiddle(ratio: Double) {
        val script = """
            function scrollToMiddle(ratio) {
                const targetY = document.body.scrollHeight * ratio;
                window.scrollTo(0, targetY);
                return true;
            }
            scrollToMiddle(${ratio});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun scrollToScreen(screenNumber: Double) {
        val script = """
            function scrollToScreen(screenNumber) {
                const targetY = window.innerHeight * screenNumber;
                window.scrollTo(0, targetY);
                return true;
            }
            scrollToScreen(${screenNumber});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun mouseWheelDown(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        val script = """
            function mouseWheelDown(deltaX, deltaY) {
                window.dispatchEvent(new WheelEvent('wheel', { 
                    deltaX: ${deltaX},
                    deltaY: ${deltaY},
                    bubbles: true
                }));
                return true;
            }
            mouseWheelDown(${deltaX}, ${deltaY});
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }

    override suspend fun mouseWheelUp(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) {
        val script = """
            function mouseWheelUp(deltaX, deltaY) {
                window.dispatchEvent(new WheelEvent('wheel', { 
                    deltaX: ${deltaX},
                    deltaY: ${deltaY},
                    bubbles: true
                }));
                return true;
            }
            mouseWheelUp(${deltaX}, ${deltaY});
        """.trimIndent()

        for (i in 0 until count) {
            evaluateScript(script)
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }

    override suspend fun moveMouseTo(x: Double, y: Double) {
        val script = """
            function moveMouseTo(x, y) {
                const event = new MouseEvent('mousemove', {
                    clientX: ${x},
                    clientY: ${y},
                    bubbles: true
                });
                document.elementFromPoint(${x}, ${y})?.dispatchEvent(event);
                return true;
            }
            moveMouseTo(${x}, ${y});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun moveMouseTo(nodeId: Int, deltaX: Int, deltaY: Int) {
        val script = """
            function moveMouseToElement(nodeId, deltaX, deltaY) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                const rect = node.getBoundingClientRect();
                const x = rect.left + deltaX;
                const y = rect.top + deltaY;
                
                const event = new MouseEvent('mousemove', {
                    clientX: x,
                    clientY: y,
                    bubbles: true
                });
                node.dispatchEvent(event);
                return true;
            }
            moveMouseToElement(${nodeId}, ${deltaX}, ${deltaY});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun dragAndDrop(nodeId: Int, deltaX: Int, deltaY: Int) {
        val script = """
            function dragAndDrop(nodeId, deltaX, deltaY) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                const rect = node.getBoundingClientRect();
                const startX = rect.left + rect.width / 2;
                const startY = rect.top + rect.height / 2;
                const endX = startX + deltaX;
                const endY = startY + deltaY;
                
                // Mouse down at start position
                node.dispatchEvent(new MouseEvent('mousedown', {
                    clientX: startX,
                    clientY: startY,
                    bubbles: true
                }));
                
                // Mouse move to end position
                document.dispatchEvent(new MouseEvent('mousemove', {
                    clientX: endX,
                    clientY: endY,
                    bubbles: true
                }));
                
                // Mouse up at end position
                document.dispatchEvent(new MouseEvent('mouseup', {
                    clientX: endX,
                    clientY: endY,
                    bubbles: true
                }));
                
                return true;
            }
            dragAndDrop(${nodeId}, ${deltaX}, ${deltaY});
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun outerHTML(): String? {
        return domAPI.getOuterHTML(null, null, null)
    }

    override suspend fun outerHTML(nodeId: Int): String? {
        return domAPI.getOuterHTML(nodeId, null, null)
    }

    override suspend fun selectFirstTextOrNull(nodeId: Int, selector: String): String? {
        val script = """
            function selectFirstText(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                return node ? node.textContent : null;
            }
            selectFirstText(${nodeId});
        """.trimIndent()

        return evaluateScript(script)?.toString()
    }

    override suspend fun selectTextAll(nodeId: Int, selector: String): List<String> {
        domAPI.querySelectorAll(nodeId, selector).map {

        }

        return listOf()
    }

    override suspend fun selectFirstAttributeOrNull(nodeId: Int, selector: String, attrName: String): String? {
        val nodeId0 = domAPI.querySelector(nodeId, selector) ?: return null

        val attribute = domAPI.getAttributes(nodeId0).zipWithNext().firstOrNull { it.first == attrName }

        return attribute?.second
    }

    override suspend fun selectAttributes(nodeId: Int): Map<String, String> {
        val script = """
            function selectAttributes(nodeId) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return {};
                
                const attrs = {};
                for (const attr of node.attributes) {
                    attrs[attr.name] = attr.value;
                }
                return attrs;
            }
            selectAttributes(${nodeId});
        """.trimIndent()

        val result = evaluateScript(script)
        if (result is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return result as Map<String, String>
        }
        return emptyMap()
    }

    override suspend fun selectAttributeAll(
        nodeId: Int,
        attrName: String,
        start: Int,
        limit: Int
    ): List<String> {
        val script = """
            function selectAttributeAll(nodeId, attrName, start, limit) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                return Array.from(nodes)
                    .slice(${start}, ${start + limit})
                    .map(node => node.getAttribute('${attrName}'))
                    .filter(attr => attr !== null);
            }
            selectAttributeAll(${nodeId}, '${attrName}', ${start}, ${limit});
        """.trimIndent()

        val result = evaluateScript(script)
        if (result is List<*>) {
            return result.mapNotNull { it?.toString() }
        }
        return emptyList()
    }

    override suspend fun setAttribute(nodeId: Int, attrName: String, attrValue: String) {
        domAPI.setAttributeValue(nodeId, attrName, attrValue)
    }

    override suspend fun setAttributeAll(nodeId: Int, attrName: String, attrValue: String) {
        val script = """
            function setAttributeAll(nodeId, attrName, attrValue) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                nodes.forEach(node => {
                    node.setAttribute('${attrName}', '${attrValue.replace("'", "\\'")}');
                });
                return nodes.length;
            }
            setAttributeAll(${nodeId}, '${attrName}', '${attrValue.replace("'", "\\'")}');
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun selectFirstPropertyValueOrNull(nodeId: Int, propName: String): String? {
        val script = """
            function selectFirstPropertyValue(nodeId, propName) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                return node ? node['${propName}'] : null;
            }
            selectFirstPropertyValue(${nodeId}, '${propName}');
        """.trimIndent()

        return evaluateScript(script)?.toString()
    }

    override suspend fun selectPropertyValueAll(
        nodeId: Int,
        propName: String,
        start: Int,
        limit: Int
    ): List<String> {
        val script = """
            function selectPropertyValueAll(nodeId, propName, start, limit) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                return Array.from(nodes)
                    .slice(${start}, ${start + limit})
                    .map(node => node['${propName}'])
                    .filter(prop => prop !== undefined)
                    .map(prop => String(prop));
            }
            selectPropertyValueAll(${nodeId}, '${propName}', ${start}, ${limit});
        """.trimIndent()

        val result = evaluateScript(script)
        if (result is List<*>) {
            return result.mapNotNull { it?.toString() }
        }
        return emptyList()
    }

    override suspend fun setProperty(nodeId: Int, propName: String, propValue: String) {
        val script = """
            function setProperty(nodeId, propName, propValue) {
                const node = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`);
                if (!node) return false;
                
                node['${propName}'] = '${propValue.replace("'", "\\'")}';
                return true;
            }
            setProperty(${nodeId}, '${propName}', '${propValue.replace("'", "\\'")}');
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun setPropertyAll(nodeId: Int, propName: String, propValue: String) {
        val script = """
            function setPropertyAll(nodeId, propName, propValue) {
                const nodes = document.querySelectorAll(`[data-pulsar-node-id="${nodeId}"]`);
                nodes.forEach(node => {
                    node['${propName}'] = '${propValue.replace("'", "\\'")}';
                });
                return nodes.length;
            }
            setPropertyAll(${nodeId}, '${propName}', '${propValue.replace("'", "\\'")}');
        """.trimIndent()

        evaluateScript(script)
    }

    override suspend fun selectHyperlinks(nodeId: Int, offset: Int, limit: Int): List<Hyperlink> {
        val script = """
            function selectHyperlinks(nodeId, offset, limit) {
                const root = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`) || document;
                const links = root.querySelectorAll('a[href]');
                
                return Array.from(links)
                    .slice(${offset - 1}, ${offset - 1 + limit})
                    .map(link => ({
                        url: link.href,
                        anchor: link.textContent.trim(),
                        title: link.getAttribute('title') || ''
                    }));
            }
            selectHyperlinks(${nodeId}, ${offset}, ${limit});
        """.trimIndent()

        val result = evaluateScript(script)
        if (result is List<*>) {
            return result.mapNotNull { item ->
                if (item is Map<*, *>) {
                    val url = item["url"]?.toString() ?: return@mapNotNull null
                    val anchor = item["anchor"]?.toString() ?: ""

                    Hyperlink(url, anchor)
                } else null
            }
        }
        return emptyList()
    }

    override suspend fun selectImages(nodeId: Int, offset: Int, limit: Int): List<String> {
        val script = """
            function selectImages(nodeId, offset, limit) {
                const root = document.querySelector(`[data-pulsar-node-id="${nodeId}"]`) || document;
                const images = root.querySelectorAll('img[src]');
                
                return Array.from(images)
                    .slice(${offset - 1}, ${offset - 1 + limit})
                    .map(img => img.src)
                    .filter(src => src && !src.startsWith('data:'));
            }
            selectImages(${nodeId}, ${offset}, ${limit});
        """.trimIndent()

        val result = evaluateScript(script)
        if (result is List<*>) {
            return result.mapNotNull { it?.toString() }
        }
        return emptyList()
    }

    // Helper method to evaluate JavaScript in the page context
    private suspend fun evaluateScript(script: String): Any? {
        return mutex.withLock {
            try {
                val result = runtimeAPI.evaluate(script)
                result.result.value
            } catch (e: Exception) {
                null
            }
        }
    }
}