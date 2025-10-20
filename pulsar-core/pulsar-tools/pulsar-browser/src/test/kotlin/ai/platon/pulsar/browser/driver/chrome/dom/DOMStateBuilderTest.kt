package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.model.TinyNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DOMStateBuilderTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize filters attributes and populates selector map`() {
        val childOriginal = DOMTreeNodeEx(
            nodeId = 2,
            nodeName = "SPAN",
            attributes = mapOf("data-test" to "value", "aria-label" to "ok"),
            elementHash = "child-hash"
        )
        val rootOriginal = DOMTreeNodeEx(
            nodeId = 1,
            nodeName = "DIV",
            attributes = mapOf("id" to "card", "data-id" to "123"),
            elementHash = "root-hash"
        )

        val root = TinyNode(
            originalNode = rootOriginal,
            children = listOf(TinyNode(originalNode = childOriginal))
        )

        val result = DOMStateBuilder.build(root, listOf("data-id", "aria-label"))
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

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
        val scrollableNode = DOMTreeNodeEx(
            nodeId = 3,
            nodeName = "div",
            attributes = emptyMap(),
            elementHash = "scroll-hash",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow" to "auto"),
                clientRects = DOMRect(0.0, 0.0, 200.0, 200.0),
                scrollRects = DOMRect(0.0, 0.0, 400.0, 400.0)
            )
        )
        val rootOriginal = DOMTreeNodeEx(
            nodeId = 1,
            nodeName = "BODY",
            nodeType = NodeType.ELEMENT_NODE,
            elementHash = "body-hash"
        )

        val simplified = TinyNode(
            originalNode = rootOriginal,
            children = listOf(TinyNode(originalNode = scrollableNode))
        )

        val result = DOMStateBuilder.build(simplified)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)
        val child = tree.get("children").first()

        assertNotNull(child.get("should_show_scroll_info"), "Scroll flag should be present when helper returns true")
        assertEquals("scrollable (both) [200x200 < 400x400]", child.get("scroll_info_text").asText())
    }

    @Test
    fun `serialize with paint order pruning removes high paint order elements`() {
        val highPaintOrderNode = DOMTreeNodeEx(
            nodeId = 4,
            nodeName = "DIV",
            elementHash = "high-paint-hash",
            snapshotNode = SnapshotNodeEx(
                paintOrder = 1500 // Above default threshold of 1000
            )
        )
        val normalNode = DOMTreeNodeEx(
            nodeId = 5,
            nodeName = "SPAN",
            elementHash = "normal-hash",
            snapshotNode = SnapshotNodeEx(
                paintOrder = 500 // Below threshold
            )
        )
        val rootOriginal = DOMTreeNodeEx(
            nodeId = 1,
            nodeName = "BODY",
            elementHash = "body-hash"
        )

        val simplified = TinyNode(
            originalNode = rootOriginal,
            children = listOf(
                TinyNode(originalNode = highPaintOrderNode),
                TinyNode(originalNode = normalNode)
            )
        )

        val options = DOMStateBuilder.CompactOptions(
            enablePaintOrderPruning = true,
            maxPaintOrderThreshold = 1000
        )
        val result = DOMStateBuilder.build(simplified, emptyList(), options)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

        val children = tree.get("children")
        assertEquals(2, children.size()) // Both children should be present but high paint order should be pruned

        val highPaintChild = children.first { it.get("original_node").get("element_hash").asText() == "high-paint-hash" }
        assertEquals(false, highPaintChild.get("should_display").asBoolean(), "High paint order node should not be displayed")
        assertEquals(true, highPaintChild.get("ignored_by_paint_order").asBoolean(), "High paint order node should be marked as ignored")
    }

    @Test
    fun `serialize detects compound components correctly`() {
        val listItem = DOMTreeNodeEx(
            nodeId = 6,
            nodeName = "LI",
            elementHash = "li-hash"
        )
        val listNode = DOMTreeNodeEx(
            nodeId = 5,
            nodeName = "UL",
            elementHash = "ul-hash"
        )
        val rootOriginal = DOMTreeNodeEx(
            nodeId = 1,
            nodeName = "BODY",
            elementHash = "body-hash"
        )

        val simplified = TinyNode(
            originalNode = rootOriginal,
            children = listOf(
                TinyNode(
                    originalNode = listNode,
                    children = List(5) { TinyNode(originalNode = listItem) } // 5 children to meet threshold
                )
            )
        )

        val options = DOMStateBuilder.CompactOptions(
            enableCompoundComponentDetection = true,
            compoundComponentMinChildren = 3
        )
        val result = DOMStateBuilder.build(simplified, emptyList(), options)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

        val ulNode = tree.get("children").first()
        assertEquals(true, ulNode.get("is_compound_component").asBoolean(), "UL with multiple children should be detected as compound component")
    }

    @Test
    fun `serialize aligns attribute casing correctly`() {
        val node = DOMTreeNodeEx(
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

        val simplified = TinyNode(originalNode = node)

        val options = DOMStateBuilder.CompactOptions(
            enableAttributeCasingAlignment = true,
            preserveOriginalCasing = false
        )
        val result = DOMStateBuilder.build(simplified, listOf("class", "for", "readonly", "customattr"), options)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

        val attrs = tree.get("original_node").get("attributes")
        assertEquals("my-input", attrs.get("class").asText(), "className should be normalized to class")
        assertEquals("my-label", attrs.get("for").asText(), "htmlFor should be normalized to for")
        assertEquals("true", attrs.get("readonly").asText(), "READONLY should be normalized to readonly")
        assertEquals("value", attrs.get("customattr").asText(), "customAttr should be normalized to lowercase")
    }

    @Test
    fun `serialize builds enhanced selector map with multiple keys`() {
        val node = DOMTreeNodeEx(
            nodeId = 8,
            nodeName = "BUTTON",
            elementHash = "button-hash",
            xpath = "/html/body/div[1]/button[2]",
            backendNodeId = 12345
        )

        val simplified = TinyNode(originalNode = node)

        val result = DOMStateBuilder.build(simplified)

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
        val node = DOMTreeNodeEx(
            nodeId = 9,
            nodeName = "CustomElement",
            elementHash = "custom-hash"
        )

        val simplified = TinyNode(originalNode = node)

        val options = DOMStateBuilder.CompactOptions(
            preserveOriginalCasing = true
        )
        val result = DOMStateBuilder.build(simplified, emptyList(), options)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

        assertEquals("CustomElement", tree.get("original_node").get("node_name").asText(),
            "Original casing should be preserved when configured")
    }

    @Test
    fun `serialize handles deep tree end-to-end`() {
        val levels = 30

        // Build a deep chain of SlimNodes: node-1 -> node-2 -> ... -> node-29 -> node-30(leaf)
        var leaf: TinyNode = TinyNode(
            originalNode = DOMTreeNodeEx(
                nodeId = levels,
                nodeName = "SPAN",
                elementHash = "node-$levels"
            )
        )
        for (i in levels - 1 downTo 1) {
            val parentOriginal = DOMTreeNodeEx(
                nodeId = i,
                nodeName = "DIV",
                elementHash = "node-$i"
            )
            leaf = TinyNode(
                originalNode = parentOriginal,
                children = listOf(leaf)
            )
        }

        val result = DOMStateBuilder.build(leaf)
        val json = DOMStateBuilder.toJson(result.compactTree)
        val tree = mapper.readTree(json)

        // Traverse down the first-child chain and count levels
        var cursor = tree
        var count = 1 // count root
        while (cursor.has("children") && cursor.get("children").size() > 0) {
            cursor = cursor.get("children").first()
            count++
        }
        assertEquals(levels, count, "All $levels levels should be preserved in serialization")

        // The last node should be the SPAN leaf
        val lastNodeName = cursor.get("original_node").get("node_name").asText()
        assertEquals("span", lastNodeName)

        // Ensure selector map contains all element hash keys
        for (i in 1..levels) {
            assertTrue(result.selectorMap.containsKey("node-$i"), "selectorMap should contain element hash for node-$i")
        }
    }
}
