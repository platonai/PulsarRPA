package ai.platon.pulsar.browser.driver.chrome.dom.model

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.math.roundTo
import org.apache.commons.lang3.StringUtils
import kotlin.math.max

class MicroToNanoTreeHelper(
    private val microTree: MicroDOMTreeNode,
    private val seenChunks: MutableList<Pair<Double, Double>>,
) {
    private val logger = getLogger(this)

    private var numNodes = 0

    fun toNanoTreeInViewport(
        viewportHeight: Double,
        viewportIndex: Int = 1,
        scale: Double = 1.0
    ): NanoDOMTree {
        require(viewportIndex >= 1) { "viewportIndex must be >= 1, but was $viewportIndex" }
        require(viewportHeight > 0) { "viewportHeight must be > 0, but was $viewportHeight" }

        val deltaFactor = (scale - 1).coerceAtLeast(0.0)
        val baseY = (viewportIndex - 1) * viewportHeight
        val startY = baseY - (deltaFactor * viewportHeight)
        val endY = baseY + viewportHeight + (deltaFactor * viewportHeight)

        return toNanoTreeInRange(startY, endY)
    }

    fun toNanoTreeInRange(startY: Double = 0.0, endY: Double = 100000.0): NanoDOMTree {
        val tree = toNanoTreeInRangeRecursive(microTree, startY, endY)

        if (seenChunks.size > 1) {
            val merged = mergeChunks()
            seenChunks.clear()
            merged.map { max(0.0, it.first) to max(0.0, it.second) }.toCollection(seenChunks)
        }

        val y1 = startY.roundTo(1)
        val y2 = endY.roundTo(1)
        logger.info("Nano-tree generated | nodes: $numNodes | Y-axis: ($y1, $y2] | seen chunks: $seenChunks")

        return tree
    }

    fun toNanoTreeInRangeRecursive(
        microNode: MicroDOMTreeNode,
        startY: Double = 0.0,
        endY: Double = 100000.0
    ): NanoDOMTree {
        // Create the current node from the micro node
        val root = newNode(microNode) ?: return NanoDOMTree()

        // Recursively create child nano nodes, filter out empty placeholders
        val childNanoList = microNode.children
            ?.asSequence()
            ?.filter { child -> !child.children.isNullOrEmpty() } /* non-empty children */
            ?.filter { inYRange(it, startY, endY) }
            ?.map { toNanoTreeInRangeRecursive(it, startY, endY) }
            ?.toList()

        return if (childNanoList.isNullOrEmpty()) {
            root
        } else {
            val y1 = childNanoList.minOf { it.bounds?.y ?: 100000.0 } // remove nulls
            val y2 = childNanoList.maxOf { it.bounds?.y ?: -100000.0 } // remove nulls

            seenChunks.add(Pair(y1, y2))
            numNodes++
            root.copy(children = childNanoList)
        }
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
            viewportIndex = o.viewportIndex,
            microTreeNode = n,
        )
    }

    /**
     * Returns true if any portion of the node lies within the vertical interval (startY, endY],
     * where startY and endY are coordinates along the page’s y-axis.
     * */
    fun inYRange(no: MicroDOMTreeNode, startY: Double, endY: Double): Boolean {
        // Invalid interval: (startY, endY] must have start < end
        if (startY.isNaN() || endY.isNaN() || startY >= endY) return false

        val o = no.originalNode ?: return false
        // Prefer absolute page coordinates first, then bounds, then client rects
        val r = (o.absoluteBounds ?: o.bounds ?: o.clientRects)?.uncompact() ?: return false
        val y = r.y
        val h = r.height

        // If height is missing or non-positive, treat it as a point at y
        if (!(h > 0.0)) {
            // open at start, closed at end: (startY, endY]
            return y > startY && y <= endY
        }

        val top = y
        val bottom = y + h
        // Any overlap between [top, bottom] and (startY, endY]
        // Open at start (>) and closed at end (<=)
        return top <= endY && bottom > startY
    }

    fun mergeChunks(): List<Pair<Double, Double>> {
        // merge chunks in seenChunks that intersects
        if (seenChunks.isEmpty()) {
            return emptyList()
        }
        if (seenChunks.size == 1) {
            return seenChunks
        }

        val eps = 50
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

class MicroDOMTreeNodeHelper(
    private val root: MicroDOMTreeNode,
    private val seenChunks: MutableList<Pair<Double, Double>>,
    private val currentViewportIndex: Int,
    private val maxViewportIndex: Int = 10000,
    private val maxNonInteractiveTextLength: Int = 100,
) {
    companion object {
        fun slimHTML(n: MicroDOMTreeNode): String? {
            val o = n.originalNode ?: return null

            val nodeName = o.nodeName
            val nodeValue = Strings.compactWhitespaces(o.nodeValue)

            fun normalizeAttrValue(attrValue: Any?): String? {
                if (attrValue == null) return null
                val compacted = Strings.compactWhitespaces(attrValue.toString().trim())
                return Strings.singleQuoteIfNonAlphanumeric(compacted)
            }

            val attrs = o.attributes
                ?.mapNotNull { (it.key to normalizeAttrValue(it.value)) }
                ?.joinToString(" ", " ") { (k, v) -> "$k=$v" }
                ?: ""
            return if (nodeValue == null) {
                "<${nodeName}$attrs />"
            } else {
                """<${nodeName}$attrs>$nodeValue</${nodeName}>"""
            }
        }

        fun estimatedSize(n: InteractiveDOMTreeNode): Int {
            return "[0,812]{1}(369,1659,87,13)".length + (n.textBefore?.length ?: 0) + (n.slimHTML?.length ?: 0)
        }

        fun estimatedSize(nodes: List<InteractiveDOMTreeNode>): Int {
            return nodes.sumOf { estimatedSize(it) }
        }
    }

    fun toInteractiveDOMTreeNodeList(): InteractiveDOMTreeNodeList {
        val collected = mutableListOf<InteractiveDOMTreeNode>()

        // Keep mapping from interactive index -> backing DOM node id
        val interactiveNodeIdByIndex = mutableMapOf<Int, Int>()
        // Collect non-interactive node texts with their nodeIds
        val nonInteractiveTexts = mutableListOf<Pair<Int, String>>()

        fun nonInteractiveText(o: CleanedDOMTreeNode): String? {
            val sb = StringBuilder()
            fun appendToken(v: Any?) {
                val t = v?.toString()?.trim()
                if (!t.isNullOrEmpty()) {
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(t)
                }
            }
            // Prefer nodeValue
            appendToken(o.nodeValue)
            // Include meaningful attributes if any
            val attrs = o.attributes
            if (!attrs.isNullOrEmpty()) {
                DefaultIncludeAttributes.ATTRIBUTES.forEach { key ->
                    attrs[key]?.let { appendToken(it) }
                }
            }
            val s = Strings.compactWhitespaces(sb.toString())
            return s.ifEmpty { null }
        }

        fun visit(n: MicroDOMTreeNode) {
            val o = n.originalNode
            val idx = n.interactiveIndex
            if (o != null && idx != null) {
                collected += InteractiveDOMTreeNode(
                    interactiveIndex = idx,
                    // remove prefix to reduce serialized size, align with Nano tree
                    locator = o.locator.substringAfterLast(":"),
                    slimHTML = slimHTML(n),
                    textBefore = null,
                    viewportIndex = o.viewportIndex,
                    scrollable = o.isScrollable?.takeIf { it },
                    // All nodes are visible unless `invisible` == true explicitly
                    invisible = if (o.isVisible == true) null else true,
                    bounds = o.bounds?.round(),
                    clientRects = o.clientRects?.round(),
                    scrollRects = o.scrollRects?.round(),
                    absoluteBounds = o.absoluteBounds?.round(),
                    prevInteractiveIndex = null,
                    nextInteractiveIndex = null,
                )
                interactiveNodeIdByIndex[idx] = o.nodeId
            } else if (o != null) {
                val order = o.paintOrder ?: o.backendNodeId ?: o.nodeId
                nonInteractiveText(o)?.let { nonInteractiveTexts += (order to it) }
            }
            n.children?.forEach { visit(it) }
        }

        visit(root)

        // Build a map from interactiveIndex -> concatenated text of non-interactive nodes
        val textBeforeByInteractiveIndex = mutableMapOf<Int, String?>()
        val sortedInteractiveIdx = interactiveNodeIdByIndex.keys.sorted()
        if (sortedInteractiveIdx.isNotEmpty()) {
            // Sort non-interactive entries by nodeId once for efficient slicing
            val nonInteractiveSorted = nonInteractiveTexts.sortedBy { it.first }
            for (i in 0 until sortedInteractiveIdx.size) {
                val currentIdx = sortedInteractiveIdx[i]
                val currentNodeId = interactiveNodeIdByIndex[currentIdx] ?: continue
                val nextIdx = sortedInteractiveIdx.getOrNull(i + 1)
                if (nextIdx == null) {
                    // No next interactive node -> leave null
                    textBeforeByInteractiveIndex[currentIdx] = null
                    continue
                }
                val nextNodeId = interactiveNodeIdByIndex[nextIdx] ?: continue
                val low = minOf(currentNodeId, nextNodeId)
                val high = maxOf(currentNodeId, nextNodeId)
                val texts = nonInteractiveSorted
                    .asSequence()
                    .filter { (nodeId, _) -> nodeId in (low + 1)..<high }
                    .map { it.second }
                    .filter { it.isNotBlank() }
                    .toList()
                val joined = Strings.compactWhitespaces(texts.joinToString(" "))
                textBeforeByInteractiveIndex[currentIdx] = joined.ifEmpty { null }
            }
        }

        // Sort by interactive index, then by locator for stability and fill prev/next/textUntilNextNode
        val sorted = collected.sortedWith(compareBy({ it.interactiveIndex }, { it.locator ?: "" }))
        val nodes = sorted.mapIndexed { i, it ->
            val prev = if (i > 0) sorted[i - 1].interactiveIndex else null
            val next = if (i < sorted.lastIndex) sorted[i + 1].interactiveIndex else null
            var textBefore = textBeforeByInteractiveIndex.getOrDefault(it.interactiveIndex - 1, null)
            textBefore = textBefore?.takeIf { it.isNotBlank() }
                ?.let { StringUtils.abbreviateMiddle(textBefore, "...", maxNonInteractiveTextLength) }
            it.copy(
                prevInteractiveIndex = prev,
                nextInteractiveIndex = next,
                textBefore = textBefore
            )
        }

        val desiredViewports = mutableSetOf(1, 2, currentViewportIndex, maxViewportIndex)
        var shorterNodeList = nodes.filter { (it.viewportIndex ?: -1) in desiredViewports }

        fun goodSize() = estimatedSize(shorterNodeList) <= 100_000

        if (goodSize()) return InteractiveDOMTreeNodeList(shorterNodeList)

        shorterNodeList = shorterNodeList.map {
            it.copy(textBefore = StringUtils.abbreviateMiddle(it.textBefore ?: "", "...", 50))
        }
        if (goodSize()) return InteractiveDOMTreeNodeList(shorterNodeList)

        val discardViewportIndexes = listOf(2, maxViewportIndex, 1)
        discardViewportIndexes.forEach { discardViewportIndex ->
            shorterNodeList = shorterNodeList.filterNot { it.viewportIndex == discardViewportIndex }
            if (goodSize()) return InteractiveDOMTreeNodeList(shorterNodeList)
        }

        shorterNodeList = shorterNodeList.filterNot { it.isAnchor() }
        if (goodSize()) return InteractiveDOMTreeNodeList(shorterNodeList)

        return InteractiveDOMTreeNodeList(shorterNodeList)
    }
}
