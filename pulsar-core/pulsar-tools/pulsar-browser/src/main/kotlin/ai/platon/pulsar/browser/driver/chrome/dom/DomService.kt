package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*

/**
 * Kotlin-native DOM service interface.
 * Mirrors the Python DomService contract while allowing phased migration.
 */
interface DomService {
    /**
     * Collect all trees (DOM, AX, Snapshot) for the given target.
     * Maps to Python DomService._get_all_trees
     */
    fun getAllTrees(target: PageTarget, options: SnapshotOptions = SnapshotOptions()): TargetAllTrees
    
    /**
     * Build the enhanced DOM tree by merging DOM, AX, and Snapshot data.
     * Maps to Python DomService._construct_enhanced_node (recursively)
     */
    fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode
    
    /**
     * Serialize SimplifiedNode tree for LLM consumption.
     * Maps to Python DOMTreeSerializer.serialize_for_llm
     */
    fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String> = emptyList()): String
    
    /**
     * Find an element by various criteria (CSS selector, XPath, element hash).
     */
    fun findElement(ref: ElementRefCriteria): EnhancedDOMTreeNode?
    
    /**
     * Convert EnhancedDOMTreeNode to DOMInteractedElement for agent interaction.
     */
    fun toInteractedElement(node: EnhancedDOMTreeNode): DOMInteractedElement
    
    /**
     * Build simplified node tree from enhanced DOM tree.
     * This is an intermediate step before LLM serialization.
     */
    fun buildSimplifiedTree(root: EnhancedDOMTreeNode): SimplifiedNode
}
