package ai.platon.pulsar.dom.select

import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.nodes.GeoAnchor
import ai.platon.pulsar.dom.nodes.TraverseState
import ai.platon.pulsar.dom.nodes.node.ext.cleanText
import ai.platon.pulsar.dom.nodes.node.ext.rectangle
import ai.platon.pulsar.dom.nodes.node.ext.sequence
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import java.util.*
import kotlin.NoSuchElementException

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
    val matcher = BOX_SYNTAX_PATTERN.toPattern().matcher(cssQuery)
    if (matcher.find()) {
        return cssQuery.split(",")
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

fun Node.selectFirst(predicate: (Node) -> Boolean): Node {
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

fun Node.first(predicate: (Node) -> Boolean) = selectFirst(predicate)

fun Node.any(predicate: (Node) -> Boolean): Boolean {
    return selectFirstOrNull(predicate) != null
}

fun Node.none(predicate: (Node) -> Boolean) = !any(predicate)

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
fun Node.select(query: String, offset: Int, limit: Int = Int.MAX_VALUE): Elements {
    if (this !is Element) {
        return Elements()
    }

    return PowerSelector.select(query, this, offset, limit)
}

fun <O> Node.select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> O): List<O> {
    return if (this is Element) {
        PowerSelector.select(query, this, offset, limit, transformer)
    } else listOf()
}

/**
 * TODO: Jsoup native supported selectTo
 * */
inline fun <R : Any, C : MutableCollection<in R>> Node.selectTo(destination: C,
        query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> R) {
    if (this is Element) {
        select(query, offset, limit).mapTo(destination) { transformer(it) }
    }
}

inline fun <R : Any> Node.selectNotNull(
    query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> R?
): List<R> {
    return if (this is Element) {
        select(query, offset, limit).mapNotNull { transformer(it) }
    } else listOf()
}

inline fun <R : Any, C : MutableCollection<in R>> Node.selectNotNullTo(
    destination: C, query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> R?
) {
    if (this is Element) {
        select(query, offset, limit).mapNotNullTo(destination) { transformer(it) }
    }
}

@JvmOverloads
fun Node.select2(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    return select(query, offset, limit)
}

fun Node.selectFirstOrNull(query: String): Element? {
    return (this as? Element)?.let { PowerSelector.selectFirst(query, it) }
}

fun <O> Node.selectFirstOrNull(query: String, transformer: (Element) -> O): O? {
    return selectFirstOrNull(query)?.let(transformer)
}

fun Node.selectFirstOptional(query: String): Optional<Element> {
    return Optional.ofNullable(selectFirstOrNull(query))
}

@JvmOverloads
fun Node.selectTexts(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String> {
    return select(query, offset, limit) { it.text() }
}

@JvmOverloads
fun Node.selectFirstText(query: String, defaultValue: String = "") = firstTextOrNull(query) ?: defaultValue

fun Node.selectFirstTextOrNull(query: String) = selectFirstOrNull(query)?.text()

fun Node.selectFirstTextOptional(query: String) = Optional.ofNullable(firstTextOrNull(query))

@JvmOverloads
fun Node.firstText(query: String, defaultValue: String = "") = selectFirstText(query, defaultValue)

fun Node.firstTextOrNull(query: String) = selectFirstTextOrNull(query)

fun Node.firstTextOptional(query: String) = selectFirstTextOptional(query)

@JvmOverloads
fun Node.selectAttributes(query: String, attrName: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String> {
    return select(query, offset, limit) { it.attr(attrName) }
}

@JvmOverloads
fun Node.selectFirstAttribute(query: String, attrName: String, defaultValue: String = "") =
    firstAttributeOrNull(query, attrName) ?: defaultValue

fun Node.selectFirstAttributeOrNull(query: String, attrName: String) =
    selectFirstOrNull(query)?.attr(attrName)

fun Node.selectFirstAttributeOptional(query: String, attrName: String) =
    Optional.ofNullable(firstAttributeOrNull(query, attrName))

@JvmOverloads
fun Node.firstAttribute(query: String, attrName: String, defaultValue: String = "") =
    selectFirstAttribute(query, attrName, defaultValue)

fun Node.firstAttributeOrNull(query: String, attrName: String) = selectFirstAttributeOrNull(query, attrName)

fun Node.firstAttributeOptional(query: String, attrName: String) = selectFirstAttributeOptional(query, attrName)

/**
 * TODO: experimental
 * TODO: may not as efficient as Node.collectIfTo since very call of e.nextElementSibling() generate a new element list
 * */
inline fun <C : MutableCollection<Element>> Element.collectIfTo(destination: C, crossinline filter: (Element) -> Boolean): C {
    ElementTraversor.traverse(this) { if (filter(it)) { destination.add(it) } }
    return destination
}

@JvmOverloads
fun Node.selectHyperlinks(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink> {
    val cssQuery = appendSelectorIfMissing(query, "a")

//    return select(cssQuery, offset, limit).mapNotNull {
//        it.takeIf { UrlUtils.isStandard(it.absUrl("href")) }
//            ?.let { Hyperlink(it.absUrl("href"), it.cleanText, referrer = baseUri()) }

    return select(cssQuery, offset, limit).asSequence()
        .map { it to it.absUrl("href") }
        .filter { UrlUtils.isStandard(it.second) }
        .map { Hyperlink(it.second, it.first.cleanText, href = it.second, referrer = it.first.baseUri()) }
        .toList()
}

private fun Element.anchorOrNull() = absUrl("href").takeIf { UrlUtils.isStandard(it) }
        ?.let { GeoAnchor(it, cleanText, cssSelector(), rectangle) }

@JvmOverloads
fun Node.selectAnchors(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<GeoAnchor> {
    val cssQuery = appendSelectorIfMissing(query, "a")
    return select(cssQuery, offset, limit) { it.anchorOrNull() }.filterNotNull()
//    return select(cssQuery, offset, limit).mapNotNull {
//        it.takeIf { UrlUtils.isStandard(it.absUrl("href")) }
//            ?.let { GeoAnchor(it.absUrl("href"), it.cleanText, it.cssSelector(), it.rectangle) }
//    }
}

@JvmOverloads
fun Node.selectImages(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<String> {
    val cssQuery = appendSelectorIfMissing(query, "img")
    return select(cssQuery, offset, limit) { it.absUrl("src").takeIf { UrlUtils.isStandard(it) } }
        .filterNotNull()
}

@Deprecated("Inappropriate name", ReplaceWith("selectAnchors(query, offset, limit)"))
fun Element.getAnchors(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) = selectAnchors(query, offset, limit)

@Deprecated("Inappropriate name", ReplaceWith("selectImages(query, offset, limit)"))
fun Element.getImages(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) = selectImages(query, offset, limit)

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
