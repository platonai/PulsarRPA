package ai.platon.pulsar.dom.nodes.node.ext

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.AppConstants.PULSAR_ATTR_HIDDEN
import ai.platon.pulsar.common.config.AppConstants.PULSAR_ATTR_OVERFLOW_HIDDEN
import ai.platon.pulsar.common.geometric.str
import ai.platon.pulsar.common.geometric.str2
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.math.vectors.set
import ai.platon.pulsar.dom.features.FeatureEntry
import ai.platon.pulsar.dom.features.FeatureFormatter
import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.defined.*
import ai.platon.pulsar.dom.model.createLink
import ai.platon.pulsar.dom.nodes.*
import org.apache.commons.lang3.StringUtils
import org.apache.commons.math3.linear.ArrayRealVector
import org.jsoup.nodes.*
import org.jsoup.select.NodeTraversor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty

class DoubleFeature(val name: Int) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): Double = thisRef.extension.features[name]

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: Double) {
        thisRef.extension.features[name] = value
    }
}

class IntFeature(val name: Int) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): Int = thisRef.extension.features[name].toInt()

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: Int) {
        thisRef.extension.features[name] = value.toDouble()
    }
}

class MapField<T>(val initializer: (Node) -> T) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): T =
            thisRef.extension.variables[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: T): T {
        thisRef.extension.variables[property.name] = value
        return value
    }
}

class NullableMapField<T> {
    operator fun getValue(thisRef: Node, property: KProperty<*>): T? = thisRef.extension.variables[property.name] as T?

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: T?) {
        thisRef.extension.variables[property.name] = value
    }
}

fun <T> field(initializer: (Node) -> T): MapField<T> {
    return MapField(initializer)
}

inline fun <reified T> nullableField(): NullableMapField<T> {
    return NullableMapField()
}

class ExportPaths(val uri: String) {
    val namedPath = mutableMapOf<String, Path>()

    val filename get() = AppPaths.fromUri(uri, "", ".htm")
    val portal get() = get("portal", filename)
    val annotatedView get() = get("annotated", filename)
    val tileView get() = get("tile", filename)
    val entityView get() = get("entity", filename)

    fun byType(type: String) = namedPath.computeIfAbsent(type) { get(type, filename) }

    companion object {
        fun get(first: String, second: String): Path {
            return AppPaths.DOC_EXPORT_DIR.resolve(first).resolve(second)
        }
    }
}

val nilDocument = Document.createShell(AppConstants.NIL_PAGE_URL)

val nilElement = nilDocument.body()

val nilNode = nilElement as Node

val Document.isNil get() = this === nilDocument

val Document.pulsarMetaElement get() = getElementById("#${AppConstants.PULSAR_META_INFORMATION_ID}")

val Document.pulsarScriptElement get() = getElementById("#${AppConstants.PULSAR_SCRIPT_SECTION_ID}")

val Document.pulsarScript get() = ownerDocument.pulsarScriptElement?.text()

var Document.isInitialized by field { AtomicBoolean() }

val Document.threadIds by field { ConcurrentSkipListSet<Long>() }

val Document.viewPort by field { it.calculateViewPort() }

// geometric grid, we have two grids, a bigger one and a smaller one

var Document.primaryGrid by field { Dimension(0, 0) }

var Document.secondaryGrid by field { Dimension(0, 0) }

var Document.grid by field { Dimension(0, 0) }

var Document.unitArea by field { 0 }

var Document.exportPaths by field { ExportPaths(it.baseUri()) }

var Document.annotated by field { false }

// TODO: check if this override Node.isNil or not?
val Element.isNil get() = this === nilElement

fun Element.addClasses(vararg classNames: String): Element {
    classNames.forEach { addClass(it) }
    return this
}

fun Element.slimCopy(): Element {
    val ele = this.clone()
    ele.forEachElement(includeRoot = true) { it.removeNonStandardAttrs() }
    return ele
}

fun Element.ownTexts(): List<String> {
    return this.childNodes().mapNotNullTo(mutableListOf()) { (it as? TextNode)?.text() }
}

fun Element.qualifiedClassNames(): Set<String> {
    val classNames = className().split("\\s+".toRegex()).toMutableSet()
    return getQualifiedClassNames(classNames)
}

fun Element.anyAttr(attributeKey: String, attributeValue: Any): Element {
    this.attr(attributeKey, attributeValue.toString())
    return this
}

fun Element.removeTemporaryAttrs(): Element {
    this.attributes().map { it.key }.filter { it in TEMPORARY_ATTRIBUTES || it.startsWith("tv") }.forEach {
        this.removeAttr(it)
    }
    return this
}

fun Element.removeNonStandardAttrs(): Element {
    this.attributes().map { it.key }.forEach { if (it !in STANDARD_ATTRIBUTES) {
        this.removeAttr(it)
    } }
    return this
}

fun Element.parseStyle(): Array<String> {
    return Strings.stripNonChar(attr("style"), ":;")
            .split(";".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
}

fun Element.getStyle(styleKey: String): String {
    return getStyle(parseStyle(), styleKey)
}

val Node.isNil get() = this === nilNode

val Node.ownerDocument get() = Objects.requireNonNull(extension.ownerDocumentNode) as Document

/**
 * Get the URL this Document was parsed from. If the starting URL is a redirect,
 * this will return the final URL from which the document was served from.
 *
 * Note: In most cases the base URL is simply the location of the document, but it can be affected by many factors,
 * including the <base> element in HTML and the xml:base attribute in XML.
 *
 * The base URL of a document is used to resolve relative URLs when the browser needs to obtain an absolute URL,
 * for example when processing the HTML <img> element's src attribute or XML xlink:href attribute.
 *
 * @return location
 */
val Node.location: String get() = ownerDocument.location()

var Node.depth by IntFeature(DEP)

val Node.sequence by IntFeature(SEQ)

val Node.globalId: String get() = "$location $sequence-$left-$top-$width-$height"

/*********************************************************************
 * Geometric information
 * *******************************************************************/

var Node.left by IntFeature(LEFT)

var Node.top by IntFeature(TOP)

var Node.width: Int by IntFeature(WIDTH)

var Node.height: Int by IntFeature(HEIGHT)

val Node.right: Int get() = left + width

val Node.bottom: Int get() = top + height

val Node.x get() = left

val Node.y get() = top

val Node.x2 get() = right

val Node.y2 get() = bottom

val Node.centerX get() = (x + x2) / 2

val Node.centerY get() = (y + y2) / 2

val Node.geoLocation get() = Point(x, y)

val Node.dimension get() = Dimension(width, height)

val Node.rectangle get() = Rectangle(x, y, width, height)

val Node.area get() = width * height

/** Hidden flag set by browser */
val Node.hasHiddenFlag: Boolean get() = hasAttr(PULSAR_ATTR_HIDDEN)
/** Overflow hidden flag set by browser */
val Node.hasOverflowHiddenFlag: Boolean get() = hasAttr(PULSAR_ATTR_OVERFLOW_HIDDEN)
/** Check if the node is visible or not */
val Node.isVisible: Boolean get() {
    return when {
        isImage -> !hasHiddenFlag && !hasOverflowHiddenFlag // TODO: why a visible image have an empty rectangle?
        else -> !hasHiddenFlag && !hasOverflowHiddenFlag && x >= 0 && y >= 0 && !rectangle.isEmpty
    }
}

/** Check if the node is visible or not */
val Node.isHidden: Boolean get() = !this.isVisible
/** Whether the element is floating */
// val Node.isAbsolute: Boolean get() = hasAttr("_absolute")
// val Node.isFixed: Boolean get() = hasAttr("_fixed")

val Node.isText: Boolean get() = this is TextNode

val Node.isBlankText: Boolean get() { return this is TextNode && this.isBlank }

val Node.isNonBlankText: Boolean get() { return this is TextNode && !this.isBlank }

val Node.isRegularText: Boolean get() { return isVisible && isNonBlankText }

val Node.isImage: Boolean get() = this.nodeName() == "img"

val Node.isRegularImage: Boolean get() { return isImage && isVisible && hasAttr("src") }

/**
 * A <img> tag can contain any tag
 * */
val Node.isAnchorImage: Boolean get() = isImage && this.hasAncestor { it.isAnchor }

val Node.isAnchor: Boolean get() = this.nodeName() == "a"

val Node.isRegularAnchor: Boolean get() = isVisible && this.nodeName() == "a"

val Node.isImageAnchor: Boolean get() = isAnchor && this.numImages == 1

val Node.isRegularImageAnchor: Boolean get() = isRegularAnchor && this.numImages == 1

val Node.isTable: Boolean get() = this.nodeName() == "table"

val Node.isList: Boolean get() = this.nodeName() in arrayOf("ul", "ol")

/**
 * If the text is short
 * */
val Node.isShortText get() = isRegularText && cleanText.length in 1..9

val Node.isMediumText get() = isRegularText && cleanText.length in 1..20

val Node.isLongText get() = isRegularText && cleanText.length > 20

val Node.isCurrencyUnit get() = isShortText && cleanText in arrayOf("¥", "$")

val Node.isNumeric get() = isMediumText && StringUtils.isNumeric(cleanText)

// TODO: "isShortText" should be in -2147483648 to 2147483647, it's mapped to java.lang.Integer.
// TODO: detect all SQL types
val Node.isInt get() = isShortText && StringUtils.isNumeric(cleanText)

val Node.isFloat get() = isShortText && Strings.isFloat(cleanText)

/**
 * If the text is numeric and have non-numeric surroundings
 * */
val Node.isNumericLike get() = isMediumText && Strings.isNumericLike(cleanText)

val Node.isMoneyLike get() = isShortText && Strings.isMoneyLike(cleanText)

val Node.intValue by field { SParser(it.cleanText).getInt(Int.MIN_VALUE) }

val Node.doubleValue by field { SParser(it.cleanText).getDouble(Double.NaN) }

/*********************************************************************
 * Distinguished features
 * *******************************************************************/
var Node.numChars by IntFeature(CH)
var Node.numSiblings by IntFeature(SIB)
var Node.numChildren by IntFeature(C)
/** Number of descend text nodes */
var Node.numTextNodes by IntFeature(TN)

/** Number of descend images */
var Node.numImages by IntFeature(IMG)

/** Number of descend anchors */
var Node.numAnchors by IntFeature(A)

/** Text node density */
var Node.textNodeDensity by DoubleFeature(DNS)

// semantics
val Node.selectorOrName: String
    get() = when {
        this is Element -> this.cssSelector()
        else -> nodeName()
    }

val Node.captionOrName: String
    get() = when {
        hasCaption() -> caption
        else -> name
    }

val Node.captionOrSelector: String
    get() = when {
        hasCaption() -> caption
        else -> selectorOrName
    }

/**
 * The trimmed text of this node.
 *
 * TextNodes' texts are calculated and stored while Elements' clean texts are calculated on the fly.
 * This is a balance of space and time.
 * */
val Node?.cleanText: String get() =
    when (this) {
        is TextNode -> extension.immutableText.trim()
        is Element -> accumulateText(this).trim()
        else -> ""
    }.trim()

val Node.textRepresentation: String get() =
    when {
        isImage -> attr("abs:src")
        isAnchor -> attr("abs:href")
        this is TextNode -> cleanText
        this is Element -> cleanText
        else -> ""
    }

/**
 * TODO: slim table
 * */
val Node.slimHtml by field {
    val nm = it.nodeName()
    when {
        it.isImage || it.isAnchor || it.isNumericLike || it.isMoneyLike || it is TextNode || nm == "li" || nm == "td" -> atomSlimHtml(it)
        it is Element && (nm == "ul" || nm == "ol" || nm == "tr") ->
            String.format("<$nm>%s</$nm>", it.children().joinToString("") { c -> atomSlimHtml(c) })
        it is Element -> it.slimCopy().removeNonStandardAttrs().outerHtml()
        else -> String.format("<b>%s</b>", it.name)
    }
}

private fun atomSlimHtml(node: Node): String {
    val nm = node.nodeName()
    return when {
        node is TextNode -> String.format("<span>%s</span>", node.cleanText)
        node.isImage -> createSlimImageHtml(node)
        node.isAnchor -> createLink(node as Element, keepMetadata = false, lazy = true).toString()
        node.isNumericLike || node.isMoneyLike -> "<em>${node.cleanText}</em>"
        nm == "li" || nm == "td" || nm == "th" -> String.format("<$nm>%s</$nm>", node.cleanText)
        node is Element -> node.cleanText
        else -> String.format("<b>%s</b>", node.name)
    }
}

private fun createSlimImageHtml(node: Node): String = node.run { String.format("<img src='%s' vi='%s' alt='%s'/>",
        absUrl("src"), attr("vi"), attr("alt")) }

val Node.key: String get() = "$location#$sequence"

val Node.name: String
    get() {
        return when(this) {
            is Document -> ":root"
            is Element -> {
                val id = id()
                if (id.isNotEmpty()) {
                    return "#$id"
                }

                val cls = qualifiedClassNames()
                if (cls.isNotEmpty()) {
                    return cls.joinToString(".", ".") { it }
                }

                nodeName()
            }
            is TextNode -> {
                val postfix = if (siblingNodes().size > 1) {
                    "~" + siblingIndex()
                } else ""
                return bestElement.name + postfix
            }
            else -> nodeName()
        }
    }

val Node.canonicalName: String
    get() {
        when(this) {
            is Document -> {
                return location
            }
            is Element -> {
                var id = id().trim()
                if (!id.isEmpty()) {
                    id = "#$id"
                }

                var classes = ""
                if (id.isEmpty()) {
                    val cls = qualifiedClassNames()
                    if (cls.isNotEmpty()) {
                        classes = cls.joinToString(".", ".") { it }
                    }
                }

                return "${nodeName()}$id$classes"
            }
            is TextNode -> {
                val postfix = if (siblingNodes().size > 1) {
                    "~" + siblingIndex()
                } else ""
                return bestElement.canonicalName + postfix
            }
            else -> return nodeName()
        }
    }

val Node.uniqueName: String get() = "$sequence-$canonicalName"

val Node.namedRect: String get() = "$name-${rectangle.str}"

val Node.namedRect2: String get() = "$name-${rectangle.str2}"

/**
 * Get the parent element of this node, an exception is thrown if it's root
 * */
val Node.parentElement get() = this.parent() as Element

/**
 * Returns a best element to represent this node: if the node itself is an element, returns itself
 * otherwise, returns it's parent
 * */
val Node.bestElement get() = (this as? Element)?:parentElement

/**
 * The caption of an Element is a joined text values of all non-blank text nodes
 * */
val Node.caption get() = getCaptionWords().joinToString(";")

fun Node.attrOrNull(attributeKey: String): String? = (this as? Element)?.attr(attributeKey)?.takeIf { it.isNotBlank() }

fun Node.getFeature(key: Int): Double = extension.features[key]

fun Node.getFeature(name: String): Double = extension.features[NodeFeature.getKey(name)]

fun Node.getFeatureEntry(key: Int): FeatureEntry = FeatureEntry(key, getFeature(key))

fun Node.setFeature(key: Int, value: Double) {
    extension.features[key] = value
}

fun Node.setFeature(key: Int, value: Int) {
    extension.features[key] = value.toDouble()
}

fun Node.removeFeature(key: Int): Node {
    extension.features[key] = 0.0
    return this
}

fun Node.clearFeatures(): Node {
    extension.features = ArrayRealVector()
    return this
}

/**
 * Temporary node variables
 * */
inline fun <reified T> Node.getVariable(name: String): T? {
    val v = extension.variables[name]
    return if (v is T) v else null
}

inline fun <reified T> Node.getVariable(name: String, defaultValue: T): T {
    val v = extension.variables[name]
    return if (v is T) v else defaultValue
}

inline fun <reified T> Node.computeVariableIfAbsent(name: String, mappingFunction: (String) -> T): T {
    var v = extension.variables[name]
    if (v !is T) {
        v = mappingFunction(name)
        extension.variables[name] = v
    }
    return v
}

fun Node.setVariable(name: String, value: Any) {
    extension.variables[name] = value
}

fun Node.setVariableIfNotNull(name: String, value: Any?) {
    if (value != null) {
        extension.variables[name] = value
    }
}

fun Node.hasVariable(name: String): Boolean {
    return extension.variables.containsKey(name)
}

fun Node.removeVariable(name: String): Any? {
    return extension.variables.remove(name)
}

/**
 * Set attribute [attributeKey] to [attributeValue]
 * */
fun Node.anyAttr(attributeKey: String, attributeValue: Any): Node {
    this.attr(attributeKey, attributeValue.toString())
    return this
}

/**
 * Set attribute [attributeKey] to [attributeValue] and return [attributeValue]
 * */
fun Node.rAttr(attributeKey: String, attributeValue: String): String {
    this.attr(attributeKey, attributeValue)
    return attributeValue
}

/**
 * Set attribute [attributeKey] to [attributeValue] and return [attributeValue]
 * */
fun Node.rAnyAttr(attributeKey: String, attributeValue: Any): Any {
    this.attr(attributeKey, attributeValue.toString())
    return attributeValue
}

/**
 * append an attribute, no guarantee for uniqueness
 * */
fun Node.appendAttr(attributeKey: String, attributeValue: String, separator: String = StringUtils.SPACE) {
    var value = attr(attributeKey)
    if (!value.isEmpty()) {
        value += separator
    }
    value += attributeValue
    attr(attributeKey, value)
}

/**
 * Tuple data
 * */
fun Node.addTupleItem(tupleName: String, item: Any): Boolean {
    return extension.tuples.computeIfAbsent(tupleName) { mutableListOf() }.add(item)
}

/**
 *
 * */
fun Node.removeTupleItem(tupleName: String, item: Any): Boolean {
    return extension.tuples[tupleName]?.remove(item)?:return false
}

fun Node.getTuple(tupleName: String): List<Any> {
    return extension.tuples[tupleName]?:return listOf()
}

fun Node.hasTupleItem(tupleName: String, item: String): Boolean {
    return extension.tuples[tupleName]?.contains(item)?:return false
}

fun Node.hasTuple(tupleName: String): Boolean {
    return extension.tuples.containsKey(tupleName)
}

fun Node.clearTuple(tupleName: String) {
    extension.tuples[tupleName]?.clear()
}

fun Node.removeTuple(tupleName: String) {
    extension.tuples.remove(tupleName)
}

/**
 * Labels are unique, so if we add a labels into a node twice, we can get only one such label
 * */
fun Node.addLabel(label: String) {
    addTupleItem(A_LABELS, label.trim())
}

fun Node.removeLabel(label: String): Boolean {
    return removeTupleItem(A_LABELS, label)
}

fun Node.getLabels(): List<String> {
    return getTuple(A_LABELS).map { it.toString() }
}

fun Node.hasLabel(label: String): Boolean {
    return hasTupleItem(A_LABELS, label)
}

fun Node.clearLabels() {
    removeTuple(A_LABELS)
}

/**
 * Ml labels are unique, so if we add a labels into a node twice, we can get only one such label
 * */
fun Node.addMlLabel(label: String) {
    addTupleItem(A_ML_LABELS, label.trim())
}

fun Node.removeMlLabel(label: String): Boolean {
    return removeTupleItem(A_ML_LABELS, label)
}

fun Node.getMlLabels(): List<String> {
    return getTuple(A_ML_LABELS).map { it.toString() }
}

fun Node.hasMlLabel(label: String): Boolean {
    return hasTupleItem(A_ML_LABELS, label)
}

fun Node.clearMlLabels() {
    removeTuple(A_ML_LABELS)
}

fun Node.addCaptionWord(word: String) {
    addTupleItem(A_CAPTION, Strings.stripNonCJKChar(word))
}

fun Node.removeCaptionWord(word: String): Boolean {
    return removeTupleItem(A_CAPTION, word)
}

fun Node.getCaptionWords(): List<String> {
    return getTuple(A_CAPTION).map { it.toString() }
}

fun Node.hasCaptionWord(word: String): Boolean {
    return hasTupleItem(A_CAPTION, word)
}

fun Node.hasCaption(): Boolean {
    return hasTuple(A_CAPTION)
}

fun Node.clearCaption() {
    removeTuple(A_CAPTION)
}

fun Node.removeAttrs(vararg attributeKeys: String) {
    attributeKeys.forEach { this.removeAttr(it) }
}

fun Node.removeAttrs(attributeKeys: Iterable<String>) {
    attributeKeys.forEach { this.removeAttr(it) }
}

fun Node.removeAttrsIf(filter: (Attribute) -> Boolean) {
    val keys = attributes().mapNotNull { it.takeIf { filter(it) }?.key }
    removeAttrs(keys)
}

fun Node.formatEachFeatures(vararg featureKeys: Int): String {
    val sb = StringBuilder()
    NodeTraversor.traverse({ node: Node, _ ->
        FeatureFormatter.format(node.extension.features, featureKeys.asIterable(), sb = sb)
        sb.append('\n')
    }, this)
    return sb.toString()
}

fun Node.formatFeatures(vararg featureKeys: Int): String {
    return FeatureFormatter.format(extension.features, featureKeys.asIterable()).toString()
}

fun Node.formatNamedFeatures(): String {
    val sb = StringBuilder()

    NodeTraversor.traverse({ node, _ ->
        FeatureFormatter.format(node.extension.variables, sb)
        sb.append('\n')
    }, this)

    return sb.toString()
}

fun Node.hasAncestor(predicate: (Element) -> Boolean): Boolean {
    return findFirstAncestor(predicate) != null
}

fun Node.hasAncestor(stop: (Node) -> Boolean, predicate: (Element) -> Boolean): Boolean {
    return findFirstAncestor(predicate) != null
}

fun Node.isAncestorOf(other: Node): Boolean {
    return other.findFirstAncestor { it == this} != null
}

fun Node.isAncestorOf(other: Node, stop: (Node) -> Boolean): Boolean {
    return other.findFirstAncestor(stop) { it == this} != null
}

fun Node.ancestors(): List<Element> {
    val ancestors = mutableListOf<Element>()
    var p = this.parent()
    while (p is Element) {
        ancestors.add(p)
        p = p.parent()
    }

    return ancestors
}

private fun accumulateText(root: Element): String {
    val sb = StringBuilder()
    NodeTraversor.traverse({ node, depth ->
        if (node is TextNode) {
            if (node.extension.immutableText.isNotBlank()) {
                sb.append(node.extension.immutableText)
            }
        } else if (node is Element) {
            if (sb.isNotEmpty() && (node.isBlock || node.tagName() == "br")
                    && !(sb.isNotEmpty() && sb[sb.length - 1] == ' '))
                sb.append(" ")
        }
    }, root)

    return sb.toString()
}

private fun getQualifiedClassNames(classNames: MutableSet<String>): MutableSet<String> {
    classNames.remove("")
    if (classNames.isEmpty()) return classNames
    arrayOf("clearfix", "left", "right", "l", "r").forEach {
        classNames.remove(it)
        if (classNames.isEmpty()) {
            classNames.add(it)
            return@forEach
        }
    }
    return classNames
}

private fun Node.calculateViewPort(): Dimension {
    val default = AppConstants.DEFAULT_VIEW_PORT
    val ob = extension.ownerBody ?: return default
    val parts = ob.attr("view-port").split("x")
    return if (parts.size == 2) {
        Dimension(parts[0].toIntOrNull() ?: default.width, parts[1].toIntOrNull() ?: default.height)
    } else default
}
