package ai.platon.pulsar.dom.select

import org.jsoup.nodes.Element
import org.jsoup.select.NodeTraversor
import javax.lang.model.element.ElementVisitor

interface IElementVisitor {
    /**
     * Callback for when a node is first visited.
     *
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     * of that will have depth 1.
     */
    fun head(node: Element, depth: Int)

    /**
     * Callback for when a node is last visited, after all of its descendants have been visited.
     *
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     * of that will have depth 1.
     */
    fun tail(node: Element, depth: Int) {
    }
}

/**
 * Node visitor interface. Provide an implementing class to [ElementVisitor] to iterate through nodes.
 *
 * This interface provides two methods, `head` and `tail`. The head method is called when the node is first
 * seen, and the tail method when all of the node's children have been visited. As an example, head can be used to
 * create a start tag for a node, and tail to create the end tag.
 */
abstract class ElementVisitor: IElementVisitor {

    var isStopped: Boolean = false

    fun stop() {
        isStopped = true
    }
}
