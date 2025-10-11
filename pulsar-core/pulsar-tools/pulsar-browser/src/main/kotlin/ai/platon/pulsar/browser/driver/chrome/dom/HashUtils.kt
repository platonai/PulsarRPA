package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import ai.platon.pulsar.browser.driver.chrome.dom.model.StaticAttributes
import java.security.MessageDigest

/**
 * Utility functions for computing element hashes and parent branch hashes.
 * Maps to Python hash computation logic in views.py
 */
object HashUtils {
    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute element hash based on parent-branch path + STATIC_ATTRIBUTES.
     * Maps to Python EnhancedDOMTreeNode.element_hash property.
     * 
     * @param node The DOM node
     * @param parentBranchHash The hash of the parent branch (optional)
     * @return SHA256 hex string
     */
    fun elementHash(node: EnhancedDOMTreeNode, parentBranchHash: String? = null): String {
        val parts = mutableListOf<String>()
        
        // Include parent branch hash if available
        if (!parentBranchHash.isNullOrEmpty()) {
            parts.add(parentBranchHash)
        }
        
        // Add tag name
        parts.add(node.nodeName.lowercase())
        
        // Add static attributes in sorted order
        val staticAttrs = node.attributes.filterKeys { it in StaticAttributes.ATTRIBUTES }
            .toSortedMap()
            .map { "${it.key}=${it.value}" }
        parts.addAll(staticAttrs)
        
        // Include backend node ID if available for uniqueness
        node.backendNodeId?.let { parts.add("backend:$it") }
        
        // Include session ID if available
        node.sessionId?.let { parts.add("session:$it") }
        
        val key = parts.joinToString("|")
        return sha256Hex(key.toByteArray(Charsets.UTF_8))
    }

    /**
     * Compute parent branch hash for a node based on its position in the tree.
     * This is used to build unique paths through the DOM tree.
     * Maps to Python EnhancedDOMTreeNode.parent_branch_hash property.
     * 
     * @param ancestors List of ancestor nodes from root to immediate parent
     * @return SHA256 hex string representing the path to this node
     */
    fun parentBranchHash(ancestors: List<EnhancedDOMTreeNode>): String {
        val parts = ancestors.map { node ->
            val tag = node.nodeName.lowercase()
            val id = node.attributes["id"]
            val classes = node.attributes["class"]
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.sorted()
                ?.joinToString(".")
                ?: ""
            
            buildString {
                append(tag)
                if (!id.isNullOrEmpty()) append("#$id")
                if (classes.isNotEmpty()) append(".$classes")
            }
        }
        
        val key = parts.joinToString("/")
        return sha256Hex(key.toByteArray(Charsets.UTF_8))
    }

    /**
     * Simple element hash for quick lookups (without parent branch).
     * Useful for backward compatibility with existing code.
     */
    fun simpleElementHash(node: EnhancedDOMTreeNode): String {
        return elementHash(node, parentBranchHash = null)
    }
}
