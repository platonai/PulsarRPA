package ai.platon.pulsar.browser.driver.chrome.dom.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DOM node types based on the DOM specification.
 * Maps to Python NodeType enum in views.py
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
 * DOM rectangle with coordinates.
 * Maps to Python DOMRect dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DOMRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
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
 * Maps to Python EnhancedAXProperty dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnhancedAXProperty(
    val name: String,
    val value: Any? = null
)

/**
 * Enhanced accessibility node with essential AX tree information.
 * Maps to Python EnhancedAXNode dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnhancedAXNode(
    @JsonProperty("ax_node_id")
    val axNodeId: String,
    val ignored: Boolean = false,
    val role: String? = null,
    val name: String? = null,
    val description: String? = null,
    val properties: List<EnhancedAXProperty>? = null,
    @JsonProperty("child_ids")
    val childIds: List<String>? = null,
    @JsonProperty("backend_node_id")
    val backendNodeId: Int? = null,
    @JsonProperty("frame_id")
    val frameId: String? = null
)

/**
 * Enhanced snapshot node data extracted from DOMSnapshot.
 * Maps to Python EnhancedSnapshotNode dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnhancedSnapshotNode(
    @JsonProperty("is_clickable")
    val isClickable: Boolean? = null,
    @JsonProperty("cursor_style")
    val cursorStyle: String? = null,
    val bounds: DOMRect? = null,
    @JsonProperty("clientRects")
    val clientRects: DOMRect? = null,
    @JsonProperty("scrollRects")
    val scrollRects: DOMRect? = null,
    @JsonProperty("computed_styles")
    val computedStyles: Map<String, String>? = null,
    @JsonProperty("paint_order")
    val paintOrder: Int? = null,
    @JsonProperty("stacking_contexts")
    val stackingContexts: Int? = null
)

/**
 * Enhanced DOM tree node containing merged information from DOM, AX, and Snapshot trees.
 * Maps to Python EnhancedDOMTreeNode dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnhancedDOMTreeNode(
    // DOM Node data
    @JsonProperty("node_id")
    val nodeId: Int = 0,
    @JsonProperty("backend_node_id")
    val backendNodeId: Int? = null,
    @JsonProperty("node_type")
    val nodeType: NodeType = NodeType.ELEMENT_NODE,
    @JsonProperty("node_name")
    val nodeName: String = "",
    @JsonProperty("node_value")
    val nodeValue: String = "",
    val attributes: Map<String, String> = emptyMap(),
    @JsonProperty("is_scrollable")
    val isScrollable: Boolean? = null,
    @JsonProperty("is_visible")
    val isVisible: Boolean? = null,
    @JsonProperty("absolute_position")
    val absolutePosition: DOMRect? = null,

    // Frame information
    @JsonProperty("target_id")
    val targetId: String? = null,
    @JsonProperty("frame_id")
    val frameId: String? = null,
    @JsonProperty("session_id")
    val sessionId: String? = null,

    // Tree structure
    @JsonProperty("children_nodes")
    val children: List<EnhancedDOMTreeNode> = emptyList(),
    @JsonProperty("shadow_roots")
    val shadowRoots: List<EnhancedDOMTreeNode> = emptyList(),
    @JsonProperty("content_document")
    val contentDocument: EnhancedDOMTreeNode? = null,

    // Snapshot data
    @JsonProperty("snapshot_node")
    val snapshotNode: EnhancedSnapshotNode? = null,

    // AX data
    @JsonProperty("ax_node")
    val axNode: EnhancedAXNode? = null,

    // XPath and hash
    @JsonProperty("x_path")
    val xPath: String? = null,
    @JsonProperty("element_hash")
    val elementHash: String? = null,
    @JsonProperty("parent_branch_hash")
    val parentBranchHash: String? = null,

    // Visibility and interaction
    @JsonProperty("is_interactable")
    val isInteractable: Boolean? = null,
    @JsonProperty("interactive_index")
    val interactiveIndex: Int? = null
)

/**
 * Simplified node for LLM serialization.
 * Maps to Python SimplifiedNode dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimplifiedNode(
    @JsonProperty("original_node")
    val originalNode: EnhancedDOMTreeNode,
    val children: List<SimplifiedNode> = emptyList(),
    @JsonProperty("should_display")
    val shouldDisplay: Boolean = true,
    @JsonProperty("interactive_index")
    val interactiveIndex: Int? = null,
    @JsonProperty("is_new")
    val isNew: Boolean = false,
    @JsonProperty("ignored_by_paint_order")
    val ignoredByPaintOrder: Boolean = false,
    @JsonProperty("excluded_by_parent")
    val excludedByParent: Boolean = false,
    @JsonProperty("is_shadow_host")
    val isShadowHost: Boolean = false,
    @JsonProperty("is_compound_component")
    val isCompoundComponent: Boolean = false
)

/**
 * DOM interacted element for agent interaction.
 * Maps to Python DOMInteractedElement.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DOMInteractedElement(
    @JsonProperty("element_hash")
    val elementHash: String,
    @JsonProperty("x_path")
    val xPath: String? = null,
    val bounds: DOMRect? = null,
    @JsonProperty("is_visible")
    val isVisible: Boolean? = null,
    @JsonProperty("is_interactable")
    val isInteractable: Boolean? = null
)

/**
 * Static attributes used for element hashing.
 * Maps to Python STATIC_ATTRIBUTES set.
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
 * Maps to Python DEFAULT_INCLUDE_ATTRIBUTES.
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
