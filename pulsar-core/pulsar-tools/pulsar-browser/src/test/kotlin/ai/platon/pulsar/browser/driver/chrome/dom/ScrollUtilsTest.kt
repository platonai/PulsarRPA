package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotNodeEx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrollUtilsTest {

    @Test
    fun `isActuallyScrollable returns true when scroll rect exceeds client rect`() {
        val node = DOMTreeNodeEx(
            nodeName = "DIV",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow" to "auto"),
                clientRects = DOMRect(0.0, 0.0, 200.0, 150.0),
                scrollRects = DOMRect(0.0, 0.0, 400.0, 300.0)
            )
        )

        assertTrue(ScrollUtils.isActuallyScrollable(node))
    }

    @Test
    fun `isActuallyScrollable returns false when overflow hidden`() {
        val node = DOMTreeNodeEx(
            nodeName = "DIV",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow" to "hidden"),
                clientRects = DOMRect(0.0, 0.0, 200.0, 150.0),
                scrollRects = DOMRect(0.0, 0.0, 400.0, 300.0)
            )
        )

        assertFalse(ScrollUtils.isActuallyScrollable(node))
    }

    @Test
    fun `shouldShowScrollInfo hides nested scroll containers`() {
        val outer = DOMTreeNodeEx(
            nodeName = "DIV",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow" to "auto"),
                clientRects = DOMRect(0.0, 0.0, 300.0, 300.0),
                scrollRects = DOMRect(0.0, 0.0, 600.0, 600.0)
            )
        )
        val inner = DOMTreeNodeEx(
            nodeName = "DIV",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow" to "auto"),
                clientRects = DOMRect(0.0, 0.0, 150.0, 150.0),
                scrollRects = DOMRect(0.0, 0.0, 400.0, 400.0)
            )
        )

        assertTrue(ScrollUtils.shouldShowScrollInfo(outer, emptyList()))
        assertFalse(ScrollUtils.shouldShowScrollInfo(inner, listOf(outer)))
    }

    @Test
    fun `getScrollInfoText describes dominant scroll axes`() {
        val node = DOMTreeNodeEx(
            nodeName = "DIV",
            snapshotNode = SnapshotNodeEx(
                computedStyles = mapOf("overflow-x" to "auto", "overflow-y" to "hidden"),
                clientRects = DOMRect(0.0, 0.0, 150.0, 150.0),
                scrollRects = DOMRect(0.0, 0.0, 300.0, 150.0)
            )
        )

        val info = ScrollUtils.getScrollInfoText(node)
        assertEquals("scrollable (horizontal) [150x150 < 300x150]", info)
    }
}
