package `fun`.platonic.pulsar.dom.select

import `fun`.platonic.pulsar.dom.nodes.depth
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor

object BackwardNodeTraversor {

    /**
     * Start a depth-first backward traverse from the start node to the doc root.
     * Notice: actually the traversal is from the parent of the start node to keep consistent,
     * if we do not do so, the parent node will be visited once rather than twice.
     * @param visitor Node visitor.
     * @param startNode the node point where start to traverse.
     */
    fun traverse(visitor: NodeVisitor, startNode: Node) {
        val startParent = startNode.parentNode()
        var siblingIndex = startNode.siblingIndex()

        var node: Node? = startParent
        // depth can used for double check
        var depth = node?.depth?:0

        while (node != null) {
            visitor.head(node, depth)

            if (node != startParent) {
                siblingIndex = node.childNodeSize() - 1
            }
            if (siblingIndex > -1) {
                node = node.childNode(siblingIndex)
                depth++
            } else {
                visitor.tail(node, depth)

                while (node!!.previousSibling() == null && depth > 0) {
                    node = node.parentNode()
                    depth--
                    visitor.tail(node, depth)
                }

                node = node.previousSibling()
            }
        }
    }
}
