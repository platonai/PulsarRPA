package ai.platon.pulsar.browser.driver.chrome.dom.model

import ai.platon.pulsar.common.Strings
import org.apache.commons.lang3.StringUtils
import java.util.ArrayList

class MicroDOMTreeNodeHelper(
    private val root: MicroDOMTreeNode,
    private val seenChunks: MutableList<Pair<Double, Double>>,
    private val maxNonInteractiveTextLength: Int = 200,
) {
    companion object {
        fun slimHTML(n: MicroDOMTreeNode): String? {
            val o = n.originalNode ?: return null

            fun normalizeAttrValue(attrValue: Any?): String? {
                if (attrValue == null) return null
                val compacted = Strings.compactWhitespaces(attrValue.toString().trim())
                return Strings.singleQuoteIfContainsWhitespace(compacted)
            }

            val attrs = o.attributes
                ?.mapNotNull { (it.key to normalizeAttrValue(it.value)) }
                ?.joinToString(" ", " ") { (k, v) -> "$k=$v" }
                ?: ""
            val nodeName = o.nodeName
            val nodeValue = Strings.compactWhitespaces(o.nodeValue)
            return if (nodeValue == null) {
                "<${nodeName}$attrs />"
            } else {
                """<${nodeName}$attrs>$nodeValue</${nodeName}>"""
            }
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
                nonInteractiveText(o)?.let { nonInteractiveTexts += o.nodeId to it }
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

        return InteractiveDOMTreeNodeList(nodes)
    }

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
            viewportIndex = o.viewportIndex,
            microTreeNode = n,
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
