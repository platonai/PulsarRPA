package ai.platon.pulsar.browser.driver.chrome.dom.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

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
 * DOM rectangle with coordinates.
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
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AXPropertyEx(
    val name: String,
    val value: Any? = null
)

/**
 * Enhanced accessibility node with essential AX tree information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AXNodeEx(
    @get:JsonProperty("ax_node_id") @param:JsonProperty("ax_node_id")
    val axNodeId: String,
    val ignored: Boolean = false,
    val role: String? = null,
    val name: String? = null,
    val description: String? = null,
    val properties: List<AXPropertyEx>? = null,
    @get:JsonProperty("child_ids") @param:JsonProperty("child_ids")
    val childIds: List<String>? = null,
    @get:JsonProperty("backend_node_id") @param:JsonProperty("backend_node_id")
    val backendNodeId: Int? = null,
    @get:JsonProperty("frame_id") @param:JsonProperty("frame_id")
    val frameId: String? = null
)

/**
 * Enhanced snapshot node data extracted from DOMSnapshot.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SnapshotNodeEx(
    @get:JsonProperty("is_clickable") @param:JsonProperty("is_clickable")
    val isClickable: Boolean? = null,
    @get:JsonProperty("cursor_style") @param:JsonProperty("cursor_style")
    val cursorStyle: String? = null,
    val bounds: DOMRect? = null,
    @get:JsonProperty("clientRects") @param:JsonProperty("clientRects")
    val clientRects: DOMRect? = null,
    @get:JsonProperty("scrollRects") @param:JsonProperty("scrollRects")
    val scrollRects: DOMRect? = null,
    @get:JsonProperty("computed_styles") @param:JsonProperty("computed_styles")
    val computedStyles: Map<String, String>? = null,
    @get:JsonProperty("paint_order") @param:JsonProperty("paint_order")
    val paintOrder: Int? = null,
    @get:JsonProperty("stacking_contexts") @param:JsonProperty("stacking_contexts")
    val stackingContexts: Int? = null,
    @get:JsonProperty("absolute_bounds") @param:JsonProperty("absolute_bounds")
    val absoluteBounds: DOMRect? = null
)

/**
 * Enhanced DOM tree node containing merged information from DOM, AX, and Snapshot trees.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DOMTreeNodeEx(
    // DOM Node data
    @get:JsonProperty("node_id") @param:JsonProperty("node_id")
    val nodeId: Int = 0,
    @get:JsonProperty("backend_node_id") @param:JsonProperty("backend_node_id")
    val backendNodeId: Int? = null,
    @get:JsonProperty("node_type") @param:JsonProperty("node_type")
    val nodeType: NodeType = NodeType.ELEMENT_NODE,
    @get:JsonProperty("node_name") @param:JsonProperty("node_name")
    val nodeName: String = "",
    @get:JsonProperty("node_value") @param:JsonProperty("node_value")
    val nodeValue: String = "",
    val attributes: Map<String, String> = emptyMap(),
    @get:JsonProperty("is_scrollable") @param:JsonProperty("is_scrollable")
    val isScrollable: Boolean? = null,
    @get:JsonProperty("is_visible") @param:JsonProperty("is_visible")
    val isVisible: Boolean? = null,
    @get:JsonProperty("absolute_position") @param:JsonProperty("absolute_position")
    val absolutePosition: DOMRect? = null,

    // Frame information
    @get:JsonProperty("target_id") @param:JsonProperty("target_id")
    val targetId: String? = null,
    @get:JsonProperty("frame_id") @param:JsonProperty("frame_id")
    val frameId: String? = null,
    @get:JsonProperty("session_id") @param:JsonProperty("session_id")
    val sessionId: String? = null,

    // Tree structure
    @get:JsonProperty("children_nodes") @param:JsonProperty("children_nodes")
    val children: List<DOMTreeNodeEx> = emptyList(),
    @get:JsonProperty("shadow_roots") @param:JsonProperty("shadow_roots")
    val shadowRoots: List<DOMTreeNodeEx> = emptyList(),
    @get:JsonProperty("content_document") @param:JsonProperty("content_document")
    val contentDocument: DOMTreeNodeEx? = null,

    // Snapshot data
    @get:JsonProperty("snapshot_node") @param:JsonProperty("snapshot_node")
    val snapshotNode: SnapshotNodeEx? = null,

    // AX data
    @get:JsonProperty("ax_node") @param:JsonProperty("ax_node")
    val axNode: AXNodeEx? = null,

    // XPath and hash
    @get:JsonProperty("x_path") @param:JsonProperty("x_path")
    val xPath: String? = null,
    @get:JsonProperty("element_hash") @param:JsonProperty("element_hash")
    val elementHash: String? = null,
    @get:JsonProperty("parent_branch_hash") @param:JsonProperty("parent_branch_hash")
    val parentBranchHash: String? = null,

    // Visibility and interaction
    @get:JsonProperty("is_interactable") @param:JsonProperty("is_interactable")
    val isInteractable: Boolean? = null,
    @get:JsonProperty("interactive_index") @param:JsonProperty("interactive_index")
    val interactiveIndex: Int? = null
)

/**
 * Simplified node for LLM serialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SlimNode(
    @get:JsonProperty("original_node") @param:JsonProperty("original_node")
    val originalNode: DOMTreeNodeEx,
    val children: List<SlimNode> = emptyList(),
    @get:JsonProperty("should_display") @param:JsonProperty("should_display")
    val shouldDisplay: Boolean = true,
    @get:JsonProperty("interactive_index") @param:JsonProperty("interactive_index")
    val interactiveIndex: Int? = null,
    @get:JsonProperty("is_new") @param:JsonProperty("is_new")
    val isNew: Boolean = false,
    @get:JsonProperty("ignored_by_paint_order") @param:JsonProperty("ignored_by_paint_order")
    val ignoredByPaintOrder: Boolean = false,
    @get:JsonProperty("excluded_by_parent") @param:JsonProperty("excluded_by_parent")
    val excludedByParent: Boolean = false,
    @get:JsonProperty("is_shadow_host") @param:JsonProperty("is_shadow_host")
    val isShadowHost: Boolean = false,
    @get:JsonProperty("is_compound_component") @param:JsonProperty("is_compound_component")
    val isCompoundComponent: Boolean = false
)

/**
 * DOM interacted element for agent interaction.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DOMInteractedElement(
    @get:JsonProperty("element_hash") @param:JsonProperty("element_hash")
    val elementHash: String,
    @get:JsonProperty("x_path") @param:JsonProperty("x_path")
    val xPath: String? = null,
    val bounds: DOMRect? = null,
    @get:JsonProperty("is_visible") @param:JsonProperty("is_visible")
    val isVisible: Boolean? = null,
    @get:JsonProperty("is_interactable") @param:JsonProperty("is_interactable")
    val isInteractable: Boolean? = null
)

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
