package ai.platon.pulsar.dom

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.AppConstants.INTERNAL_URL_PREFIX
import ai.platon.pulsar.common.math.vectors.isNotEmpty
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.dom.nodes.forEach
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.select
import ai.platon.pulsar.dom.select.select2
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.dom.select.selectHyperlinks
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import java.awt.Dimension
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * The featured document.
 *
 * A featured document have all it's features calculated
 * */
open class FeaturedDocument(val document: Document) {
    companion object {
        var SELECTOR_IN_BOX_DEVIATION = 25

        var primaryGridDimension = Dimension(30, 15) // about 1 em
        var secondaryGridDimension = Dimension(5, 5)
        var densityUnitArea = 400 * 400

        val sequencer = AtomicInteger()

        val NIL = FeaturedDocument(nilDocument)
        val NIL_DOC_HTML = NIL.unbox().outerHtml()
        val NIL_DOC_LENGTH = NIL_DOC_HTML.length

        fun createShell(baseUri: String): FeaturedDocument {
            val document = Document.createShell(baseUri)
            return FeaturedDocument(document)
        }

        /**
         * A node is Nil when it's owner document is nil
         * */
        fun isNil(doc: FeaturedDocument): Boolean {
            return doc == NIL || doc.location == NIL.location
        }

        fun isInternal(doc: FeaturedDocument): Boolean {
            return doc.location.startsWith(INTERNAL_URL_PREFIX)
        }
    }

    @Deprecated("Fragment is no longer used")
    val fragments by lazy { DocumentFragments(this) }

    val documentOrNull get() = document.takeIf { isNotInternal() }

    constructor(baseUri: String) : this(Document(baseUri))

    constructor(other: FeaturedDocument) : this(other.unbox())

    init {
        initialize()
    }

    val sequence get() = sequencer.incrementAndGet()

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
            document.outputSettings().prettyPrint()
            return document.html()
                .replace("s-features", "\n\t\t\ts-features")
                .replace("s-named-features", "\n\t\t\ts-named-features")
                .replace("s-caption", "\n\t\t\ts-caption")
        }

    var features: RealVector
        get() = document.extension.features
        set(value) {
            document.extension.features = value
        }

    fun unbox() = document

    fun isNil() = isNil(this)

    fun isInternal() = isInternal(this)

    fun isNotInternal() = !isInternal()

    fun createElement(tagName: String) = document.createElement(tagName)

    /**
     * For some site without a <title> tag inside a <head> tag
     * */
    fun guessTitle(): String {
        return title.takeUnless { it.isBlank() }
            ?: firstTextOrNull("title")
            ?: firstTextOrNull("h1")
            ?: firstTextOrNull("h2")
            ?: ""
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

    fun selectFirst(query: String): Element {
        return document.selectFirstOrNull(query) ?: throw NoSuchElementException("No element matching $query")
    }

    fun <T> selectFirst(query: String, transformer: (Element) -> T): T {
        return document.selectFirstOrNull(query)?.let { transformer(it) }
            ?: throw NoSuchElementException("No element matching $query")
    }

    fun selectFirstOrNull(query: String): Element? {
        return document.selectFirstOrNull(query)
    }

    fun <T> selectFirstOrNull(query: String, transformer: (Element) -> T): T? {
        return document.selectFirstOrNull(query)?.let { transformer(it) }
    }

    /**
     * Return the first element, if no element matches the query, return an Optional object.
     * */
    fun selectFirstOptional(query: String): Optional<Element> {
        return Optional.ofNullable(document.selectFirstOrNull(query))
    }

    /**
     * Return the first element, if no element matches the query, return an Optional object.
     * */
    fun <T> selectFirstOptional(query: String, transformer: (Element) -> T): Optional<T> {
        return Optional.ofNullable(document.selectFirstOrNull(query)?.let { transformer(it) })
    }

    @JvmOverloads
    fun selectHyperlinks(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): List<Hyperlink> {
        return document.selectHyperlinks(query, offset, limit)
    }

    @Deprecated("Inappropriate name", ReplaceWith("selectFirst(query)"))
    fun first(query: String): Element? {
        return document.selectFirstOrNull(query)
    }

    @Deprecated("Inappropriate name", ReplaceWith("selectFirst(query, transformer)"))
    fun <T> first(query: String, transformer: (Element) -> T): T? {
        return document.selectFirstOrNull(query)?.let { transformer(it) }
    }

    fun firstText(query: String): String {
        return firstTextOrNull(query) ?: throw NoSuchElementException("No element matching $query")
    }

    fun firstTextOrNull(query: String): String? {
        return document.selectFirstOrNull(query)?.text()
    }

    fun firstTextOptional(query: String): Optional<String> {
        return Optional.ofNullable(firstTextOrNull(query))
    }

    fun firstAttribute(query: String, attrName: String, defaultValue: String = ""): String {
        return firstAttributeOrNull(query, attrName) ?: defaultValue
    }

    fun firstAttributeOrNull(query: String, attrName: String): String? {
        return selectFirstOrNull(query)?.attr(attrName)
    }

    fun firstAttributeOptional(query: String, attrName: String): Optional<String> {
        return Optional.ofNullable(firstAttributeOrNull(query, attrName))
    }

    fun getFeature(key: Int) = document.getFeature(key)

    fun formatFeatures(vararg featureKeys: Int) = document.formatEachFeatures(*featureKeys)

    fun formatNamedFeatures() = document.formatNamedFeatures()

    fun removeAttrs(vararg attributeKeys: String) {
        NodeTraversor.traverse({ node: Node, _ ->  node.extension.removeAttrs(*attributeKeys) }, document)
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
        if (this === other) {
            return true;
        }

        return other is FeaturedDocument && location == other.location
    }

    override fun hashCode() = location.hashCode()

    override fun toString() = document.uniqueName

    private fun initialize() {
        // TODO: Only one thread is allow to access the document
        document.threadIds.add(Thread.currentThread().id)
        if(document.threadIds.size != 1) {
            val threads = document.threadIds.joinToString()
            System.err.println("Warning: multiple threads ($threads) are process document | $location")
        }

        if (document.isInitialized.compareAndSet(false, true)) {
            FeatureCalculatorFactory.calculator.calculate(document)
            require(features.isNotEmpty)

            sequencer.incrementAndGet()

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
