package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType

/**
 * Utility functions for generating XPath expressions.
 * Maps to Python XPath generation logic in views.py
 */
object XPathUtils {
    
    /**
     * Generate XPath for a node based on its position in the tree.
     * Maps to Python EnhancedDOMTreeNode.x_path property.
     * 
     * Rules:
     * - Stops at shadow root boundaries (returns path up to shadow host)
     * - Stops at iframe boundaries (returns path up to iframe element)
     * - Uses 1-based indexing for sibling elements with same tag
     * - Includes id attribute when available for robustness
     * 
     * @param node The target node
     * @param ancestors List of ancestor nodes from root to immediate parent
     * @param siblings Map of parent node IDs to their children (for index calculation)
     * @return XPath string
     */
    fun generateXPath(
        node: EnhancedDOMTreeNode,
        ancestors: List<EnhancedDOMTreeNode> = emptyList(),
        siblings: Map<Int, List<EnhancedDOMTreeNode>> = emptyMap()
    ): String {
        // Only generate XPath for element nodes
        if (node.nodeType != NodeType.ELEMENT_NODE) {
            return ""
        }
        
        val parts = mutableListOf<String>()
        
        // Build path from ancestors
        var lastWasShadowHost = false
        for (i in ancestors.indices) {
            val ancestor = ancestors[i]
            
            // Skip non-element nodes
            if (ancestor.nodeType != NodeType.ELEMENT_NODE) continue
            
            // Check if this is a shadow host (has shadow roots)
            if (ancestor.shadowRoots.isNotEmpty()) {
                lastWasShadowHost = true
                // For shadow host, add the element but mark that we're entering shadow DOM
                parts.add(buildXPathSegment(ancestor, ancestors.getOrNull(i - 1), siblings))
                continue
            }
            
            // Check if this is an iframe - stop here
            if (ancestor.nodeName.equals("IFRAME", ignoreCase = true) ||
                ancestor.nodeName.equals("FRAME", ignoreCase = true)) {
                parts.add(buildXPathSegment(ancestor, ancestors.getOrNull(i - 1), siblings))
                // Stop at iframe boundary
                break
            }
            
            parts.add(buildXPathSegment(ancestor, ancestors.getOrNull(i - 1), siblings))
        }
        
        // Add the target node itself
        val parent = ancestors.lastOrNull()
        parts.add(buildXPathSegment(node, parent, siblings))
        
        // Build final XPath
        return if (parts.isEmpty()) {
            "/" + node.nodeName.lowercase()
        } else {
            "/" + parts.joinToString("/")
        }
    }
    
    /**
     * Build XPath segment for a single node.
     * Includes tag name, optional id predicate, and index for disambiguation.
     */
    private fun buildXPathSegment(
        node: EnhancedDOMTreeNode,
        parent: EnhancedDOMTreeNode?,
        siblings: Map<Int, List<EnhancedDOMTreeNode>>
    ): String {
        val tag = node.nodeName.lowercase()
        val id = node.attributes["id"]
        
        // If node has an ID, use it for robustness
        if (!id.isNullOrEmpty()) {
            return "$tag[@id='$id']"
        }
        
        // Calculate sibling index (1-based)
        val index = if (parent != null) {
            val parentChildren = siblings[parent.nodeId] ?: parent.children
            val sameTagSiblings = parentChildren.filter { 
                it.nodeName.equals(node.nodeName, ignoreCase = true) &&
                it.nodeType == NodeType.ELEMENT_NODE
            }
            
            if (sameTagSiblings.size > 1) {
                val idx = sameTagSiblings.indexOfFirst { it.nodeId == node.nodeId }
                if (idx >= 0) idx + 1 else null
            } else {
                null
            }
        } else {
            null
        }
        
        return if (index != null && index > 1) {
            "$tag[$index]"
        } else {
            tag
        }
    }
    
    /**
     * Generate a simplified XPath for display purposes.
     * Uses only tag names and IDs, without indices.
     */
    fun generateSimpleXPath(node: EnhancedDOMTreeNode, ancestors: List<EnhancedDOMTreeNode> = emptyList()): String {
        val parts = ancestors.mapNotNull { ancestor ->
            if (ancestor.nodeType != NodeType.ELEMENT_NODE) return@mapNotNull null
            
            val tag = ancestor.nodeName.lowercase()
            val id = ancestor.attributes["id"]
            
            if (!id.isNullOrEmpty()) {
                "$tag[@id='$id']"
            } else {
                tag
            }
        }.toMutableList()
        
        // Add target node
        val tag = node.nodeName.lowercase()
        val id = node.attributes["id"]
        parts.add(if (!id.isNullOrEmpty()) "$tag[@id='$id']" else tag)
        
        return "/" + parts.joinToString("/")
    }
}
