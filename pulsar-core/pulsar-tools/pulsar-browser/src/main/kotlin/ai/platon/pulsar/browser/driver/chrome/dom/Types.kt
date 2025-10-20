package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.CompactRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.awt.Dimension
import java.util.*

/**
 * Cleaned original node without children_nodes and shadow_roots.
 * Enhanced with additional snapshot information for LLM consumption.
 * This prevents duplication since SimplifiedNode.children already contains them.
 */
@JsonIgnoreProperties(value = ["nodeId", "backendNodeId", "frameId", "xpath", "elementHash"])
data class CleanedDOMTreeNodeEx(
    /**
     * Locator format: `frameIndex/backendNodeId`
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
    val isScrollable: Boolean?,
    val isVisible: Boolean?,
    val isInteractable: Boolean?,
    val interactiveIndex: Int?,
    val bounds: CompactRect?,
    val clientRects: CompactRect?,
    val scrollRects: CompactRect?,
    val absoluteBounds: CompactRect? = null,
    val paintOrder: Int? = null,
    val stackingContexts: Int? = null,
    val contentDocument: CleanedDOMTreeNodeEx?
    // Note: children_nodes and shadow_roots are intentionally omitted
)

/**
 * Serializable SimplifiedNode structure.
 * Enhanced with compound component marking and paint order information.
 */
data class CompactDOMTreeNode(
    val shouldDisplay: Boolean?,
    val interactiveIndex: Int?,
    val ignoredByPaintOrder: Boolean?,
    val excludedByParent: Boolean?,
    val isCompoundComponent: Boolean? = null,
    val originalNode: CleanedDOMTreeNodeEx,
    val children: List<CompactDOMTreeNode>?,
    val shouldShowScrollInfo: Boolean?,
    val scrollInfoText: String?
)

typealias CompactDOMTree = CompactDOMTreeNode

// Keep the serialization result as a top-level data class for reuse

data class DOMState(
    val compactDOMTree: CompactDOMTree,
    val json: String,
    val frameIds: List<String>,
    val selectorMap: Map<String, DOMTreeNodeEx>,
    val locatorMap: LocatorMap? = null
)

data class ClientInfo(
    // "Asia/Shanghai"
    val timeZone: String,
    val locale: Locale,
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
)

data class BrowserBasicState(
    val url: String,
    val goBackUrl: String,
    val goForwardUrl: String,
    val clientInfo: ClientInfo,
    val scrollState: ScrollState
)

data class BrowserState(
    val basicState: BrowserBasicState,
    val domState: DOMState
)
