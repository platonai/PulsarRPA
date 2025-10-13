package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Serializer for DOM trees optimized for LLM consumption.
 * Maps to Python DOMTreeSerializer.
 */
object DomLLMSerializer {
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    /**
     * Serialize SimplifiedNode tree to JSON string for LLM.
     * Enhanced with paint-order pruning, compound component marking, and attribute casing alignment.
     * Maps to Python DOMTreeSerializer.serialize_for_llm
     *
     * @param root The simplified node tree root
     * @param includeAttributes List of attribute names to include (empty = use defaults)
     * @param options Serialization options for enhanced features
     * @return JSON string
     */
    fun serialize(
        root: SimplifiedNode,
        includeAttributes: List<String> = emptyList(),
        options: SerializationOptions = SerializationOptions()
    ): DomLLMSerialization {
        val attrs = if (includeAttributes.isEmpty()) {
            DefaultIncludeAttributes.ATTRIBUTES
        } else {
            includeAttributes
        }.map { it.lowercase() }.toSet()

        val selectorMap = linkedMapOf<String, EnhancedDOMTreeNode>()
        val serializable = buildSerializableEnhanced(root, attrs, emptyList(), selectorMap, options)
        val json = mapper.writeValueAsString(serializable)
        return DomLLMSerialization(json = json, selectorMap = selectorMap)
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
    
    /**
     * Enhanced buildSerializable with paint-order pruning and compound component detection.
     */
    private fun buildSerializableEnhanced(
        node: SimplifiedNode,
        includeAttributes: Set<String>,
        ancestors: List<EnhancedDOMTreeNode>,
        selectorMap: MutableMap<String, EnhancedDOMTreeNode>,
        options: SerializationOptions,
        depth: Int = 0
    ): SerializableNode {
        // Apply paint-order pruning if enabled
        if (options.enablePaintOrderPruning && shouldPruneByPaintOrder(node, options)) {
            // Return a pruned node with minimal information
            return createPrunedNode(node, ancestors, selectorMap)
        }

        // Clean original node with enhanced attribute casing alignment
        val cleanedOriginal = cleanOriginalNodeEnhanced(node.originalNode, includeAttributes, options)

        val showScrollInfo = ScrollUtils.shouldShowScrollInfo(node.originalNode, ancestors)
        val scrollInfoText = if (showScrollInfo) {
            ScrollUtils.getScrollInfoText(node.originalNode)
        } else {
            null
        }

        // Add to selector map with multiple keys for enhanced lookup
        addToEnhancedSelectorMap(node.originalNode, selectorMap)

        // Detect compound components if enabled
        val isCompoundComponent = if (options.enableCompoundComponentDetection) {
            detectCompoundComponent(node, options)
        } else {
            false
        }

        // Recursively serialize children with enhanced logic
        val childAncestors = ancestors + node.originalNode
        val filteredChildren = if (options.enablePaintOrderPruning) {
            filterChildrenByPaintOrder(node.children, options)
        } else {
            node.children
        }

        val serializedChildren = filteredChildren.map {
            buildSerializableEnhanced(it, includeAttributes, childAncestors, selectorMap, options, depth + 1)
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
    private fun shouldPruneByPaintOrder(node: SimplifiedNode, options: SerializationOptions): Boolean {
        val paintOrder = node.originalNode.snapshotNode?.paintOrder ?: return false
        return paintOrder > options.maxPaintOrderThreshold
    }

    /**
     * Filter children based on paint order for performance optimization.
     */
    private fun filterChildrenByPaintOrder(children: List<SimplifiedNode>, options: SerializationOptions): List<SimplifiedNode> {
        if (!options.enablePaintOrderPruning) return children

        return children.filter { child ->
            val paintOrder = child.originalNode.snapshotNode?.paintOrder
            paintOrder == null || paintOrder <= options.maxPaintOrderThreshold
        }
    }

    /**
     * Detect if a node represents a compound component.
     */
    private fun detectCompoundComponent(node: SimplifiedNode, options: SerializationOptions): Boolean {
        // Skip if not enough children
        if (node.children.size < options.compoundComponentMinChildren) return false

        val originalNode = node.originalNode

        // Check for common compound component patterns
        val hasCompoundStructure = when {
            // Form components
            originalNode.nodeName.equals("form", ignoreCase = true) -> true

            // List components
            originalNode.nodeName.equals("ul", ignoreCase = true) ||
            originalNode.nodeName.equals("ol", ignoreCase = true) ||
            originalNode.nodeName.equals("dl", ignoreCase = true) -> true

            // Table components
            originalNode.nodeName.equals("table", ignoreCase = true) -> true

            // Navigation components
            originalNode.nodeName.equals("nav", ignoreCase = true) -> true

            // Custom elements (web components)
            originalNode.nodeName.contains("-") -> true

            // Check for ARIA roles that indicate compound components
            originalNode.axNode?.role?.let { role ->
                role in setOf("list", "grid", "tree", "tablist", "menu", "toolbar", "navigation")
            } ?: false

            else -> false
        }

        return hasCompoundStructure
    }

    /**
     * Create a pruned node with minimal information for high paint-order elements.
     */
    private fun createPrunedNode(
        node: SimplifiedNode,
        ancestors: List<EnhancedDOMTreeNode>,
        selectorMap: MutableMap<String, EnhancedDOMTreeNode>
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
        addToEnhancedSelectorMap(node.originalNode, selectorMap)

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
        node: EnhancedDOMTreeNode,
        includeAttributes: Set<String>,
        options: SerializationOptions
    ): CleanedOriginalNode {
        // Filter attributes with enhanced casing alignment
        val filteredAttrs = if (options.enableAttributeCasingAlignment) {
            alignAttributeCasing(node.attributes, includeAttributes, options)
        } else {
            node.attributes.filterKeys { key ->
                key.lowercase() in includeAttributes
            }
        }

        // Extract AX attributes if present with enhanced processing
        val axAttrs = mutableMapOf<String, Any>()
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
                    prop.value?.let { axAttrs[key] = it }
                }
            }
        }

        // Merge DOM and AX attributes
        val mergedAttrs = filteredAttrs + axAttrs

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
            absoluteBounds = absoluteBounds,
            paintOrder = paintOrder,
            stackingContexts = stackingContexts,
            // contentDocument is cleaned recursively if present
            contentDocument = node.contentDocument?.let { cleanOriginalNodeEnhanced(it, includeAttributes, options) }
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
        node: EnhancedDOMTreeNode,
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
     * Maps to Python SimplifiedNode.__json__
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class SerializableNode(
        @JsonProperty("should_display")
        val shouldDisplay: Boolean,
        @JsonProperty("interactive_index")
        val interactiveIndex: Int?,
        @JsonProperty("ignored_by_paint_order")
        val ignoredByPaintOrder: Boolean,
        @JsonProperty("excluded_by_parent")
        val excludedByParent: Boolean,
        @JsonProperty("is_compound_component")
        val isCompoundComponent: Boolean? = null,
        @JsonProperty("original_node")
        val originalNode: CleanedOriginalNode,
        val children: List<SerializableNode>,
        @JsonProperty("should_show_scroll_info")
        val shouldShowScrollInfo: Boolean?,
        @JsonProperty("scroll_info_text")
        val scrollInfoText: String?
    )
    
    /**
     * Cleaned original node without children_nodes and shadow_roots.
     * Enhanced with additional snapshot information for LLM consumption.
     * This prevents duplication since SimplifiedNode.children already contains them.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class CleanedOriginalNode(
        @JsonProperty("node_id")
        val nodeId: Int,
        @JsonProperty("backend_node_id")
        val backendNodeId: Int?,
        @JsonProperty("node_type")
        val nodeType: Int,
        @JsonProperty("node_name")
        val nodeName: String,
        @JsonProperty("node_value")
        val nodeValue: String?,
        val attributes: Map<String, Any>?,
        @JsonProperty("frame_id")
        val frameId: String?,
        @JsonProperty("session_id")
        val sessionId: String?,
        @JsonProperty("is_scrollable")
        val isScrollable: Boolean?,
        @JsonProperty("is_visible")
        val isVisible: Boolean?,
        @JsonProperty("is_interactable")
        val isInteractable: Boolean?,
        @JsonProperty("x_path")
        val xPath: String?,
        @JsonProperty("element_hash")
        val elementHash: String?,
        @JsonProperty("interactive_index")
        val interactiveIndex: Int?,
        val bounds: DOMRect?,
        @JsonProperty("clientRects")
        val clientRects: DOMRect?,
        @JsonProperty("scrollRects")
        val scrollRects: DOMRect?,
        @JsonProperty("absolute_bounds")
        val absoluteBounds: DOMRect? = null,
        @JsonProperty("paint_order")
        val paintOrder: Int? = null,
        @JsonProperty("stacking_contexts")
        val stackingContexts: Int? = null,
        @JsonProperty("content_document")
        val contentDocument: CleanedOriginalNode?
        // Note: children_nodes and shadow_roots are intentionally omitted
    )
}

    /**
     * Add node to enhanced selector map with multiple lookup keys.
     * Supports element hash, XPath, and backend node ID for comprehensive element lookup.
     */
    private fun addToEnhancedSelectorMap(
        node: EnhancedDOMTreeNode,
        selectorMap: MutableMap<String, EnhancedDOMTreeNode>
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

data class DomLLMSerialization(
    val json: String,
    val selectorMap: Map<String, EnhancedDOMTreeNode>
)
