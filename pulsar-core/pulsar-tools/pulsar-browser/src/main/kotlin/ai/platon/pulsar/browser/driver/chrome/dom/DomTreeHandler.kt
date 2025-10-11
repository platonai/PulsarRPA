package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.v2023.types.dom.Node as CdpNode

class DomTreeHandler(private val devTools: RemoteDevTools) {
    fun getDocument(maxDepth: Int = 0): EnhancedDOMTreeNode {
        val doc = devTools.dom.document ?: return EnhancedDOMTreeNode()
        return mapNode(doc, 0, maxDepth)
    }

    private fun mapNode(node: CdpNode, depth: Int, maxDepth: Int): EnhancedDOMTreeNode {
        val attrs = (node.attributes ?: emptyList())
            .chunked(2)
            .associate { (k, v) -> k to v }

        val children: List<EnhancedDOMTreeNode> = when {
            maxDepth == 0 || depth < maxDepth -> (node.children ?: emptyList()).map { mapNode(it, depth + 1, maxDepth) }
            else -> emptyList()
        }

        return EnhancedDOMTreeNode(
            nodeId = node.nodeId ?: 0,
            backendNodeId = node.backendNodeId,
            nodeName = node.nodeName ?: "",
            nodeType = node.nodeType ?: 0,
            attributes = attrs,
            children = children,
        )
    }
}
