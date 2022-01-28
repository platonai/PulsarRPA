package ai.platon.pulsar.dom.select

import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.urls.Urls
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class PowerSelectorParseException(msg: String, vararg params: Any) : IllegalArgumentException(String.format(msg, *params))

/**
 * CSS element selector, that finds elements matching a query.
 *
 * <h2>Selector syntax</h2>
 *
 * A selector is a chain of simple selectors, separated by combinators. Selectors are **case insensitive** (including against
 * elements, attributes, and attribute values).
 *
 * The universal selector (*) is implicit when no element selector is supplied (i.e. `*.header` and `.header`
 * is equivalent).
 *
 * <table summary="">
 * <tr><th align="left">Pattern</th><th align="left">Matches</th><th align="left">Example</th></tr>
 * <tr><td>`*`</td><td>any element</td><td>`*`</td></tr>
 * <tr><td>`tag`</td><td>elements with the given tag name</td><td>`div`</td></tr>
 * <tr><td>`*|E`</td><td>elements of type E in any namespace *ns*</td><td>`*|name` finds `<fb:name>` elements</td></tr>
 * <tr><td>`ns|E`</td><td>elements of type E in the namespace *ns*</td><td>`fb|name` finds `<fb:name>` elements</td></tr>
 * <tr><td>`#id`</td><td>elements with attribute ID of "id"</td><td>`div#wrap`, `#logo`</td></tr>
 * <tr><td>`.class`</td><td>elements with a class name of "class"</td><td>`div.left`, `.result`</td></tr>
 * <tr><td>`[attr]`</td><td>elements with an attribute named "attr" (with any value)</td><td>`a[href]`, `[title]`</td></tr>
 * <tr><td>`[^attrPrefix]`</td><td>elements with an attribute name starting with "attrPrefix". Use to find elements with HTML5 datasets</td><td>`[^data-]`, `div[^data-]`</td></tr>
 * <tr><td>`[attr=val]`</td><td>elements with an attribute named "attr", and value equal to "val"</td><td>`img[width=500]`, `a[rel=nofollow]`</td></tr>
 * <tr><td>`[attr="val"]`</td><td>elements with an attribute named "attr", and value equal to "val"</td><td>`span[hello="Cleveland"][goodbye="Columbus"]`, `a[rel="nofollow"]`</td></tr>
 * <tr><td>`[attr^=valPrefix]`</td><td>elements with an attribute named "attr", and value starting with "valPrefix"</td><td>`a[href^=http:]`</td></tr>
 * <tr><td>`[attr$=valSuffix]`</td><td>elements with an attribute named "attr", and value ending with "valSuffix"</td><td>`img[src$=.png]`</td></tr>
 * <tr><td>`[attr*=valContaining]`</td><td>elements with an attribute named "attr", and value containing "valContaining"</td><td>`a[href*=/search/]`</td></tr>
 * <tr><td>`[attr~=*regex*]`</td><td>elements with an attribute named "attr", and value matching the regular expression</td><td>`img[src~=(?i)\\.(png|jpe?g)]`</td></tr>
 * <tr><td></td><td>The above may be combined in any order</td><td>`div.header[title]`</td></tr>
 * <tr><td></td><td colspan="3"><h3>Combinators</h3></td></tr>
 * <tr><td>`E F`</td><td>an F element descended from an E element</td><td>`div a`, `.logo h1`</td></tr>
 * <tr><td>`E > F`</td><td>an F direct child of E</td><td>`ol > li`</td></tr>
 * <tr><td>`E + F`</td><td>an F element immediately preceded by sibling E</td><td>`li + li`, `div.head + div`</td></tr>
 * <tr><td>`E ~ F`</td><td>an F element preceded by sibling E</td><td>`h1 ~ p`</td></tr>
 * <tr><td>`E, F, G`</td><td>all matching elements E, F, or G</td><td>`a[href], div, h3`</td></tr>
 * <tr><td></td><td colspan="3"><h3>Pseudo selectors</h3></td></tr>
 * <tr><td>`:lt(*n*)`</td><td>elements whose sibling index is less than *n*</td><td>`td:lt(3)` finds the first 3 cells of each row</td></tr>
 * <tr><td>`:gt(*n*)`</td><td>elements whose sibling index is greater than *n*</td><td>`td:gt(1)` finds cells after skipping the first two</td></tr>
 * <tr><td>`:eq(*n*)`</td><td>elements whose sibling index is equal to *n*</td><td>`td:eq(0)` finds the first cell of each row</td></tr>
 * <tr><td>`:has(*selector*)`</td><td>elements that contains at least one element matching the *selector*</td><td>`div:has(p)` finds divs that contain p elements </td></tr>
 * <tr><td>`:not(*selector*)`</td><td>elements that do not match the *selector*. See also [Elements.not]</td><td>`div:not(.logo)` finds all divs that do not have the "logo" class.
 *
 *`div:not(:has(div))` finds divs that do not contain divs.</td></tr>
 * <tr><td>`:contains(*text*)`</td><td>elements that contains the specified text. The search is case insensitive. The text may appear in the found element, or any of its descendants.</td><td>`p:contains(dom)` finds p elements containing the text "dom".</td></tr>
 * <tr><td>`:matches(*regex*)`</td><td>elements whose text matches the specified regular expression. The text may appear in the found element, or any of its descendants.</td><td>`td:matches(\\d+)` finds table cells containing digits. `div:matches((?i)login)` finds divs containing the text, case insensitively.</td></tr>
 * <tr><td>`:containsOwn(*text*)`</td><td>elements that directly contain the specified text. The search is case insensitive. The text must appear in the found element, not any of its descendants.</td><td>`p:containsOwn(dom)` finds p elements with own text "dom".</td></tr>
 * <tr><td>`:matchesOwn(*regex*)`</td><td>elements whose own text matches the specified regular expression. The text must appear in the found element, not any of its descendants.</td><td>`td:matchesOwn(\\d+)` finds table cells directly containing digits. `div:matchesOwn((?i)login)` finds divs containing the text, case insensitively.</td></tr>
 * <tr><td>`:containsData(*data*)`</td><td>elements that contains the specified *data*. The contents of `script` and `style` elements, and `comment` nodes (etc) are considered data nodes, not text nodes. The search is case insensitive. The data may appear in the found element, or any of its descendants.</td><td>`script:contains(dom)` finds script elements containing the data "dom".</td></tr>
 * <tr><td></td><td>The above may be combined in any order and with other selectors</td><td>`.light:contains(name):eq(0)`</td></tr>
 * <tr><td colspan="3"><h3>Structural pseudo selectors</h3></td></tr>
 * <tr><td>`:root`</td><td>The element that is the root of the document. In HTML, this is the `html` element</td><td>`:root`</td></tr>
 * <tr><td>`:nth-child(*a*n+*b*)`</td><td>
 *
 *elements that have `*a*n+*b*-1` siblings **before** it in the document tree, for any positive integer or zero value of `n`, and has a parent element. For values of `a` and `b` greater than zero, this effectively divides the element's children into groups of a elements (the last group taking the remainder), and selecting the *b*th element of each group. For example, this allows the selectors to address every other row in a table, and could be used to alternate the color of paragraph text in a cycle of four. The `a` and `b` values must be integers (positive, negative, or zero). The index of the first child of an element is 1.
 * In addition to this, `:nth-child()` can take `odd` and `even` as arguments instead. `odd` has the same signification as `2n+1`, and `even` has the same signification as `2n`.</td><td>`tr:nth-child(2n+1)` finds every odd row of a table. `:nth-child(10n-1)` the 9th, 19th, 29th, etc, element. `li:nth-child(5)` the 5h li</td></tr>
 * <tr><td>`:nth-last-child(*a*n+*b*)`</td><td>elements that have `*a*n+*b*-1` siblings **after** it in the document tree. Otherwise like `:nth-child()`</td><td>`tr:nth-last-child(-n+2)` the last two rows of a table</td></tr>
 * <tr><td>`:nth-of-type(*a*n+*b*)`</td><td>pseudo-class notation represents an element that has `*a*n+*b*-1` siblings with the same expanded element name *before* it in the document tree, for any zero or positive integer value of n, and has a parent element</td><td>`img:nth-of-type(2n+1)`</td></tr>
 * <tr><td>`:nth-last-of-type(*a*n+*b*)`</td><td>pseudo-class notation represents an element that has `*a*n+*b*-1` siblings with the same expanded element name *after* it in the document tree, for any zero or positive integer value of n, and has a parent element</td><td>`img:nth-last-of-type(2n+1)`</td></tr>
 * <tr><td>`:first-child`</td><td>elements that are the first child of some other element.</td><td>`div > p:first-child`</td></tr>
 * <tr><td>`:last-child`</td><td>elements that are the last child of some other element.</td><td>`ol > li:last-child`</td></tr>
 * <tr><td>`:first-of-type`</td><td>elements that are the first sibling of its type in the list of children of its parent element</td><td>`dl dt:first-of-type`</td></tr>
 * <tr><td>`:last-of-type`</td><td>elements that are the last sibling of its type in the list of children of its parent element</td><td>`tr > td:last-of-type`</td></tr>
 * <tr><td>`:only-child`</td><td>elements that have a parent element and whose parent element hasve no other element children</td><td></td></tr>
 * <tr><td>`:only-of-type`</td><td> an element that has a parent element and whose parent element has no other element children with the same expanded element name</td><td></td></tr>
 * <tr><td>`:empty`</td><td>elements that have no children at all</td><td></td></tr></table>
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 * @see Element.select
 */
object PowerSelector {

    private val logger = LoggerFactory.getLogger(PowerSelector::class.java)
    private val cache = ConcurrentExpiringLRUCache<String, Evaluator?>(Duration.ofMinutes(10))
    private val parseExceptions = ConcurrentExpiringLRUCache<String, AtomicInteger>(Duration.ofMinutes(10))

    /**
     * Find elements matching selector.
     *
     * @param query CSS selector
     * @param root  root element to descend into
     * @return matching elements, empty if none
     */
    fun select(cssQuery: String, root: Element): Elements {
        val cssQuery0 = cssQuery.trim()
        if (cssQuery0.isBlank()) {
            return Elements()
        }

        val evaluator = parseOrNullCached(cssQuery0, root.baseUri()) ?: return Elements()
        return select(evaluator, root)
    }

    fun select(cssQuery: String, root: Element, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
        checkArguments(cssQuery, offset, limit)
        return select(cssQuery, root).asSequence().drop(offset - 1).take(limit).toCollection(Elements())
    }

    fun <O> select(cssQuery: String,
                   root: Element, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> O): List<O> {
        checkArguments(cssQuery, offset, limit)
        // TODO: do the filter inside Collector.collect
        return select(cssQuery, root).asSequence().drop(offset - 1).take(limit).map { transformer(it) }.toList()
    }

    /**
     * Find elements matching selector.
     *
     * @param cssQuery CSS query
     * @param roots root elements to descend into
     * @return matching elements, empty if none
     */
    fun select(cssQuery: String, roots: Iterable<Element>): Elements {
        val cssQuery0 = cssQuery.trim()
        if (cssQuery0.isBlank() || !roots.iterator().hasNext()) {
            return Elements()
        }

        val evaluator = parseOrNullCached(cssQuery0, roots.first().baseUri())?: return Elements()
        val elements = ArrayList<Element>()
        val seenElements = IdentityHashMap<Element, Boolean>()
        // dedupe elements by identity, not equality

        for (root in roots) {
            val found = select(evaluator, root)
            for (el in found) {
                if (!seenElements.containsKey(el)) {
                    elements.add(el)
                    seenElements[el] = java.lang.Boolean.TRUE
                }
            }
        }

        return Elements(elements)
    }

    /**
     * Find the first element that matches the query.
     * @param cssQuery CSS selector
     * @param root root element to descend into
     * @return the matching element, or **null** if none.
     */
    fun selectFirst(cssQuery: String, root: Element): Element? {
        val cssQuery0 = cssQuery.trim()
        if (cssQuery0.isBlank()) {
            return null
        }

        val evaluator = parseOrNullCached(cssQuery, root.baseUri()) ?: return null
        return PowerCollector.findFirst(evaluator, root)
    }

    /**
     * Find elements matching selector.
     *
     * @param evaluator CSS selector
     * @param root root element to descend into
     * @return matching elements, empty if none
     */
    private fun select(evaluator: Evaluator, root: Element): Elements {
        return PowerCollector.collect(evaluator, root)
    }

    private fun parseOrNullCached(cssQuery: String, baseUri: String): Evaluator? {
        // JCommand do not remove surrounding quotes, like jcommander.parse("-outlink \"ul li a[href~=item]\"")
        val cssQuery0 = cssQuery.removeSurrounding("\"").takeIf { it.isNotBlank() } ?: return null
        val key = "$baseUri $cssQuery0"
        return cache.computeIfAbsent(key) { parseOrNull(cssQuery0, baseUri) }
    }

    private fun parseOrNull(cssQuery: String, baseUri: String): Evaluator? {
        try {
            return PowerQueryParser.parse(cssQuery)
        } catch (e: PowerSelectorParseException) {
            var message = e.message
            if (!message.isNullOrBlank()) {
                val host = URL(baseUri).host
                val key = "$host $cssQuery"
                message = "$key\n>>>$message<<<"
                val count = parseExceptions.computeIfAbsent(message) { AtomicInteger() }.incrementAndGet()
                if (count == 1) {
                    logger.warn("Failed to parse css query for document | $cssQuery | $baseUri", e)
                } else if (count % 10 == 0) {
                    logger.warn("Caught $count parse exceptions: ", e.message)
                }
            }
        }

        return null
    }

    private fun checkArguments(cssQuery: String, offset: Int = 1, limit: Int) {
        if (cssQuery.isBlank()) {
            throw IllegalArgumentException("cssQuery should not be empty")
        }

        if (offset < 1) {
            throw IllegalArgumentException("Offset should be > 1")
        }

        if (limit < 0) {
            throw IllegalArgumentException("Limit should be >= 0")
        }
    }
}
