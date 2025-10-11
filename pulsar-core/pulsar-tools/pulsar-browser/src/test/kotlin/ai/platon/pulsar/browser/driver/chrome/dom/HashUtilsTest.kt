package ai.platon.pulsar.browser.driver.chrome.dom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HashUtilsTest {
    @Test
    fun `elementHash is stable for same node`() {
        val node1 = EnhancedDOMTreeNode(
            nodeName = "DIV",
            backendNodeId = 123,
            attributes = mapOf("id" to "foo", "class" to "bar baz"),
            axRole = "button"
        )
        val node2 = EnhancedDOMTreeNode(
            nodeName = "DIV",
            backendNodeId = 123,
            attributes = mapOf("class" to "baz bar", "id" to "foo"),
            axRole = "button"
        )
        val h1 = HashUtils.elementHash(node1)
        val h2 = HashUtils.elementHash(node2)
        assertEquals(h1, h2, "Hash should be stable and order-insensitive for classes")
    }
}
