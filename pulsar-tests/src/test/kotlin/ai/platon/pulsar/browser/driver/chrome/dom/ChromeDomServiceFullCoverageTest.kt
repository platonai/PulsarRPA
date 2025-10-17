package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.ElementRefCriteria
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class ChromeDomServiceFullCoverageTest : WebDriverTestBase() {
    private val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"

    // Helper to DFS find the first node by id in the enhanced tree
    private fun findNodeById(root: DOMTreeNodeEx, id: String): DOMTreeNodeEx? {
        if (root.attributes["id"] == id) return root
        root.children.forEach { findNodeById(it, id)?.let { return it } }
        root.shadowRoots.forEach { findNodeById(it, id)?.let { return it } }
        root.contentDocument?.let { findNodeById(it, id)?.let { return it } }
        return null
    }

    @Test
    fun `Get trees, build and serialize end-to-end with assertions`() = runEnhancedWebDriverTest(testURL) { driver ->
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

        fun collectRoot(): DOMTreeNodeEx {
            repeat(3) { attempt ->
                val t = service.getAllTrees(target = PageTarget(), options = options)
                // Print TargetAllTrees summary once per attempt (last one will be the most recent)
                println(DomDebug.summarize(t))
                val r = service.buildEnhancedDomTree(t)
                if (r.children.isNotEmpty() || attempt == 2) return r
                Thread.sleep(300)
            }
            // Unreachable
            return service.buildEnhancedDomTree(service.getAllTrees(PageTarget(), options))
        }

        val enhancedRoot = collectRoot()
        // Print enhanced DOMTreeNodeEx summary and basic tree stats
        println(DomDebug.summarize(enhancedRoot))
        assertTrue(enhancedRoot.children.isNotEmpty())

        val simplified = service.buildSimplifiedTree(enhancedRoot)
        // Print SlimNode summary and basic tree stats
        println(DomDebug.summarize(simplified))
        assertTrue(simplified.children.isNotEmpty())

        val llm = service.serialize(simplified)
        assertTrue(llm.json.length > 100)
        assertTrue(llm.selectorMap.isNotEmpty())

        // Selector map should include at least node: and possibly xpath: keys for some nodes
        val anySelectorKey = llm.selectorMap.keys.any { it.startsWith("node:") || it.startsWith("xpath:") }
        assertTrue(anySelectorKey, "Selector map should contain node:/xpath: keys")
    }

    @Test
    fun `Find element using css, xpath, backend id, element hash and convert to interacted element`() = runEnhancedWebDriverTest(testURL) { driver ->
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

        fun collectRoot(): DOMTreeNodeEx {
            repeat(3) { attempt ->
                val t = service.getAllTrees(target = PageTarget(), options = options)
                println(DomDebug.summarize(t))
                val r = service.buildEnhancedDomTree(t)
                if (r.children.isNotEmpty() || attempt == 2) return r
                Thread.sleep(300)
            }
            return service.buildEnhancedDomTree(service.getAllTrees(PageTarget(), options))
        }
        val root = collectRoot()
        assertNotNull(root)
        println(DomDebug.summarize(root))

        // Try CSS by id first
        var target = service.findElement(ElementRefCriteria(cssSelector = "#loadingContainer"))
        assertNotNull(target)

        val direct = findNodeById(root, "loadingContainer")
        assertNotNull(direct)
        requireNotNull(direct)
        println(DomDebug.summarize(direct))

        val viaXPath = direct.xPath?.let { service.findElement(ElementRefCriteria(xPath = it)) }
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
        val xPath = t.xPath
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
        println(DomDebug.summarize(interacted))
        assertTrue(interacted.elementHash.isNotBlank())
    }

    @Test
    fun `Options toggling - no AX or Snapshot yields minimal enhanced nodes`() = runEnhancedWebDriverTest(testURL) { driver ->
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

        val trees = service.getAllTrees(PageTarget(), options)
        println(DomDebug.summarize(trees))
        assertTrue(trees.snapshotByBackendId.isEmpty())
        assertTrue(trees.axTree.isEmpty())

        val root = service.buildEnhancedDomTree(trees)
        println(DomDebug.summarize(root))
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
    fun `Scrollability and interactivity analysis on dynamic content`() = runEnhancedWebDriverTest(testURL) { driver ->
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

        // Wait deterministically for the container to exist in the DOM before snapshotting
        var hasContainer = false
        repeat(50) { // up to ~10s
            val ok = runCatching {
                devTools.runtime.evaluate("document.getElementById('virtualScrollContainer') != null")
            }.getOrNull()?.result?.value?.toString()?.equals("true", ignoreCase = true) == true
            if (ok) { hasContainer = true; return@repeat }
            Thread.sleep(200)
        }
        assertTrue(hasContainer, "Expected #virtualScrollContainer to be present after generateLargeList(1000)")

        fun collectRoot(): DOMTreeNodeEx {
            repeat(3) { attempt ->
                val t = service.getAllTrees(target = PageTarget(), options = options)
                println(DomDebug.summarize(t))
                val r = service.buildEnhancedDomTree(t)
                if (r.children.isNotEmpty() || attempt == 2) return r
                Thread.sleep(300)
            }
            val allTrees = service.getAllTrees(PageTarget(), options)
            return service.buildEnhancedDomTree(allTrees)
        }
        val root = collectRoot()
        println(DomDebug.summarize(root))
        assertNotNull(root)

        // Locate scroll container by CSS, fallback to DFS by id
        var scrollContainer = service.findElement(ElementRefCriteria(cssSelector = "#virtualScrollContainer"))
        assertNotNull(scrollContainer, "Expected scroll container to be found and resolved")
        requireNotNull(scrollContainer)
        println(DomDebug.summarize(scrollContainer))
        assertEquals(true, scrollContainer.isScrollable, "Expected #virtualScrollContainer to be marked scrollable")

        val direct = findNodeById(root, "virtualScrollContainer")
        assertNotNull(direct)
        requireNotNull(direct)

        val xPath = direct.xPath
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
        println(anyVirtualBtn?.let { DomDebug.summarize(it) })
        assertEquals(true, anyVirtualBtn!!.isInteractable)
    }

    @Test
    fun `Dynamic content load is reflected in enhanced DOM tree`() = runEnhancedWebDriverTest(testURL) { driver ->
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

        fun collectRoot(): DOMTreeNodeEx {
            repeat(3) { attempt ->
                val t = service.getAllTrees(target = PageTarget(), options = options)
                println(DomDebug.summarize(t))
                val r = service.buildEnhancedDomTree(t)
                if (r.children.isNotEmpty() || attempt == 2) return r
                Thread.sleep(300)
            }
            return service.buildEnhancedDomTree(service.getAllTrees(PageTarget(), options))
        }
        val root = collectRoot()
        println(DomDebug.summarize(root))

        val dynamic = service.findElement(ElementRefCriteria(cssSelector = "#dynamicContent"))
            ?: findNodeById(root, "dynamicContent")
        assertNotNull(dynamic, "Expected #dynamicContent to exist in DOM")
        val klass = dynamic!!.attributes["class"] ?: ""
        println(DomDebug.summarize(dynamic))

        // Hard assertion: ensure 'loaded' class is present in the enhanced DOM snapshot as well
        assertTrue(klass.split(" ").contains("loaded"), "Expected #dynamicContent to have 'loaded' class in enhanced DOM snapshot")
    }
}
