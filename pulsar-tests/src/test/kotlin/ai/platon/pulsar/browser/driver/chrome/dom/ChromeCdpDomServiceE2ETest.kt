package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.driver.chrome.dom.model.ElementRefCriteria
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromeCdpDomServiceE2ETest : WebDriverTestBase() {
    private val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"

    @Test
    fun `test getAllTrees`() = runWebDriverTest(testURL) { driver ->
        // Ensure we are using the Pulsar CDP-backed driver
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation
        val service = ChromeCdpDomService(devTools)

        // Collect all trees with defaults (AX + Snapshot enabled)
        val trees = service.getAllTrees(target = PageTarget(), options = SnapshotOptions())

        // Basic sanity checks on collected data
        assertTrue(trees.devicePixelRatio > 0.1, "devicePixelRatio should be positive")
        assertTrue(trees.cdpTiming.isNotEmpty(), "cdpTiming should record phases")
        assertTrue(trees.domByBackendId.isNotEmpty(), "DOM backend-node index should be populated")
        if (trees.options.includeAX) {
            // For a standard SPA page, AX tree should be present when includeAX=true
            assertTrue(trees.axTree.isNotEmpty(), "AX tree should be present when includeAX=true")
        }
        if (trees.options.includeSnapshot) {
            assertTrue(trees.snapshotByBackendId.isNotEmpty(), "Snapshot should be present when includeSnapshot=true")
        }

        // Root should be a document node or at least have children
        assertTrue(
            trees.domTree.nodeType == NodeType.DOCUMENT_NODE || trees.domTree.children.isNotEmpty(),
            "DOM root should be a document or have children"
        )

        // Merge into enhanced tree
        val enhancedRoot = service.buildEnhancedDomTree(trees)
        assertTrue(enhancedRoot.children.isNotEmpty(), "Enhanced root should contain children")

        // Build simplified tree and serialize for LLM
        val simplified = service.buildSimplifiedTree(enhancedRoot)
        val llm = service.serializeForLLM(simplified)
        assertTrue(llm.json.length > 50, "Serialized JSON should not be trivial")
        assertTrue(llm.selectorMap.isNotEmpty(), "Selector map should contain lookup keys")

        // Try finding a simple, stable element and converting it to an interacted element
        val bodyNode = service.findElement(ElementRefCriteria(cssSelector = "body"))
        assertNotNull(bodyNode, "Should locate <body> element from enhanced tree")
        val interacted = service.toInteractedElement(bodyNode)
        assertTrue(interacted.elementHash.isNotBlank(), "Interacted element should carry a non-empty hash")
    }
}
