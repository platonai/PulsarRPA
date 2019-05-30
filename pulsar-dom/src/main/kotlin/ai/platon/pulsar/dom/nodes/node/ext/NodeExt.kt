package ai.platon.pulsar.dom.nodes.node.ext

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.geometric.str
import ai.platon.pulsar.common.geometric.str2
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.math.vectors.set
import ai.platon.pulsar.dom.features.FeatureEntry
import ai.platon.pulsar.dom.features.FeatureFormatter
import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.defined.*
import ai.platon.pulsar.dom.model.createImage
import ai.platon.pulsar.dom.model.createLink
import ai.platon.pulsar.dom.nodes.*
import ai.platon.pulsar.dom.select.ElementTraversor
import ai.platon.pulsar.dom.select.MathematicalSelector
import ai.platon.pulsar.dom.select.any
import org.apache.commons.lang3.StringUtils
import org.apache.commons.math3.linear.ArrayRealVector
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import kotlin.reflect.KProperty

class DoubleFeature(val name: Int) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): Double = thisRef.features[name]

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: Double) {
        thisRef.features[name] = value
    }
}

class IntFeature(val name: Int) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): Int = thisRef.features[name].toInt()

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: Int) {
        thisRef.features[name] = value.toDouble()
    }
}

class MapField<T>(val initializer: (Node) -> T) {
    operator fun getValue(thisRef: Node, property: KProperty<*>): T =
            thisRef.variables[property.name] as? T ?: setValue(thisRef, property, initializer(thisRef))

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: T): T {
        thisRef.variables[property.name] = value
        return value
    }
}

class NullableMapField<T> {
    operator fun getValue(thisRef: Node, property: KProperty<*>): T? =
            thisRef.variables[property.name] as T?

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: T?) {
        thisRef.variables[property.name] = value
    }
}

fun <T> field(initializer: (Node) -> T): MapField<T> {
    return MapField(initializer)
}

inline fun <reified T> nullableField(): NullableMapField<T> {
    return NullableMapField()
}

val nilNode = Element("div") as Node

val nilElement = Element("div")

val nilDocument = Document.createShell("")

val Node.isNil get() = this === nilNode

val Node.ownerDocument get() = ownerDocumentNode as Document

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

// The Uniform Resource Identifier of this document, it's simply the location of the document
val Node.uri: String get() = location

var Node.depth by IntFeature(DEP)

val Node.sequence by IntFeature(SEQ)

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
val Node.hasHiddenFlag: Boolean get() = hasAttr("_hidden")
/** Overflow hidden flag set by browser */
val Node.hasOverflowHiddenFlag: Boolean get() = hasAttr("_o_hidden")
/** Check if the node is visible or not */
val Node.isVisible: Boolean get() {
    return when {
        isImage -> !hasHiddenFlag && !hasOverflowHiddenFlag // TODO: why a visible image have a empty rectangle?
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

val Node.isAnchorImage: Boolean get() = isImage && this.parent().isAnchor

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

/**
 * If the text is numeric and have non-numeric surroundings
 * */
val Node.isNumericLike get() = isMediumText && StringUtil.isNumericLike(cleanText)

val Node.isMoneyLike get() = isShortText && StringUtil.isMoneyLike(cleanText)

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

val Node.textOrNull: String? by field {
    when {
        it.hasAttr("src") -> it.attr("abs:src")
        it.hasAttr("href") -> it.attr("abs:href")
        it is TextNode -> it.text().trim()
        it is Element -> it.text().trim()
        else -> null
    }
}

val Node.textOrEmpty by field { it.textOrNull?:"" }

val Node.textOrName by field { it.textOrNull?:it.name }

val Node.cleanTextOrNull: String? by field {
        when {
            it.hasAttr("src") -> it.attr("abs:src")
            it.hasAttr("href") -> it.attr("abs:href")
            it is TextNode -> StringUtil.stripNonPrintableChar(it.text())
            it is Element -> StringUtil.stripNonPrintableChar(it.text())
            else -> null
        }
    }

val Node.cleanTextOrName by field { it.cleanTextOrNull?:it.name }

val Node.cleanText by field { it.cleanTextOrNull?:"" }

val Node.slimHtml by field {
    when {
        it.isImage -> createImage(it as Element, keepMetadata = false, lazy = true).toString()
        it.isAnchor -> createLink(it as Element, keepMetadata = false, lazy = true).toString()
        it.isNumericLike || it.isMoneyLike -> "<em>${it.cleanText}</em>"
        it is TextNode -> String.format("<span>%s</span>", it.cleanText)
        it is Element -> String.format("<div>%s</div>", it.cleanText)
        else -> String.format("<b>%s</b>", it.name)
    }
}

val Node.key: String get() = "${baseUri()}#$sequence"

val Node.name: String
    get() {
        return when(this) {
            is Document -> ":root"
            is Element -> {
                val id = id()
                if (id.isNotEmpty()) {
                    return "#$id"
                }

                val cls = className()
                if (cls.isNotEmpty()) {
                    return "." + cls.replace("\\s+".toRegex(), ".")
                }

                nodeName()
            }
            is TextNode -> {
                val postfix = if (siblingSize() > 1) { "~" + siblingIndex() } else ""
                return bestElement.name + postfix
            }
            else -> nodeName()
        }
    }

val Node.canonicalName: String
    get() {
        when(this) {
            is Document -> {
                var baseUri = baseUri()
                if (baseUri.isEmpty()) {
                    baseUri = ownerBody.attr("baseUri")
                }
                return ":root@$baseUri"
            }
            is Element -> {
                var id = id().trim()
                if (!id.isEmpty()) {
                    id = "#$id"
                }

                var cls = ""
                if (id.isEmpty()) {
                    cls = className().trim()
                    if (!cls.isEmpty()) {
                        cls = "." + cls.replace("\\s+".toRegex(), ".")
                    }
                }

                return "${nodeName()}$id$cls"
            }
            is TextNode -> {
                val postfix = if (siblingSize() > 1) { "~" + siblingIndex() } else ""
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
val Node.bestElement: Element
    get() {
        return if (this is Element) {
            this
        } else this.parentElement
    }

/**
 * The caption of an Element is a joined text values of all non-blank text nodes
 * */
val Node.caption: String
    get() = getCaptionWords().joinToString(";")

fun Node.getFeature(key: Int): Double {
    return features[key]
}

fun Node.getFeature(name: String): Double {
    return features[NodeFeature.getKey(name)]
}

fun Node.getFeatureEntry(key: Int): FeatureEntry {
    return FeatureEntry(key, getFeature(key))
}

fun Node.setFeature(key: Int, value: Double) {
    features[key] = value
}

fun Node.setFeature(key: Int, value: Int) {
    features[key] = value.toDouble()
}

fun Node.removeFeature(key: Int): Node {
    features[key] = 0.0
    return this
}

fun Node.clearFeatures(): Node {
    features = ArrayRealVector()
    return this
}

/**
 * Temporary node variables
 * */
inline fun <reified T> Node.getVariable(name: String): T? {
    val v = variables[name]
    return if (v is T) v else null
}

inline fun <reified T> Node.getVariable(name: String, defaultValue: T): T {
    val v = variables[name]
    return if (v is T) v else defaultValue
}

inline fun <reified T> Node.computeVariableIfAbsent(name: String, mappingFunction: (String) -> T): T {
    var v = variables[name]
    if (v !is T) {
        v = mappingFunction(name)
        variables[name] = v
    }
    return v
}

fun Node.setVariable(name: String, value: Any) {
    variables[name] = value
}

fun Node.setVariableIfNotNull(name: String, value: Any?) {
    if (value != null) {
        variables[name] = value
    }
}

fun Node.hasVariable(name: String): Boolean {
    return variables.containsKey(name)
}

fun Node.removeVariable(name: String): Any? {
    return variables.remove(name)
}

fun Node.anyAttr(attributeKey: String, attributeValue: Any): Node {
    this.attr(attributeKey, attributeValue.toString())
    return this
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
    return tuples.computeIfAbsent(tupleName) { mutableListOf() }.add(item)
}

/**
 *
 * */
fun Node.removeTupleItem(tupleName: String, item: Any): Boolean {
    return tuples[tupleName]?.remove(item)?:return false
}

fun Node.getTuple(tupleName: String): List<Any> {
    return tuples[tupleName]?:return listOf()
}

fun Node.hasTupleItem(tupleName: String, item: String): Boolean {
    return tuples[tupleName]?.contains(item)?:return false
}

fun Node.hasTuple(tupleName: String): Boolean {
    return tuples.containsKey(tupleName)
}

fun Node.clearTuple(tupleName: String) {
    tuples[tupleName]?.clear()
}

fun Node.removeTuple(tupleName: String) {
    tuples.remove(tupleName)
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
    addTupleItem(A_CAPTION, ai.platon.pulsar.common.StringUtil.stripNonCJKChar(word))
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

fun Node.formatEachFeatures(vararg featureKeys: Int): String {
    val sb = StringBuilder()
    NodeTraversor.traverse({ node, _ ->
        FeatureFormatter.format(node.features, featureKeys = *featureKeys, sb = sb)
        sb.append('\n')
    }, this)
    return sb.toString()
}

fun Node.formatFeatures(vararg featureKeys: Int): String {
    return FeatureFormatter.format(features, featureKeys = *featureKeys).toString()
}

fun Node.formatVariables(): String {
    val sb = StringBuilder()

    NodeTraversor.traverse({ node, _ ->
        FeatureFormatter.format(node.variables, sb)
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

// TODO: override Node.isNil or not?
val Element.isNil get() = this === nilElement

fun Element.addClasses(vararg classNames: String): Element {
    classNames.forEach { addClass(it) }
    return this
}

fun Element.anyAttr(attributeKey: String, attributeValue: Any): Element {
    this.attr(attributeKey, attributeValue.toString())
    return this
}

/**
 * TODO: experimental
 * TODO: may not as efficient as Node.collectIfTo since very call of e.nextElementSibling() generate a new element list
 * */
inline fun <C : MutableCollection<Element>> Element.collectIfTo(destination: C, crossinline filter: (Element) -> Boolean): C {
    ElementTraversor.traverse(this) { if (filter(it)) { destination.add(it) } }
    return destination
}

fun Element.parseStyle(): Array<String> {
    return StringUtil.stripNonChar(attr("style"), ":;")
            .split(";".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
}

fun Element.getStyle(styleKey: String): String {
    return getStyle(parseStyle(), styleKey)
}

val Document.isNil get() = this === nilDocument
