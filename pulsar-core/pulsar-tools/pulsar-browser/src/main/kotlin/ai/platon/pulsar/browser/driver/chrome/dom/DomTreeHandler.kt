package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.common.getLogger
import kotlin.jvm.Volatile
import com.github.kklisura.cdt.protocol.v2023.types.dom.Node as CdpNode

/**
 * Handler for DOM tree operations.
 * Fetches and converts CDP DOM tree to enhanced representation.
 */
class DomTreeHandler(private val devTools: RemoteDevTools) {
    private val logger = getLogger(this)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }

    @Volatile
    private var lastBackendLookup: Map<Int, EnhancedDOMTreeNode> = emptyMap()

    /**
     * Expose the last backend-node lookup built during document fetch.
     */
    fun lastBackendNodeLookup(): Map<Int, EnhancedDOMTreeNode> = lastBackendLookup

    /**
     * Get the full DOM document tree.
     *
     * @param target Page/frame targeting info (frameId/targetId/sessionId)
     * @param maxDepth Maximum depth to traverse (0 means full tree)
     * @return Enhanced DOM tree root node; returns an empty root on failure
     */
    fun getDocument(target: PageTarget?, maxDepth: Int = 0): EnhancedDOMTreeNode {
        val dom = devTools.dom
        val depth = maxDepth.takeIf { it > 0 }
        val document = try {
            when {
                dom == null -> null
                depth != null -> dom.getDocument(depth, /* pierce */ true)
                else -> dom.getDocument(null, /* pierce */ true)
            }
        } catch (e: Exception) {
            logger.warn("DOM.getDocument failed | frameId={} | err={}", target?.frameId, e.toString())
            tracer?.debug("DOM.getDocument exception", e)
            null
        }

        if (document == null) {
            lastBackendLookup = emptyMap()
            return EnhancedDOMTreeNode()
        }

        val backendIndex = mutableMapOf<Int, EnhancedDOMTreeNode>()
        val root = mapNode(
            node = document,
            depth = 0,
            maxDepth = maxDepth,
            frameId = target?.frameId ?: document.frameId,
            targetId = target?.targetId,
            sessionId = target?.sessionId,
            backendIndex = backendIndex
        )

        lastBackendLookup = backendIndex
        tracer?.debug(
            "DOM tree collected | rootId={} backendIndexed={} depthLimit={}",
            root.nodeId, backendIndex.size, maxDepth
        )
        return root
    }

    /**
     * Map CDP Node to EnhancedDOMTreeNode recursively.
     *
     * @param node CDP node
     * @param depth Current depth in tree
     * @param maxDepth Maximum depth to traverse (0 = no limit)
     * @param frameId Frame ID for this node
     * @param targetId Target ID for this node
     * @param sessionId Session ID for this node
     * @return EnhancedDOMTreeNode
     */
    private fun mapNode(
        node: CdpNode,
        depth: Int,
        maxDepth: Int,
        frameId: String?,
        targetId: String?,
        sessionId: String?,
        backendIndex: MutableMap<Int, EnhancedDOMTreeNode>
    ): EnhancedDOMTreeNode {
        // Parse attributes from CDP format (flat list: [name1, value1, name2, value2, ...])
        val attrs = (node.attributes ?: emptyList())
            .chunked(2)
            .associate { (k, v) -> k to v }

        // Recursively process children if within depth limit
        val children: List<EnhancedDOMTreeNode> = when {
            maxDepth == 0 || depth < maxDepth -> {
                (node.children ?: emptyList()).map {
                    mapNode(
                        it,
                        depth + 1,
                        maxDepth,
                        node.frameId ?: frameId,
                        targetId,
                        sessionId,
                        backendIndex
                    )
                }
            }
            else -> emptyList()
        }

        // Process shadow roots if present
        val shadowRoots: List<EnhancedDOMTreeNode> = if (maxDepth == 0 || depth < maxDepth) {
            (node.shadowRoots ?: emptyList()).map {
                mapNode(
                    it,
                    depth + 1,
                    maxDepth,
                    node.frameId ?: frameId,
                    targetId,
                    sessionId,
                    backendIndex
                )
            }
        } else {
            emptyList()
        }

        // Process content document for iframes
        val contentDocument: EnhancedDOMTreeNode? = if (maxDepth == 0 || depth < maxDepth) {
            node.contentDocument?.let {
                mapNode(
                    it,
                    depth + 1,
                    maxDepth,
                    it.frameId ?: node.frameId ?: frameId,
                    targetId,
                    sessionId,
                    backendIndex
                )
            }
        } else {
            null
        }

        val enhanced = EnhancedDOMTreeNode(
            nodeId = node.nodeId ?: 0,
            backendNodeId = node.backendNodeId,
            nodeName = node.nodeName ?: "",
            nodeType = NodeType.fromValue(node.nodeType ?: 1),
            nodeValue = node.nodeValue ?: "",
            attributes = attrs,
            frameId = node.frameId ?: frameId,
            targetId = targetId,
            sessionId = sessionId,
            children = children,
            shadowRoots = shadowRoots,
            contentDocument = contentDocument
        )

        enhanced.backendNodeId?.let { backendIndex[it] = enhanced }

        return enhanced
    }
}
