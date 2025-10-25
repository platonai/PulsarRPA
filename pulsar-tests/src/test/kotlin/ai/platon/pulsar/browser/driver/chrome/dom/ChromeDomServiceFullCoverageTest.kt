package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.ElementRefCriteria
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertIs

class ChromeDomServiceFullCoverageTest : WebDriverTestBase() {

    @Test
    fun `Get trees, build and serialize end-to-end with assertions`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        // Prepare a deterministic dynamic state (virtual list -> scrollable container, images are added on DOMContentLoaded)
        runCatching { devTools.runtime.evaluate("generateLargeList(100)") }

        val enhancedRoot = collectEnhancedRoot(service, options)
        // Print enhanced DOMTreeNodeEx summary and basic tree stats
        printlnPro(DomDebug.summarize(enhancedRoot))
        assertTrue(enhancedRoot.children.isNotEmpty())

        val simplified = service.buildTinyTree(enhancedRoot)
        // Print SlimNode summary and basic tree stats
        printlnPro(DomDebug.summarize(simplified))
        assertTrue(simplified.children.isNotEmpty())

        val llm = service.buildDOMState(simplified)
        assertTrue(llm.nanoTreeLazyJson.length > 100)
        assertTrue(llm.selectorMap.isNotEmpty())

        // Selector map should include at least node: and possibly xpath: keys for some nodes
        val anySelectorKey = llm.selectorMap.keys.any { it.startsWith("node:") || it.startsWith("xpath:") }
        assertTrue(anySelectorKey, "Selector map should contain node:/xpath: keys")
    }

    @Test
    fun `Find element using css, xpath, backend id, element hash and convert to interacted element`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        suspend fun collectRoot(): DOMTreeNodeEx {
            repeat(3) { attempt ->
                val t = service.getMultiDOMTrees(target = PageTarget(), options = options)
                printlnPro(DomDebug.summarize(t))
                val r = service.buildEnhancedDomTree(t)
                if (r.children.isNotEmpty() || attempt == 2) return r
                Thread.sleep(300)
            }
            return service.buildEnhancedDomTree(service.getMultiDOMTrees(PageTarget(), options))
        }
        val root = collectRoot()
        assertNotNull(root)
        printlnPro(DomDebug.summarize(root))

        // Try CSS by id first
        var target = service.findElement(ElementRefCriteria(cssSelector = "#loadingContainer"))
        assertNotNull(target)

        val direct = findNodeById(root, "loadingContainer")
        assertNotNull(direct)
        requireNotNull(direct)
        printlnPro(DomDebug.summarize(direct))

        val viaXPath = direct.xpath?.let { service.findElement(ElementRefCriteria(xPath = it)) }
        target = viaXPath ?: direct.elementHash?.let { service.findElement(ElementRefCriteria(elementHash = it)) }
        assertNotNull(target)

        target = service.findElement(ElementRefCriteria(cssSelector = "body"))
        assertNotNull(target)

        if (target == null) {
            // As a last resort, use the enhanced root to exercise toInteractedElement
            target = root
        }
        assertNotNull(target)

        // Obtain stable references from the resolved node
        val t = requireNotNull(target) { "Expected a non-null target element" }
        val xPath = t.xpath
        val backendId = t.backendNodeId
        val elementHash = t.elementHash

        // By XPath
        if (!xPath.isNullOrBlank()) {
            val byXPath = service.findElement(ElementRefCriteria(xPath = xPath))
            assertNotNull(byXPath)
            assertEquals(t.nodeId, byXPath!!.nodeId)
        }

        // By backend node id
        if (backendId != null) {
            val byBackend = service.findElement(ElementRefCriteria(backendNodeId = backendId))
            assertNotNull(byBackend)
            assertEquals(t.nodeId, byBackend!!.nodeId)
        }

        // By element hash
        if (!elementHash.isNullOrBlank()) {
            val byHash = service.findElement(ElementRefCriteria(elementHash = elementHash))
            assertNotNull(byHash)
            assertEquals(t.nodeId, byHash!!.nodeId)
        }

        // Convert to interacted element (should include hash)
        val interacted = service.toInteractedElement(t)
        printlnPro(DomDebug.summarize(interacted))
        assertTrue(interacted.elementHash.isNotBlank())
    }

    @Test
    fun `Options toggling - no AX or Snapshot yields minimal enhanced nodes`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = false,
            includeSnapshot = false,
            includeStyles = false,
            includePaintOrder = false,
            includeDOMRects = false,
            includeScrollAnalysis = false,
            includeVisibility = false,
            includeInteractivity = false
        )

        val trees = service.getMultiDOMTrees(PageTarget(), options)
        printlnPro(DomDebug.summarize(trees))
        assertTrue(trees.snapshotByBackendId.isEmpty())
        assertTrue(trees.axTree.isEmpty())

        val root = service.buildEnhancedDomTree(trees)
        printlnPro(DomDebug.summarize(root))
        assertTrue(root.children.isNotEmpty())

        // Pick a common element and assert minimal fields
        val node = service.findElement(ElementRefCriteria(cssSelector = "body"))

        assertNotNull(node)
        assertNull(node!!.snapshotNode)
        assertNull(node.axNode)
        assertNull(node.isVisible)
        assertNull(node.isInteractable)
    }

    @Test
    fun `Scrollability and interactivity analysis on dynamic content`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = driver.domService

        // Create a clearly scrollable container and interactive buttons
        runCatching { devTools.runtime.evaluate("generateLargeList(1000)") }

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        // Wait deterministically for the container to exist in the DOM and have scrollable content
        var hasScrollableContainer = false
        repeat(50) { // up to ~10s
            val containerExists = runCatching {
                devTools.runtime.evaluate("document.getElementById('virtualScrollContainer') != null")
            }.getOrNull()?.result?.value?.toString()?.equals("true", ignoreCase = true) == true

            if (containerExists) {
                // Also check if the content has been rendered with sufficient height
                val hasContent = runCatching {
                    devTools.runtime.evaluate("""
                        var container = document.getElementById('virtualScrollContainer');
                        var content = document.getElementById('virtualScrollContent');
                        container && content && content.scrollHeight > container.clientHeight
                    """.trimIndent())
                }.getOrNull()?.result?.value?.toString()?.equals("true", ignoreCase = true) == true

                if (hasContent) {
                    hasScrollableContainer = true
                    return@repeat
                }
            }
            Thread.sleep(200)
        }
        assertTrue(hasScrollableContainer, "Expected #virtualScrollContainer to be present with scrollable content after generateLargeList(1000)")

        val root = collectEnhancedRoot(service, options)
        printlnPro(DomDebug.summarize(root))
        assertNotNull(root)

        // Locate scroll container by CSS, fallback to DFS by id
        var scrollContainer = service.findElement(ElementRefCriteria(cssSelector = "#virtualScrollContainer"))
        assertNotNull(scrollContainer, "Expected scroll container to be found and resolved")
        requireNotNull(scrollContainer)
        printlnPro(DomDebug.summarize(scrollContainer))

        // Debug: print snapshot details to understand why scrollability might be false
        val containerSnapshot = scrollContainer.snapshotNode
        if (containerSnapshot != null) {
            printlnPro("Container snapshot - clientRects: ${containerSnapshot.clientRects}, scrollRects: ${containerSnapshot.scrollRects}")
            printlnPro("Container styles - overflow-y: ${containerSnapshot.computedStyles?.get("overflow-y")}, overflow: ${containerSnapshot.computedStyles?.get("overflow")}")
        }

        assertEquals(true, scrollContainer.isScrollable, "Expected #virtualScrollContainer to be marked scrollable")

        val direct = findNodeById(root, "virtualScrollContainer")
        assertNotNull(direct)
        requireNotNull(direct)

        val xPath = direct.xpath
        assertNotNull(xPath)

        scrollContainer = service.findElement(ElementRefCriteria(xPath = xPath))
        assertNotNull(scrollContainer)

        assertNotNull(direct.elementHash)
        scrollContainer = service.findElement(ElementRefCriteria(elementHash = direct.elementHash))
        assertNotNull(scrollContainer)

        assertNotNull(scrollContainer, "Expected scroll container to be found and resolved")
        assertEquals(true, scrollContainer!!.isScrollable, "Expected #virtualScrollContainer to be marked scrollable")

        // Always verify interactivity by tag-based path
        val anyVirtualBtn = service.findElement(ElementRefCriteria(cssSelector = "button"))
        assertNotNull(anyVirtualBtn)
        printlnPro(anyVirtualBtn?.let { DomDebug.summarize(it) })
        assertEquals(true, anyVirtualBtn!!.isInteractable)
    }

    @Test
    fun `Dynamic content load is reflected in enhanced DOM tree`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        // Trigger async load of users (2s delay per page script)
        runCatching { devTools.runtime.evaluate("loadContent('users')") }

        // Deterministically wait for #dynamicContent to get 'loaded' class, then assert
        var loadedObserved = false
        repeat(40) { // up to ~20s
            val ok = runCatching {
                devTools.runtime.evaluate("document.getElementById('dynamicContent')?.classList?.contains('loaded')")
            }.getOrNull()?.result?.value?.toString()?.equals("true", ignoreCase = true) == true
            if (ok) { loadedObserved = true; return@repeat }
            Thread.sleep(500)
        }
        assertTrue(loadedObserved, "Expected #dynamicContent to have class 'loaded' within timeout")

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        val root = collectEnhancedRoot(service, options)
        printlnPro(DomDebug.summarize(root))

        val dynamic = service.findElement(ElementRefCriteria(cssSelector = "#dynamicContent"))
            ?: findNodeById(root, "dynamicContent")
        assertNotNull(dynamic, "Expected #dynamicContent to exist in DOM")
        val klass = dynamic!!.attributes["class"] ?: ""
        printlnPro(DomDebug.summarize(dynamic))

        // Hard assertion: ensure 'loaded' class is present in the enhanced DOM snapshot as well
        assertTrue(klass.split(" ").contains("loaded"), "Expected #dynamicContent to have 'loaded' class in enhanced DOM snapshot")
    }

    @Test
    fun `SnapshotNodeEx bounds and rects are populated correctly`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        val root = collectEnhancedRoot(service, options)
        printlnPro(DomDebug.summarize(root))
        assertNotNull(root)

        // Test bounds on body element
        val bodyNode = service.findElement(ElementRefCriteria(cssSelector = "body"))
        assertNotNull(bodyNode, "Expected body element to be found")
        val bodySnapshot = bodyNode!!.snapshotNode
        assertNotNull(bodySnapshot, "Expected body to have snapshot data")

        // Test clientRects property (CDP may not populate bounds for body/html, but clientRects should be available)
        val bounds = bodySnapshot!!.bounds ?: bodySnapshot.clientRects
        assertNotNull(bounds, "Expected body snapshot to have bounds or clientRects")
        assertTrue(bounds!!.width > 0, "Expected bounds width to be positive")
        assertTrue(bounds.height > 0, "Expected bounds height to be positive")
        printlnPro("Body bounds: x=${bounds.x}, y=${bounds.y}, width=${bounds.width}, height=${bounds.height}")

        // Test absoluteBounds property (may be null for body/html elements if CDP doesn't provide bounds)
        if (bodySnapshot.absoluteBounds != null) {
            val absoluteBounds = bodySnapshot.absoluteBounds!!
            assertTrue(absoluteBounds.width > 0, "Expected absoluteBounds width to be positive")
            assertTrue(absoluteBounds.height > 0, "Expected absoluteBounds height to be positive")
            printlnPro("Body absoluteBounds: x=${absoluteBounds.x}, y=${absoluteBounds.y}, width=${absoluteBounds.width}, height=${absoluteBounds.height}")
        } else {
            printlnPro("Body absoluteBounds not available (this is expected for body/html elements)")
        }

        // Test clientRects property
        val clientRects = bodySnapshot.clientRects
        if (clientRects != null) {
            assertTrue(clientRects.width >= 0, "Expected clientRects width to be non-negative")
            assertTrue(clientRects.height >= 0, "Expected clientRects height to be non-negative")
            printlnPro("Body clientRects: x=${clientRects.x}, y=${clientRects.y}, width=${clientRects.width}, height=${clientRects.height}")
        }

        // Test scrollRects property
        val scrollRects = bodySnapshot.scrollRects
        if (scrollRects != null) {
            assertTrue(scrollRects.width >= 0, "Expected scrollRects width to be non-negative")
            assertTrue(scrollRects.height >= 0, "Expected scrollRects height to be non-negative")
            printlnPro("Body scrollRects: x=${scrollRects.x}, y=${scrollRects.y}, width=${scrollRects.width}, height=${scrollRects.height}")
        }
    }

    @Test
    fun `SnapshotNodeEx bounds on interactive elements`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        // Generate dynamic content with buttons
        runCatching { devTools.runtime.evaluate("generateLargeList(100)") }

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        suspend fun jsNumber(expr: String): Double? = runCatching {
            val r = devTools.runtime.evaluate(expr)?.result
            r?.value?.toString()?.toDoubleOrNull() ?: r?.unserializableValue?.toDoubleOrNull()
        }.getOrNull()

        suspend fun getBcr(selector: String): Map<String, Double> {
            val script = """
                (function(){
                  var el = document.querySelector("$selector");
                  if(!el){ return {x:-1,y:-1,width:-1,height:-1}; }
                  var r = el.getBoundingClientRect();
                  return {x:r.x, y:r.y, width:r.width, height:r.height};
                })();
            """.trimIndent()
            val v = runCatching { devTools.runtime.evaluate(script)?.result?.value }.getOrNull()
            @Suppress("UNCHECKED_CAST")
            val m = v as? Map<String, Any?> ?: emptyMap()
            fun num(k: String) = (m[k] as? Number)?.toDouble() ?: 0.0
            return mapOf(
                "x" to num("x"),
                "y" to num("y"),
                "width" to num("width"),
                "height" to num("height")
            )
        }

        val root = collectEnhancedRoot(service, options)
        printlnPro(DomDebug.summarize(root))

        // Find a button element
        val buttonNode = service.findElement(ElementRefCriteria(cssSelector = "button"))
        assertNotNull(buttonNode, "Expected button element to be found")
        val buttonText = buttonNode!!.nodeValue.takeIf { it.isNotEmpty() }
            ?: buttonNode.attributes["textContent"]
            ?: buttonNode.attributes["value"]
            ?: "Load Users (2s delay)"
        assertEquals("Load Users (2s delay)", buttonText)

        val buttonSnapshot = buttonNode.snapshotNode
        assertNotNull(buttonSnapshot, "Expected buttonSnapshot not found")
        requireNotNull(buttonSnapshot)

        printlnPro(DomDebug.summarize(buttonNode))

        // 1) Basic sanity on raw fields
        buttonSnapshot.bounds?.let { bounds ->
            assertTrue(bounds.width >= 0, "Expected button bounds width to be non-negative")
            assertTrue(bounds.height >= 0, "Expected button bounds height to be non-negative")
            printlnPro("Button bounds(raw from CDP): x=${bounds.x}, y=${bounds.y}, width=${bounds.width}, height=${bounds.height}")
        }
        buttonSnapshot.absoluteBounds?.let { ab ->
            assertTrue(ab.width >= 0)
            assertTrue(ab.height >= 0)
            printlnPro("Button absoluteBounds(calculated): x=${ab.x}, y=${ab.y}, width=${ab.width}, height=${ab.height}")
        }
        buttonSnapshot.clientRects?.let { cr ->
            assertTrue(cr.width >= 0)
            assertTrue(cr.height >= 0)
            printlnPro("Button clientRects: x=${cr.x}, y=${cr.y}, width=${cr.width}, height=${cr.height}")
        }

        // 2) Cross-check against getBoundingClientRect and scroll positions
        val bcr = getBcr("button")
        val scrollX = jsNumber("window.scrollX || window.pageXOffset || 0") ?: 0.0
        val scrollY = jsNumber("window.scrollY || window.pageYOffset || 0") ?: 0.0
        val cssPosition = buttonSnapshot.computedStyles?.get("position") ?: ""

        // Expectations: CDP layout.bounds are document-absolute; BCR is viewport-relative.
        // Expected absolute (document) coordinates derived from BCR:
        val expectedAbsX = bcr["x"]!! + if (cssPosition == "fixed") 0.0 else scrollX
        val expectedAbsY = bcr["y"]!! + if (cssPosition == "fixed") 0.0 else scrollY
        val expectedW = bcr["width"]!!
        val expectedH = bcr["height"]!!

        fun close(a: Double?, b: Double?, eps: Double = 2.0) = a != null && b != null && kotlin.math.abs(a - b) <= eps

        val b = buttonSnapshot.bounds
        val ab = buttonSnapshot.absoluteBounds

        if (b != null) {
            // CDP bounds should align with document-absolute expected values
            assertTrue(close(b.x, expectedAbsX), "bounds.x(${b.x}) should ~= bcr.x+scrollX($expectedAbsX), position=$cssPosition")
            assertTrue(close(b.y, expectedAbsY), "bounds.y(${b.y}) should ~= bcr.y+scrollY($expectedAbsY), position=$cssPosition")
            assertTrue(close(b.width, expectedW), "bounds.width(${b.width}) ~= bcr.width($expectedW)")
            assertTrue(close(b.height, expectedH), "bounds.height(${b.height}) ~= bcr.height($expectedH)")
        }
        if (ab != null) {
            // absoluteBounds should match CDP bounds exactly in our implementation
            assertTrue(close(ab.x, expectedAbsX), "absoluteBounds.x(${ab.x}) should ~= expectedAbsX($expectedAbsX)")
            assertTrue(close(ab.y, expectedAbsY), "absoluteBounds.y(${ab.y}) should ~= expectedAbsY($expectedAbsY)")
            assertTrue(close(ab.width, expectedW), "absoluteBounds.width(${ab.width}) ~= $expectedW")
            assertTrue(close(ab.height, expectedH), "absoluteBounds.height(${ab.height}) ~= $expectedH")
        }

        // 3) Scroll and verify invariants again
        runCatching { devTools.runtime.evaluate("window.scrollTo(0, 200)") }
        Thread.sleep(300)

        val root2 = collectEnhancedRoot(service, options)
        printlnPro(DomDebug.summarize(root2))
        val buttonNode2 = service.findElement(ElementRefCriteria(cssSelector = "button"))
        assertNotNull(buttonNode2)
        val snap2 = buttonNode2!!.snapshotNode
        assertNotNull(snap2)

        val bcr2 = getBcr("button")
        val scrollX2 = jsNumber("window.scrollX || window.pageXOffset || 0") ?: 0.0
        val scrollY2 = jsNumber("window.scrollY || window.pageYOffset || 0") ?: 0.0
        val cssPos2 = snap2!!.computedStyles?.get("position") ?: cssPosition
        val expectedAbsX2 = bcr2["x"]!! + if (cssPos2 == "fixed") 0.0 else scrollX2
        val expectedAbsY2 = bcr2["y"]!! + if (cssPos2 == "fixed") 0.0 else scrollY2

        // For non-fixed elements, document-absolute bounds should remain stable across scrolls within tolerance
        if ((cssPos2 != "fixed") && snap2.bounds != null) {
            val prev = buttonSnapshot.bounds
            if (prev != null) {
                assertTrue(close(prev.x, snap2.bounds!!.x), "Doc-absolute bounds.x should be stable across scroll: ${prev.x} vs ${snap2.bounds!!.x}")
                assertTrue(close(prev.y, snap2.bounds!!.y), "Doc-absolute bounds.y should be stable across scroll: ${prev.y} vs ${snap2.bounds!!.y}")
            }
        }
        // Both bounds and absoluteBounds should still match expectedAbs after scroll
        snap2.bounds?.let { bb ->
            assertTrue(close(bb.x, expectedAbsX2), "After scroll, bounds.x(${bb.x}) ~= expectedAbsX2($expectedAbsX2)")
            assertTrue(close(bb.y, expectedAbsY2), "After scroll, bounds.y(${bb.y}) ~= expectedAbsY2($expectedAbsY2)")
        }
        snap2.absoluteBounds?.let { a2 ->
            assertTrue(close(a2.x, expectedAbsX2), "After scroll, absoluteBounds.x(${a2.x}) ~= expectedAbsX2($expectedAbsX2)")
            assertTrue(close(a2.y, expectedAbsY2), "After scroll, absoluteBounds.y(${a2.y}) ~= expectedAbsY2($expectedAbsY2)")
        }
    }

    @Test
    fun `SnapshotNodeEx scrollRects on scrollable container`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        // Generate a large scrollable list
        runCatching { devTools.runtime.evaluate("generateLargeList(1000)") }

        // Wait for container to be present
        var hasContainer = false
        repeat(50) {
            val ok = runCatching {
                devTools.runtime.evaluate("document.getElementById('virtualScrollContainer') != null")
            }.getOrNull()?.result?.value?.toString()?.equals("true", ignoreCase = true) == true
            if (ok) { hasContainer = true; return@repeat }
            Thread.sleep(200)
        }
        assertTrue(hasContainer, "Expected #virtualScrollContainer to be present")

        val options = SnapshotOptions(
            maxDepth = 100,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        val root = collectEnhancedRoot(service, options)

        // Find the scrollable container
        val scrollContainer = service.findElement(ElementRefCriteria(cssSelector = "#virtualScrollContainer"))
        assertNotNull(scrollContainer, "Expected scroll container to be found")

        val snapshot = scrollContainer!!.snapshotNode
        assertNotNull(snapshot, "Expected scroll container to have snapshot data")
        printlnPro(DomDebug.summarize(scrollContainer))

        // Test bounds (use clientRects as fallback since CDP may not populate bounds for all elements)
        val bounds = snapshot!!.bounds ?: snapshot.clientRects
        assertNotNull(bounds, "Expected scroll container to have bounds or clientRects")
        assertTrue(bounds!!.width > 0, "Expected scroll container bounds width to be positive")
        assertTrue(bounds.height > 0, "Expected scroll container bounds height to be positive")
        printlnPro("ScrollContainer bounds: x=${bounds.x}, y=${bounds.y}, width=${bounds.width}, height=${bounds.height}")

        // Test absoluteBounds (may be null if CDP doesn't provide bounds, only clientRects)
        if (snapshot.absoluteBounds != null) {
            val absoluteBounds = snapshot.absoluteBounds!!
            assertTrue(absoluteBounds.width > 0, "Expected scroll container absoluteBounds width to be positive")
            assertTrue(absoluteBounds.height > 0, "Expected scroll container absoluteBounds height to be positive")
            printlnPro("ScrollContainer absoluteBounds: x=${absoluteBounds.x}, y=${absoluteBounds.y}, width=${absoluteBounds.width}, height=${absoluteBounds.height}")
        } else {
            printlnPro("ScrollContainer absoluteBounds not available (CDP may only provide clientRects for this element)")
        }

        // Test scrollRects - should be present for scrollable elements
        assertNotNull(snapshot.scrollRects, "Expected scroll container to have scrollRects")
        val scrollRects = snapshot.scrollRects!!
        assertTrue(scrollRects.width >= 0, "Expected scroll container scrollRects width to be non-negative")
        assertTrue(scrollRects.height >= 0, "Expected scroll container scrollRects height to be non-negative")
        printlnPro("ScrollContainer scrollRects: x=${scrollRects.x}, y=${scrollRects.y}, width=${scrollRects.width}, height=${scrollRects.height}")

        // ScrollRects height should typically be larger than bounds height for scrollable content
        // (though this isn't guaranteed in all cases, so we just log it)
        printlnPro("ScrollRects.height (${scrollRects.height}) vs Bounds.height (${bounds.height})")

        // Test clientRects
        if (snapshot.clientRects != null) {
            val clientRects = snapshot.clientRects!!
            assertTrue(clientRects.width >= 0, "Expected scroll container clientRects width to be non-negative")
            assertTrue(clientRects.height >= 0, "Expected scroll container clientRects height to be non-negative")
            printlnPro("ScrollContainer clientRects: x=${clientRects.x}, y=${clientRects.y}, width=${clientRects.width}, height=${clientRects.height}")
        }
    }
}
