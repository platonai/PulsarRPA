package ai.platon.pulsar.dom

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes.NODE_FEATURE_CALCULATOR
import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_NODE_FEATURE_CALCULATOR
import ai.platon.pulsar.common.config.PulsarConstants.NIL_PAGE_URL
import ai.platon.pulsar.common.math.vectors.isEmpty
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.select
import ai.platon.pulsar.dom.select.select2
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.nio.file.Path

open class FeaturedDocument(val document: Document) {
    companion object {
        var SELECTOR_IN_BOX_DEVIATION = 25
        private val nodeFeatureCalculatorClass: Class<NodeVisitor> by lazy { loadFeatureCalculatorClass() }
        val nodeFeatureCalculator: NodeVisitor get() = nodeFeatureCalculatorClass.newInstance()

        val NIL = createShell(NIL_PAGE_URL)
        val NIL_DOC_HTML = NIL.unbox().outerHtml()
        val NIL_DOC_LENGTH = NIL_DOC_HTML.length

        fun createShell(baseUri: String): FeaturedDocument {
            val document = Document.createShell(baseUri)
            return FeaturedDocument(document)
        }
        /**
         * An node is Nil, if it's owner document is nil
         * */
        fun isNil(doc: FeaturedDocument): Boolean {
            return doc == NIL || doc.location == NIL.location
        }
        fun getExportFilename(uri: String): String {
            return PulsarPaths.fromUri(uri, ".htm")
        }
        fun getExportPath(url: String, ident: String): Path {
            return PulsarPaths.get(PulsarPaths.webCacheDir, ident, getExportFilename(url))
        }

        private fun loadFeatureCalculatorClass(): Class<NodeVisitor> {
            val defaultClassName = DEFAULT_NODE_FEATURE_CALCULATOR
            val className = System.getProperty(NODE_FEATURE_CALCULATOR, defaultClassName)
            return ResourceLoader.loadUserClass(className)
        }
    }

    val fragments by lazy { DocumentFragments(this) }

    constructor(baseUri: String): this(Document(baseUri))

    constructor(other: FeaturedDocument): this(other.unbox().clone())

    init {
        if (features.isEmpty) {
            NodeTraversor.traverse(nodeFeatureCalculator, document)
        }
    }

    var title: String
        get() = document.title()
        set(value) = document.title(value)

    val location: String get() = document.location()

    val head: Element
        get() = document.head() ?: throw RuntimeException("Bad document, head tag is missing")

    val body: Element
        get() = document.body() ?: throw RuntimeException("Bad document, body tag is missing")

    val text: String get() = document.text()

    val html: String get() = document.html()

    val prettyHtml: String
        get() {
            document.outputSettings().prettyPrint()
            return document.html()
                    .replace("s-features", "\n\t\t\ts-features")
                    .replace("s-named-features", "\n\t\t\ts-named-features")
                    .replace("s-caption", "\n\t\t\ts-caption")
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
        return location == NIL.location
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
    fun select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
        return document.select2(query, offset, limit)
    }

    fun <T> select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE, transformer: (Element) -> T): List<T> {
        return document.select(query, offset, limit, transformer = transformer)
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

    fun export(): Path {
        val filename = PulsarPaths.fromUri(location, ".html")
        val path = PulsarPaths.get(PulsarPaths.webCacheDir, "featured", filename)
        return exportTo(path)
    }

    fun exportTo(path: Path): Path {
        return PulsarFiles.saveTo(prettyHtml.toByteArray(), path, deleteIfExists = true)
    }

    override fun equals(other: Any?): Boolean {
        return other is FeaturedDocument && location == other.location
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }

    override fun toString(): String {
        return document.uniqueName
    }
}
