package ai.platon.pulsar.dom.select

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.dom.nodes.Anchor
import ai.platon.pulsar.dom.nodes.TraverseState
import ai.platon.pulsar.dom.nodes.node.ext.cleanText
import ai.platon.pulsar.dom.nodes.node.ext.rectangle
import ai.platon.pulsar.dom.nodes.node.ext.sequence
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor

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

inline fun <O> Node.collect(crossinline transform: (Node) -> O?): List<O?> {
    val destination = mutableListOf<O?>()
    NodeTraversor.traverse({ node, _-> destination.add(transform(node)) }, this)
    return destination
}

inline fun <O> Node.collectNotNull(crossinline transform: (Node) -> O?): List<O> {
    return collectNotNullTo(mutableListOf(), transform)
}

inline fun <O, C : MutableCollection<O?>> Node.collectTo(destination: C, crossinline transform: (Node) -> O?): C {
    NodeTraversor.traverse({ node, _-> destination.add(transform(node)) }, this)
    return destination
}

inline fun <O, C : MutableCollection<O>> Node.collectNotNullTo(destination: C, crossinline transform: (Node) -> O?): C {
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

fun Node.first(predicate: (Node) -> Boolean): Node {
    return selectFirstOrNull(predicate) ?:throw NoSuchElementException("Node contains no descendant matching the predicate.")
}

fun Node.selectFirstOrNull(predicate: (Node) -> Boolean): Node? {
    var result: Node? = null

    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            return if (predicate(node)) {
                result = node
                NodeFilter.FilterResult.STOP
            }
            else NodeFilter.FilterResult.CONTINUE
        }
    }, this)
    return result
}

fun Node.any(predicate: (Node) -> Boolean): Boolean {
    return selectFirstOrNull(predicate) != null
}

fun Node.all(predicate: (Node) -> Boolean): Boolean {
    var r = true

    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            return if (!predicate(node)) {
                r = false
                NodeFilter.FilterResult.STOP
            }
            else NodeFilter.FilterResult.CONTINUE
        }
    }, this)

    return r
}

/**
 * Notice: do not provide default value for offset, it overrides the default version in Node
 * offset is 1 based
 * */
fun Node.select(cssQuery: String, offset: Int, limit: Int = Int.MAX_VALUE): Elements {
    if (this !is Element) {
        return Elements()
    }

    return MathematicalSelector.select(cssQuery, this, offset, limit)
}

fun <O> Node.select(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
                    transformer: (Element) -> O): List<O> {
    return if (this is Element) {
        MathematicalSelector.select(cssQuery, this, offset, limit, transformer)
    } else listOf()
}

/**
 * TODO: Jsoup native supported selectTo
 * */
inline fun <R : Any, C : MutableCollection<in R>> Node.selectTo(destination: C,
        query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
        transformer: (Element) -> R) {
    if (this is Element) {
        select(query, offset, limit).mapTo(destination) { transformer(it) }
    }
}

inline fun <R : Any> Node.selectNotNull(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
                                  transformer: (Element) -> R?): List<R> {
    return if (this is Element) {
        select(query, offset, limit).mapNotNull { transformer(it) }
    } else listOf()
}

inline fun <R : Any, C : MutableCollection<in R>> Node.selectNotNullTo(destination: C,
        query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
        transformer: (Element) -> R?) {
    if (this is Element) {
        select(query, offset, limit).mapNotNullTo(destination) { transformer(it) }
    }
}

fun Node.select2(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    return select(cssQuery, offset, limit)
}

fun Node.selectFirstOrNull(cssQuery: String): Element? {
    return (this as? Element)?.let { MathematicalSelector.selectFirst(cssQuery, it) }
}

fun <O> Node.selectFirstOrNull(cssQuery: String, transformer: (Element) -> O): O? {
    return selectFirstOrNull(cssQuery)?.let(transformer)
}

/**
 * TODO: experimental
 * TODO: may not as efficient as Node.collectIfTo since very call of e.nextElementSibling() generate a new element list
 * */
inline fun <C : MutableCollection<Element>> Element.collectIfTo(destination: C, crossinline filter: (Element) -> Boolean): C {
    ElementTraversor.traverse(this) { if (filter(it)) { destination.add(it) } }
    return destination
}

fun appendSelectorIfMissing(cssQuery: String, appendix: String): String {
    var q = cssQuery.replace("\\s+".toRegex(), " ").trim()
    val ap = appendix.trim()

    val parts = q.split(" ")
    // consider: body > div:nth-child(10) > ul > li:nth-child(3) > a:nth-child(2)
    if (!parts[parts.size - 1].startsWith(ap, ignoreCase = true)) {
        q += " $ap"
    }

    return q
}

fun Element.getAnchors(restrictCss: String, offset: Int = 0, limit: Int = Int.MAX_VALUE): Collection<Anchor> {
    val cssQuery = appendSelectorIfMissing(restrictCss, "a")
    return select(cssQuery, offset, limit).mapNotNull {
        it.takeIf { Urls.isValidUrl(it.absUrl("href")) }
                ?.let { Anchor(it.absUrl("href"), it.cleanText, it.cssSelector(), it.rectangle) }
    }
}

fun Element.getImages(restrictCss: String, offset: Int = 0, limit: Int = Int.MAX_VALUE): Collection<String> {
    val cssQuery = appendSelectorIfMissing(restrictCss, "img")
    return select(cssQuery, offset, limit) {
        it.absUrl("src").takeIf { Urls.isValidUrl(it) }
    }.filterNotNull()
}
