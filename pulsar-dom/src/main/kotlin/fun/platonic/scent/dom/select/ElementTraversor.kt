package `fun`.platonic.scent.dom.select

import `fun`.platonic.scent.dom.nodes.DescriptiveDocument
import org.jsoup.nodes.Element

/**
 * Depth-first e traversor. Use to iterate through all es under and including the specified root e.
 *
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 */
object ElementTraversor {

    @JvmStatic
    fun traverse(visitor: AbstractElementVisitor, root: DescriptiveDocument) {
        traverse(visitor, root.unbox())
    }

    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param root the root e point to traverse.
     */
    @JvmStatic
    fun traverse(visitor: AbstractElementVisitor, root: Element?) {
        var e = root
        var depth = 0

        while (e != null && !visitor.stopped()) {
            visitor.head(e, depth)

            if (visitor.stopped()) break

            if (e.children().size > 0) {
                e = e.child(0)
                depth++
            } else {
                while (e!!.nextElementSibling() == null && depth > 0 && !visitor.stopped()) {
                    visitor.tail(e, depth)
                    e = e.parent()
                    depth--
                }

                visitor.tail(e, depth)

                if (e === root)
                    break
                e = e.nextElementSibling()
            }
        }
    }
}
