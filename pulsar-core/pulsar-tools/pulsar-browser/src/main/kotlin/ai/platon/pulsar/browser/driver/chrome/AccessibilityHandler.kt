package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.common.AppContext
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.page.FrameTree
import java.util.LinkedHashMap

class AccessibilityHandler(
    private val devTools: RemoteDevTools,
    private val experimental: Boolean = true
) {
    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }
    private val accessibilityAPI get() = devTools.accessibility.takeIf { isActive }

    fun getAccessibilityTree(selector: String? = null, targetFrameId: String? = null) {
        // 1. Decide params
        val mainFrame = pageAPI?.frameTree?.frame
        if (targetFrameId != null && targetFrameId != mainFrame?.id) {
            // detect frame id
            TODO("Frame will be supported in the later version")
        }

        // 2. Fetch raw AX nodes using getFullAXTree
        val nodes = getFullAXTree(null, targetFrameId)

        // 3. Scrollable detection
//        val scrollableIds = findScrollableElementIds(
//            targetFrameId,
//        );
    }

    fun findScrollableElementIds(targetFrameId: String? = null) {

    }

    /**
     * Fetches the entire accessibility tree for the root Document
     *
     * @param depth The maximum depth at which descendants of the root node should be retrieved. If
     * omitted, the full tree is returned.
     * @param frameId The frame for whose document the AX tree should be retrieved. If omited, the
     * root frame is used.
     */
    fun getFullAXTree(depth: Int? = null, frameId: String? = null): List<AXNode> {
        return accessibilityAPI?.getFullAXTree(depth, frameId) ?: emptyList()
    }

    /**
     * Recursively collects AX trees for the target frame or every frame/iframe reachable from the
     * current page. Each returned node is annotated with a frameId so that callers can associate
     * AX information with DOM/backend nodes across frames.
     */
    fun getFullAXTreeRecursive(targetFrameId: String? = null, depth: Int? = null): AccessibilityTreeResult {
    val frameTree = pageAPI?.frameTree ?: return AccessibilityTreeResult.EMPTY
    val accessibility = accessibilityAPI ?: return AccessibilityTreeResult.EMPTY
        val idToTree = linkedMapOf<String, FrameTree>()
        collectFrameTree(frameTree, idToTree)

        val frameIds = when {
            targetFrameId != null && idToTree.containsKey(targetFrameId) -> listOf(targetFrameId)
            targetFrameId != null -> emptyList()
            else -> idToTree.keys.toList()
        }
        if (frameIds.isEmpty()) {
            if (targetFrameId != null) {
                val nodes = accessibility.getFullAXTree(depth, targetFrameId)
                return if (nodes.isEmpty()) AccessibilityTreeResult.EMPTY else singleFrameResult(nodes, targetFrameId)
            }
            return AccessibilityTreeResult.EMPTY
        }

        val allNodes = mutableListOf<AXNode>()
        val byFrame = LinkedHashMap<String, MutableList<AXNode>>(frameIds.size)
        val seenPairs = mutableSetOf<Pair<String?, String>>()

        frameIds.forEach { frameId ->
            val nodes = accessibility.getFullAXTree(depth, frameId)
            val targetList = byFrame.getOrPut(frameId) { mutableListOf() }
            nodes.forEach { node ->
                val stamped = stampFrameId(node, frameId)
                val key = stamped.frameId to stamped.nodeId
                if (seenPairs.add(key)) {
                    targetList += stamped
                    allNodes += stamped
                }
            }
        }

        return AccessibilityTreeResult(
            nodes = allNodes,
            nodesByFrameId = byFrame.mapValues { it.value.toList() }
        )
    }

    data class AccessibilityTreeResult(
        val nodes: List<AXNode>,
        val nodesByFrameId: Map<String, List<AXNode>>,
    ) {
        companion object {
            val EMPTY = AccessibilityTreeResult(emptyList(), emptyMap())
        }
    }

    private fun collectFrameTree(tree: FrameTree, acc: MutableMap<String, FrameTree>) {
        val frame = tree.frame
        val frameId = frame?.id
        if (!frameId.isNullOrBlank()) {
            acc.putIfAbsent(frameId, tree)
        }
        tree.childFrames?.forEach { child ->
            collectFrameTree(child, acc)
        }
    }

    private fun stampFrameId(node: AXNode, frameId: String): AXNode {
        if (node.frameId == null) {
            node.frameId = frameId
        }
        return node
    }

    private fun singleFrameResult(nodes: List<AXNode>, frameId: String): AccessibilityTreeResult {
        val stamped = nodes.map { stampFrameId(it, frameId) }
        return AccessibilityTreeResult(
            nodes = stamped,
            nodesByFrameId = mapOf(frameId to stamped)
        )
    }

}
