package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.CompactRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.common.serialize.json.Double2Serializer
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.Dimension
import java.util.*

/**
 * Cleaned original node without children_nodes and shadow_roots.
 * Enhanced with additional snapshot information for LLM consumption.
 * This prevents duplication since SimplifiedNode.children already contains them.
 */
@JsonIgnoreProperties(value = ["nodeId", "backendNodeId", "frameId", "xpath", "elementHash"])
data class CleanedDOMTreeNode(
    /**
     * Locator format: `frameIndex-backendNodeId`
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
    val contentDocument: CleanedDOMTreeNode?
    // Note: children_nodes and shadow_roots are intentionally omitted
)

/**
 * Serializable SimplifiedNode structure.
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

data class DOMState(
    val microTree: MicroDOMTree,
    val interactiveNodes: List<MicroDOMTreeNode>,
    val frameIds: List<String>,
    val selectorMap: Map<String, DOMTreeNodeEx>,
    val locatorMap: LocatorMap
) {
    @get:JsonIgnore
    val microTreeJson: String by lazy { DOMSerializer.toJson(microTree) }

    @get:JsonIgnore
    val interactiveNodesJson: String by lazy { DOMSerializer.toJson(interactiveNodes) }
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
)

data class BrowserState(
    val url: String,
    val goBackUrl: String? = null,
    val goForwardUrl: String? = null,
    val clientInfo: ClientInfo,
    val scrollState: ScrollState
) {
    @get:JsonIgnore
    val json: String by lazy { DOMSerializer.toJson(this) }
}

data class BrowserUseState(
    val browserState: BrowserState,
    val domState: DOMState
)

object DOMSerializer {
    val MAPPER: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        val module = SimpleModule().apply {
            addSerializer(Double::class.java, Double2Serializer())
            // Keep double value length minimal
            addSerializer(Double::class.javaPrimitiveType, Double2Serializer())
        }
        registerModule(module)
    }

    fun toJson(root: MicroDOMTree): String {
        return MAPPER.writeValueAsString(root)
    }

    fun toJson(nodes: List<MicroDOMTreeNode>): String {
        return MAPPER.writeValueAsString(nodes)
    }

    fun toJson(browserState: BrowserState): String {
        return MAPPER.writeValueAsString(browserState)
    }
}
