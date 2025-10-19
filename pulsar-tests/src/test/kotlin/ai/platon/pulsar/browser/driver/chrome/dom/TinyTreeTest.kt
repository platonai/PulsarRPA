package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.model.TinyNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class TinyTreeTest : WebDriverTestBase() {

    private fun flattenSlim(root: TinyNode): List<TinyNode> {
        val out = ArrayList<TinyNode>()
        fun dfs(n: TinyNode) {
            out.add(n)
            n.children.forEach { dfs(it) }
        }
        dfs(root)
        return out
    }

    @Test
    fun `DOMTinyTreeBuilder invariants on interactive-dynamic page`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
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

        val enhancedRoot = collectEnhancedRoot(service, options)
        logPrintln(DomDebug.summarize(enhancedRoot))
        assertTrue(enhancedRoot.children.isNotEmpty(), "Enhanced root should have children")

        val tinyTree = DOMTinyTreeBuilder(enhancedRoot).build()
        assertNotNull(tinyTree, "Simplified Slim DOM should not be null")
        tinyTree!!
        logPrintln(DomDebug.summarize(tinyTree))
        assertTrue(tinyTree.children.isNotEmpty(), "Simplified Slim DOM should have children")

        val all = flattenSlim(tinyTree)
        // There should be at least one interactive element on example.com (links)
        val interactiveIndices = all.mapNotNull { it.interactiveIndex }
        assertTrue(interactiveIndices.isNotEmpty(), "Expected at least one interactive element with index")
        assertEquals(1, interactiveIndices.first(), "First interactive index should start from 1")
        assertTrue(interactiveIndices.zipWithNext().all { (a, b) -> b > a }, "Interactive indices should be strictly increasing")

        // Pruning invariants: no disabled elements in simplified tree
        val disabledTags = setOf("style", "script", "head", "meta", "link", "title")
        all.forEach { n ->
            val o = n.originalNode
            if (o.nodeType == NodeType.ELEMENT_NODE) {
                val tag = o.nodeName.lowercase()
                assertFalse(tag in disabledTags, "Disabled element <$tag> should not appear in simplified tree")
            }
        }

        // OptimizeTree invariants: no leaf element that is invisible and not scrollable
        all.forEach { n ->
            val o = n.originalNode
            val isLeaf = n.children.isEmpty()
            val isText = o.nodeType == NodeType.TEXT_NODE
            val visible = (o.isVisible == true)
            val scrollable = (o.isScrollable == true)
            if (isLeaf && !isText) {
                assertTrue(visible || scrollable, "Invisible, non-scrollable leaf elements should be pruned")
            }
        }

        // Assignment invariants: indexed nodes must be visible + interactable and not excluded/ignored
        all.filter { it.interactiveIndex != null }.forEach { n ->
            assertEquals(true, n.originalNode.isVisible, "Indexed node must be visible")
            assertEquals(true, n.originalNode.isInteractable, "Indexed node must be interactable")
            assertFalse(n.excludedByParent, "Indexed node must not be excluded by parent")
            assertFalse(n.ignoredByPaintOrder, "Indexed node must not be ignored by paint order")
        }

        // If a node is excludedByParent, it should never be indexed
        all.filter { it.excludedByParent }.forEach { n ->
            assertNull(n.interactiveIndex, "Excluded node must not have interactive index")
        }

        // TEXT_NODEs included must contain non-trivial text and be marked shouldDisplay
        all.filter { it.originalNode.nodeType == NodeType.TEXT_NODE }.forEach { n ->
            assertTrue(n.shouldDisplay, "Text nodes in simplified tree must be displayable")
            val text = n.originalNode.nodeValue.orEmpty().trim()
            assertTrue(text.length > 1, "Text nodes must contain non-trivial content")
        }
    }

    @Test
    fun `isNew flag respects previous backend node ids`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
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

        val enhancedRoot = collectEnhancedRoot(service, options)
        val allBackendIds = mutableSetOf<Int>()
        fun collectBackendIds(n: DOMTreeNodeEx) {
            n.backendNodeId?.let { allBackendIds.add(it) }
            n.children.forEach { collectBackendIds(it) }
            n.shadowRoots.forEach { collectBackendIds(it) }
            n.contentDocument?.let { collectBackendIds(it) }
        }
        collectBackendIds(enhancedRoot)
        assertTrue(allBackendIds.isNotEmpty(), "Expected some backend node IDs in the enhanced DOM")

        val simplifiedInitial = DOMTinyTreeBuilder(enhancedRoot).build()
        assertNotNull(simplifiedInitial)
        val nodesInitial = flattenSlim(simplifiedInitial!!)
        // With empty previous set, nodes that have backend IDs should be considered new (best-effort)
        val anyNew = nodesInitial.any { it.isNew }
        assertTrue(anyNew, "Expected at least one node marked isNew on first build")

        // Build again with previous IDs supplied: nodes should now be marked as not new
        val simplifiedSecond = DOMTinyTreeBuilder(enhancedRoot, previousBackendNodeIds = allBackendIds).build()
        assertNotNull(simplifiedSecond)
        val nodesSecond = flattenSlim(simplifiedSecond!!)
        nodesSecond.forEach { n ->
            val backendId = n.originalNode.backendNodeId
            if (backendId != null && backendId in allBackendIds) {
                assertFalse(n.isNew, "Node with known backendId should not be marked isNew")
            }
        }
    }

    @Test
    fun `optimizeTree prunes invisible wrapper with pruned children on real page`() = runEnhancedWebDriverTest(interactiveDynamicURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation as RemoteDevTools
        val service = ChromeCdpDomService(devTools)

        // Inject an invisible wrapper with trivial content; children will be pruned first, then wrapper by optimizeTree
        runCatching {
            devTools.runtime.evaluate(
                """
                (function(){
                  var el = document.getElementById('invisibleWrapper');
                  if (!el) {
                    var html = '<div id="invisibleWrapper" style="display:none">' +
                               '  <span> </span>' + // trivial text inside
                               '</div>';
                    document.body.insertAdjacentHTML('beforeend', html);
                  }
                })();
                """.trimIndent()
            )
        }

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

        val enhancedRoot = collectEnhancedRoot(service, options)
        assertTrue(enhancedRoot.children.isNotEmpty())

        val simplified = DOMTinyTreeBuilder(enhancedRoot).build()
        assertNotNull(simplified)
        val flat = flattenSlim(simplified!!)

        // Assert: the invisible wrapper should be absent from simplified DOM
        val hasWrapper = flat.any { it.originalNode.attributes["id"] == "invisibleWrapper" }
        assertFalse(hasWrapper, "Invisible non-scrollable wrapper with pruned children must be removed by optimizeTree")
    }
}

