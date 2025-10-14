package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedSnapshotNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.model.SimplifiedNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomLLMSerializerTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize filters attributes and populates selector map`() {
        val childOriginal = EnhancedDOMTreeNode(
            nodeId = 2,
            nodeName = "SPAN",
            attributes = mapOf("data-test" to "value", "aria-label" to "ok"),
            elementHash = "child-hash"
        )
        val rootOriginal = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "DIV",
            attributes = mapOf("id" to "card", "data-id" to "123"),
            elementHash = "root-hash"
        )

        val root = SimplifiedNode(
            originalNode = rootOriginal,
            children = listOf(SimplifiedNode(originalNode = childOriginal))
        )

        val result = DomLLMSerializer.serialize(root, listOf("data-id", "aria-label"))
        val tree = mapper.readTree(result.json)

        val rootAttrs = tree.get("original_node").get("attributes")
        assertEquals(1, rootAttrs.size(), "Only whitelisted root attribute should be present")
        assertEquals("123", rootAttrs.get("data-id").asText())

        val childAttrs = tree.get("children").first().get("original_node").get("attributes")
        assertEquals(1, childAttrs.size(), "Child should also honor whitelist")
        assertEquals("ok", childAttrs.get("aria-label").asText())

        // Enhanced selector map includes multiple keys per node; ensure required hash keys are present
        assertTrue(result.selectorMap.containsKey("root-hash"))
        assertTrue(result.selectorMap.containsKey("child-hash"))
    }

    @Test
    fun `serialize propagates scroll info only when helper allows it`() {
        val scrollableNode = EnhancedDOMTreeNode(
            nodeId = 3,
            nodeName = "div",
            attributes = emptyMap(),
            elementHash = "scroll-hash",
            snapshotNode = EnhancedSnapshotNode(
                computedStyles = mapOf("overflow" to "auto"),
                clientRects = DOMRect(0.0, 0.0, 200.0, 200.0),
                scrollRects = DOMRect(0.0, 0.0, 400.0, 400.0)
            )
        )
        val rootOriginal = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BODY",
            nodeType = NodeType.ELEMENT_NODE,
            elementHash = "body-hash"
        )

        val simplified = SimplifiedNode(
            originalNode = rootOriginal,
            children = listOf(SimplifiedNode(originalNode = scrollableNode))
        )

        val result = DomLLMSerializer.serialize(simplified)
        val tree = mapper.readTree(result.json)
        val child = tree.get("children").first()

        assertNotNull(child.get("should_show_scroll_info"), "Scroll flag should be present when helper returns true")
        assertEquals("scrollable (both) [200x200 < 400x400]", child.get("scroll_info_text").asText())
    }

    @Test
    fun `serialize with paint order pruning removes high paint order elements`() {
        val highPaintOrderNode = EnhancedDOMTreeNode(
            nodeId = 4,
            nodeName = "DIV",
            elementHash = "high-paint-hash",
            snapshotNode = EnhancedSnapshotNode(
                paintOrder = 1500 // Above default threshold of 1000
            )
        )
        val normalNode = EnhancedDOMTreeNode(
            nodeId = 5,
            nodeName = "SPAN",
            elementHash = "normal-hash",
            snapshotNode = EnhancedSnapshotNode(
                paintOrder = 500 // Below threshold
            )
        )
        val rootOriginal = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BODY",
            elementHash = "body-hash"
        )

        val simplified = SimplifiedNode(
            originalNode = rootOriginal,
            children = listOf(
                SimplifiedNode(originalNode = highPaintOrderNode),
                SimplifiedNode(originalNode = normalNode)
            )
        )

        val options = DomLLMSerializer.SerializationOptions(
            enablePaintOrderPruning = true,
            maxPaintOrderThreshold = 1000
        )
        val result = DomLLMSerializer.serialize(simplified, emptyList(), options)
        val tree = mapper.readTree(result.json)

        val children = tree.get("children")
        assertEquals(2, children.size()) // Both children should be present but high paint order should be pruned

        val highPaintChild = children.first { it.get("original_node").get("element_hash").asText() == "high-paint-hash" }
        assertEquals(false, highPaintChild.get("should_display").asBoolean(), "High paint order node should not be displayed")
        assertEquals(true, highPaintChild.get("ignored_by_paint_order").asBoolean(), "High paint order node should be marked as ignored")
    }

    @Test
    fun `serialize detects compound components correctly`() {
        val listItem = EnhancedDOMTreeNode(
            nodeId = 6,
            nodeName = "LI",
            elementHash = "li-hash"
        )
        val listNode = EnhancedDOMTreeNode(
            nodeId = 5,
            nodeName = "UL",
            elementHash = "ul-hash"
        )
        val rootOriginal = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BODY",
            elementHash = "body-hash"
        )

        val simplified = SimplifiedNode(
            originalNode = rootOriginal,
            children = listOf(
                SimplifiedNode(
                    originalNode = listNode,
                    children = List(5) { SimplifiedNode(originalNode = listItem) } // 5 children to meet threshold
                )
            )
        )

        val options = DomLLMSerializer.SerializationOptions(
            enableCompoundComponentDetection = true,
            compoundComponentMinChildren = 3
        )
        val result = DomLLMSerializer.serialize(simplified, emptyList(), options)
        val tree = mapper.readTree(result.json)

        val ulNode = tree.get("children").first()
        assertEquals(true, ulNode.get("is_compound_component").asBoolean(), "UL with multiple children should be detected as compound component")
    }

    @Test
    fun `serialize aligns attribute casing correctly`() {
        val node = EnhancedDOMTreeNode(
            nodeId = 7,
            nodeName = "INPUT",
            elementHash = "input-hash",
            attributes = mapOf(
                "className" to "my-input", // Should be normalized to "class"
                "htmlFor" to "my-label",   // Should be normalized to "for"
                "READONLY" to "true",      // Should be normalized to "readonly"
                "customAttr" to "value"    // Should remain as "customattr"
            )
        )

        val simplified = SimplifiedNode(originalNode = node)

        val options = DomLLMSerializer.SerializationOptions(
            enableAttributeCasingAlignment = true,
            preserveOriginalCasing = false
        )
        val result = DomLLMSerializer.serialize(simplified, listOf("class", "for", "readonly", "customattr"), options)
        val tree = mapper.readTree(result.json)

        val attrs = tree.get("original_node").get("attributes")
        assertEquals("my-input", attrs.get("class").asText(), "className should be normalized to class")
        assertEquals("my-label", attrs.get("for").asText(), "htmlFor should be normalized to for")
        assertEquals("true", attrs.get("readonly").asText(), "READONLY should be normalized to readonly")
        assertEquals("value", attrs.get("customattr").asText(), "customAttr should be normalized to lowercase")
    }

    @Test
    fun `serialize builds enhanced selector map with multiple keys`() {
        val node = EnhancedDOMTreeNode(
            nodeId = 8,
            nodeName = "BUTTON",
            elementHash = "button-hash",
            xPath = "/html/body/div[1]/button[2]",
            backendNodeId = 12345
        )

        val simplified = SimplifiedNode(originalNode = node)

        val result = DomLLMSerializer.serialize(simplified)

        // Check that all expected keys are present in the selector map
        assertTrue(result.selectorMap.containsKey("button-hash"), "Element hash key should be present")
        assertTrue(result.selectorMap.containsKey("xpath:/html/body/div[1]/button[2]"), "XPath key should be present")
        assertTrue(result.selectorMap.containsKey("backend:12345"), "Backend node ID key should be present")
        assertTrue(result.selectorMap.containsKey("node:8"), "Node ID key should be present")

        // All keys should map to the same node
        val expectedNode = result.selectorMap["button-hash"]
        assertNotNull(expectedNode)
        assertEquals(expectedNode, result.selectorMap["xpath:/html/body/div[1]/button[2]"])
        assertEquals(expectedNode, result.selectorMap["backend:12345"])
        assertEquals(expectedNode, result.selectorMap["node:8"])
    }

    @Test
    fun `serialize preserves original casing when configured`() {
        val node = EnhancedDOMTreeNode(
            nodeId = 9,
            nodeName = "CustomElement",
            elementHash = "custom-hash"
        )

        val simplified = SimplifiedNode(originalNode = node)

        val options = DomLLMSerializer.SerializationOptions(
            preserveOriginalCasing = true
        )
        val result = DomLLMSerializer.serialize(simplified, emptyList(), options)
        val tree = mapper.readTree(result.json)

        assertEquals("CustomElement", tree.get("original_node").get("node_name").asText(),
            "Original casing should be preserved when configured")
    }
}
