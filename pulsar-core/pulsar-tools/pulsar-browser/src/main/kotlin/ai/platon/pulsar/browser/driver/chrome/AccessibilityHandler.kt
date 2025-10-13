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

    /**
     * Get accessibility tree with optional selector filtering and scrollable detection.
     *
     * @param selector Optional CSS selector to filter nodes
     * @param targetFrameId Optional frame ID to target specific frame
     * @return Accessibility tree result with filtered nodes
     */
    fun getAccessibilityTree(selector: String? = null, targetFrameId: String? = null): AccessibilityTreeResult {
        // 1. Fetch raw AX nodes using getFullAXTree
        val axResult = getFullAXTreeRecursive(targetFrameId, depth = null)

        if (selector.isNullOrBlank()) {
            return axResult
        }

        // 2. Apply selector filtering if provided
        return filterNodesBySelector(axResult, selector)
    }

    /**
     * Filter accessibility nodes by CSS selector.
     * This uses Runtime.evaluate to find matching DOM nodes and then filters AX nodes by backendNodeId.
     */
    private fun filterNodesBySelector(axResult: AccessibilityTreeResult, selector: String): AccessibilityTreeResult {
        val runtime = devTools.runtime.takeIf { isActive } ?: return axResult

        // Find DOM nodes matching the selector
        val evaluation = runtime.evaluate("""
            Array.from(document.querySelectorAll('$selector')).map(el => {
                const id = el.backendNodeId || (() => {
                    const walker = document.createTreeWalker(
                        document,
                        NodeFilter.SHOW_ELEMENT,
                        null,
                        false
                    );
                    let node;
                    while (node = walker.nextNode()) {
                        if (node === el) return node.backendNodeId;
                    }
                    return null;
                })();
                return id;
            }).filter(id => id != null)
        """.trimIndent())

        val matchingBackendIds = try {
            evaluation?.result?.value?.toString()
                ?.removeSurrounding("[", "]")
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        if (matchingBackendIds.isEmpty()) {
            return AccessibilityTreeResult.EMPTY
        }

        // Filter AX nodes by matching backendNodeIds
        val filteredNodes = axResult.nodes.filter { node ->
            node.backendDOMNodeId in matchingBackendIds
        }

        return AccessibilityTreeResult(
            nodes = filteredNodes,
            nodesByFrameId = filteredNodes.groupBy { it.frameId ?: "" },
            nodesByBackendNodeId = filteredNodes.groupBy { it.backendDOMNodeId ?: -1 }
        )
    }

    /**
     * Find scrollable element IDs using AX properties and DOM evaluation.
     *
     * @param targetFrameId Optional frame ID to target specific frame
     * @return List of backend node IDs that are scrollable
     */
    fun findScrollableElementIds(targetFrameId: String? = null): List<Int> {
        val axResult = getFullAXTreeRecursive(targetFrameId, depth = null)
        val runtime = devTools.runtime.takeIf { isActive }

        if (axResult.nodes.isEmpty() || runtime == null) {
            return emptyList()
        }

        val scrollableBackendIds = mutableSetOf<Int>()

        // First pass: identify scrollable nodes from AX properties
        axResult.nodes.forEach { node ->
            val backendId = node.backendDOMNodeId ?: return@forEach

            // Check if AX node indicates scrollable
            val isScrollableAX = node.properties?.any { prop ->
                prop.name.toString().lowercase() == "scrollable" &&
                prop.value?.value == true
            } ?: false

            if (isScrollableAX) {
                scrollableBackendIds.add(backendId)
            }
        }

        // Second pass: verify with DOM evaluation for additional scrollable elements
        try {
            val evaluation = runtime.evaluate("""
                (() => {
                    const scrollables = [];
                    const walker = document.createTreeWalker(
                        document,
                        NodeFilter.SHOW_ELEMENT,
                        null,
                        false
                    );

                    let node;
                    while (node = walker.nextNode()) {
                        const style = window.getComputedStyle(node);
                        const hasOverflow = style.overflow === 'auto' ||
                                          style.overflow === 'scroll' ||
                                          style.overflowX === 'auto' ||
                                          style.overflowX === 'scroll' ||
                                          style.overflowY === 'auto' ||
                                          style.overflowY === 'scroll';

                        if (hasOverflow && node.scrollHeight > node.clientHeight + 1) {
                            scrollables.push(node.backendNodeId);
                        }
                    }
                    return scrollables;
                })()
            """.trimIndent())

            val additionalIds = evaluation?.result?.value?.toString()
                ?.removeSurrounding("[", "]")
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()

            scrollableBackendIds.addAll(additionalIds)
        } catch (e: Exception) {
            // Fallback to AX-only detection
        }

        return scrollableBackendIds.toList()
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

        val frameTree = try {
            page.getFrameTree()
        } catch (e: Exception) {
            page.frameTree
        } ?: return AccessibilityTreeResult.EMPTY

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
            val nodes = runCatching { accessibility.getFullAXTree(depth, frameId) }
                .getOrElse { emptyList() }
            if (nodes.isEmpty()) {
                return@forEach
            }
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
