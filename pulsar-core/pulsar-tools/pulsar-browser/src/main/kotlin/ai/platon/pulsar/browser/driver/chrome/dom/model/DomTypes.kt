package ai.platon.pulsar.browser.driver.chrome.dom.model

/**
 * Marker for target and session details.
 */
data class PageTarget(
    val targetId: String? = null,
    val frameId: String? = null,
    val sessionId: String? = null
)

/**
 * Options controlling snapshot breadth/depth and expensive fields.
 */
data class SnapshotOptions(
    val maxDepth: Int = 1000, // 0 means full tree TODO: it seems a bug with 0 for full tree logic
    val includeAX: Boolean = true,
    val includeSnapshot: Boolean = true,
    val includeStyles: Boolean = true,
    val includePaintOrder: Boolean = true,
    val includeDOMRects: Boolean = true,
    val includeScrollAnalysis: Boolean = true,
    val includeVisibility: Boolean = true,
    val includeInteractivity: Boolean = true
)

/**
 * Result from collecting all trees (DOM, AX, Snapshot) for a target.
 */
data class TargetMultiTrees(
    val snapshot: Map<String, Any>? = null,
    val domTree: DOMTreeNodeEx = DOMTreeNodeEx(),
    val axTree: List<AXNodeEx> = emptyList(),
    val devicePixelRatio: Double = 1.0,
    val cdpTiming: Map<String, Long> = emptyMap(),
    val options: SnapshotOptions = SnapshotOptions(),

    // Internal mappings for merging
    val snapshotByBackendId: Map<Int, SnapshotNodeEx> = emptyMap(),
    val axByBackendId: Map<Int, AXNodeEx> = emptyMap(),
    val axTreeByFrameId: Map<String, List<AXNodeEx>> = emptyMap(),
    val domByBackendId: Map<Int, DOMTreeNodeEx> = emptyMap()
)

/**
 * Criteria for finding a DOM element.
 */
data class ElementRefCriteria(
    val cssSelector: String? = null,
    val xPath: String? = null,
    val elementHash: String? = null,
    val backendNodeId: Int? = null
)

/**
 * Current page targets information.
 */
data class CurrentPageTargets(
    val pageSession: Map<String, Any>,
    val iframeSessions: List<Map<String, Any>> = emptyList()
)

/**
 * Propagating bounds for filtering children in serialization.
 */
data class PropagatingBounds(
    val tag: String,
    val bounds: DOMRect,
    val nodeId: Int,
    val depth: Int
)
