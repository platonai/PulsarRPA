package ai.platon.pulsar.browser.driver.chrome.dom

object ScrollUtils {
    /**
     * Rough predicate: scrollable if overflow is scroll/auto and clientRect smaller than scrollRect.
     * This will be refined to match Python's is_actually_scrollable.
     */
    fun isActuallyScrollable(node: EnhancedDOMTreeNode): Boolean {
        val styles = node.computedStyles ?: return false
        val overflow = styles["overflow"] ?: styles["overflow-y"] ?: styles["overflow-x"]
        val isOverflowScroll = overflow == "scroll" || overflow == "auto"
        val client = node.clientRect
        val scroll = node.scrollRect
        val hasScroll = client != null && scroll != null && area(scroll) > area(client) + 0.5
        return isOverflowScroll && hasScroll
    }

    private fun area(rect: List<Double>): Double {
        if (rect.size < 4) return 0.0
        // Many CDP rects are 8 numbers; fallback to width/height using (x2-x1)*(y3-y2) if 8 present
        return if (rect.size >= 8) {
            val width = (rect[2] - rect[0]).coerceAtLeast(0.0)
            val height = (rect[5] - rect[1]).coerceAtLeast(0.0)
            width * height
        } else {
            0.0
        }
    }
}
