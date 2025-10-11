package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools

/**
 * Kotlin-native DOM service interface.
 * Mirrors the Python DomService contract while allowing phased migration.
 */
interface DomService {
    fun getAllTrees(target: PageTarget, options: SnapshotOptions = SnapshotOptions()): TargetAllTrees
    fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode
    fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String> = emptyList()): String
    fun findElement(ref: ElementRefCriteria): EnhancedDOMTreeNode?
    fun toInteractedElement(node: EnhancedDOMTreeNode): DOMInteractedElement
}

/** Marker for target and session details */
data class PageTarget(
    val frameId: String? = null,
    val sessionId: String? = null,
)

/** Options controlling snapshot breadth/depth and expensive fields */
data class SnapshotOptions(
    val maxDepth: Int = 0, // 0 means full
    val includeAX: Boolean = true,
    val includeSnapshot: Boolean = true,
    val includeStyles: Boolean = true,
)

// --- Models (minimal placeholders; to be expanded to match Python structure) ---

data class EnhancedDOMTreeNode(
    val nodeId: Int = 0,
    val backendNodeId: Int? = null,
    val frameId: String? = null,
    val sessionId: String? = null,
    val nodeName: String = "",
    val nodeType: Int = 0,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<EnhancedDOMTreeNode> = emptyList(),
    // Merged snapshot fields (optional)
    val bounds: List<Double>? = null,
    val offsetRect: List<Double>? = null,
    val scrollRect: List<Double>? = null,
    val clientRect: List<Double>? = null,
    val paintOrder: Int? = null,
    val computedStyles: Map<String, String>? = null,
    // AX fields
    val axRole: String? = null,
    val axName: String? = null,
)

data class EnhancedAXNode(
    val axNodeId: String,
    val role: String? = null,
    val name: String? = null,
    val frameId: String? = null,
    val backendNodeId: Int? = null,
)

data class EnhancedSnapshotNode(
    val style: Map<String, String> = emptyMap(),
    val bounds: List<Double>? = null, // [x1,y1,x2,y2,x3,y3,x4,y4]
    val offsetRect: List<Double>? = null,
    val scrollRect: List<Double>? = null,
    val clientRect: List<Double>? = null,
    val paintOrder: Int? = null,
)

data class SimplifiedNode(
    val tag: String,
    val id: String? = null,
    val classes: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val text: String? = null,
    val interactiveIndex: Int? = null,
    val shouldDisplay: Boolean = true,
    val children: List<SimplifiedNode> = emptyList(),
    val shadowRoots: List<SimplifiedNode> = emptyList(),
)

data class TargetAllTrees(
    val axTree: List<EnhancedAXNode> = emptyList(),
    val domTree: EnhancedDOMTreeNode = EnhancedDOMTreeNode(),
    val snapshot: List<EnhancedSnapshotNode> = emptyList(),
    val snapshotByBackendId: Map<Int, EnhancedSnapshotNode> = emptyMap(),
    val axByBackendId: Map<Int, EnhancedAXNode> = emptyMap(),
    val axTreeByFrameId: Map<String, List<EnhancedAXNode>> = emptyMap(),
    val devicePixelRatio: Double = 1.0,
    val cdpTiming: Map<String, Long> = emptyMap(),
)

data class ElementRefCriteria(
    val cssSelector: String? = null,
    val xPath: String? = null,
    val elementHash: String? = null,
)

data class DOMInteractedElement(
    val elementHash: String,
    val xPath: String? = null,
    val bounds: String? = null,
)
