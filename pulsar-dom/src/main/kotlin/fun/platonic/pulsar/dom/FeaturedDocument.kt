package `fun`.platonic.pulsar.dom

import `fun`.platonic.pulsar.common.config.PulsarConstants.NIL_PAGE_URL
import `fun`.platonic.pulsar.common.math.vectors.isEmpty
import `fun`.platonic.pulsar.dom.features.FeatureCalculator
import `fun`.platonic.pulsar.dom.nodes.node.ext.*
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor

open class FeaturedDocument(val document: Document) {
    companion object {
        val NIL: FeaturedDocument = FeaturedDocument.createShell(NIL_PAGE_URL)
        val NIL_DOC_HTML = NIL.unbox().outerHtml()
        val NIL_DOC_LENGTH = NIL_DOC_HTML.length

        var SELECTOR_IN_BOX_DEVIATION = 25

        fun createShell(baseUri: String): FeaturedDocument {
            val document = Document.createShell(baseUri)
            return FeaturedDocument(document)
        }

        /**
         * An element is Nil, if it's owner document is nil
         * */
        fun isNil(node: Node): Boolean {
            return node.baseUri() == NIL.baseUri
        }
    }

    val fragments by lazy { DocumentFragments(this) }

    constructor(baseUri: String): this(Document(baseUri))

    constructor(other: FeaturedDocument): this(other.unbox().clone())

    init {
        if (document.features.isEmpty) {
            NodeTraversor.traverse(FeatureCalculator(document), document)
        }
    }

    var baseUri: String
        get() = document.baseUri()
        set(value) = document.setBaseUri(value)

    var title: String
        get() = document.title()
        set(value) = document.title(value)

    val location: String
        get() = document.location()

    val head: Element
        get() = document.head() ?: throw RuntimeException("Bad document, head tag is missing")

    val body: Element
        get() = document.body() ?: throw RuntimeException("Bad document, body tag is missing")

    val text: String
        get() = document.text()

    val html: String
        get() = document.html()

    val prettyHtml: String
        get() {
            val str = document.html()
                    .replace("s-features", "\n\t\t\ts-features")
                    .replace("s-named-features", "\n\t\t\ts-named-features")
                    .replace("s-caption", "\n\t\t\ts-caption")

            return str
        }

    var features: RealVector
        get() = document.features
        set(value) {
            document.features = value
        }

    fun unbox(): Document {
        return document
    }

    fun isNil(): Boolean {
        return baseUri == NIL.baseUri
    }

    fun createElement(tagName: String): Element {
        return document.createElement(tagName)
    }

    fun absoluteLinks() {
        document.forEachElement {
            if (it.hasAttr("href")) {
                it.attr("href", it.attr("abs:href"))
            } else if (it.hasAttr("src")) {
                it.attr("src", it.attr("abs:src"))
            }
        }
    }

    @JvmOverloads
    fun select2(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
        return document.select2(query, offset, limit)
    }

    fun select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
        return document.select2(query, offset, limit)
    }

    fun <T> select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, extractor: (Element) -> T): List<T> {
        return document.select2(query, offset, limit).map { extractor(it) }
    }

    fun first(query: String): Element? {
        return document.first(query)
    }

    fun <T> first(query: String, extractor: (Element) -> T): T? {
        return document.first(query)?.let { extractor(it) }
    }

    fun getFeature(key: Int): Double {
        return document.getFeature(key)
    }

    fun formatFeatures(vararg featureKeys: Int): String {
        return document.formatEachFeatures(*featureKeys)
    }

    fun formatNamedFeatures(): String {
        return document.formatVariables()
    }

    fun removeAttrs(vararg attributeKeys: String) {
        NodeTraversor.traverse({ node, _ ->  node.removeAttrs(*attributeKeys) }, document)
    }

    fun stripScripts() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ ->  if (node.nodeName() == "script") removal.add(node) }, document)
        removal.forEach { it.remove() }
    }

    fun stripStyles() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ ->  if (node.nodeName() == "style") removal.add(node) }, document)
        removal.forEach { it.remove() }
    }

    override fun equals(other: Any?): Boolean {
        return other is FeaturedDocument && baseUri == other.baseUri
    }

    override fun hashCode(): Int {
        return baseUri.hashCode()
    }

    override fun toString(): String {
        return document.uniqueName
    }
}
