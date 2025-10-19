package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.CompactRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.DefaultIncludeAttributes
import ai.platon.pulsar.browser.driver.chrome.dom.model.TinyNode
import ai.platon.pulsar.common.serialize.json.Double2Serializer
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.Dimension
import java.util.*

/**
 * Serializer for DOM trees optimized for LLM consumption.
 */
object PulsarDOMSerializer {
    val MAPPER: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        val module = SimpleModule().apply {
            addSerializer(Double::class.java, Double2Serializer())
            // Keep double value length minimal
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

        val frameIdSet = mutableSetOf<String>()
        // Traverse root to collect all frame ids
        collectFrameIds(root, frameIdSet)

        val frameIds = frameIdSet.toList()
        // Build a new LocatorMap for optimized element lookup
        val locatorMap = LocatorMap()
        val serializable = buildSerializableNode(
            root,
            includeAttributes,
            emptyList(),
            locatorMap,
            frameIds,
            options,
            depth = 0,
            includeOrder = attrsList.map { it.lowercase() })
        val json = MAPPER.writeValueAsString(serializable)
        // Export legacy selector map view for backward compatibility and diagnostics/tests
        val legacySelectorMap = locatorMap.toLegacyStringMap()
        return DOMState(json = json, selectorMap = legacySelectorMap, frameIds = frameIds, locatorMap = locatorMap)
    }

    private fun collectFrameIds(root: TinyNode, frameIds: MutableSet<String>) {
        root.originalNode.frameId?.let { frameIds.add(it) }
        root.children.forEach {
            collectFrameIds(it, frameIds)
        }
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
        locatorMap: LocatorMap,
        frameIds: List<String>,
        options: SerializationOptions,
        depth: Int = 0,
        includeOrder: List<String> = emptyList()
    ): SerializableNode {
        // Apply paint-order pruning if enabled
        if (options.enablePaintOrderPruning && shouldPruneByPaintOrder(node, options)) {
            // Return a pruned node with minimal information
            return createPrunedNode(node, ancestors, locatorMap, frameIds)
        }

        // Clean original node with enhanced attribute casing alignment
        val cleanedOriginal = cleanOriginalNodeEnhanced(node.originalNode, includeAttributes, options, includeOrder, frameIds)

        val showScrollInfo = ScrollUtils.shouldShowScrollInfo(node.originalNode, ancestors)
        val scrollInfoText = if (showScrollInfo) {
            ScrollUtils.getScrollInfoText(node.originalNode)
        } else {
            null
        }

        // Add to selector map with multiple keys for enhanced lookup
        addToLocatorMap(node.originalNode, locatorMap)
        // Also add interactive index mapping if present on SlimNode
        node.interactiveIndex?.let { idx ->
            locatorMap.add(LocatorType.INDEX, idx.toString(), node.originalNode)
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
            buildSerializableNode(it, includeAttributes, childAncestors, locatorMap, frameIds, options, depth + 1, includeOrder)
        }

        return SerializableNode(
            shouldDisplay = node.shouldDisplay.takeIf { it },
            interactiveIndex = node.interactiveIndex,
            ignoredByPaintOrder = node.ignoredByPaintOrder.takeIf { it },
            excludedByParent = node.excludedByParent.takeIf { it },
            isCompoundComponent = isCompoundComponent.takeIf { it },
            originalNode = cleanedOriginal,
            children = serializedChildren.takeIf { it.isNotEmpty() },
            shouldShowScrollInfo = showScrollInfo.takeIf { it },
            scrollInfoText = scrollInfoText?.takeIf { it.isNotEmpty() },
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
        locatorMap: LocatorMap,
        frameIds: List<String>,
    ): SerializableNode {
        val prunedOriginal = CleanedOriginalNode(
            locator = createNodeLocator(node.originalNode, frameIds),
            nodeId = node.originalNode.nodeId,
            backendNodeId = node.originalNode.backendNodeId,
            nodeType = node.originalNode.nodeType.value,
            nodeName = node.originalNode.nodeName.lowercase(),
            nodeValue = node.originalNode.nodeValue.takeIf { it.isNotEmpty() },
            attributes = null, // Minimal attributes for pruned nodes
            frameId = node.originalNode.frameId,
            sessionId = node.originalNode.sessionId,
            isScrollable = null,
            isVisible = null,
            isInteractable = null,
            xpath = node.originalNode.xpath,
            elementHash = node.originalNode.elementHash,
            interactiveIndex = node.originalNode.interactiveIndex,
            bounds = null, // No bounds for pruned nodes
            clientRects = null,
            scrollRects = null,
            contentDocument = null
        )

        // Add to selector map with enhanced lookup keys
        addToLocatorMap(node.originalNode, locatorMap)

        return SerializableNode(
            shouldDisplay = null, // Pruned nodes are not displayed
            interactiveIndex = node.interactiveIndex,
            ignoredByPaintOrder = true, // Mark as ignored by paint order
            excludedByParent = node.excludedByParent.takeIf { it },
            isCompoundComponent = null,
            originalNode = prunedOriginal,
            children = null, // No children for pruned nodes
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
        includeOrder: List<String>,
        frameIds: List<String>,
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
            locator = createNodeLocator(node, frameIds),
            nodeId = node.nodeId,
            backendNodeId = node.backendNodeId,
            nodeType = node.nodeType.value,
            nodeName = if (options.preserveOriginalCasing) node.nodeName else node.nodeName.lowercase(),
            nodeValue = node.nodeValue.takeIf { it.isNotEmpty() },
            attributes = merged.takeIf { it.isNotEmpty() },
            frameId = node.frameId,
            sessionId = node.sessionId,
            isScrollable = node.isScrollable?.takeIf { it },
            isVisible = node.isVisible?.takeIf { it },
            isInteractable = node.isInteractable?.takeIf { it },
            xpath = node.xpath,
            elementHash = node.elementHash,
            interactiveIndex = node.interactiveIndex,
            bounds = bounds?.compact(),
            clientRects = clientRects?.compact(),
            scrollRects = scrollRects?.compact(),
            absoluteBounds = absoluteBounds?.compact(),
            paintOrder = paintOrder,
            stackingContexts = stackingContexts,
            // contentDocument is cleaned recursively if present
            contentDocument = node.contentDocument?.let { cleanOriginalNodeEnhanced(it, includeAttributes, options, includeOrder, frameIds) }
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
        includeAttributes: Set<String>,
        frameIds: List<String>,
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
            locator = createNodeLocator(node, frameIds),
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
            xpath = node.xpath,
            elementHash = node.elementHash,
            interactiveIndex = node.interactiveIndex,
            bounds = bounds?.compact(),
            clientRects = clientRects?.compact(),
            scrollRects = scrollRects?.compact(),
            // contentDocument is cleaned recursively if present
            contentDocument = node.contentDocument?.let { cleanOriginalNode(it, includeAttributes, frameIds) }
        )
    }

    /**
     * Serializable SimplifiedNode structure.
     * Enhanced with compound component marking and paint order information.
     */
    private data class SerializableNode(
        val shouldDisplay: Boolean?,
        val interactiveIndex: Int?,
        val ignoredByPaintOrder: Boolean?,
        val excludedByParent: Boolean?,
        val isCompoundComponent: Boolean? = null,
        val originalNode: CleanedOriginalNode,
        val children: List<SerializableNode>?,
        val shouldShowScrollInfo: Boolean?,
        val scrollInfoText: String?
    )

    /**
     * Cleaned original node without children_nodes and shadow_roots.
     * Enhanced with additional snapshot information for LLM consumption.
     * This prevents duplication since SimplifiedNode.children already contains them.
     */
    @JsonIgnoreProperties(value = ["nodeId", "backendNodeId", "frameId", "xpath", "elementHash"])
    private data class CleanedOriginalNode(
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
        val contentDocument: CleanedOriginalNode?
        // Note: children_nodes and shadow_roots are intentionally omitted
    )

    /**
     * Add node to enhanced selector map with multiple lookup keys (via LocatorMap).
     * Supports element hash, XPath, backend node ID, frame/backend combo and node ID for comprehensive element lookup.
     */
    private fun addToLocatorMap(
        node: DOMTreeNodeEx,
        locatorMap: LocatorMap
    ) {
        // Add by element hash (primary key)
        node.elementHash?.takeIf { it.isNotBlank() }?.let { h ->
            locatorMap.add(LocatorType.HASH, h, node)
        }

        // Add by XPath (secondary key)
        node.xpath?.takeIf { it.isNotBlank() }?.let { xp ->
            locatorMap.add(LocatorType.XPATH, xp, node)
        }

        val frameId = node.frameId
        val backendNodeId = node.backendNodeId

        // Add by backend node ID (tertiary key)
        backendNodeId?.let { bn ->
            locatorMap.add(LocatorType.BACKEND_NODE_ID, bn.toString(), node)
        }

        // Add by `$frameId/$backendNodeId` as node ID
        if (frameId != null && backendNodeId != null) {
            val selector = "$frameId/$backendNodeId"
            locatorMap.add(LocatorType.FRAME_BACKEND_NODE_ID, selector, node)
        }

        // Add by node ID (fallback key)
        locatorMap.add(LocatorType.NODE_ID, node.nodeId.toString(), node)
    }

    private fun createNodeLocator(node: DOMTreeNodeEx, frameIds: List<String>): String {
        // Returns -1 if the list does not contain element.
        val index = frameIds.indexOf(node.frameId)
        return "$index/${node.backendNodeId}"
    }
}

enum class LocatorType(val value: String) {
    HASH("hash"),
    XPATH("xpath"),
    BACKEND_NODE_ID("backend"),
    FRAME_BACKEND_NODE_ID("fbn"),
    NODE_ID("node"),
    INDEX("index");

    override fun toString() = value
}

data class Locator(
    val type: LocatorType,
    val selector: String,
) {
    override fun toString(): String = "${type}:${selector}"
}

class LocatorMap {
    private val map = mutableMapOf<Locator, DOMTreeNodeEx>()

    fun add(type: LocatorType, selector: String, node: DOMTreeNodeEx) {
        map.putIfAbsent(Locator(type, selector), node)
    }

    fun select(locator: Locator): DOMTreeNodeEx? {
        return map[locator]
    }

    fun select(key: String): DOMTreeNodeEx? {
        // Support legacy string keys like plain hash, and prefixed forms like xpath:, backend:, node:, index:
        // Try prefixed first
        val colon = key.indexOf(':')
        if (colon > 0) {
            val typeStr = key.substring(0, colon)
            val selector = key.substring(colon + 1)
            val type = LocatorType.values().firstOrNull { it.value == typeStr }
            if (type != null) return select(Locator(type, selector))
        }
        // Fallback: treat as element hash without prefix
        return map.entries.firstOrNull { it.key.type == LocatorType.HASH && it.key.selector == key }?.value
    }

    fun toLegacyStringMap(): Map<String, DOMTreeNodeEx> {
        // Preserve insertion order similar to linkedMapOf in previous implementation
        val out = LinkedHashMap<String, DOMTreeNodeEx>(map.size)
        map.forEach { (k, v) ->
            val legacyKey = when (k.type) {
                LocatorType.HASH -> k.selector // no prefix for hash for backward compatibility
                LocatorType.XPATH -> "xpath:${k.selector}"
                LocatorType.BACKEND_NODE_ID -> "backend:${k.selector}"
                LocatorType.FRAME_BACKEND_NODE_ID -> "fbn:${k.selector}"
                LocatorType.NODE_ID -> "node:${k.selector}"
                LocatorType.INDEX -> "index:${k.selector}"
            }
            out.putIfAbsent(legacyKey, v)
        }
        return out
    }
}

// Keep the serialization result as a top-level data class for reuse

data class DOMState(
    val json: String,
    val selectorMap: Map<String, DOMTreeNodeEx>,
    val frameIds: List<String>,
    // Expose new optimized locator map alongside legacy map
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
