package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.common.AppContext
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.page.FrameTree
import java.util.LinkedHashMap
import kotlin.collections.ArrayDeque

class AccessibilityHandler(
    private val devTools: RemoteDevTools,
    private val experimental: Boolean = true
) {
    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
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
        val page = pageAPI ?: return AccessibilityTreeResult.EMPTY
        val accessibility = accessibilityAPI ?: return AccessibilityTreeResult.EMPTY
        val frameTree = page.frameTree ?: return AccessibilityTreeResult.EMPTY

        val frameById = linkedMapOf<String, FrameTree>()
        collectFrameTree(frameTree, frameById)

        val frameIds: List<String> = when {
            targetFrameId == null -> frameById.keys.toList()
            frameById.containsKey(targetFrameId) -> listOf(targetFrameId)
            else -> emptyList()
        }

        if (frameIds.isEmpty()) {
            if (targetFrameId != null) {
                val nodes = accessibility.getFullAXTree(depth, targetFrameId)
                return if (nodes.isEmpty()) AccessibilityTreeResult.EMPTY else singleFrameResult(nodes, targetFrameId)
            }
            return AccessibilityTreeResult.EMPTY
        }

        val seenPairs = mutableSetOf<Pair<String?, String>>()
        val allNodes = mutableListOf<AXNode>()
        val byFrame = LinkedHashMap<String, MutableList<AXNode>>(frameIds.size)
        val byBackend = LinkedHashMap<Int, MutableList<AXNode>>()

        frameIds.forEach { frameId ->
            val nodes = accessibility.getFullAXTree(depth, frameId)
            val frameBucket = byFrame.getOrPut(frameId) { mutableListOf() }
            nodes.forEach { node ->
                val stamped = stampFrameId(node, frameId)
                val key = stamped.frameId to stamped.nodeId
                if (seenPairs.add(key)) {
                    frameBucket += stamped
                    allNodes += stamped
                    val backendId = stamped.backendDOMNodeId
                    if (backendId != null) {
                        byBackend.getOrPut(backendId) { mutableListOf() } += stamped
                    }
                }
            }
        }

        return AccessibilityTreeResult(
            nodes = allNodes,
            nodesByFrameId = byFrame.mapValues { it.value.toList() },
            nodesByBackendNodeId = byBackend.mapValues { it.value.toList() }
        )
    }

    data class AccessibilityTreeResult(
        val nodes: List<AXNode>,
        val nodesByFrameId: Map<String, List<AXNode>>,
        val nodesByBackendNodeId: Map<Int, List<AXNode>>,
    ) {
        companion object {
            val EMPTY = AccessibilityTreeResult(emptyList(), emptyMap(), emptyMap())
        }
    }

    private fun collectFrameTree(tree: FrameTree, acc: MutableMap<String, FrameTree>) {
        val queue = ArrayDeque<FrameTree>()
        queue.add(tree)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val frame = current.frame
            val frameId = frame?.id
            if (!frameId.isNullOrBlank()) {
                acc.putIfAbsent(frameId, current)
            }
            current.childFrames?.forEach { queue.add(it) }
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
        val backendBuckets = mutableMapOf<Int, MutableList<AXNode>>()
        stamped.forEach { node ->
            val backendId = node.backendDOMNodeId ?: return@forEach
            backendBuckets.getOrPut(backendId) { mutableListOf() } += node
        }
        return AccessibilityTreeResult(
            nodes = stamped,
            nodesByFrameId = mapOf(frameId to stamped),
            nodesByBackendNodeId = backendBuckets.mapValues { it.value.toList() }
        )
    }

}
