package ai.platon.pulsar.browser.driver.chrome.dom.model

import com.fasterxml.jackson.annotation.JsonIgnore

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
    fun toDOMRect(): DOMRect {
        return DOMRect(x?:0.0, y?:0.0, width?:0.0, height?:0.0)
    }
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
        return CompactRect(x.takeIf { it != 0.0 }, y.takeIf { it != 0.0 },
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
data class SnapshotNodeEx(
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
data class DOMTreeNodeEx(
    // DOM Node data
    @JsonIgnore
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
    val xPath: String? = null,
    val elementHash: String? = null,
    val parentBranchHash: String? = null,

    // Visibility and interaction
    val isInteractable: Boolean? = null,
    val interactiveIndex: Int? = null
)

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
