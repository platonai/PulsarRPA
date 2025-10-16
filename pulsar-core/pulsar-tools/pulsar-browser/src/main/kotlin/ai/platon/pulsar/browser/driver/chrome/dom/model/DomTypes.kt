package ai.platon.pulsar.browser.driver.chrome.dom.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Marker for target and session details.
 */
data class PageTarget(
    @get:JsonProperty("target_id") @param:JsonProperty("target_id")
    val targetId: String? = null,
    @get:JsonProperty("frame_id") @param:JsonProperty("frame_id")
    val frameId: String? = null,
    @get:JsonProperty("session_id") @param:JsonProperty("session_id")
    val sessionId: String? = null
)

/**
 * Options controlling snapshot breadth/depth and expensive fields.
 */
data class SnapshotOptions(
    @get:JsonProperty("max_depth") @param:JsonProperty("max_depth")
    val maxDepth: Int = 0, // 0 means full tree
    @get:JsonProperty("include_ax") @param:JsonProperty("include_ax")
    val includeAX: Boolean = true,
    @get:JsonProperty("include_snapshot") @param:JsonProperty("include_snapshot")
    val includeSnapshot: Boolean = true,
    @get:JsonProperty("include_styles") @param:JsonProperty("include_styles")
    val includeStyles: Boolean = true,
    @get:JsonProperty("include_paint_order") @param:JsonProperty("include_paint_order")
    val includePaintOrder: Boolean = true,
    @get:JsonProperty("include_dom_rects") @param:JsonProperty("include_dom_rects")
    val includeDOMRects: Boolean = true,
    @get:JsonProperty("include_scroll_analysis") @param:JsonProperty("include_scroll_analysis")
    val includeScrollAnalysis: Boolean = true,
    @get:JsonProperty("include_visibility") @param:JsonProperty("include_visibility")
    val includeVisibility: Boolean = true,
    @get:JsonProperty("include_interactivity") @param:JsonProperty("include_interactivity")
    val includeInteractivity: Boolean = true
)

/**
 * Result from collecting all trees (DOM, AX, Snapshot) for a target.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TargetAllTrees(
    val snapshot: Map<String, Any>? = null,
    @get:JsonProperty("dom_tree") @param:JsonProperty("dom_tree")
    val domTree: DOMTreeNodeEx = DOMTreeNodeEx(),
    @get:JsonProperty("ax_tree") @param:JsonProperty("ax_tree")
    val axTree: List<AXNodeEx> = emptyList(),
    @get:JsonProperty("device_pixel_ratio") @param:JsonProperty("device_pixel_ratio")
    val devicePixelRatio: Double = 1.0,
    @get:JsonProperty("cdp_timing") @param:JsonProperty("cdp_timing")
    val cdpTiming: Map<String, Long> = emptyMap(),
    val options: SnapshotOptions = SnapshotOptions(),

    // Internal mappings for merging
    @get:JsonProperty("snapshot_by_backend_id") @param:JsonProperty("snapshot_by_backend_id")
    val snapshotByBackendId: Map<Int, SnapshotNodeEx> = emptyMap(),
    @get:JsonProperty("ax_by_backend_id") @param:JsonProperty("ax_by_backend_id")
    val axByBackendId: Map<Int, AXNodeEx> = emptyMap(),
    @get:JsonProperty("ax_tree_by_frame_id") @param:JsonProperty("ax_tree_by_frame_id")
    val axTreeByFrameId: Map<String, List<AXNodeEx>> = emptyMap(),
    @get:JsonProperty("dom_by_backend_id") @param:JsonProperty("dom_by_backend_id")
    val domByBackendId: Map<Int, DOMTreeNodeEx> = emptyMap()
)

/**
 * Criteria for finding a DOM element.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ElementRefCriteria(
    @get:JsonProperty("css_selector") @param:JsonProperty("css_selector")
    val cssSelector: String? = null,
    @get:JsonProperty("x_path") @param:JsonProperty("x_path")
    val xPath: String? = null,
    @get:JsonProperty("element_hash") @param:JsonProperty("element_hash")
    val elementHash: String? = null,
    @get:JsonProperty("backend_node_id") @param:JsonProperty("backend_node_id")
    val backendNodeId: Int? = null
)

/**
 * Current page targets information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CurrentPageTargets(
    @get:JsonProperty("page_session") @param:JsonProperty("page_session")
    val pageSession: Map<String, Any>,
    @get:JsonProperty("iframe_sessions") @param:JsonProperty("iframe_sessions")
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
    @get:JsonProperty("node_id") @param:JsonProperty("node_id")
    val nodeId: Int,
    val depth: Int
)
