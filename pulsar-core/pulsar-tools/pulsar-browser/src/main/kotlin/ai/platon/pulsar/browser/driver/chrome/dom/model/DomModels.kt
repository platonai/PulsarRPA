package ai.platon.pulsar.browser.driver.chrome.dom.model

import ai.platon.pulsar.browser.driver.chrome.dom.CSSSelectorUtils
import ai.platon.pulsar.browser.driver.chrome.dom.DOMSerializer
import ai.platon.pulsar.browser.driver.chrome.dom.FBNLocator
import ai.platon.pulsar.browser.driver.chrome.dom.LocatorMap
import ai.platon.pulsar.common.math.roundTo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
    fun roundTo(decimals: Int = 1, mode: RoundingMode = RoundingMode.HALF_UP): CompactRect {
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
    fun compact(): CompactRect {
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
 */
data class DOMInteractedElement(
    val elementHash: String,
    val xPath: String? = null,
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
    /** The absolute position bounding box. */
    val bounds: CompactRect?,

    val clientRects: CompactRect?,
    val scrollRects: CompactRect?,
    val absoluteBounds: CompactRect? = null,
    val paintOrder: Int? = null,
    val stackingContexts: Int? = null,
    val contentDocument: CleanedDOMTreeNode?
    // Note: children_nodes and shadow_roots are intentionally omitted
)

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
)

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
    val bounds: CompactRect? = null,
    val clientRects: CompactRect? = null,
    val scrollRects: CompactRect? = null,
    val absoluteBounds: CompactRect? = null,
    val children: List<NanoDOMTreeNode>? = null,
) {
    companion object {
        fun create(microTree: MicroDOMTreeNode, startY: Int = 0, endY: Int = 100000): NanoDOMTree {
            // Create the current node from the micro node
            val root = newNode(microTree) ?: return NanoDOMTree()

            // Recursively create child nano nodes, filter out empty placeholders
            val childNanoList = microTree.children
                ?.map { create(it, startY, endY) }
                ?.filter { child ->
                    // keep nodes that have any meaningful data (locator or nodeName or non-empty children)
                    !(child.locator == null && child.nodeName == null && (child.children == null || child.children.isEmpty()))
                }

            return if (childNanoList.isNullOrEmpty()) root else root.copy(children = childNanoList)
        }

        private fun newNode(n: MicroDOMTreeNode?): NanoDOMTree? {
            val o = n?.originalNode ?: return null

            // remove locator's prefix to reduce serialized size
            return NanoDOMTree(
                o.locator.substringAfterLast(":"),
                o.nodeName,
                o.nodeValue,
                o.attributes,
                scrollable = o.isScrollable,
                interactive = o.isInteractable,
                // All nodes are visible unless `invisible` == true explicitly.
                invisible = if (o.isVisible == true) null else true,
                bounds = o.bounds?.round(),
                clientRects = o.clientRects?.round(),
                scrollRects = o.scrollRects?.round(),
                absoluteBounds = o.absoluteBounds?.round(),
            )
        }
    }
}

typealias NanoDOMTree = NanoDOMTreeNode

data class DOMState(
    val microTree: MicroDOMTree,
    val interactiveNodes: List<MicroDOMTreeNode>,
    val frameIds: List<String>,
    val selectorMap: Map<String, DOMTreeNodeEx>,
    val locatorMap: LocatorMap
) {
    @get:JsonIgnore
    val microTreeLazyJson: String by lazy { DOMSerializer.toJson(microTree) }

    @get:JsonIgnore
    @Suppress("unused")
    val nanoTreeLazyJson: String by lazy {
        // convert micro tree to nano tree first, then serialize
        val nano = NanoDOMTreeNode.create(microTree)
        DOMSerializer.toJson(nano)
    }

    @get:JsonIgnore
    val interactiveNodesLazyJson: String by lazy { DOMSerializer.toJson(interactiveNodes) }

    fun getNanoTree(): NanoDOMTree {
        return NanoDOMTreeNode.create(microTree)
    }

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

data class ScrollState(
    val x: Double,
    val y: Double,
    val viewport: Dimension,
    val totalHeight: Double,
    val scrollYRatio: Double,
) {
    val chunksSeen get() = (viewport.height * scrollYRatio + 1).roundToInt()
    val chunksTotal get() = ceil(totalHeight / viewport.height).roundToInt()
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

data class BrowserState(
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
)
