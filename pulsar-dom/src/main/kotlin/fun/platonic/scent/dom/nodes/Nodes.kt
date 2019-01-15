package `fun`.platonic.scent.dom.nodes

import `fun`.platonic.pulsar.common.AlignType
import `fun`.platonic.pulsar.common.SParser
import `fun`.platonic.pulsar.common.StringUtil
import `fun`.platonic.pulsar.common.testAlignment
import `fun`.platonic.scent.dom.*
import `fun`.platonic.scent.dom.data.BrowserControl
import `fun`.platonic.scent.dom.model.createImage
import `fun`.platonic.scent.dom.model.createLink
import `fun`.platonic.scent.dom.nodes.DocumentSettings.Companion.PAGE_PRIMARY_GRID_DIMENSION
import `fun`.platonic.scent.dom.nodes.DocumentSettings.Companion.PAGE_SECONDARY_GRID_DIMENSION
import `fun`.platonic.scent.dom.select.MathematicalSelector
import com.ibm.icu.lang.UCharacter.JoiningGroup.TAH
import com.ibm.icu.lang.UCharacter.JoiningGroup.TAW
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.hadoop.net.DNS
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.util.regex.Pattern

/**
 * Attribute names
 * */
const val A_DEFINED_SYMBOL_PATH = "defined-symbol-path"
const val A_DEFINED_STATISTIC_PATH = "defined-statistic-path"

const val A_EXTRACTOR = "s-extractor"
const val A_CORPUS_SIZE = "s-corpus-size"
const val A_UNIT_AREA = "s-unit-area"
const val A_CORPUS_TIME = "s-corpus-time"
const val A_LABELS = "s-labels"
const val A_ML_LABELS = "s-ml-labels"
const val A_CLUES = "s-clues"
const val A_FEATURES = "s-features"
const val A_NAMED_FEATURES = "s-named-features"
const val A_CAPTION = "s-caption"
const val A_CAPTION_REF = "s-caption-ref"
const val A_COLOR = "s-color"
const val A_COMPONENT_TYPE = "s-component-type"
const val A_COMPONENT = "s-component"
const val A_WEAK_ROW_NUMBER = "s-weak-row-number"
const val A_MANUAL_LABEL = "s-man-label"

/**
 * Variable names
 * */
const val V_ANNOTATED_DOCUMENT_EXPORT_PATH = "ANNOTATED_DOCUMENT_EXPORT_PATH"
const val V_OWNER_DOCUMENT = "OWNER_DOCUMENT"
const val V_OWNER_BODY = "V_OWNER_BODY"
const val V_VIEW_PORT = "VIEW_PORT"
const val V_GRID_PRIMARY_DIMENSION = "GRID_PRIMARY_DIMENSION"
const val V_GRID_SECONDARY_DIMENSION = "GRID_SECONDARY_DIMENSION"
const val V_GRID_ACTIVE_GRID = "GRID_ACTIVE_DIMENSION"

val BOX_SYNTAX_PATTERN_1 = Pattern.compile("(\\d+)[xX](\\d+)")
val BOX_SYNTAX_PATTERN = Pattern.compile("$BOX_SYNTAX_PATTERN_1(,$BOX_SYNTAX_PATTERN_1)?")

/**
 * Cases:
 * <ul>
 *   <li>div:in-box(200, 200, 300, 300)</li>
 *   <li>div:in-box(200, 200, 300, 300, 50)</li>
 *   <li>*:in-box(200, 200, 300, 300)</li>
 *   <li>*:in-box(200, 200, 300, 300, 50)</li>
 *   <li>div:in-box(200, 200, 300, 300),*:in-box(200, 200, 300, 300, 50)</li>
 * </ul>
 * */
val BOX_CSS_PATTERN_1 = Pattern.compile(".{1,5}:in-box\\(\\d+(,\\d+\\){1,4})")
val BOX_CSS_PATTERN = Pattern.compile("$BOX_CSS_PATTERN_1(,$BOX_CSS_PATTERN_1)?")

data class DOMRect(var left: Double = 0.0, var top: Double = 0.0, var width: Double = 0.0, var height: Double = 0.0)

val DOMRect.isEmpty get() = left == 0.0 && top == 0.0 && width == 0.0 && height == 0.0

fun parseDOMRect(rect: String): DOMRect {
    if (rect.isBlank()) {
        return DOMRect()
    }

    val a = rect.split(SPACE.toRegex()).dropLastWhile { it.isEmpty() }
    if (a.size != 4) {
        return DOMRect()
    }

    val left = NumberUtils.toDouble(a[0])
    val top = NumberUtils.toDouble(a[1])
    val width = NumberUtils.toDouble(a[2])
    val height = NumberUtils.toDouble(a[3])

    return DOMRect(left, top, width, height)
}

enum class ComponentType {
    CONNECTED, MASSED, MAIN_IMAGE, BIG_IMAGE, BLACK_HOLE, CONSTANT_CAPTION, UNIQUE_PATH, DATA_TAG, MANUAL, UNKNOWN;

    companion object {
        fun parse(str: String, defaultValue: ComponentType? = ComponentType.UNKNOWN): ComponentType? {
            val upperCaseStr = str.toUpperCase()
            return when (upperCaseStr) {
                "CONNECTED" -> ComponentType.CONNECTED
                "MASSED" -> ComponentType.MASSED
                "BLACK_HOLE" -> ComponentType.BLACK_HOLE
                "CONSTANT_CAPTION" -> ComponentType.CONSTANT_CAPTION
                "UNIQUE_PATH" -> ComponentType.UNIQUE_PATH
                "MANUAL" -> ComponentType.MANUAL
                "UNKNOWN" -> ComponentType.UNKNOWN
                else -> defaultValue
            }
        }
    }
}

// use the orange color family. See http://www.ip138.com/yanse/common.htm
val componentColors = mapOf(
        ComponentType.CONNECTED to Color(0xFFCC99),
        ComponentType.MASSED to Color(0x996600),
        ComponentType.BLACK_HOLE to Color(0x000000),
        ComponentType.CONSTANT_CAPTION to Color(0xFF9933),
        ComponentType.MANUAL to Color(0xCC6600),
        ComponentType.UNIQUE_PATH to Color(0x990033),
        ComponentType.UNKNOWN to Color(0x999999)
)

val documentComparator = kotlin.Comparator { d1: Document, d2 -> d1.baseUri().compareTo(d2.baseUri()) }

val descriptiveDocumentComparator = kotlin.Comparator {
    d1: DescriptiveDocument, d2 -> d1.baseUri.compareTo(d2.baseUri) }

val nodeComparator = kotlin.Comparator { n1: Node, n2 ->
    val r = documentComparator.compare(n1.ownerDocument(), n2.ownerDocument())
    if (r == 0) n1.sequence - n2.sequence else r
}

val nodePositionComparator = kotlin.Comparator { n1: Node, n2 ->
    var r = documentComparator.compare(n1.ownerDocument(), n2.ownerDocument())
    if (r == 0) r = n1.top - n2.top
    if (r == 0) r = n1.left - n2.left
    r
}

class TraverseState(val match: Boolean = false, val state: NodeFilter.FilterResult = CONTINUE) {
    companion object {
        /** Continue processing the tree  */
        val CONTINUE = NodeFilter.FilterResult.CONTINUE
        /** Skip the child nodes, but do call [NodeFilter.tail] next.  */
        val SKIP_CHILDREN = NodeFilter.FilterResult.SKIP_CHILDREN
        /** Skip the subtree, and do not call [NodeFilter.tail].  */
        val SKIP_ENTIRELY = NodeFilter.FilterResult.SKIP_ENTIRELY
        /** Remove the node and its children  */
        val REMOVE = NodeFilter.FilterResult.REMOVE
        /** Stop processing  */
        val STOP = NodeFilter.FilterResult.STOP

        val TRUE_CONTINUE = TraverseState(true, CONTINUE)
        val TRUE_SKIP_CHILDREN = TraverseState(true, SKIP_CHILDREN)
        val TRUE_SKIP_ENTIRELY = TraverseState(true, SKIP_ENTIRELY)
        val TRUE_REMOVE = TraverseState(true, REMOVE)
        val TRUE_STOP = TraverseState(true, STOP)

        val FALSE_CONTINUE = TraverseState(false, CONTINUE)
        val FALSE_SKIP_CHILDREN = TraverseState(false, SKIP_CHILDREN)
        val FALSE_SKIP_ENTIRELY = TraverseState(false, SKIP_ENTIRELY)
        val FALSE_REMOVE = TraverseState(false, REMOVE)
        val FALSE_STOP = TraverseState(false, STOP)
    }
}

/**
 * Get the document owns the node
 * */
val Node.ownerDocument: Document get() {
    return computeVariableIfAbsent<Document>(V_OWNER_DOCUMENT, ownerDocument())
}

val Node.ownerBody: Element get() {
    return computeVariableIfAbsent<Element>(V_OWNER_BODY, ownerDocument.body())
}

val Node.viewPort: Dimension get() {
    return ownerDocument.computeVariableIfAbsent(V_VIEW_PORT, ownerDocument.calculateViewPort())
}

// basic info
val Node.uri: String get() = baseUri()

val Node.sequence: Int get() = getFeature(SEQ).toInt()

var Node.depth: Int
    get() = getFeature(DEP).toInt()
    set(value) = setFeature(DEP, value.toDouble())

/*********************************************************************
 * Boolean characters
 * *******************************************************************/

/** Hidden flag set by browser */
val Node.hasHiddenFlag: Boolean get() = hasAttr("_hidden")
/** Check if the node is visible or not */
val Node.isVisible: Boolean get() = x >= 0 && y >= 0 && !rectangle.isEmpty && !hasHiddenFlag
/** Check if the node is visible or not */
val Node.isHidden: Boolean get() = !this.isVisible

val Node.isText: Boolean get() = this is TextNode

val Node.isBlankText: Boolean get() { return this is TextNode && this.isBlank }

val Node.isNonBlankText: Boolean get() { return this is TextNode && !this.isBlank }

val Node.isImage: Boolean get() = this.nodeName() == "img"

val Node.isRegularImage: Boolean get() { return isVisible && isImage && hasAttr("src") }

val Node.isAnchorImage: Boolean get() = this.isImage && this.parent().isAnchor

val Node.isAnchor: Boolean get() = this.nodeName() == "a"

val Node.isImageAnchor: Boolean get() = this.isAnchor && this.numImages == 1

/**
 * Regular nodes play core roles in DOM analysis, eg, DOM node clustering
 *
 * A regular node is visible, and can be one of the following:
 * 1. a non-blank text
 * 2. an image
 *
 * Other type of nodes might be added to the list if there are good reasons
 * */
val Node.isRegular: Boolean get() { return isVisible && (isNonBlankText || isImage) }

/**
 * List like node
 * */
val Node.isListLike: Boolean get() {
    return nodeName() in arrayOf("table", "tbody", "ul")
}

/*********************************************************************
 * Geometric information
 * *******************************************************************/

// geometric grid, we have two grids, a bigger one and a smaller one

var Node.primaryGrid: Dimension
    get() = ownerDocument.getVariable(V_GRID_PRIMARY_DIMENSION)?: PAGE_PRIMARY_GRID_DIMENSION
    set(value) {
        ownerDocument.setVariable(V_GRID_PRIMARY_DIMENSION, value)
    }

var Node.secondaryGrid: Dimension
    get() = ownerDocument.getVariable(V_GRID_SECONDARY_DIMENSION)?: PAGE_SECONDARY_GRID_DIMENSION
    set(value) {
        ownerDocument.setVariable(V_GRID_SECONDARY_DIMENSION, value)
    }

var Node.grid: Dimension
    get() = ownerDocument.getVariable(V_GRID_ACTIVE_GRID)?: primaryGrid
    set(value) {
        ownerDocument.setVariable(V_GRID_ACTIVE_GRID, value)
    }

/**
 * The corpus size last calculated
 * */
var Node.corpusSize: Int
    get() = ownerDocument.getVariable(A_CORPUS_SIZE, 0)
    set(value) {
        ownerDocument.setVariable(A_CORPUS_SIZE, value)
    }

var Node.unitArea: Int
    get() = ownerDocument.getVariable(A_UNIT_AREA, 0)
    set(value) {
        ownerDocument.setVariable(A_UNIT_AREA, value)
    }

var Node.left
    get() = getFeature(LEFT).toInt()
    set(value) = setFeature(LEFT, value.toDouble())

var Node.top
    get() = getFeature(TOP).toInt()
    set(value) = setFeature(TOP, value.toDouble())

var Node.width: Int
    get() = getFeature(WIDTH).toInt()
    set(value) = setFeature(WIDTH, value.toDouble())

var Node.height: Int
    get() = getFeature(HEIGHT).toInt()
    set(value) = setFeature(HEIGHT, value.toDouble())

val Node.right: Int
    get() = left + width

val Node.bottom: Int
    get() = top + height

val Node.alignedLeft
    get() = alignX(left)

val Node.alignedRight
    get() = alignX(right)

val Node.alignedTop
    get() = alignY(top)

val Node.alignedBottom
    get() = alignY(bottom)

val Node.x
    get() = left

val Node.y
    get() = top

val Node.x2
    get() = right

val Node.y2
    get() = bottom

val Node.centerX
    get() = (x + x2) / 2

val Node.centerY
    get() = (y + y2) / 2

val Node.alignedX
    get() = alignX(x)

val Node.alignedY
    get() = alignY(y)

val Node.alignedCenterX
    get() = alignY((x + x2) / 2.0f)

val Node.alignedCenterY
    get() = alignY((y + y2) / 2.0f)

val Node.alignedX2
    get() = alignX(x2)

val Node.alignedY2
    get() = alignY(y2)

val Node.location: Point
    get() = Point(x, y)

val Node.dimension: Dimension
    get() = Dimension(width, height)

val Node.rectangle: Rectangle
    get() = Rectangle(x, y, width, height)

val Node.area: Int
    get() = width * height

/*********************************************************************
 * Distinguished features
 * *******************************************************************/
/**
 * The number of element siblings
 * features\[SIB] = features\[C]
 * TODO: keep SIB feature be consistent with Node.siblingNodes.size
 * */
val Node.numSiblings
    get() = getFeature(SIB).toInt()
val Node.numChildren
    get() = getFeature(C).toInt()

/** Number of descend text nodes */
var Node.numTextNodes
    get() = getFeature(TN).toInt()
    set(value) = setFeature(TN, value.toDouble())

/** Number of descend images */
var Node.numImages
    get() = getFeature(IMG).toInt()
    set(value) = setFeature(IMG, value.toDouble())

/** Number of descend anchors */
var Node.numAnchors
    get() = getFeature(A).toInt()
    set(value) = setFeature(A, value.toDouble())

val Node.numRegularNodes get() = numTextNodes + numImages

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

val Node.textOrNull: String?
    get() = when {
        this.isImage -> this.attr("abs:src")
        this is TextNode -> this.text().trim()
        this is Element -> this.text().trim()
        else -> null
    }

val Node.textOrEmpty: String get() = textOrNull?:""

val Node.textOrName: String get() = textOrNull?:name

val Node.slimHtml: String
    get() = when {
        this.isImage -> createImage(this as Element, keepMetadata = false, lazy = true).toString()
        this.isAnchor -> createLink(this as Element, keepMetadata = false, lazy = true).toString()
        this is TextNode -> String.format("<span>%s</span>", this.text())
        this is Element -> String.format("<div>%s</div>", this.text())
        else -> String.format("<b>%s</b>", name)
    }

/**
 * The caption of an Element is a joined text values of all non-blank text nodes
 * */
val Node.caption: String
    get() = getCaptionWords().joinToString(";")

var Node.componentType: ComponentType?
    get() = getVariable(A_COMPONENT_TYPE)
    set(value) {
        if (value is ComponentType) {
            setVariable(A_COMPONENT_TYPE, value)
        }
    }

val Node.isComponent get() = hasVariable(A_COMPONENT_TYPE)

var Node.color: Color
    get() = getVariable(A_COLOR)?:Color.WHITE
    set(value) {
        setVariable(A_COLOR, value)
    }

val Node.isColored: Boolean get() = hasVariable(A_COLOR)

fun Node.getFeature(key: Int): Double {
    return features[key]
}

fun Node.getFeature(name: String): Double {
    return features[getKey(name)]
}

fun Node.getFeatureEntry(key: Int): Feature {
    return Feature(key, getFeature(key))
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
 * Temporary variables
 * */
inline fun <reified T> Node.getVariable(name: String): T? {
    val v = variables[name]
    return if (v is T) v else null
}

inline fun <reified T> Node.getVariable(name: String, defaultValue: T): T {
    val v = variables[name]
    return if (v is T) v else defaultValue
}

inline fun <reified T> Node.computeVariableIfAbsent(name: String, defaultValue: T): T {
    var v = variables[name]
    if (v !is T) {
        variables[name] = defaultValue
        v = defaultValue
    }
    return v
}

fun Node.setVariable(name: String, value: Any) {
    variables[name] = value
}

fun Node.hasVariable(name: String): Boolean {
    return variables.containsKey(name)
}

fun Node.removeVariable(name: String): Any? {
    return variables.remove(name)
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
    addTupleItem(A_CAPTION, StringUtil.stripNonCJKChar(word))
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

fun Node.alignX(x: Float): Int {
    return Math.round(x / grid.width) * grid.width
}

fun Node.alignX(x: Int): Int {
    return alignX(x.toFloat())
}

fun Node.alignY(y: Float): Int {
    return Math.round(y / grid.height) * grid.height
}

fun Node.alignY(y: Int): Int {
    return alignY(y.toFloat())
}

fun Node.testAlignment(other: Node): AlignType {
    return rectangle.testAlignment(other.rectangle)
}

fun Node.removeAttrs(vararg attributeKeys: String) {
    attributeKeys.forEach { this.removeAttr(it) }
}

fun Node.formatEachFeatures(vararg featureKeys: Int): String {
    val sb = StringBuilder()
    NodeTraversor.traverse({node, _ ->
        format(node.features, featureKeys = *featureKeys, sb = sb)
        sb.append('\n')
    }, this)
    return sb.toString()
}

fun Node.formatFeatures(vararg featureKeys: Int): String {
    return format(features, featureKeys = *featureKeys).toString()
}

fun Node.formatVariables(): String {
    val sb = StringBuilder()

    NodeTraversor.traverse({node, _ ->
        format(node.variables, sb)
        sb.append('\n')
    }, this)

    return sb.toString()
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
        return when(this) {
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

/**
 * Returns a best element to represent this node: if the node itself is an element, return itself
 * otherwise, returns it's parent
 * */
val Node.bestElement: Element get() = (this as? Element ?: this.parent() as Element)

/*********************************************************************
 * Actions
 * *******************************************************************/

fun Node.forEachAncestor(action: (Element) -> Unit) {
    var p = this.parent()
    while (p != null) {
        action(p as Element)
        p = p.parent()
    }
}

fun Node.forEachAncestorIf(filter: (Element) -> Boolean, action: (Element) -> Unit) {
    var p = this.parent() as Element?

    while (p != null && filter(p)) {
        action(p)
        p = p.parent()
    }
}

/**
 * Find first ancestor matches the predication
 * */
fun Node.findFirstAncestor(predicate: (Element) -> Boolean): Element? {
    var p = this.parent() as Element?
    var match = false

    while (p != null && !match) {
        match = predicate(p)
        if (!match) {
            p = p.parent()
        }
    }

    return if (match) p else null
}

fun Node.hasAncestor(predicate: (Element) -> Boolean): Boolean {
    return findFirstAncestor(predicate) != null
}

fun Node.isAncestorOf(other: Node): Boolean {
    return other.findFirstAncestor { it == this} != null
}

/**
 * For each posterity
 * */
fun Node.forEach(includeRoot: Boolean = false, action: (Node) -> Unit) {
    NodeTraversor.traverse({node, _-> if (includeRoot || node != this) { action(node) } }, this)
}

fun Node.forEachElement(includeRoot: Boolean = false, action: (Element) -> Unit) {
    NodeTraversor.traverse({node, _->
        if ((includeRoot || node != this) && node is Element) { action(node) }
    }, this)
}

/**
 * Find posterity matches the condition
 * */
fun Node.find(predicate: (Node) -> Boolean): List<Node> {
    return collectIf(predicate)
}

fun Node.findFirst(predicate: (Node) -> Boolean): Node? {
    val root = this
    var result: Node? = null
    NodeTraversor.filter(object: NodeFilter {
        override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
            return if (predicate(node)) {
                result = node
                NodeFilter.FilterResult.STOP
            }
            else NodeFilter.FilterResult.CONTINUE
        }
    }, root)

    return result
}

inline fun Node.collectIf(crossinline filter: (Node) -> Boolean): List<Node> {
    return collectIfTo(mutableListOf(), filter)
}

inline fun <C : MutableCollection<Node>> Node.collectIfTo(destination: C, crossinline filter: (Node) -> Boolean): C {
    NodeTraversor.traverse({node, _-> if (filter(node)) { destination.add(node) } }, this)
    return destination
}

inline fun <O: Node> Node.collect(crossinline transform: (Node) -> O?): List<O> {
    return collectTo(mutableListOf(), transform)
}

inline fun <O: Node, C : MutableCollection<O>> Node.collectTo(destination: C, crossinline transform: (Node) -> O?): C {
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

fun Node.isAncestor(node: Node): Boolean {
    var p = this.parent()
    while (p != null && p.depth < node.depth && p != node) {
        p = p.parent()
    }
    return p == node
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

fun Node.findBySequence(seq: Int): Node? {
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

fun Node.accumulate(featureKey: Int, includeRoot: Boolean = true): Double {
    return accumulate(featureKey, includeRoot) { true }
}

fun Node.accumulate(featureKey: Int, includeRoot: Boolean = true, filter: (Node) -> Boolean): Double {
    var sum = 0.0
    forEach(includeRoot = includeRoot) {
        if (filter(it)) {
            sum += it.features[featureKey]
        }
    }
    return sum
}

fun Node.minmax(featureKey: Int): Pair<Double, Double> {
    var min = Double.MAX_VALUE
    var max = Double.MIN_VALUE
    forEach {
        val v = it.features[featureKey]
        if (v > max) {
            max = v
        }
        if (v < min) {
            min = v
        }
    }
    return min to max
}

fun Node.minByDouble(transform: (Node) -> Double): Node? {
    var min = Double.MAX_VALUE
    var node: Node? = null
    forEach {
        val v = transform(it)
        if (v < min) {
            min = v
            node = it
        }
    }
    return node
}

fun Node.maxByDouble(transform: (Node) -> Double): Node? {
    var max = Double.MIN_VALUE
    var node: Node? = null
    forEach {
        val v = transform(it)
        if (v > max) {
            max = v
            node = it
        }
    }
    return node
}

fun Node.countMatches(predicate: (Node) -> Boolean = {true}): Int {
    var count = 0
    forEach { if (predicate(it)) ++count }
    return count
}

@JvmOverloads
fun Element.select2(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    if (offset == 1 && limit == Int.MAX_VALUE) {
        return MathematicalSelector.select(cssQuery, this)
    }

    // TODO: do the filtering inside [MathematicalSelector#select]
    var i = 1
    return MathematicalSelector.select(cssQuery, this)
            .takeWhile { i++ >= offset && i <= limit }
            .toCollection(Elements())
}

@JvmOverloads
fun Elements.select2(cssQuery: String, offset: Int = 1, limit: Int = Int.MAX_VALUE): Elements {
    if (offset == 1 && limit == Int.MAX_VALUE) {
        return MathematicalSelector.select(cssQuery, this)
    }

    var i = 1
    return MathematicalSelector.select(cssQuery, this)
            .takeWhile { i++ >= offset && i <= limit }
            .toCollection(Elements())
}

@Deprecated("Use first instead", ReplaceWith("MathematicalSelector.selectFirst(cssQuery, this)"))
fun Element.selectFirst2(cssQuery: String): Element? {
    return MathematicalSelector.selectFirst(cssQuery, this)
}

fun <O> Element.select(query: String, offset: Int = 1, limit: Int = Int.MAX_VALUE,
                       transformer: (Element) -> O): List<O> {
    return select2(query, offset, limit).map { transformer(it) }
}

fun Element.first(cssQuery: String): Element? {
    return MathematicalSelector.selectFirst(cssQuery, this)
}

fun <O> Element.first(cssQuery: String, transformer: (Element) -> O): O? {
    return first(cssQuery)?.let { transformer(it) }
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

fun Element.pixelatedValue(value: String, defaultValue: Double): Double {
    // TODO : we currently handle only px
    val units = arrayOf("in", "%", "cm", "mm", "ex", "pt", "pc", "px")
    return NumberUtils.toDouble(StringUtils.removeEnd(value, "px"), defaultValue)
}

fun getStyle(styles: Array<String>, styleKey: String): String {
    val styleValue = ""

    val search = "$styleKey:"
    for (style in styles) {
        if (style.startsWith(search)) {
            return style.substring(search.length)
        }
    }

    return styleValue
}

fun convertBox(box: String): String {
    val box2 = box.replace(" ", "")
    val matcher = BOX_SYNTAX_PATTERN.matcher(box2)
    if (matcher.find()) {
        return box2.split(",")
                .map { it.split('x', 'X') }
                .map { "*:in-box(${it[0]}, ${it[1]})" }
                .joinToString()
    } else if (BOX_CSS_PATTERN.matcher(box2).matches()) {
        return box2
    }

    // Bad syntax, no element should find
    return "non:in-box(0, 0, 0, 0)"
}

private fun Node.calculateViewPort(): Dimension {
    val viewPort = BrowserControl.viewPort
    val parts = ownerBody.attr("view-port").split("x".toRegex())
    if (parts.size != 2) return viewPort

    val w = SParser(parts[0]).getInt(viewPort.width)
    val h = SParser(parts[1]).getInt(viewPort.height)
    return Dimension(w, h)
}
