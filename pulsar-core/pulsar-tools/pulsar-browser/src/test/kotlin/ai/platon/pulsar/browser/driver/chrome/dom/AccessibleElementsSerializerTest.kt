package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AccessibleElementsSerializerTest {

    @Test
    fun `bbox filtering excludes contained non-exception children`() {
        // Parent propagating element: <div role="button"> with bounds 0,0,100x40
        val parent = DOMTreeNodeEx(
            nodeId = 1,
            nodeName = "DIV",
            attributes = mapOf("role" to "button"),
            snapshotNode = SnapshotNodeEx(bounds = DOMRect(0.0, 0.0, 100.0, 40.0)),
            isVisible = true
        )
        // Child element fully inside: <span>
        val childInside = DOMTreeNodeEx(
            nodeId = 2,
            nodeName = "SPAN",
            snapshotNode = SnapshotNodeEx(bounds = DOMRect(5.0, 5.0, 20.0, 10.0)),
            isVisible = true
        )
        val root = parent.copy(children = listOf(childInside))

        val slim = AccessibleElementsSerializer(root, enableBBoxFiltering = true).run()
        assertNotNull(slim)
        val childSlim = slim!!.children.firstOrNull()
        assertNotNull(childSlim)
        assertTrue(childSlim!!.excludedByParent, "Child inside propagating bounds should be excluded by parent")
    }

    @Test
    fun `interactive indices assigned only to visible and interactive nodes and appear in selector map`() {
        // Two interactive visible buttons
        val btn1 = DOMTreeNodeEx(
            nodeId = 10,
            nodeName = "BUTTON",
            isVisible = true,
            isInteractable = true,
            elementHash = "btn1"
        )
        val btn2 = DOMTreeNodeEx(
            nodeId = 11,
            nodeName = "A",
            isVisible = true,
            isInteractable = true,
            elementHash = "btn2"
        )
        val root = DOMTreeNodeEx(
            nodeId = 9,
            nodeName = "DIV",
            isVisible = true,
            children = listOf(btn1, btn2)
        )

        val slim = AccessibleElementsSerializer(root, enableBBoxFiltering = false).run()
        assertNotNull(slim)

        // Serialize to build selector map with index:* entries
        val state = DomSerializer.serialize(slim!!)
        val keys = state.selectorMap.keys
        assertTrue(keys.any { it == "index:1" }, "Selector map should include index:1")
        assertTrue(keys.any { it == "index:2" }, "Selector map should include index:2")
    }
}

