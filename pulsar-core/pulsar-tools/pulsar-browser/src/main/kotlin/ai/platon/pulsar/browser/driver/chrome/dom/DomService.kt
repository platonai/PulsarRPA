package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*

/**
 * Kotlin-native DOM service interface.
 */
interface DomService {

    suspend fun getBrowserUseState(target: PageTarget = PageTarget(), snapshotOptions: SnapshotOptions = SnapshotOptions()): BrowserUseState

    suspend fun getDOMState(target: PageTarget = PageTarget(), snapshotOptions: SnapshotOptions = SnapshotOptions()): DOMState

    /**
     * Collect all trees (DOM, AX, Snapshot) for the given target.
     */
    suspend fun getMultiDOMTrees(target: PageTarget = PageTarget(), options: SnapshotOptions = SnapshotOptions()): TargetTrees

    /**
     * Build the enhanced DOM tree by merging DOM, AX, and Snapshot data.
     */
    fun buildEnhancedDomTree(trees: TargetTrees): DOMTreeNodeEx

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
    fun buildTinyTree(root: DOMTreeNodeEx): TinyTree

    suspend fun buildTinyTree(): TinyTree

    fun buildTinyTree(trees: TargetTrees): TinyTree

    /**
     * Serialize SimplifiedNode tree for LLM consumption.
     */
    fun buildDOMState(root: TinyTree, includeAttributes: List<String> = emptyList()): DOMState

    suspend fun buildBrowserState(domState: DOMState): BrowserUseState

    /**
     * Compute a rich client profile from the active browser context.
     */
    suspend fun computeFullClientInfo(): FullClientInfo

    suspend fun addHighlights(elements: InteractiveDOMTreeNodeList)

    suspend fun removeHighlights(elements: InteractiveDOMTreeNodeList)

    suspend fun removeHighlights(force: Boolean = false)

}
