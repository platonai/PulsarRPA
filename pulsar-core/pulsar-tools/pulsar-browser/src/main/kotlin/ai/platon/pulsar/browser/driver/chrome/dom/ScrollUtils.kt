package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx

/**
 * Utility functions for scroll-related logic.
 * Maps to Python scroll detection logic in views.py
 */
object ScrollUtils {

    /**
     * Determine if a node is actually scrollable.
     * Maps to Python is_actually_scrollable function.
     *
     * Rules:
     * - Check CSS overflow properties (scroll or auto)
     * - Compare scrollRect vs clientRect dimensions
     * - Special handling for iframe/body/html elements
     * - Nested scroll container deduplication
     *
     * @param node The DOM node to check
     * @return true if the node is scrollable
     */
    fun isActuallyScrollable(node: DOMTreeNodeEx): Boolean {
        val snapshot = node.snapshotNode ?: return false
        val styles = snapshot.computedStyles ?: return false

        // Get overflow properties
        val overflow = styles["overflow"]
        val overflowX = styles["overflow-x"]
        val overflowY = styles["overflow-y"]

        // Check if any overflow property indicates scrollability
        val hasScrollOverflow = listOfNotNull(overflow, overflowX, overflowY)
            .any { it == "scroll" || it == "auto" }

        if (!hasScrollOverflow) return false

        // Get rects
        val clientRect = snapshot.clientRects
        val scrollRect = snapshot.scrollRects

        if (clientRect == null || scrollRect == null) return false

        // Special handling for iframe, body, html
        val tag = node.nodeName.uppercase()
        if (tag == "IFRAME" || tag == "FRAME") {
            // Iframes are scrollable if they have scroll dimensions
            return scrollRect.width > clientRect.width + 1.0 ||
                   scrollRect.height > clientRect.height + 1.0
        }

        if (tag == "BODY" || tag == "HTML") {
            // Body/HTML are scrollable if document is larger than viewport
            return scrollRect.width > clientRect.width + 1.0 ||
                   scrollRect.height > clientRect.height + 1.0
        }

        // For regular elements, check if scroll area is larger than client area
        val isHorizontallyScrollable = scrollRect.width > clientRect.width + 1.0
        val isVerticallyScrollable = scrollRect.height > clientRect.height + 1.0

        return isHorizontallyScrollable || isVerticallyScrollable
    }

    /**
     * Generate scroll information text for display.
     * Maps to Python get_scroll_info_text function.
     *
     * @param node The DOM node
     * @return Human-readable scroll info or null if not scrollable
     */
    fun getScrollInfoText(node: DOMTreeNodeEx): String? {
        if (!isActuallyScrollable(node)) return null

        val snapshot = node.snapshotNode ?: return null
        val clientRect = snapshot.clientRects ?: return null
        val scrollRect = snapshot.scrollRects ?: return null

        val tag = node.nodeName.uppercase()

        // Special formatting for iframes
        if (tag == "IFRAME" || tag == "FRAME") {
            return buildString {
                append("iframe scrollable")
                val id = node.attributes["id"]
                val name = node.attributes["name"]
                if (!id.isNullOrEmpty()) append(" id='$id'")
                else if (!name.isNullOrEmpty()) append(" name='$name'")
            }
        }

        // Regular elements - show dimensions
        return buildString {
            append("scrollable")

            val isHorizontal = scrollRect.width > clientRect.width + 1.0
            val isVertical = scrollRect.height > clientRect.height + 1.0

            if (isHorizontal && isVertical) {
                append(" (both)")
            } else if (isHorizontal) {
                append(" (horizontal)")
            } else if (isVertical) {
                append(" (vertical)")
            }

            append(" [${clientRect.width.toInt()}x${clientRect.height.toInt()}")
            append(" < ${scrollRect.width.toInt()}x${scrollRect.height.toInt()}]")
        }
    }

    /**
     * Check if scroll info should be displayed for this node.
     * Avoids duplicating scroll info for nested scroll containers.
     *
     * @param node The DOM node
     * @param ancestors List of ancestor nodes
     * @return true if scroll info should be shown
     */
    fun shouldShowScrollInfo(node: DOMTreeNodeEx, ancestors: List<DOMTreeNodeEx>): Boolean {
        if (!isActuallyScrollable(node)) return false

        // Check if any ancestor is also scrollable in the same direction
        val snapshot = node.snapshotNode ?: return true
        val clientRect = snapshot.clientRects ?: return true
        val scrollRect = snapshot.scrollRects ?: return true

        val nodeHorizontalScroll = scrollRect.width > clientRect.width + 1.0
        val nodeVerticalScroll = scrollRect.height > clientRect.height + 1.0

        // Look for scrollable ancestors
        for (ancestor in ancestors.reversed()) {
            if (!isActuallyScrollable(ancestor)) continue

            val ancestorSnapshot = ancestor.snapshotNode ?: continue
            val ancestorClient = ancestorSnapshot.clientRects ?: continue
            val ancestorScroll = ancestorSnapshot.scrollRects ?: continue

            val ancestorHorizontal = ancestorScroll.width > ancestorClient.width + 1.0
            val ancestorVertical = ancestorScroll.height > ancestorClient.height + 1.0

            // If ancestor scrolls in same direction, hide current node's scroll info
            if ((nodeHorizontalScroll && ancestorHorizontal) ||
                (nodeVerticalScroll && ancestorVertical)) {
                return false
            }
        }

        return true
    }

    /**
     * Calculate rect area for comparison.
     */
    private fun area(rect: DOMRect): Double {
        return rect.width * rect.height
    }
}
