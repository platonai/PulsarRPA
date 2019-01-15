package `fun`.platonic.scent.dom.nodes

import `fun`.platonic.pulsar.common.config.PulsarConstants.NIL_PAGE_URL
import `fun`.platonic.scent.dom.*
import `fun`.platonic.scent.dom.select.ElementTraversor
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import org.perf4j.slf4j.Slf4JStopWatch
import java.awt.Dimension

open class DocumentSettings {

    companion object {
        var SELECTOR_IN_BOX_DEVIATION = 25
        var CONST_TEXT_NODE_RATE_THRESHOLD = 0.85
        var MIN_TN_IN_CONSTANT_TEXT_BLOCK = 4

        var PAGE_PRIMARY_GRID_DIMENSION = Dimension(10, 10)
        var PAGE_SECONDARY_GRID_DIMENSION = Dimension(5, 5)

        val default = DocumentSettings()

        var defaultDisplayFeatures = intArrayOf(
                LEFT, TOP, WIDTH, HEIGHT, IMG, A
        )
    }

    var primaryGridWidth = PAGE_PRIMARY_GRID_DIMENSION.width
    var primaryGridHeight = PAGE_PRIMARY_GRID_DIMENSION.height
    var secondaryGridWidth = PAGE_SECONDARY_GRID_DIMENSION.width
    var secondaryGridHeight = PAGE_SECONDARY_GRID_DIMENSION.height
    var densityUnitArea = 400 * 400 // unit area in pixel
}

class DescriptiveDocument(
        val document: Document,
        val settings: DocumentSettings = DocumentSettings.default
) {
    companion object {
        val NIL: DescriptiveDocument = DescriptiveDocument.createShell(NIL_PAGE_URL)

        fun createShell(baseUri: String): DescriptiveDocument {
            val document = Document.createShell(baseUri)
            return DescriptiveDocument(document, DocumentSettings.default)
        }

        /**
         * An element is Nil, if it's owner document is nil
         * */
        fun isNil(node: Node): Boolean {
            return node.baseUri() == NIL.baseUri
        }
    }

    private val featuresToIndex: IntArray = intArrayOf(CH, IMG, A, C)
    val nodeIndexer = MultiIntNodeIndexer(*featuresToIndex)

    constructor(baseUri: String, settings: DocumentSettings = DocumentSettings.default)
            : this(Document(baseUri), settings)

    constructor(other: DescriptiveDocument) : this(other.unbox().clone(), other.settings)

    init {
        if (document.features.isEmpty) {
            document.unitArea = settings.densityUnitArea
            document.primaryGrid = Dimension(settings.primaryGridWidth, settings.primaryGridHeight)
            document.secondaryGrid = Dimension(settings.secondaryGridWidth, settings.secondaryGridHeight)

            calculateFeatures()
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
            if (it.tagName() in arrayOf("a", "link")) {
                it.attr("href", it.attr("abs:href"))
            } else if (it.isImage) {
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
        return other is DescriptiveDocument && baseUri == other.baseUri
    }

    override fun hashCode(): Int {
        return baseUri.hashCode()
    }

    override fun toString(): String {
        return document.uniqueName
    }

    private fun calculateFeatures() {
        if (baseUri.startsWith("http")) {
            calculateFeaturesVerbose()
        } else {
            calculateFeatures(document)
            createNodeIndex(C)
        }
    }

    private fun calculateFeaturesVerbose() {
        val stopWatch = Slf4JStopWatch()
        // stopWatch.start("calc features")
        calculateFeatures(document)
        createNodeIndex(C)
        // stopWatch.stop("features calc finished")
    }

    private fun createNodeIndex(feature: Int) {
        createNodeIndexes(feature)
    }

    private fun createNodeIndexes(vararg features: Int) {
        ElementTraversor.traverse(NodeIndexerCalculator(nodeIndexer, features), document)
    }

    private fun calculateFeatures(ele: Element) {
        NodeTraversor.traverse(FeatureCalculator(this), ele)
    }
}
