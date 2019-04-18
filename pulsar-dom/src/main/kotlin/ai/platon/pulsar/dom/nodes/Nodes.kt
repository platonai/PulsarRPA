package ai.platon.pulsar.dom.nodes

import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.left
import ai.platon.pulsar.dom.nodes.node.ext.sequence
import ai.platon.pulsar.dom.nodes.node.ext.top
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.math.NumberUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import org.jsoup.select.NodeFilter
import java.util.regex.Pattern

/**
 * Attribute names
 * */
const val A_LABELS = "s-labels"
const val A_ML_LABELS = "s-ml-labels"
const val A_CAPTION = "s-caption"

/**
 * Variable names
 * */
const val V_OWNER_BODY = "V_OWNER_BODY"
const val V_VIEW_PORT = "VIEW_PORT"

val BOX_SYNTAX_PATTERN_1 = Pattern.compile("(\\d+)[xX](\\d+)")
val BOX_SYNTAX_PATTERN = Pattern.compile("$BOX_SYNTAX_PATTERN_1(,$BOX_SYNTAX_PATTERN_1)?")

/**
 * Cases:
 * <ul>
 *   <li>div:in-box(200, 200, 300, 300)</li>
 *   <li>div:in-box(200, 200, 300, 300, 50)</li>
 *   <li>*:in-box(200, 200, 300, 300)</li>
 *   <li>*:in-box(200, 200, 300, 300, 50)</li>
 *   <li>div:in-box(200, 200, 300, 300),*:in-box(200, 200, 300, 300, 50)</li>
 * </ul>
 * */
val BOX_CSS_PATTERN_1 = Pattern.compile(".{1,5}:in-box\\(\\d+(,\\d+\\){1,4})")
val BOX_CSS_PATTERN = Pattern.compile("$BOX_CSS_PATTERN_1(,$BOX_CSS_PATTERN_1)?")

data class DOMRect(var left: Double = 0.0, var top: Double = 0.0, var width: Double = 0.0, var height: Double = 0.0) {
    val isEmpty get() = left == 0.0 && top == 0.0 && width == 0.0 && height == 0.0

    companion object {
        fun parseDOMRect(rect: String): DOMRect {
            if (rect.isBlank()) {
                return DOMRect()
            }

            val a = rect.split(SPACE.toRegex()).dropLastWhile { it.isEmpty() }
            if (a.size != 4) {
                return DOMRect()
            }

            val left = NumberUtils.toDouble(a[0])
            val top = NumberUtils.toDouble(a[1])
            val width = NumberUtils.toDouble(a[2])
            val height = NumberUtils.toDouble(a[3])

            return DOMRect(left, top, width, height)
        }
    }
}

class TraverseState(val match: Boolean = false, val state: NodeFilter.FilterResult = CONTINUE) {
    companion object {
        /** Continue processing the tree  */
        val CONTINUE = NodeFilter.FilterResult.CONTINUE
        /** Skip the child nodes, but do call [NodeFilter.tail] next.  */
        val SKIP_CHILDREN = NodeFilter.FilterResult.SKIP_CHILDREN
        /** Skip the subtree, and do not call [NodeFilter.tail].  */
        val SKIP_ENTIRELY = NodeFilter.FilterResult.SKIP_ENTIRELY
        /** Remove the node and its children  */
        val REMOVE = NodeFilter.FilterResult.REMOVE
        /** Stop processing  */
        val STOP = NodeFilter.FilterResult.STOP

        val TRUE_CONTINUE = TraverseState(true, CONTINUE)
        val TRUE_SKIP_CHILDREN = TraverseState(true, SKIP_CHILDREN)
        val TRUE_SKIP_ENTIRELY = TraverseState(true, SKIP_ENTIRELY)
        val TRUE_REMOVE = TraverseState(true, REMOVE)
        val TRUE_STOP = TraverseState(true, STOP)

        val FALSE_CONTINUE = TraverseState(false, CONTINUE)
        val FALSE_SKIP_CHILDREN = TraverseState(false, SKIP_CHILDREN)
        val FALSE_SKIP_ENTIRELY = TraverseState(false, SKIP_ENTIRELY)
        val FALSE_REMOVE = TraverseState(false, REMOVE)
        val FALSE_STOP = TraverseState(false, STOP)
    }
}

val documentComparator = kotlin.Comparator { d1: Document, d2 -> d1.baseUri().compareTo(d2.baseUri()) }

val descriptiveDocumentComparator = kotlin.Comparator {
    d1: FeaturedDocument, d2 -> d1.location.compareTo(d2.location) }

val nodeComparator = kotlin.Comparator { n1: Node, n2 ->
    val r = documentComparator.compare(n1.ownerDocument(), n2.ownerDocument())
    if (r == 0) n1.sequence - n2.sequence else r
}

val nodePositionComparator = kotlin.Comparator { n1: Node, n2 ->
    var r = documentComparator.compare(n1.ownerDocument(), n2.ownerDocument())
    if (r == 0) r = n1.top - n2.top
    if (r == 0) r = n1.left - n2.left
    r
}
