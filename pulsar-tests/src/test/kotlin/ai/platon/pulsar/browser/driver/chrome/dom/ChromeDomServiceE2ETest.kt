package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.browser.driver.chrome.dom.model.ElementRefCriteria
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertIs
import java.io.File
import java.time.Instant

class ChromeDomServiceE2ETest : WebDriverTestBase() {
    private val testURL get() = "$generatedAssetsBaseURL/interactive-dynamic.html"

    private data class Metrics(
        val url: String,
        val timestamp: String,
        val case: String,
        val cdpTiming: Map<String, Long> = emptyMap(),
        val devicePixelRatio: Double? = null,
        val domNodeCount: Int? = null,
        val axNodeCount: Int? = null,
        val snapshotEntryCount: Int? = null,
        val serializeJsonSize: Int? = null,
        val notes: String? = null,
        val differenceType: String = "meta"
    )

    private fun writeMetrics(metrics: Metrics) {
        val dir = File("logs/chat-model")
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "domservice-e2e.json")
        val mapper = prettyPulsarObjectMapper()
        out.appendText(mapper.writeValueAsString(metrics) + System.lineSeparator())
    }

    private fun countDomNodes(root: ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode?): Int {
        if (root == null) return 0
        var n = 1
        root.children.forEach { n += countDomNodes(it) }
        root.shadowRoots.forEach { n += countDomNodes(it) }
        root.contentDocument?.let { n += countDomNodes(it) }
        return n
    }

    @Test
    fun `Given interactive page When collecting all trees Then get DOM AX and Snapshot with timings`() = runWebDriverTest(testURL) { driver ->
        assertIs<PulsarWebDriver>(driver)
        val devTools = driver.implementation
        val service = ChromeCdpDomService(devTools)

        val options = SnapshotOptions(
            maxDepth = 0,
            includeAX = true,
            includeSnapshot = true,
            includeStyles = true,
            includePaintOrder = true,
            includeDOMRects = true,
            includeScrollAnalysis = true,
            includeVisibility = true,
            includeInteractivity = true
        )

        val trees = service.getAllTrees(target = PageTarget(), options = options)
        assertTrue(trees.devicePixelRatio > 0.1, "devicePixelRatio should be positive")
        assertTrue(trees.cdpTiming.isNotEmpty(), "cdpTiming should record phases")

        val enhancedRoot = service.buildEnhancedDomTree(trees)
        val simplified = service.buildSimplifiedTree(enhancedRoot)
        val llm = service.serializeForLLM(simplified)

        assertTrue(llm.json.length > 50, "Serialized JSON should not be trivial")
        assertTrue(llm.selectorMap.isNotEmpty(), "Selector map should contain entries")

        // Probe a stable element
        val bodyNode = service.findElement(ElementRefCriteria(cssSelector = "body"))
        assertNotNull(bodyNode, "Should locate <body> element")
        val interacted = service.toInteractedElement(bodyNode!!)
        assertTrue(interacted.elementHash.isNotBlank(), "Interacted element hash should be non-empty")

        val domCount = countDomNodes(enhancedRoot)
        writeMetrics(
            Metrics(
                url = testURL,
                timestamp = Instant.now().toString(),
                case = "ChromeDomServiceE2E",
                cdpTiming = trees.cdpTiming,
                devicePixelRatio = trees.devicePixelRatio,
                domNodeCount = domCount,
                axNodeCount = trees.axTree.size,
                snapshotEntryCount = trees.snapshotByBackendId.size,
                serializeJsonSize = llm.json.length,
                notes = "End-to-end validation with LLM serialization"
            )
        )
    }
}
