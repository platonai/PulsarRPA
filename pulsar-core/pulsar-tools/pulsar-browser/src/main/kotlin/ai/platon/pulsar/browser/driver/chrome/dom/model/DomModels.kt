package ai.platon.pulsar.browser.driver.chrome.dom.model

import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.FBNLocator
import ai.platon.pulsar.browser.driver.chrome.dom.LocatorMap
import ai.platon.pulsar.browser.driver.chrome.dom.model.MicroDOMTreeNodeHelper.Companion.estimatedSize
import ai.platon.pulsar.browser.driver.chrome.dom.util.CSSSelectorUtils
import ai.platon.pulsar.common.math.roundTo
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import java.awt.Dimension
import java.math.RoundingMode
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * DOM node types based on the DOM specification.
 */
enum class NodeType(val value: Int) {
    ELEMENT_NODE(1),
    ATTRIBUTE_NODE(2),
    TEXT_NODE(3),
    CDATA_SECTION_NODE(4),
    ENTITY_REFERENCE_NODE(5),
    ENTITY_NODE(6),
    PROCESSING_INSTRUCTION_NODE(7),
    COMMENT_NODE(8),
    DOCUMENT_NODE(9),
    DOCUMENT_TYPE_NODE(10),
    DOCUMENT_FRAGMENT_NODE(11),
    NOTATION_NODE(12);

    companion object {
        fun fromValue(value: Int): NodeType =
            entries.find { it.value == value } ?: ELEMENT_NODE
    }
}

/**
 * Static attributes used for element hashing.
 */
object StaticAttributes {
    val ATTRIBUTES = setOf(
        "class", "id", "name", "type", "placeholder", "aria-label", "title",
        "role", "data-testid", "data-test", "data-cy", "data-selenium",
        "for", "required", "disabled", "readonly", "checked", "selected",
        "multiple", "href", "target", "rel", "aria-describedby",
        "aria-labelledby", "aria-controls", "aria-owns", "aria-live",
        "aria-atomic", "aria-busy", "aria-disabled", "aria-hidden",
        "aria-pressed", "aria-checked", "aria-selected", "tabindex",
        "alt", "src", "lang", "itemscope", "itemtype", "itemprop",
        "pseudo", "aria-valuemin", "aria-valuemax", "aria-valuenow",
        "aria-placeholder"
    )
}

/**
 * Default attributes to include in LLM serialization.
 */
object DefaultIncludeAttributes {
    val ATTRIBUTES = listOf(
        "title", "type", "checked", "id", "name", "role", "value",
        "placeholder", "data-date-format", "alt", "aria-label",
        "aria-expanded", "data-state", "aria-checked", "aria-valuemin",
        "aria-valuemax", "aria-valuenow", "aria-placeholder", "pattern",
        "min", "max", "minlength", "maxlength", "step", "pseudo",
        "checked", "selected", "expanded", "pressed", "disabled",
        "invalid", "valuemin", "valuemax", "valuenow", "keyshortcuts",
        "haspopup", "multiselectable", "required", "valuetext", "level",
        "busy", "live", "ax_name"
    )
}

data class CompactRect(
    val x: Double? = null,
    val y: Double? = null,
    val width: Double? = null,
    val height: Double? = null
) {
    /**
     * Round every field to the nearest integer
     * */
    fun roundTo(decimals: Int = 1, mode: RoundingMode = RoundingMode.HALF_UP): CompactRect? {
        if (x == null && y == null && width == null && height == null) {
            return null
        }

        return CompactRect(
            x?.roundTo(decimals),
            y?.roundTo(decimals),
            width?.roundTo(decimals),
            height?.roundTo(decimals),
        )
    }

    fun round() = roundTo(1)
}

/**
 * DOM rectangle with coordinates.
 */
data class DOMRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    fun compact(): CompactRect? {
        if (x == 0.0 && y == 0.0 && width == 0.0 && height == 0.0) {
            return null
        }

        return CompactRect(
            x.takeIf { it != 0.0 }, y.takeIf { it != 0.0 },
            width.takeIf { it != 0.0 }, height.takeIf { it != 0.0 })
    }

    fun intersects(other: DOMRect): Boolean {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y
    }

    fun area(): Double = width * height

    companion object {
        /**
         * Create DOMRect from CDP's 8-element bounds array: [x1, y1, x2, y2, x3, y3, x4, y4]
         */
        fun fromBoundsArray(bounds: List<Double>): DOMRect? {
            if (bounds.size < 8) return null
            val x = bounds[0]
            val y = bounds[1]
            val width = bounds[2] - bounds[0]
            val height = bounds[5] - bounds[1]
            return DOMRect(x, y, width, height)
        }

        /**
         * Create DOMRect from CDP's 4-element rect array: [x, y, width, height]
         */
        fun fromRectArray(rect: List<Double>): DOMRect? {
            if (rect.size < 4) return null
            return DOMRect(rect[0], rect[1], rect[2], rect[3])
        }
    }
}

/**
 * Enhanced accessibility property.
 */
data class AXPropertyEx(
    val name: String,
    val value: Any? = null
)

/**
 * Enhanced accessibility node with essential AX tree information.
 */
data class AXNodeEx(
    val axNodeId: String,
    val ignored: Boolean = false,
    val role: String? = null,
    val name: String? = null,
    val description: String? = null,
    val properties: List<AXPropertyEx>? = null,
    val childIds: List<String>? = null,
    val backendNodeId: Int? = null,
    val frameId: String? = null
)

/**
 * Enhanced snapshot node data extracted from DOMSnapshot.
 */
data class SnapshotNodeEx constructor(
    val isClickable: Boolean? = null,
    val cursorStyle: String? = null,
    val bounds: DOMRect? = null,
    val clientRects: DOMRect? = null,
    val scrollRects: DOMRect? = null,
    val computedStyles: Map<String, String>? = null,
    val paintOrder: Int? = null,
    val stackingContexts: Int? = null,
    val absoluteBounds: DOMRect? = null
)

/**
 * Enhanced DOM tree node containing merged information from DOM, AX, and Snapshot trees.
 */
data class DOMTreeNodeEx constructor(
    // DOM Node data
    val nodeId: Int = 0,
    val backendNodeId: Int? = null,
    val nodeType: NodeType = NodeType.ELEMENT_NODE,
    val nodeName: String = "",
    val nodeValue: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val isScrollable: Boolean? = null,
    val isVisible: Boolean? = null,
    val absolutePosition: DOMRect? = null,

    // Frame information
    val targetId: String? = null,
    val frameId: String? = null,
    val sessionId: String? = null,

    // Tree structure
    val children: List<DOMTreeNodeEx> = emptyList(),
    val shadowRoots: List<DOMTreeNodeEx> = emptyList(),
    val contentDocument: DOMTreeNodeEx? = null,

    // Snapshot data
    val snapshotNode: SnapshotNodeEx? = null,

    // AX data
    val axNode: AXNodeEx? = null,

    // XPath and hash
    val xpath: String? = null,
    val elementHash: String? = null,
    val parentBranchHash: String? = null,

    // Visibility and interaction
    val isInteractable: Boolean? = null,
    val interactiveIndex: Int? = null
) {
    fun textContent(): String {
        val sb = StringBuilder()

        fun appendToken(s: String?) {
            val t = s?.trim()
            if (!t.isNullOrEmpty()) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(t)
            }
        }

        when (nodeType) {
            NodeType.TEXT_NODE -> appendToken(nodeValue)
            else -> {
                // Prefer accessible name if present
                appendToken(axNode?.name)
                // Include meaningful attributes
                if (attributes.isNotEmpty()) {
                    DefaultIncludeAttributes.ATTRIBUTES.forEach { key ->
                        attributes[key]?.let { appendToken(it) }
                    }
                }
            }
        }

        // Recurse into descendants
        children.forEach { appendToken(it.textContent()) }

        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Build a best-effort CSS selector for this node.
     * Strategy:
     * - If an id exists, prefer #id (or tag[id="..."] if id is not a valid identifier)
     * - Else, use up to a few stable classes: tag.class1.class2
     * - Else, fall back to stable attributes like data-*, aria-label, name, type, role
     * - Else, return the lowercase tag name (or "*")
     */
    fun cssSelector(): String = CSSSelectorUtils.generateCSSSelector(this)
}

typealias DOMTreeEx = DOMTreeNodeEx

/**
 * Simplified node for LLM serialization.
 */
data class TinyNode(
    val originalNode: DOMTreeNodeEx,
    val children: List<TinyNode> = emptyList(),
    val shouldDisplay: Boolean = true,
    val interactiveIndex: Int? = null,
    val isNew: Boolean = false,
    val ignoredByPaintOrder: Boolean = false,
    val excludedByParent: Boolean = false,
    val isShadowHost: Boolean = false,
    val isCompoundComponent: Boolean = false
)

typealias TinyTree = TinyNode

/**
 * DOM interacted element for agent interaction.
 *
 * @property elementHash A hash code calculated from the element
 * @property xpath The xpath of the node
 * @property bounds Bounds are in the page (document) absolute coordinate space.
 *      Origin is the document’s top‑left, not the viewport and not the element’s offset parent.
 *      Viewport coords = bounds - (window.scrollX, window.scrollY).
 *      For iframes, each document’s bounds are relative to its own document; accumulate frame offsets
 *      to get page/screen coords. clientRects/scrollRects here are treated in the same absolute
 *      document space as bounds.
 * @param isVisible If the element is visible
 * @property isInteractable If the element is interactive
 */
data class DOMInteractedElement(
    val elementHash: String,
    val xpath: String? = null,
    val bounds: DOMRect? = null,
    val isVisible: Boolean? = null,
    val isInteractable: Boolean? = null
)

/**
 * Cleaned original node without children_nodes and shadow_roots.
 * Enhanced with additional snapshot information for LLM consumption.
 * This prevents duplication since SimplifiedNode.children already contains them.
 */
data class CleanedDOMTreeNode constructor(
    /**
     * Locator format: `frameIndex,backendNodeId`
     * */
    val locator: String,
    val frameId: String?,
    val xpath: String?,
    val elementHash: String?,
    val nodeId: Int,
    val backendNodeId: Int?,

    val nodeType: Int,
    val nodeName: String,
    val nodeValue: String?,
    val attributes: Map<String, Any>?,
    val sessionId: String?,
    val isScrollable: Boolean?,   // null means false
    val isVisible: Boolean?,      // null means false
    val isInteractable: Boolean?, // null means false
    val interactiveIndex: Int?,

    val clientRects: CompactRect?,
    val scrollRects: CompactRect?,
    /** The absolute position bounding box. */
    val bounds: CompactRect?,
    val absoluteBounds: CompactRect? = null,
    /** A 1-based viewport index */
    val viewportIndex: Int? = null,

    val paintOrder: Int? = null,
    val stackingContexts: Int? = null,
    val contentDocument: CleanedDOMTreeNode?
    // Note: children_nodes and shadow_roots are intentionally omitted
)

data class InteractiveDOMTreeNode(
    /**
     * Locator format: `frameIndex,backendNodeId`
     * */
    val locator: String? = null,
    val slimHTML: String? = null,
    val textBefore: String? = null,
    val scrollable: Boolean? = null,   // null means false
    val invisible: Boolean? = null,    // null means false
    val viewportIndex: Int? = null,
    val clientRects: CompactRect? = null,
    @JsonIgnore
    val bounds: CompactRect? = null,
    @JsonIgnore
    val scrollRects: CompactRect? = null,
    @JsonIgnore
    val absoluteBounds: CompactRect? = null,
    @JsonIgnore
    val interactiveIndex: Int = 0,
    @JsonIgnore
    val prevInteractiveIndex: Int? = null,
    @JsonIgnore
    val nextInteractiveIndex: Int? = null,
) {
    fun isAnchor(): Boolean {
        return slimHTML?.startsWith("<a") == true
    }

    /**
     * String format:
     * ```
     * [locator]{viewport}(x,y,width,height)<slimNode>textContent</slimNode>Text-Before-This-Interactive-Element-And-After-Previous-Interactive-Element
     * ```
     * */
    override fun toString(): String {
        val b = bounds?.roundTo(0) ?: CompactRect()
        val bs = listOf(b.x, b.y, b.width, b.height)
            .map { it?.toInt() ?: 0 }
            .joinToString(",") { it.toString() }

        return buildString {
            append("[")
            append(locator)
            append("]")
            append("{")
            append(viewportIndex ?: 1)
            append("}")
            append("(")
            append(bs)
            append(")")
            append(slimHTML)
            append(textBefore)
        }
    }
}

class InteractiveDOMTreeNodeList(
    val nodes: List<InteractiveDOMTreeNode> = emptyList(),
) {
    @get:JsonIgnore
    val lazyJson by lazy { DOMSerializer.toJson(this) }

    @get:JsonIgnore
    val lazyString by lazy { toString() }

    fun estimatedSize() = nodes.sumOf { estimatedSize(it) }

    override fun toString(): String {
        return nodes.joinToString("\n")
    }
}

/**
 * Serializable DOMTreeNode structure.
 * Enhanced with compound component marking and paint order information.
 *
 * Naming conversion: mini -> tiny -> micro -> nano -> pico -> ...
 */
data class MicroDOMTreeNode(
    val shouldDisplay: Boolean? = null,
    val interactiveIndex: Int? = null,
    val ignoredByPaintOrder: Boolean? = null,
    val excludedByParent: Boolean? = null,
    val isCompoundComponent: Boolean? = null,
    val originalNode: CleanedDOMTreeNode? = null,
    val children: List<MicroDOMTreeNode>? = null,
    val shouldShowScrollInfo: Boolean? = null,
    val scrollInfoText: String? = null
) {
    private val nanoTreeCache = mutableMapOf<String, NanoDOMTree>()

    private val seenChunks = mutableListOf<Pair<Double, Double>>()

    fun hasSeen(startY: Double, endY: Double): Boolean {
        // check if the point has been seen
        val (s, e) = if (startY <= endY) startY to endY else endY to startY
        val eps = 1e-6
        if (s.isNaN() || e.isNaN()) return false
        return seenChunks.any { (ms, me) -> s >= ms - eps && e <= me + eps }
    }

    /**
     * The 1-based next chunk to see, each chunk is a viewport height.
     * */
    @Deprecated("Deprecated")
    fun nextChunkToSee(viewportHeight: Double): Int {
        if (seenChunks.isEmpty()) {
            return 1
        }

        return IntRange(1, 20).firstOrNull { i -> hasSeen(i * 1.0, i * 1.0 * viewportHeight) } ?: 1
    }

    fun slimHTML(): String? = MicroDOMTreeNodeHelper.slimHTML(this)

    fun toInteractiveDOMTreeNodeList(currentViewportIndex: Int, maxViewportIndex: Int): InteractiveDOMTreeNodeList =
        MicroDOMTreeNodeHelper(this, seenChunks, currentViewportIndex, maxViewportIndex).toInteractiveDOMTreeNodeList()

    fun toNanoTree(): NanoDOMTree = toNanoTreeInRange(0.0, 1000000.0)

    /**
     * Rendering data corresponding to a specific viewport slice of the page.
     *
     * @param viewportIndex 1-based viewport index to collect nodes for (must be >= 1).
     * @param viewportHeight The viewport height in CSS pixels (must be > 0).
     * @param scale How much extra height to include above and below the viewport. 1.0 = exact viewport, 1.2 = 20% margin.
     */
    fun toNanoTreeInViewport(viewportHeight: Int, viewportIndex: Int = 1, scale: Double = 1.0): NanoDOMTree {
        val helper = MicroToNanoTreeHelper(seenChunks)
        return helper.toNanoTreeInViewport0(this, viewportHeight, viewportIndex, scale)
    }

    fun toNanoTreeInRange(startY: Double = 0.0, endY: Double = 100000.0): NanoDOMTree {
        val helper = MicroToNanoTreeHelper(seenChunks)
//        val key = "$startY$endY"
//        return nanoTreeCache.computeIfAbsent(key) { helper.toNanoTreeInRange0(this, startY, endY) }
        return helper.toNanoTreeInRange0(this, startY, endY)
    }
}

typealias MicroDOMTree = MicroDOMTreeNode

/**
 * Serializable DOMTreeNode structure.
 * Enhanced with compound component marking and paint order information.
 *
 * Naming conversion: mini -> tiny -> micro -> nano -> pico -> ...
 */
data class NanoDOMTreeNode(
    /**
     * Locator format: `frameIndex,backendNodeId`
     * */
    val locator: String? = null,
    val nodeName: String? = null,
    val nodeValue: String? = null,
    val attributes: Map<String, Any>? = null,
    val scrollable: Boolean? = null,   // null means false
    val interactive: Boolean? = null,  // null means false
    val invisible: Boolean? = null,    // null means false
    val scrollRects: CompactRect? = null,
    val children: List<NanoDOMTreeNode>? = null,

    @JsonIgnore
    val viewportIndex: Int? = null,    // The position of this DOM node falls within the nth viewport, 1-based
    @JsonIgnore
    val interactiveIndex: Int? = null,
    @JsonIgnore
    val clientRects: CompactRect? = null,
    @JsonIgnore
    val bounds: CompactRect? = null,
    @JsonIgnore
    val absoluteBounds: CompactRect? = null,
    @JsonIgnore
    val microTreeNode: MicroDOMTree? = null,
) {
    @get:JsonIgnore
    val lazyJson: String by lazy { DOMSerializer.toJson(this) }
}

typealias NanoDOMTree = NanoDOMTreeNode

data class DOMState(
    val microTree: MicroDOMTree,
    val interactiveNodes: List<MicroDOMTreeNode>,
    val frameIds: List<String>,
    val selectorMap: Map<String, DOMTreeNodeEx>,
    val locatorMap: LocatorMap,
) {
    @get:JsonIgnore
    val nanoTreeLazyJson: String get() = microTree.toNanoTreeInRange().lazyJson

    fun getAbsoluteFBNLocator(locator: String?): FBNLocator? {
        if (locator == null) return null

        val fbnLocator = FBNLocator.parseRelaxed(locator) ?: return null
        if (fbnLocator.isAbsolute) {
            return fbnLocator
        }

        require(StringUtils.isNumeric(fbnLocator.frameId))
        val index = fbnLocator.frameId.toIntOrNull() ?: return null
        val absoluteFrameId = frameIds.getOrNull(index) ?: return null

        return FBNLocator(absoluteFrameId, fbnLocator.backendNodeId)
    }
}

data class ClientInfo(
    // time zone: "Asia/Shanghai"
    val timeZone: String,
    // locale: "zh_CN"
    val locale: Locale,
    //
    val viewportWidth: Int,
    val viewportHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int
)

data class FullClientInfo(
    val timeZone: String,
    val locale: Locale,
    val userAgent: String? = null,
    val devicePixelRatio: Double? = null,
    val viewportWidth: Int? = null,
    val viewportHeight: Int? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val colorDepth: Int? = null,
    val hardwareConcurrency: Int? = null,
    val deviceMemoryGB: Double? = null,
    val onLine: Boolean? = null,
    val networkEffectiveType: String? = null,
    val saveData: Boolean? = null,
    val prefersDarkMode: Boolean? = null,
    val prefersReducedMotion: Boolean? = null,
    val isSecureContext: Boolean? = null,
    val crossOriginIsolated: Boolean? = null,
    val doNotTrack: String? = null,
    val webdriver: Boolean? = null,
    val historyLength: Int? = null,
    val visibilityState: String? = null,
)

data class ScrollState constructor(
    val x: Double,
    val y: Double,
    val viewport: Dimension,
    val totalHeight: Double,
    val scrollYRatio: Double,
) {
    val viewportsTotal get() = ceil(totalHeight / viewport.height).roundToInt()
}

/**
 * Tab state information for multi-tab browser context.
 */
data class TabState(
    val id: String,           // Tab ID, aligned with Browser.drivers key
    val driverId: Int? = null, // Driver ID for diagnostics
    val url: String,          // Current URL of the tab
    val title: String? = null, // Tab title
    val active: Boolean = false // Whether this is the active tab
)

data class BrowserState constructor(
    val url: String,
    val goBackUrl: String? = null,
    val goForwardUrl: String? = null,
    val clientInfo: ClientInfo,
    val scrollState: ScrollState,
    val tabs: List<TabState> = emptyList(),
    val activeTabId: String? = null
) {
    @get:JsonIgnore
    val lazyJson: String by lazy { DOMSerializer.toJson(this) }
}

data class BrowserUseState(
    val browserState: BrowserState,
    val domState: DOMState
) {
    /**
     * The 1-based next chunk to see, each chunk is a viewport height.
     * */
    @Deprecated("Deprecated")
    fun nextViewportToSee() = domState.microTree.nextChunkToSee(browserState.scrollState.totalHeight)
}
