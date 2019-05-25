package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.nodes.TraverseState
import ai.platon.pulsar.dom.nodes.node.ext.sequence
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

/**
 * In-box syntax, cases:
 * <ul>
 *   <li>div:in-box(200, 200, 300, 300)</li>
 *   <li>div:in-box(200, 200, 300, 300, 50)</li>
 *   <li>*:in-box(200, 200, 300, 300)</li>
 *   <li>*:in-box(200, 200, 300, 300, 50)</li>
 *   <li>div:in-box(200, 200, 300, 300),*:in-box(200, 200, 300, 300, 50)</li>
 * </ul>
 *
 * Simplified in-box syntax version:
 * <ul>
 *   <li>200x300</li>
 *   <li>200x300,300x500</li>
 * </ul>
 * */
@JvmField val BOX_CSS_PATTERN_1 = Regex(".{1,5}:in-box\\(\\d+(,\\d+\\){1,4})")
@JvmField val BOX_CSS_PATTERN = Regex("$BOX_CSS_PATTERN_1(,$BOX_CSS_PATTERN_1)?")

@JvmField val BOX_SYNTAX_PATTERN_1 = Regex("(\\d+)[xX](\\d+)")
@JvmField val BOX_SYNTAX_PATTERN = Regex("$BOX_SYNTAX_PATTERN_1(\\s*,\\s*$BOX_SYNTAX_PATTERN_1)?")

/**
 * Convert a box query to a normal css query
 */
fun convertCssQuery(cssQuery: String): String {
    val query = cssQuery
    val matcher = BOX_SYNTAX_PATTERN.toPattern().matcher(query)
    if (matcher.find()) {
        return query.split(",")
                .map { it.split('x', 'X') }
                .joinToString { "*:in-box(${it[0]}, ${it[1]})" }
    }

    // Bad syntax, no element should find
    return cssQuery
}

inline fun Node.collectIf(crossinline filter: (Node) -> Boolean): List<Node> {
    return collectIfTo(mutableListOf(), filter)
}

inline fun <C : MutableCollection<Node>> Node.collectIfTo(destination: C, crossinline filter: (Node) -> Boolean): C {
    NodeTraversor.traverse({ node, _-> if (filter(node)) { destination.add(node) } }, this)
    return destination
}

inline fun <O: Node> Node.collect(crossinline transform: (Node) -> O?): List<O> {
    return collectTo(mutableListOf(), transform)
}

inline fun <O: Node, C : MutableCollection<O>> Node.collectTo(destination: C, crossinline transform: (Node) -> O?): C {
    NodeTraversor.traverse({ node, _-> transform(node)?.also { destination.add(it) } }, this)
    return destination
}

inline fun Node.filter(crossinline predicate: (Node) -> TraverseState): List<Node> {
    return filterTo(mutableListOf(), predicate)
}

inline fun <C : MutableCollection<Node>> Node.filterTo(destination: C, crossinline predicate: (Node) -> TraverseState): C {
    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            val result = predicate(node)
            if (result.match) {
                destination.add(node)
            }
            return result.state
        }
    }, this)
    return destination
}

fun Node.filter(seq: Int): Node? {
    var result: Node? = null
    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            return when {
                sequence < seq -> NodeFilter.FilterResult.CONTINUE
                sequence == seq -> {
                    result = node
                    NodeFilter.FilterResult.SKIP_ENTIRELY
                }
                else -> NodeFilter.FilterResult.SKIP_ENTIRELY
            }
        }
    }, this)

    return result
}

/**
 * Find posterity matches the condition
 * */
fun Node.find(predicate: (Node) -> Boolean): List<Node> {
    return collectIf(predicate)
}

fun Node.findFirst(predicate: (Node) -> Boolean): Node? {
    val root = this
    var result: Node? = null
    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            return if (predicate(node)) {
                result = node
                NodeFilter.FilterResult.STOP
            }
            else NodeFilter.FilterResult.CONTINUE
        }
    }, root)

    return result
}


@JvmOverloads
fun Node.select2(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    if (this !is Element) {
        return Elements()
    }

    if (offset == 1 && limit == Int.MAX_VALUE) {
        return MathematicalSelector.select(cssQuery, this)
    }

    // TODO: do the filtering inside [MathematicalSelector#select]
    var i = 1
    return MathematicalSelector.select(cssQuery, this)
            .takeWhile { i++ >= offset && i <= limit }
            .toCollection(Elements())
}

@Deprecated("Use first instead", ReplaceWith("MathematicalSelector.selectFirst(cssQuery, this)"))
fun Node.selectFirst2(cssQuery: String): Element? {
    return if (this is Element) {
        MathematicalSelector.selectFirst(cssQuery, this)
    } else null
}

fun <O> Node.select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
                    transformer: (Element) -> O): List<O> {
    return if (this is Element) {
        select2(query, offset, limit).map { transformer(it) }
    } else listOf()
}

fun Node.first(cssQuery: String): Element? {
    return if (this is Element) {
        MathematicalSelector.selectFirst(cssQuery, this)
    } else null
}

fun <O> Node.first(cssQuery: String, transformer: (Element) -> O): O? {
    return if (this !is Element) {
        first(cssQuery)?.let { transformer(it) }
    } else null
}

@JvmOverloads
fun Elements.select2(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    if (offset == 1 && limit == Int.MAX_VALUE) {
        return MathematicalSelector.select(cssQuery, this)
    }

    var i = 1
    return MathematicalSelector.select(cssQuery, this)
            .takeWhile { i++ >= offset && i <= limit }
            .toCollection(Elements())
}
