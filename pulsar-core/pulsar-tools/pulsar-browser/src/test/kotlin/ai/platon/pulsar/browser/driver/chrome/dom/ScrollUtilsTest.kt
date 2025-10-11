package ai.platon.pulsar.browser.driver.chrome.dom

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrollUtilsTest {

    @Test
    fun `isActuallyScrollable returns true when scroll rect exceeds client rect`() {
        val node = EnhancedDOMTreeNode(
            nodeName = "DIV",
            computedStyles = mapOf("overflow" to "auto"),
            clientRect = listOf(0.0, 0.0, 0.0, 0.0, 200.0, 150.0, 200.0, 150.0),
            scrollRect = listOf(0.0, 0.0, 0.0, 0.0, 400.0, 300.0, 400.0, 300.0)
        )

        assertTrue(ScrollUtils.isActuallyScrollable(node))
    }

    @Test
    fun `isActuallyScrollable returns false when overflow hidden`() {
        val node = EnhancedDOMTreeNode(
            nodeName = "DIV",
            computedStyles = mapOf("overflow" to "hidden"),
            clientRect = listOf(0.0, 0.0, 0.0, 0.0, 200.0, 150.0, 200.0, 150.0),
            scrollRect = listOf(0.0, 0.0, 0.0, 0.0, 400.0, 300.0, 400.0, 300.0)
        )

        assertFalse(ScrollUtils.isActuallyScrollable(node))
    }
}
