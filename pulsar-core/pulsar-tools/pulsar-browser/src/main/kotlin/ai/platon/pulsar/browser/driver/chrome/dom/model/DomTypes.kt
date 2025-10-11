package ai.platon.pulsar.browser.driver.chrome.dom.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Marker for target and session details.
 */
data class PageTarget(
    @JsonProperty("target_id")
    val targetId: String? = null,
    @JsonProperty("frame_id")
    val frameId: String? = null,
    @JsonProperty("session_id")
    val sessionId: String? = null
)

/**
 * Options controlling snapshot breadth/depth and expensive fields.
 */
data class SnapshotOptions(
    @JsonProperty("max_depth")
    val maxDepth: Int = 0, // 0 means full tree
    @JsonProperty("include_ax")
    val includeAX: Boolean = true,
    @JsonProperty("include_snapshot")
    val includeSnapshot: Boolean = true,
    @JsonProperty("include_styles")
    val includeStyles: Boolean = true,
    @JsonProperty("include_paint_order")
    val includePaintOrder: Boolean = true,
    @JsonProperty("include_dom_rects")
    val includeDOMRects: Boolean = true
)

/**
 * Result from collecting all trees (DOM, AX, Snapshot) for a target.
 * Maps to Python TargetAllTrees dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TargetAllTrees(
    val snapshot: Map<String, Any>? = null,
    @JsonProperty("dom_tree")
    val domTree: EnhancedDOMTreeNode = EnhancedDOMTreeNode(),
    @JsonProperty("ax_tree")
    val axTree: List<EnhancedAXNode> = emptyList(),
    @JsonProperty("device_pixel_ratio")
    val devicePixelRatio: Double = 1.0,
    @JsonProperty("cdp_timing")
    val cdpTiming: Map<String, Long> = emptyMap(),
    
    // Internal mappings for merging
    @JsonProperty("snapshot_by_backend_id")
    val snapshotByBackendId: Map<Int, EnhancedSnapshotNode> = emptyMap(),
    @JsonProperty("ax_by_backend_id")
    val axByBackendId: Map<Int, EnhancedAXNode> = emptyMap(),
    @JsonProperty("ax_tree_by_frame_id")
    val axTreeByFrameId: Map<String, List<EnhancedAXNode>> = emptyMap()
)

/**
 * Criteria for finding a DOM element.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ElementRefCriteria(
    @JsonProperty("css_selector")
    val cssSelector: String? = null,
    @JsonProperty("x_path")
    val xPath: String? = null,
    @JsonProperty("element_hash")
    val elementHash: String? = null,
    @JsonProperty("backend_node_id")
    val backendNodeId: Int? = null
)

/**
 * Current page targets information.
 * Maps to Python CurrentPageTargets dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CurrentPageTargets(
    @JsonProperty("page_session")
    val pageSession: Map<String, Any>,
    @JsonProperty("iframe_sessions")
    val iframeSessions: List<Map<String, Any>> = emptyList()
)

/**
 * Propagating bounds for filtering children in serialization.
 * Maps to Python PropagatingBounds dataclass.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PropagatingBounds(
    val tag: String,
    val bounds: DOMRect,
    @JsonProperty("node_id")
    val nodeId: Int,
    val depth: Int
)
