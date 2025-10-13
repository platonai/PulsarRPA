package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.common.getLogger
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.page.FrameTree
import java.util.LinkedHashMap
import kotlin.collections.forEach
import kotlin.collections.plusAssign

class AccessibilityHandler(
    private val devTools: RemoteDevTools,
    private val experimental: Boolean = true
) {
    private val logger = getLogger(this)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }

    private val isActive get() = devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val accessibilityAPI get() = devTools.accessibility.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }

    @Volatile
    private var accessibilityEnabled = false

    private fun ensureEnabled() {
        if (!isActive) return
        // Enable Page/DOM domains to stabilize frame tree & AX associations
        runCatching { devTools.page.enable() }
        runCatching { devTools.dom.enable() }

        val a11y = accessibilityAPI ?: return
        if (!accessibilityEnabled) {
            runCatching { a11y.enable() }
                .onFailure { e -> logger.warn("Accessibility.enable failed | err={}", e.toString()) }
            accessibilityEnabled = true
        }
    }

    /**
     * Get accessibility tree with optional selector filtering and scrollable detection.
     *
     * @param selector Optional CSS selector to filter nodes
     * @param targetFrameId Optional frame ID to target specific frame
     * @return Accessibility tree result with filtered nodes
     */
    fun getAccessibilityTree(selector: String? = null, targetFrameId: String? = null): AccessibilityTreeResult {
        // Ensure the Accessibility domain is enabled
        ensureEnabled()

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
     * Uses DOM.querySelectorAll + describeNode to map matched frontend nodeIds to backendNodeIds,
     * then filters AX nodes by those backendNodeIds.
     */
    private fun filterNodesBySelector(axResult: AccessibilityTreeResult, selector: String): AccessibilityTreeResult {
        val dom = domAPI ?: return axResult

        val docNodeId = runCatching { dom.document?.nodeId }
            .getOrNull()
        if (docNodeId == null || docNodeId == 0) {
            logger.debug("DOM.document not available; skip selector filtering | selector={}", selector)
            return axResult
        }

        val matchedNodeIds = runCatching { dom.querySelectorAll(docNodeId, selector) }
            .onFailure { e -> logger.warn("DOM.querySelectorAll failed | selector={} | err={}", selector, e.toString()) }
            .getOrElse { emptyList() }
        if (matchedNodeIds.isEmpty()) {
            return AccessibilityTreeResult.EMPTY
        }

        val matchedBackendIds = matchedNodeIds.mapNotNull { nodeId ->
            runCatching { dom.describeNode(nodeId, null, null, null, false)?.backendNodeId }
                .onFailure { e -> tracer?.debug("DOM.describeNode failed | nodeId={} | err={}", nodeId, e.toString()) }
                .getOrNull()
        }.toSet()

        if (matchedBackendIds.isEmpty()) {
            return AccessibilityTreeResult.EMPTY
        }

        // Filter AX nodes by matching backendNodeIds
        val filteredNodes = axResult.nodes.filter { node ->
            val b = node.backendDOMNodeId
            b != null && matchedBackendIds.contains(b)
        }

        tracer?.debug(
            "AX selector filter | selector={} matchedDom={} matchedAX={}",
            selector, matchedBackendIds.size, filteredNodes.size
        )

        return AccessibilityTreeResult(
            nodes = filteredNodes,
            nodesByFrameId = filteredNodes.groupBy { it.frameId ?: "" },
            nodesByBackendNodeId = filteredNodes.groupBy { it.backendDOMNodeId ?: -1 }
        )
    }

    /**
     * Find scrollable element backendNodeIds using AX properties.
     * We only rely on AX due to unreliable backendNodeId exposure in JS contexts.
     *
     * @param targetFrameId Optional frame ID to target specific frame
     * @return List of backend node IDs that are scrollable
     */
    fun findScrollableElementIds(targetFrameId: String? = null): List<Int> {
        val axResult = getFullAXTreeRecursive(targetFrameId, depth = null)
        if (axResult.nodes.isEmpty()) return emptyList()

        val scrollableBackendIds = mutableSetOf<Int>()

        // Identify scrollable nodes from AX properties
        axResult.nodes.forEach { node ->
            val backendId = node.backendDOMNodeId ?: return@forEach

            val isScrollableAX = try {
                val props = node.properties ?: emptyList()
                props.any { prop ->
                    val name = prop.name.toString().lowercase()
                    if (name == "scrollable") {
                        val v = prop.value?.value
                        when (v) {
                            is Boolean -> v
                            is String -> v.equals("true", ignoreCase = true)
                            else -> false
                        }
                    } else false
                }
            } catch (e: Exception) {
                false
            }

            if (isScrollableAX) {
                scrollableBackendIds.add(backendId)
            }
        }

        tracer?.debug("AX scrollables detected | count={}", scrollableBackendIds.size)
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
        // Ensure the Accessibility domain is enabled
        ensureEnabled()
        return accessibilityAPI?.getFullAXTree(depth, frameId) ?: emptyList()
    }

    /**
     * Recursively collects AX trees for the target frame or every frame/iframe reachable from the
     * current page. Each returned node is annotated with a frameId so that callers can associate
     * AX information with DOM/backend nodes across frames.
     */
    fun getFullAXTreeRecursive(targetFrameId: String? = null, depth: Int? = null): AccessibilityTreeResult {
        // Ensure the Accessibility domain is enabled
        ensureEnabled()

        val page = pageAPI ?: return AccessibilityTreeResult.EMPTY
        val accessibility = accessibilityAPI ?: return AccessibilityTreeResult.EMPTY

        // Small retry loop to wait for AX cache to populate on dynamic pages
        repeat(5) { attempt ->
            val frameTree = try {
                page.getFrameTree()
            } catch (e: Exception) {
                tracer?.debug("Page.getFrameTree failed, using last known tree | err={}", e.toString())
                page.frameTree
            }

            val frameById = linkedMapOf<String, FrameTree>()
            if (frameTree != null) {
                collectFrameTree(frameTree, frameById)
            }

            val frameIds: List<String> = when {
                targetFrameId == null -> frameById.keys.toList()
                frameById.containsKey(targetFrameId) -> listOf(targetFrameId)
                else -> emptyList()
            }

            if (frameIds.isEmpty()) {
                // Fallback: try fetching AX tree without specifying a frameId (root document)
                val nodes = runCatching { accessibility.getFullAXTree(depth, null) }.getOrElse { emptyList() }
                if (nodes.isNotEmpty()) {
                    val rootFrameId = frameTree?.frame?.id ?: ""
                    return singleFrameResult(nodes, rootFrameId)
                }
                // If a specific target frame was requested, try that directly as well
                if (targetFrameId != null) {
                    val targeted = runCatching { accessibility.getFullAXTree(depth, targetFrameId) }.getOrElse { emptyList() }
                    if (targeted.isNotEmpty()) return singleFrameResult(targeted, targetFrameId)
                }
                // Wait a bit and retry
                if (attempt < 4) Thread.sleep(250) else return AccessibilityTreeResult.EMPTY
            } else {
                val seenPairs = mutableSetOf<Pair<String?, String>>()
                val allNodes = mutableListOf<AXNode>()
                val byFrame = LinkedHashMap<String, MutableList<AXNode>>(frameIds.size)
                val byBackend = LinkedHashMap<Int, MutableList<AXNode>>()

                frameIds.forEach { frameId ->
                    val nodes = runCatching { accessibility.getFullAXTree(depth, frameId) }
                        .onFailure { e -> tracer?.debug("Accessibility.getFullAXTree failed | frameId={} err={}", frameId, e.toString()) }
                        .getOrElse { emptyList() }
                    if (nodes.isEmpty()) {
                        return@forEach
                    }
                    val frameBucket = byFrame.getOrPut(frameId) { mutableListOf() }
                    nodes.forEach { node ->
                        val stamped = stampFrameId(node, frameId)
                        val key = stamped.frameId to stamped.nodeId
                        if (seenPairs.add(key)) {
                            frameBucket plusAssign stamped
                            allNodes plusAssign stamped
                            val backendId = stamped.backendDOMNodeId
                            if (backendId != null) {
                                byBackend.getOrPut(backendId) { mutableListOf() } plusAssign stamped
                            }
                        }
                    }
                }

                if (allNodes.isNotEmpty()) {
                    tracer?.debug("AX trees collected | frames={} totalNodes={} backends={}", frameIds.size, allNodes.size, byBackend.size)
                    return AccessibilityTreeResult(
                        nodes = allNodes,
                        nodesByFrameId = byFrame.mapValues { it.value.toList() },
                        nodesByBackendNodeId = byBackend.mapValues { it.value.toList() }
                    )
                }
                // Wait a bit and retry
                if (attempt < 4) Thread.sleep(250)
            }
        }

        return AccessibilityTreeResult.EMPTY
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
