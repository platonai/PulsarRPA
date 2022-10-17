package ai.platon.pulsar.dom

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants.INTERNAL_URL_PREFIX
import ai.platon.pulsar.common.math.vectors.isNotEmpty
import ai.platon.pulsar.dom.nodes.*
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.*
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeTraversor
import java.awt.Dimension
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * A "featured document" is "very important" and all its features are calculated.
 *
 * FeaturedDocument is a wrapper for [org.jsoup.nodes.Document], every node's features
 * are calculated by a [ai.platon.pulsar.dom.features.FeatureCalculator].
 *
 * FeaturedDocument's DOM is immutable, no method is provided to append or insert elements or nodes.
 *
 * FeaturedDocument provides a set of powerful methods to select elements, text contexts, attributes
 * and so on:
 *
 * * [select]: retrieves an element collection matching the CSS query
 * * [selectFirst]: retrieves the first matching element
 * * [selectFirstText]: retrieves the text content of the first matching element
 * * [selectFirstAttribute]: retrieves the attribute value associated to the given name of the first matching element
 * * [selectHyperlinks]: retrieves all hyperlinks of elements matching the CSS query
 * * [selectAnchors]: retrieves all anchor elements matching the CSS query
 * * [selectImages]: retrieves all image elements matching the CSS query
 *
 * Other methods provided include DOM traversal, node counting, document attribute retrieval, export, and so on.
 * */
open class FeaturedDocument(val document: Document) {
    companion object {
        private val instanceSequencer = AtomicInteger()

        var SELECTOR_IN_BOX_DEVIATION = 25

        var primaryGridDimension = Dimension(30, 15) // about 1 em
        var secondaryGridDimension = Dimension(5, 5)
        var densityUnitArea = 400 * 400

        /**
         * The NIL document which is a wrapper for a nil [org.jsoup.nodes.Document]
         * */
        val NIL = FeaturedDocument(NILDocument)
        /**
         * The HTML content of a NIL document
         * */
        val NIL_DOC_HTML = NIL.unbox().outerHtml()
        /**
         * The length of a NIL document's HTML content
         * */
        val NIL_DOC_LENGTH = NIL_DOC_HTML.length

        /**
         * Create a shell document.
         * */
        fun createShell(baseUri: String): FeaturedDocument {
            val document = Document.createShell(baseUri)
            return FeaturedDocument(document)
        }

        /**
         * Check if this document is NIL.
         * */
        fun isNil(doc: FeaturedDocument): Boolean {
            return doc == NIL || doc.location == NIL.location
        }

        /**
         * Check if this document is internal.
         * */
        fun isInternal(doc: FeaturedDocument): Boolean {
            return doc.location.startsWith(INTERNAL_URL_PREFIX)
        }
    }

    /**
     * The process scope unique sequence.
     * */
    val sequence = instanceSequencer.incrementAndGet()

    /**
     * Get document title.
     * */
    val title get() = document.title()

    /**
     * Get the URL this Document was parsed from.
     */
    val baseUri get() = document.baseUri()

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     */
    val location get() = document.location()

    val head: Element get() = document.head()

    val body: Element get() = document.body()

    val text get() = document.text()

    val ownText get() = document.ownText()

    val wholeText get() = document.wholeText()

    val html get() = document.html()

    val outerHtml get() = document.outerHtml()

    val charset get() = document.charset()

    val nodeName get() = document.nodeName()

    val data get() = document.data()

    val id get() = document.id()

    val className get() = document.className()

    val prettyHtml: String
        get() {
            document.outputSettings().prettyPrint(true)
            return outerHtml
                .replace("s-features", "\n\t\t\ts-features")
                .replace("s-named-features", "\n\t\t\ts-named-features")
                .replace("s-caption", "\n\t\t\ts-caption")
        }

    var features: RealVector
        get() = document.extension.features
        set(value) {
            document.extension.features = value
        }

    @Deprecated("Fragment is no longer used")
    val fragments by lazy { DocumentFragments(this) }

    constructor(baseUri: String) : this(Document(baseUri))

    constructor(other: FeaturedDocument) : this(other.unbox())

    init {
        initialize()
    }

    fun unbox() = document

    fun isNil() = isNil(this)

    fun isInternal() = isInternal(this)

    fun isNotInternal() = !isInternal()

    fun createElement(tagName: String) = document.createElement(tagName)

    /**
     * The title should be guessed for some site without a <title> tag inside a <head> tag
     * */
    fun guessTitle(): String {
        return title.takeUnless { it.isBlank() }
            ?: firstTextOrNull("title")
            ?: firstTextOrNull("h1")
            ?: firstTextOrNull("h2")
            ?: ""
    }

    /**
     * Make all links in the document to be absolute.
     * */
    fun absoluteLinks() {
        forEachElement {
            if (it.hasAttr("href")) {
                it.attr("href", it.attr("abs:href"))
            } else if (it.hasAttr("src")) {
                it.attr("src", it.attr("abs:src"))
            }
        }
    }

    /**
     * Find elements that match the CSS query, with the document as the starting context. Matched elements
     * may include the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    @JvmOverloads
    fun select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) =
        document.select2(query, offset, limit)

    /**
     * Find elements that match the CSS query, with the document as the starting context. Matched elements
     * may include the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    fun <T> select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> T) =
        document.select(query, offset, limit, transformer = transformer)

    /**
     * Find the first element that match the CSS query, with the document as the starting context. Matched element
     * may be the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    fun selectFirst(query: String) =
        document.selectFirstOrNull(query) ?: throw NoSuchElementException("No element matching $query")

    /**
     * Find the first element that match the CSS query, with the document as the starting context. Matched element
     * may be the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    fun <T> selectFirst(query: String, transformer: (Element) -> T) =
        document.selectFirstOrNull(query)?.let { transformer(it) }
            ?: throw NoSuchElementException("No element matching $query")

    /**
     * Find the first element that match the CSS query, with the document as the starting context. Matched element
     * may be the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    fun selectFirstOrNull(query: String) = document.selectFirstOrNull(query)

    /**
     * Find the first element that match the CSS query, with the document as the starting context. Matched element
     * may be the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     * */
    fun <T> selectFirstOrNull(query: String, transformer: (Element) -> T) =
        document.selectFirstOrNull(query)?.let { transformer(it) }

    /**
     * Find elements that match the CSS query, with the document as the starting context. Matched elements
     * may include the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     *
     * Return the first element, if no element matches the query, return an Optional object.
     * */
    fun selectFirstOptional(query: String) = Optional.ofNullable(document.selectFirstOrNull(query))

    /**
     * Find elements that match the CSS query, with the document as the starting context. Matched elements
     * may include the document, or any of its children.
     *
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined.
     *
     * Return the first element, if no element matches the query, return an Optional object.
     * */
    fun <T> selectFirstOptional(query: String, transformer: (Element) -> T) =
        Optional.ofNullable(document.selectFirstOrNull(query)?.let { transformer(it) })

    @Deprecated("Inappropriate name", ReplaceWith("selectFirst(query)"))
    fun first(query: String) = document.selectFirstOrNull(query)

    @Deprecated("Inappropriate name", ReplaceWith("selectFirst(query, transformer)"))
    fun <T> first(query: String, transformer: (Element) -> T) =
        document.selectFirstOrNull(query)?.let { transformer(it) }

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun selectFirstText(query: String) =
        firstTextOrNull(query) ?: throw NoSuchElementException("No element matching $query")

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun selectFirstTextOrNull(query: String) = document.selectFirstOrNull(query)?.text()

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun selectFirstTextOptional(query: String) = Optional.ofNullable(firstTextOrNull(query))

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun firstText(query: String) = selectFirstText(query)

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun firstTextOrNull(query: String) = selectFirstTextOrNull(query)

    /**
     * Find the text content of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun firstTextOptional(query: String) = selectFirstTextOptional(query)

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    @JvmOverloads
    fun selectFirstAttribute(query: String, attrName: String, defaultValue: String = "") =
        firstAttributeOrNull(query, attrName) ?: defaultValue

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun selectFirstAttributeOrNull(query: String, attrName: String) = selectFirstOrNull(query)?.attr(attrName)

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun selectFirstAttributeOptional(query: String, attrName: String) =
        Optional.ofNullable(firstAttributeOrNull(query, attrName))

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    @JvmOverloads
    fun firstAttribute(query: String, attrName: String, defaultValue: String = "") =
        selectFirstAttribute(query, attrName, defaultValue)

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun firstAttributeOrNull(query: String, attrName: String) = selectFirstAttributeOrNull(query, attrName)

    /**
     * Find the attribute value of the first element that match the CSS query, with the document as the starting context.
     * Matched element may be the document, or any of its children.
     * */
    fun firstAttributeOptional(query: String, attrName: String) = selectFirstAttributeOptional(query, attrName)

    /**
     * Find hyperlinks in elements matching the CSS query.
     * */
    @JvmOverloads
    fun selectHyperlinks(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) =
        document.selectHyperlinks(query, offset, limit)

    /**
     * Find anchor elements matching the CSS query.
     * */
    @JvmOverloads
    fun selectAnchors(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) =
        document.selectAnchors(query, offset, limit)

    /**
     * Find image elements matching the CSS query.
     * */
    @JvmOverloads
    fun selectImages(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE) =
        document.selectImages(query, offset, limit)

    /**
     * Traverse the DOM and apply the [action] to each [Node].
     * */
    fun forEach(action: (Node) -> Unit) {
        NodeTraversor.traverse({ node: Node, _ -> action(node) }, document)
    }

    /**
     * Traverse the DOM and apply the [action] to each [Node] that matches [predicate].
     * */
    fun forEachMatching(predicate: (Node) -> Boolean, action: (Node) -> Unit) =
        document.forEachMatching(predicate, action)

    /**
     * Traverse the DOM and apply the [action] to each [Element].
     * */
    fun forEachElement(action: (Element) -> Unit) = document.forEachElement(true, action)

    /**
     * Traverse the DOM and apply the [action] to each [Element] that matches [predicate].
     * */
    fun forEachElementMatching(predicate: (Element) -> Boolean, action: (Element) -> Unit) =
        document.forEachElementMatching(predicate, action)

    /**
     * Count nodes matching [predicate].
     * */
    fun count(predicate: (Node) -> Boolean = {true}) = document.count(predicate)

    /**
     * Count elements matching [predicate].
     * */
    fun countElements(predicate: (Element) -> Boolean = {true}) = document.countElements(predicate)

    /**
     * Retrieves the feature with the given key.
     * */
    fun getFeature(key: Int) = document.getFeature(key)

    /**
     * Format node features.
     * */
    fun formatFeatures(vararg featureKeys: Int) = document.formatEachFeatures(*featureKeys)

    /**
     * Format named node features.
     * */
    fun formatNamedFeatures() = document.formatNamedFeatures()

    /**
     * Remove attributes associated with the given keys.
     * */
    fun removeAttrs(vararg attributeKeys: String) {
        NodeTraversor.traverse({ node: Node, _ -> node.extension.removeAttrs(*attributeKeys) }, document)
    }

    /**
     * Remove all script nodes in the document.
     * */
    fun removeScripts() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ -> if (node.nodeName() == "script") removal.add(node) }, document)
        removal.forEach { it.takeIf { it.hasParent() }?.remove() }
    }

    /**
     * Remove all script nodes in the document.
     * */
    fun stripScripts() = removeScripts()

    /**
     * Remove all style nodes from the document, and remove all style attributes from all elements.
     * */
    fun removeStyles() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ ->
            if (node.nodeName() == "style" || node.attr("type") == "text/css"
                || node.attr("ref") == "stylesheet"
            ) {
                removal.add(node)
            }
            node.removeAttr("style")
        }, document)
        removal.forEach { it.remove() }
    }

    fun stripStyles() = removeStyles()

    /**
     * Export the document.
     * */
    fun export(): Path {
        val filename = AppPaths.fromUri(location, "", ".htm")
        val path = AppPaths.WEB_CACHE_DIR.resolve("featured").resolve(filename)
        return exportTo(path)
    }

    /**
     * Export the document to the given path.
     * */
    fun exportTo(path: Path): Path {
        return AppFiles.saveTo(prettyHtml.toByteArray(), path, deleteIfExists = true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true;
        }

        return other is FeaturedDocument && location == other.location
    }

    override fun hashCode() = location.hashCode()

    override fun toString() = document.uniqueName

    private fun initialize() {
        // Only one thread is allow to access the document
        document.threadIds.add(Thread.currentThread().id)
        if(document.threadIds.size != 1) {
            val threads = document.threadIds.joinToString()
            System.err.println("Warning: multiple threads ($threads) are process document | $location")
        }

        if (document.isInitialized.compareAndSet(false, true)) {
            FeatureCalculatorFactory.calculator.calculate(document)
            require(features.isNotEmpty)

            document.unitArea = densityUnitArea
            document.primaryGrid = primaryGridDimension
            document.secondaryGrid = secondaryGridDimension
            document.grid = document.primaryGrid

            calculateInducedFeatures()
        }
    }

    /**
     * Calculate features depend on other features
     * */
    private fun calculateInducedFeatures() {
        // Calculate text node density
        val unitArea = document.unitArea
        document.forEach {
            // add a smooth number to make sure the dividend is not zero
            it.textNodeDensity = 1.0 * it.numTextNodes / it.area.coerceAtLeast(1) * unitArea
        }
    }
}
