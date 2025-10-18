package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.DefaultIncludeAttributes
import ai.platon.pulsar.browser.driver.chrome.dom.model.TinyNode
import ai.platon.pulsar.common.serialize.json.Double2Serializer
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ibm.icu.util.TimeZone
import java.awt.Dimension
import java.util.*

/**
 * Serializer for DOM trees optimized for LLM consumption.
 */
object PulsarDOMSerializer {
    val mapper: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        val module = SimpleModule().apply {
            addSerializer(Double::class.java, Double2Serializer())
            addSerializer(Double::class.javaPrimitiveType, Double2Serializer())
        }
        registerModule(module)
    }

    /**
     * Serialize SimplifiedNode tree to JSON string for LLM.
     * Enhanced with paint-order pruning, compound component marking, and attribute casing alignment.
     *
     * @param root The simplified node tree root
     * @param includeAttributes List of attribute names to include (empty = use defaults)
     * @param options Serialization options for enhanced features
     * @return JSON string
     */
    fun serialize(
        root: TinyNode,
        includeAttributes: List<String> = emptyList(),
        options: SerializationOptions = SerializationOptions()
    ): DOMState {
        val attrsList = includeAttributes.ifEmpty { DefaultIncludeAttributes.ATTRIBUTES }
        val includeAttributes = attrsList.map { it.lowercase() }.toSet()

        val selectorMap = linkedMapOf<String, DOMTreeNodeEx>()
        val serializable = buildSerializableNode(
            root,
            includeAttributes,
            emptyList(),
            selectorMap,
            options,
            depth = 0,
            includeOrder = attrsList.map { it.lowercase() })
        val json = mapper.writeValueAsString(serializable)
        return DOMState(json = json, selectorMap = selectorMap)
    }

    /**
     * Options for enhanced LLM serialization.
     */
    data class SerializationOptions(
        val enablePaintOrderPruning: Boolean = true,
        val enableCompoundComponentDetection: Boolean = true,
        val enableAttributeCasingAlignment: Boolean = true,
        val maxPaintOrderThreshold: Int = 1000,
        val compoundComponentMinChildren: Int = 3,
        val preserveOriginalCasing: Boolean = false
    )

    private fun buildSerializableNode(
        node: TinyNode,
        includeAttributes: Set<String>,
        ancestors: List<DOMTreeNodeEx>,
        selectorMap: MutableMap<String, DOMTreeNodeEx>,
        options: SerializationOptions,
        depth: Int = 0,
        includeOrder: List<String> = emptyList()
    ): SerializableNode {
        // Apply paint-order pruning if enabled
        if (options.enablePaintOrderPruning && shouldPruneByPaintOrder(node, options)) {
            // Return a pruned node with minimal information
            return createPrunedNode(node, ancestors, selectorMap)
        }

        // Clean original node with enhanced attribute casing alignment
        val cleanedOriginal = cleanOriginalNodeEnhanced(node.originalNode, includeAttributes, options, includeOrder)

        val showScrollInfo = ScrollUtils.shouldShowScrollInfo(node.originalNode, ancestors)
        val scrollInfoText = if (showScrollInfo) {
            ScrollUtils.getScrollInfoText(node.originalNode)
        } else {
            null
        }

        // Add to selector map with multiple keys for enhanced lookup
        addToMultiSelectorMap(node.originalNode, selectorMap)
        // Also add interactive index mapping if present on SlimNode
        node.interactiveIndex?.let { idx ->
            selectorMap.putIfAbsent("index:$idx", node.originalNode)
        }

        // Detect compound components if enabled
        val isCompoundComponent = if (options.enableCompoundComponentDetection) {
            detectCompoundComponent(node, options)
        } else {
            false
        }

        // Recursively serialize children with enhanced logic (do not filter; prune per-node)
        val childAncestors = ancestors + node.originalNode
        val serializedChildren = node.children.map {
            buildSerializableNode(it, includeAttributes, childAncestors, selectorMap, options, depth + 1, includeOrder)
        }

        return SerializableNode(
            shouldDisplay = node.shouldDisplay,
            interactiveIndex = node.interactiveIndex,
            ignoredByPaintOrder = node.ignoredByPaintOrder,
            excludedByParent = node.excludedByParent,
            isCompoundComponent = isCompoundComponent,
            originalNode = cleanedOriginal,
            children = serializedChildren,
            shouldShowScrollInfo = if (showScrollInfo) true else null,
            scrollInfoText = scrollInfoText
        )
    }

    /**
     * Determine if a node should be pruned based on paint order.
     */
    private fun shouldPruneByPaintOrder(node: TinyNode, options: SerializationOptions): Boolean {
        val paintOrder = node.originalNode.snapshotNode?.paintOrder ?: return false
        return paintOrder > options.maxPaintOrderThreshold
    }

    /**
     * Detect if a node represents a compound component.
     */
    private fun detectCompoundComponent(node: TinyNode, options: SerializationOptions): Boolean {
        val originalNode = node.originalNode
        val tag = originalNode.nodeName.lowercase()

        // Input types that usually render compound controls
        val inputCompoundTypes = setOf(
            "date", "time", "datetime-local", "month", "week", "range", "number", "color", "file"
        )

        // Check for common compound component patterns
        val hasCompoundStructure = when {
            // Specific controls that have built-in compound UI
            tag == "select" || tag == "details" || tag == "audio" || tag == "video" -> true

            // Input with certain types
            tag == "input" && originalNode.attributes["type"]?.lowercase() in inputCompoundTypes -> true

            // Form components
            tag == "form" && node.children.size >= options.compoundComponentMinChildren -> true

            // List components
            tag in setOf("ul", "ol", "dl") && node.children.size >= options.compoundComponentMinChildren -> true

            // Table components
            tag == "table" && node.children.size >= options.compoundComponentMinChildren -> true

            // Navigation components
            tag == "nav" && node.children.size >= options.compoundComponentMinChildren -> true

            // Custom elements (web components)
            originalNode.nodeName.contains("-") && node.children.size >= options.compoundComponentMinChildren -> true

            // Check for ARIA roles that indicate compound components
            originalNode.axNode?.role in setOf("list", "grid", "tree", "tablist", "menu", "toolbar", "navigation") -> true

            else -> false
        }

        return hasCompoundStructure
    }

    /**
     * Create a pruned node with minimal information for high paint-order elements.
     */
    private fun createPrunedNode(
        node: TinyNode,
        ancestors: List<DOMTreeNodeEx>,
        selectorMap: MutableMap<String, DOMTreeNodeEx>
    ): SerializableNode {
        val prunedOriginal = CleanedOriginalNode(
            nodeId = node.originalNode.nodeId,
            backendNodeId = node.originalNode.backendNodeId,
            nodeType = node.originalNode.nodeType.value,
            nodeName = node.originalNode.nodeName.lowercase(),
            nodeValue = node.originalNode.nodeValue.takeIf { it.isNotEmpty() },
            attributes = emptyMap(), // Minimal attributes for pruned nodes
            frameId = node.originalNode.frameId,
            sessionId = node.originalNode.sessionId,
            isScrollable = null,
            isVisible = null,
            isInteractable = null,
            xPath = node.originalNode.xPath,
            elementHash = node.originalNode.elementHash,
            interactiveIndex = node.originalNode.interactiveIndex,
            bounds = null, // No bounds for pruned nodes
            clientRects = null,
            scrollRects = null,
            contentDocument = null
        )

        // Add to selector map with enhanced lookup keys
        addToMultiSelectorMap(node.originalNode, selectorMap)

        return SerializableNode(
            shouldDisplay = false, // Pruned nodes are not displayed
            interactiveIndex = node.interactiveIndex,
            ignoredByPaintOrder = true, // Mark as ignored by paint order
            excludedByParent = node.excludedByParent,
            isCompoundComponent = false,
            originalNode = prunedOriginal,
            children = emptyList(), // No children for pruned nodes
            shouldShowScrollInfo = null,
            scrollInfoText = null
        )
    }

    /**
     * Enhanced cleanOriginalNode with attribute casing alignment and improved filtering.
     */
    private fun cleanOriginalNodeEnhanced(
        node: DOMTreeNodeEx,
        includeAttributes: Set<String>,
        options: SerializationOptions,
        includeOrder: List<String>
    ): CleanedOriginalNode {
        // Filter attributes with enhanced casing alignment
        val filteredAttrs: Map<String, String> = if (options.enableAttributeCasingAlignment) {
            alignAttributeCasing(node.attributes, includeAttributes, options)
        } else {
            node.attributes.filterKeys { key ->
                key.lowercase() in includeAttributes
            }
        }

        // Extract AX attributes if present with enhanced processing
        val axAttrs = linkedMapOf<String, String>()
        node.axNode?.let { ax ->
            ax.role?.let { axAttrs["role"] = it }
            ax.name?.let { axAttrs["ax_name"] = it }
            ax.properties?.forEach { prop ->
                val key = if (options.enableAttributeCasingAlignment) {
                    alignAttributeName(prop.name, options)
                } else {
                    prop.name.lowercase()
                }
                if (key in includeAttributes) {
                    val v = prop.value
                    when (v) {
                        is Boolean -> axAttrs[key] = v.toString().lowercase()
                        null -> {}
                        else -> {
                            val s = v.toString().trim()
                            if (s.isNotEmpty()) axAttrs[key] = s
                        }
                    }
                }
            }
        }

        // Merge DOM and AX attributes (DOM first, AX overrides)
        val merged = linkedMapOf<String, String>()
        filteredAttrs.forEach { (k, v) -> merged[k] = v }
        axAttrs.forEach { (k, v) -> merged[k] = v }

        // Remove 'role' that duplicates node name (align with Python)
        val nodeNameLower = node.nodeName.lowercase()
        if (merged["role"] != null && merged["role"]!!.equals(nodeNameLower, ignoreCase = true)) {
            merged.remove("role")
        }

        // Remove duplicate long values keeping first occurrence according to include order (>5 length like Python)
        if (merged.size > 1) {
            val seen = mutableMapOf<String, String>() // value -> key
            val keysToRemove = mutableSetOf<String>()
            // iterate in includeOrder priority if provided, otherwise current order
            val orderedKeys = includeOrder.filter { merged.containsKey(it) } + merged.keys.filter { it !in includeOrder }
            for (key in orderedKeys) {
                val value = merged[key] ?: continue
                if (value.length > 5) {
                    val existingKey = seen[value]
                    if (existingKey != null && existingKey != key) {
                        keysToRemove.add(key)
                    } else {
                        seen[value] = key
                    }
                }
            }
            keysToRemove.forEach { merged.remove(it) }
        }

        // Get snapshot info with enhanced processing
        val snapshot = node.snapshotNode
        val bounds = snapshot?.bounds
        val clientRects = snapshot?.clientRects
        val scrollRects = snapshot?.scrollRects
        val absoluteBounds = snapshot?.absoluteBounds
        val paintOrder = snapshot?.paintOrder
        val stackingContexts = snapshot?.stackingContexts

        return CleanedOriginalNode(
            nodeId = node.nodeId,
            backendNodeId = node.backendNodeId,
            nodeType = node.nodeType.value,
            nodeName = if (options.preserveOriginalCasing) node.nodeName else node.nodeName.lowercase(),
            nodeValue = node.nodeValue.takeIf { it.isNotEmpty() },
            attributes = merged.takeIf { it.isNotEmpty() },
            frameId = node.frameId,
            sessionId = node.sessionId,
            isScrollable = node.isScrollable,
            isVisible = node.isVisible,
            isInteractable = node.isInteractable,
            xPath = node.xPath,
            elementHash = node.elementHash,
            interactiveIndex = node.interactiveIndex,
            bounds = bounds,
            clientRects = clientRects,
            scrollRects = scrollRects,
            absoluteBounds = absoluteBounds,
            paintOrder = paintOrder,
            stackingContexts = stackingContexts,
            // contentDocument is cleaned recursively if present
            contentDocument = node.contentDocument?.let { cleanOriginalNodeEnhanced(it, includeAttributes, options, includeOrder) }
        )
    }

    /**
     * Align attribute casing to match Python implementation and improve consistency.
     */
    private fun alignAttributeCasing(
        attributes: Map<String, String>,
        includeAttributes: Set<String>,
        options: SerializationOptions
    ): Map<String, String> {
        return attributes.mapNotNull { (key, value) ->
            val alignedKey = alignAttributeName(key, options)
            if (alignedKey in includeAttributes) {
                alignedKey to value
            } else {
                null
            }
        }.toMap()
    }

    /**
     * Align attribute name casing for consistency with Python implementation.
     */
    private fun alignAttributeName(attributeName: String, options: SerializationOptions): String {
        if (options.preserveOriginalCasing) return attributeName

        // Convert to lowercase for consistency
        val lowerName = attributeName.lowercase()

        // Special casing for known attributes that should maintain specific formats
        return when (lowerName) {
            "classname" -> "class" // Normalize className to class
            "htmlfor" -> "for" // Normalize htmlFor to for
            "readonly" -> "readonly" // Keep consistent casing
            "disabled" -> "disabled"
            "checked" -> "checked"
            "selected" -> "selected"
            "multiple" -> "multiple"
            "required" -> "required"
            "autofocus" -> "autofocus"
            "autoplay" -> "autoplay"
            "controls" -> "controls"
            "loop" -> "loop"
            "muted" -> "muted"
            else -> lowerName
        }
    }

    private fun cleanOriginalNode(
        node: DOMTreeNodeEx,
        includeAttributes: Set<String>
    ): CleanedOriginalNode {
        // Filter attributes
        val filteredAttrs = node.attributes.filterKeys { key ->
            key.lowercase() in includeAttributes
        }

        // Extract AX attributes if present
        val axAttrs = mutableMapOf<String, Any>()
        node.axNode?.let { ax ->
            ax.role?.let { axAttrs["role"] = it }
            ax.name?.let { axAttrs["ax_name"] = it }
            ax.properties?.forEach { prop ->
                val key = prop.name.lowercase()
                if (key in includeAttributes) {
                    prop.value?.let { axAttrs[key] = it }
                }
            }
        }

        // Merge DOM and AX attributes
        val mergedAttrs = filteredAttrs + axAttrs

        // Get snapshot info
        val snapshot = node.snapshotNode
        val bounds = snapshot?.bounds
        val clientRects = snapshot?.clientRects
        val scrollRects = snapshot?.scrollRects

        return CleanedOriginalNode(
            nodeId = node.nodeId,
            backendNodeId = node.backendNodeId,
            nodeType = node.nodeType.value,
            nodeName = node.nodeName.lowercase(),
            nodeValue = node.nodeValue.takeIf { it.isNotEmpty() },
            attributes = mergedAttrs.takeIf { it.isNotEmpty() },
            frameId = node.frameId,
            sessionId = node.sessionId,
            isScrollable = node.isScrollable,
            isVisible = node.isVisible,
            isInteractable = node.isInteractable,
            xPath = node.xPath,
            elementHash = node.elementHash,
            interactiveIndex = node.interactiveIndex,
            bounds = bounds,
            clientRects = clientRects,
            scrollRects = scrollRects,
            // contentDocument is cleaned recursively if present
            contentDocument = node.contentDocument?.let { cleanOriginalNode(it, includeAttributes) }
        )
    }

    /**
     * Serializable SimplifiedNode structure.
     * Enhanced with compound component marking and paint order information.
     */
    private data class SerializableNode(
        val shouldDisplay: Boolean,
        val interactiveIndex: Int?,
        val ignoredByPaintOrder: Boolean,
        val excludedByParent: Boolean,
        val isCompoundComponent: Boolean? = null,
        val originalNode: CleanedOriginalNode,
        val children: List<SerializableNode>,
        val shouldShowScrollInfo: Boolean?,
        val scrollInfoText: String?
    )

    /**
     * Cleaned original node without children_nodes and shadow_roots.
     * Enhanced with additional snapshot information for LLM consumption.
     * This prevents duplication since SimplifiedNode.children already contains them.
     */
    private data class CleanedOriginalNode(
        val nodeId: Int,
        val backendNodeId: Int?,
        val nodeType: Int,
        val nodeName: String,
        val nodeValue: String?,
        val attributes: Map<String, Any>?,
        val frameId: String?,
        val sessionId: String?,
        val isScrollable: Boolean?,
        val isVisible: Boolean?,
        val isInteractable: Boolean?,
        val xPath: String?,
        val elementHash: String?,
        val interactiveIndex: Int?,
        val bounds: DOMRect?,
        val clientRects: DOMRect?,
        val scrollRects: DOMRect?,
        val absoluteBounds: DOMRect? = null,
        val paintOrder: Int? = null,
        val stackingContexts: Int? = null,
        val contentDocument: CleanedOriginalNode?
        // Note: children_nodes and shadow_roots are intentionally omitted
    )

    /**
     * Add node to enhanced selector map with multiple lookup keys.
     * Supports element hash, XPath, and backend node ID for comprehensive element lookup.
     */
    private fun addToMultiSelectorMap(
        node: DOMTreeNodeEx,
        selectorMap: MutableMap<String, DOMTreeNodeEx>
    ) {
        // Add by element hash (primary key)
        node.elementHash?.let { hash ->
            selectorMap.putIfAbsent(hash, node)
        }

        // Add by XPath (secondary key)
        node.xPath?.let { xpath ->
            if (xpath.isNotBlank()) {
                selectorMap.putIfAbsent("xpath:$xpath", node)
            }
        }

        // Add by backend node ID (tertiary key)
        node.backendNodeId?.let { backendId ->
            selectorMap.putIfAbsent("backend:$backendId", node)
        }

        // Add by node ID (fallback key)
        selectorMap.putIfAbsent("node:${node.nodeId}", node)
    }
}

// Keep the serialization result as a top-level data class for reuse

data class DOMState(
    val json: String,
    val selectorMap: Map<String, DOMTreeNodeEx>
)

data class ClientInfo(
    val timeZone: TimeZone,
    val locale: Locale,
    val userAgent: String? = null,
    val devicePixelRatio: Double? = null,
    val viewportWidth: Int? = null,
    val viewportHeight: Int? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
)

data class FullClientInfo(
    val timeZone: TimeZone,
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
    val scrollYRatio: Double
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
