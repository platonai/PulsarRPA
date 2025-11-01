package ai.platon.pulsar.browser.driver.chrome.dom.model

import java.util.ArrayList

class MicroDOMTreeNodeHelper(
    private val node: MicroDOMTreeNode,
    private val seenChunks: MutableList<Pair<Double, Double>>
) {
    fun toNanoTreeInViewport0(
        microTree: MicroDOMTreeNode,
        viewportHeight: Int,
        viewportIndex: Int = 1,
        scale: Double = 1.0
    ): NanoDOMTree {
        require(viewportIndex >= 1) { "viewportIndex must be >= 1, but was $viewportIndex" }
        require(viewportHeight > 0) { "viewportHeight must be > 0, but was $viewportHeight" }

        val deltaFactor = (scale - 1).coerceAtLeast(0.0)
        val baseY = (viewportIndex - 1) * viewportHeight
        val startY = baseY - (deltaFactor * viewportHeight)
        val endY = baseY + viewportHeight + (deltaFactor * viewportHeight)

        return toNanoTreeInRange0(microTree, startY, endY)
    }

    fun toNanoTreeInRange0(microTree: MicroDOMTree, startY: Double = 0.0, endY: Double = 100000.0): NanoDOMTree {
        val tree = toNanoTreeInRangeRecursive(microTree, startY, endY)

        val merged = mergeChunks()
        seenChunks.clear()
        seenChunks.addAll(merged)

        return tree
    }

    fun toNanoTreeInRangeRecursive(microNode: MicroDOMTreeNode, startY: Double = 0.0, endY: Double = 100000.0): NanoDOMTree {
        // Create the current node from the micro node
        val root = newNode(microNode) ?: return NanoDOMTree()

        seenChunks.add(Pair(startY, endY))

        // Recursively create child nano nodes, filter out empty placeholders
        val childNanoList = microNode.children
            ?.asSequence()
            ?.filter { child -> !(child.children == null || child.children.isEmpty()) } /* non-empty children */
            ?.filter { inYRange(it, startY, endY) }
            ?.map { toNanoTreeInRangeRecursive(it, startY, endY) }
            ?.toList()

        return if (childNanoList.isNullOrEmpty()) root else root.copy(children = childNanoList)
    }

    private fun newNode(n: MicroDOMTreeNode?): NanoDOMTree? {
        val o = n?.originalNode ?: return null

        // remove locator's prefix to reduce serialized size
        return NanoDOMTree(
            o.locator.substringAfterLast(":"),
            o.nodeName,
            o.nodeValue,
            o.attributes,
            scrollable = o.isScrollable,
            interactive = o.isInteractable,
            // All nodes are visible unless `invisible` == true explicitly.
            invisible = if (o.isVisible == true) null else true,
            clientRects = o.clientRects?.round(),
            scrollRects = o.scrollRects?.round(),
            bounds = o.bounds?.round(),
            absoluteBounds = o.absoluteBounds?.round(),
            viewportIndex = o.viewportIndex
        )
    }

    fun inYRange(no: MicroDOMTreeNode, startY: Double, endY: Double): Boolean {
        val b = no.originalNode?.bounds ?: return false
        val y = b.y ?: return false
        return y in startY..<endY
    }

    fun mergeChunks(): List<Pair<Double, Double>> {
        // merge chunks in seenChunks that intersects
        if (seenChunks.isEmpty()) return emptyList()

        val eps = 1e-6
        // Normalize and sort by start
        val sorted = seenChunks
            .map { (s, e) -> if (s <= e) s to e else e to s }
            .sortedBy { it.first }

        val merged = ArrayList<Pair<Double, Double>>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            val (cStart, cEnd) = current
            val (nStart, nEnd) = next
            // Intersects or touches within epsilon
            if (nStart <= cEnd + eps) {
                // merge by extending the end
                current = cStart to maxOf(cEnd, nEnd)
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        return merged
    }
}
