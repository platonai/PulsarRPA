package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import com.github.kklisura.cdt.protocol.v2023.types.dom.Node as CdpNode

/**
 * Handler for DOM tree operations.
 * Fetches and converts CDP DOM tree to enhanced representation.
 */
class DomTreeHandler(private val devTools: RemoteDevTools) {
    
    /**
     * Get the full DOM document tree.
     * 
     * @param maxDepth Maximum depth to traverse (0 means full tree)
     * @return Enhanced DOM tree root node
     */
    fun getDocument(maxDepth: Int = 0): EnhancedDOMTreeNode {
        val doc = devTools.dom.document ?: return EnhancedDOMTreeNode()
        return mapNode(doc, 0, maxDepth, frameId = null, targetId = null, sessionId = null)
    }

    /**
     * Map CDP Node to EnhancedDOMTreeNode recursively.
     * 
     * @param node CDP node
     * @param depth Current depth in tree
     * @param maxDepth Maximum depth to traverse
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
        sessionId: String?
    ): EnhancedDOMTreeNode {
        // Parse attributes from CDP format (flat list: [name1, value1, name2, value2, ...])
        val attrs = (node.attributes ?: emptyList())
            .chunked(2)
            .associate { (k, v) -> k to v }

        // Recursively process children if within depth limit
        val children: List<EnhancedDOMTreeNode> = when {
            maxDepth == 0 || depth < maxDepth -> {
                (node.children ?: emptyList()).map { 
                    mapNode(it, depth + 1, maxDepth, node.frameId ?: frameId, targetId, sessionId) 
                }
            }
            else -> emptyList()
        }
        
        // Process shadow roots if present
        val shadowRoots: List<EnhancedDOMTreeNode> = if (maxDepth == 0 || depth < maxDepth) {
            (node.shadowRoots ?: emptyList()).map {
                mapNode(it, depth + 1, maxDepth, node.frameId ?: frameId, targetId, sessionId)
            }
        } else {
            emptyList()
        }
        
        // Process content document for iframes
        val contentDocument: EnhancedDOMTreeNode? = if (maxDepth == 0 || depth < maxDepth) {
            node.contentDocument?.let {
                mapNode(it, depth + 1, maxDepth, it.frameId ?: node.frameId ?: frameId, targetId, sessionId)
            }
        } else {
            null
        }

        return EnhancedDOMTreeNode(
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
    }
}
