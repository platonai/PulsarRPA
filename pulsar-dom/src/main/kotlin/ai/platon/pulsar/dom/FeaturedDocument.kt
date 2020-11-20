package ai.platon.pulsar.dom

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.AppConstants.DEFAULT_NODE_FEATURE_CALCULATOR
import ai.platon.pulsar.common.config.AppConstants.INTERNAL_URL_PREFIX
import ai.platon.pulsar.common.config.CapabilityTypes.NODE_FEATURE_CALCULATOR_CLASS
import ai.platon.pulsar.common.math.vectors.isNotEmpty
import ai.platon.pulsar.dom.nodes.forEach
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.select
import ai.platon.pulsar.dom.select.select2
import ai.platon.pulsar.dom.select.selectFirstOrNull
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.awt.Dimension
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

@Deprecated("We have better implementation",
        replaceWith = ReplaceWith("ai.platon.pulsar.dom.FeatureCalculatorFactory"))
class DocumentFeatureCalculatorFactory {

    val featureCalculatorClass: Class<NodeVisitor> by lazy { loadFeatureCalculatorClass() }

    val newInstance get() = featureCalculatorClass.newInstance() as NodeVisitor

    private fun loadFeatureCalculatorClass(): Class<NodeVisitor> {
        val className = System.getProperty(NODE_FEATURE_CALCULATOR_CLASS, DEFAULT_NODE_FEATURE_CALCULATOR)
        return ResourceLoader.loadUserClass(className)
    }
}

open class FeaturedDocument(val document: Document) {
    companion object {
        var SELECTOR_IN_BOX_DEVIATION = 25

        var primaryGridDimension = Dimension(30, 15) // about 1 em
        var secondaryGridDimension = Dimension(5, 5)
        var densityUnitArea = 400 * 400

        val globalNumDocuments = AtomicInteger()

        val calculatorFactory = FeatureCalculatorFactory()

        val NIL = FeaturedDocument(nilDocument)
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

        fun isInternal(doc: FeaturedDocument): Boolean {
            return doc.location.startsWith(INTERNAL_URL_PREFIX)
        }
    }

    val fragments by lazy { DocumentFragments(this) }

    val documentOrNull get() = document.takeIf { isNotInternal() }

    constructor(baseUri: String): this(Document(baseUri))

    constructor(other: FeaturedDocument): this(other.unbox())

    init {
        // TODO: Only one thread is allow to access the document
        document.threadIds.add(Thread.currentThread().id)
        if(document.threadIds.size != 1) {
            val threads = document.threadIds.joinToString()
            System.err.println("Warning: multiple threads ($threads) are process document | $location")
        }

        if (document.isInitialized.compareAndSet(false, true)) {
            calculatorFactory.activeCalculator.calculate(document)
            require(features.isNotEmpty)

            globalNumDocuments.incrementAndGet()

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

    var title: String
        get() = document.title()
        set(value) = document.title(value)

    /**
     * Get the URL this Document was parsed from.
     */
    val baseUri get() = document.baseUri()

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     */
    val location get() = document.location()

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

    fun unbox() = document

    fun isNil() = isNil(this)

    fun isInternal() = isInternal(this)

    fun isNotInternal() = !isInternal()

    fun createElement(tagName: String) = document.createElement(tagName)

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

    fun selectFirst(query: String): Element {
        return document.selectFirstOrNull(query)?:throw NoSuchElementException()
    }

    fun <T> selectFirst(query: String, extractor: (Element) -> T): T {
        return document.selectFirstOrNull(query)?.let { extractor(it) }?:throw NoSuchElementException()
    }

    fun selectFirstOrNull(query: String): Element? {
        return document.selectFirstOrNull(query)
    }

    fun <T> selectFirstOrNull(query: String, extractor: (Element) -> T): T? {
        return document.selectFirstOrNull(query)?.let { extractor(it) }
    }

    fun first(query: String): Element? {
        return document.selectFirstOrNull(query)
    }

    fun <T> first(query: String, extractor: (Element) -> T): T? {
        return document.selectFirstOrNull(query)?.let { extractor(it) }
    }

    fun getFeature(key: Int) = document.getFeature(key)

    fun formatFeatures(vararg featureKeys: Int) = document.formatEachFeatures(*featureKeys)

    fun formatNamedFeatures() = document.formatNamedFeatures()

    fun removeAttrs(vararg attributeKeys: String) {
        NodeTraversor.traverse({ node, _ ->  node.removeAttrs(*attributeKeys) }, document)
    }

    fun stripScripts() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ ->  if (node.nodeName() == "script") removal.add(node) }, document)
        removal.forEach { it.takeIf { it.hasParent() }?.remove() }
    }

    fun stripStyles() {
        val removal = mutableSetOf<Node>()
        NodeTraversor.traverse({ node, _ ->
            if (node.nodeName() == "style" || node.attr("type") == "text/css"
                    || node.attr("ref") == "stylesheet") {
                removal.add(node)
            }
            node.removeAttr("style")
        }, document)
        removal.forEach { it.remove() }
    }

    fun export(): Path {
        val filename = AppPaths.fromUri(location, "", ".htm")
        val path = AppPaths.WEB_CACHE_DIR.resolve("featured").resolve(filename)
        return exportTo(path)
    }

    fun exportTo(path: Path): Path {
        return AppFiles.saveTo(prettyHtml.toByteArray(), path, deleteIfExists = true)
    }

    override fun equals(other: Any?): Boolean {
        return other is FeaturedDocument && location == other.location
    }

    override fun hashCode() = location.hashCode()

    override fun toString() = document.uniqueName
}
