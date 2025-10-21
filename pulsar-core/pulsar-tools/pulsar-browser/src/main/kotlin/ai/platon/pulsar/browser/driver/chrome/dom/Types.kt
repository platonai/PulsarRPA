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
    @get:JsonIgnore
    val frameId: String?,
    @get:JsonIgnore
    val xpath: String?,
    @get:JsonIgnore
    val elementHash: String?,
    @get:JsonIgnore
    val nodeId: Int,
    @get:JsonIgnore
    val backendNodeId: Int?,

    @get:JsonIgnore
    val nodeType: Int,
    val nodeName: String,
    val nodeValue: String?,
    val attributes: Map<String, Any>?,
    @get:JsonIgnore
    val sessionId: String?,
    val isScrollable: Boolean?,
    val isVisible: Boolean?,
    val isInteractable: Boolean?,
    val interactiveIndex: Int?,
    val bounds: CompactRect?,
    val clientRects: CompactRect?,
    val scrollRects: CompactRect?,
    val absoluteBounds: CompactRect? = null,
    @get:JsonIgnore
    val paintOrder: Int? = null,
    @get:JsonIgnore
    val stackingContexts: Int? = null,
    @get:JsonIgnore
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
    @get:JsonIgnore
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

data class NanoDOMTreeNode(
    /**
     * Locator format: `frameIndex-backendNodeId`
     * */
    val locator: String? = null,
    val nodeName: String? = null,
    val nodeValue: String? = null,
    val attributes: Map<String, Any>? = null,
    val isScrollable: Boolean? = null,
    val isVisible: Boolean? = null,
    val isInteractable: Boolean? = null,
    val bounds: CompactRect? = null,
    val clientRects: CompactRect? = null,
    val scrollRects: CompactRect? = null,
    val absoluteBounds: CompactRect? = null,
    val children: List<NanoDOMTreeNode>? = null,
) {
    companion object {
        fun create(microTree: MicroDOMTreeNode): NanoDOMTree {
            val o = microTree.originalNode
            if (o == null) {
                // return an empty NanoDOMTree when there's no original cleaned node
                return NanoDOMTree()
            }

            // Create the current node from the micro node
            val root = newNode(microTree) ?: return NanoDOMTree()

            // Recursively create child nano nodes, filter out empty placeholders
            val childNanoList = microTree.children
                ?.map { create(it) }
                ?.filter { child ->
                    // keep nodes that have any meaningful data (locator or nodeName or non-empty children)
                    !(child.locator == null && child.nodeName == null && (child.children == null || child.children.isEmpty()))
                }

            return if (childNanoList.isNullOrEmpty()) root else root.copy(children = childNanoList)
        }

        private fun newNode(n: MicroDOMTreeNode): NanoDOMTree? {
            val o = n.originalNode ?: return null
            return NanoDOMTree(
                o.locator,
                o.nodeName,
                o.nodeValue,
                o.attributes,
                o.isScrollable,
                o.isVisible,
                o.isInteractable,
                o.bounds,
                o.clientRects,
                o.scrollRects,
                o.absoluteBounds,
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
    val lazyJson: String by lazy { DOMSerializer.toJson(this) }
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

    // serialize nano tree
    fun toJson(nano: NanoDOMTree): String {
        return MAPPER.writeValueAsString(nano)
    }
}
