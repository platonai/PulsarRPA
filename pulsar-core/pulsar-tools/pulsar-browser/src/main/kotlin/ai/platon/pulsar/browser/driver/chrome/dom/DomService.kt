package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*

/**
 * Kotlin-native DOM service interface.
 */
interface DomService {
    /**
     * Collect all trees (DOM, AX, Snapshot) for the given target.
     */
    fun getMultiDOMTrees(target: PageTarget = PageTarget(), options: SnapshotOptions = SnapshotOptions()): TargetDetailTrees

    /**
     * Build the enhanced DOM tree by merging DOM, AX, and Snapshot data.
     */
    fun buildEnhancedDomTree(trees: TargetDetailTrees): DOMTreeNodeEx

    /**
     * Find an element by various criteria (CSS selector, XPath, element hash).
     */
    fun findElement(ref: ElementRefCriteria): DOMTreeNodeEx?

    /**
     * Convert EnhancedDOMTreeNode to DOMInteractedElement for agent interaction.
     */
    fun toInteractedElement(node: DOMTreeNodeEx): DOMInteractedElement

    /**
     * Build simplified node tree from enhanced DOM tree.
     */
    fun buildSimplifiedSlimDOM(root: DOMTreeNodeEx): SlimNode

    fun buildSlimDOM(): SlimNode

    fun buildSlimDOM(trees: TargetDetailTrees): SlimNode

    /**
     * Serialize SimplifiedNode tree for LLM consumption.
     */
    fun serialize(root: SlimNode, includeAttributes: List<String> = emptyList()): DOMState
}
