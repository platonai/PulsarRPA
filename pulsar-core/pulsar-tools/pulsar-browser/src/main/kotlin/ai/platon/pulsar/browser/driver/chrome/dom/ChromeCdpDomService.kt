package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler
import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler.AccessibilityTreeResult
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.*
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXProperty

/**
 * CDP-backed implementation of DomService using RemoteDevTools.
 * Maps to Python DomService class.
 */
class ChromeCdpDomService(
    private val devTools: RemoteDevTools,
) : DomService {

    private val accessibility = AccessibilityHandler(devTools)
    private val domTree = DomTreeHandler(devTools)
    private val snapshot = DomSnapshotHandler(devTools)
    
    @Volatile 
    private var lastEnhancedRoot: EnhancedDOMTreeNode? = null
    
    @Volatile
    private var lastAncestorMap: Map<Int, List<EnhancedDOMTreeNode>> = emptyMap()

    override fun getAllTrees(target: PageTarget, options: SnapshotOptions): TargetAllTrees {
        val startTime = System.currentTimeMillis()
        val timings = mutableMapOf<String, Long>()
        
        // Fetch AX tree
        val axResult: AccessibilityTreeResult = if (options.includeAX) {
            val axStart = System.currentTimeMillis()
            val result = accessibility.getFullAXTreeRecursive(target.frameId, depth = null)
            timings["ax_tree"] = System.currentTimeMillis() - axStart
            result
        } else AccessibilityTreeResult.EMPTY

        // Fetch DOM tree
        val domStart = System.currentTimeMillis()
        val dom = domTree.getDocument(options.maxDepth)
        timings["dom_tree"] = System.currentTimeMillis() - domStart
        
        // Fetch snapshot
        val snapshotStart = System.currentTimeMillis()
        val snapshotByBackendId = if (options.includeSnapshot) {
            snapshot.captureByBackendNodeId(options.includeStyles)
        } else {
            emptyMap()
        }
        timings["snapshot"] = System.currentTimeMillis() - snapshotStart

        // Build AX mappings
        val enhancedAx = axResult.nodes.map { it.toEnhanced() }
        val axByBackendId: Map<Int, EnhancedAXNode> = buildMap {
            axResult.nodesByBackendNodeId.forEach { (backendId, nodes) ->
                val first = nodes.firstOrNull() ?: return@forEach
                put(backendId, first.toEnhanced())
            }
        }
        val axTreeByFrame: Map<String, List<EnhancedAXNode>> = axResult.nodesByFrameId.mapValues { (_, list) ->
            list.map { it.toEnhanced() }
        }

        timings["total"] = System.currentTimeMillis() - startTime

        return TargetAllTrees(
            domTree = dom,
            axTree = enhancedAx,
            snapshotByBackendId = snapshotByBackendId,
            axByBackendId = axByBackendId,
            axTreeByFrameId = axTreeByFrame,
            devicePixelRatio = 1.0, // TODO: get from CDP
            cdpTiming = timings
        )
    }

    override fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode {
        // Build ancestor map for XPath and hash generation
        val ancestorMap = buildAncestorMap(trees.domTree)
        lastAncestorMap = ancestorMap
        
        // Build sibling map for XPath index calculation
        val siblingMap = buildSiblingMap(trees.domTree)
        
        // Merge trees recursively
        fun merge(node: EnhancedDOMTreeNode, ancestors: List<EnhancedDOMTreeNode>): EnhancedDOMTreeNode {
            val backendId = node.backendNodeId
            
            // Get snapshot data
            val snap = if (backendId != null) trees.snapshotByBackendId[backendId] else null
            
            // Get AX data
            val ax = if (backendId != null) trees.axByBackendId[backendId] else null
            
            // Calculate scroll ability and visibility
            val isScrollable = ScrollUtils.isActuallyScrollable(
                node.copy(snapshotNode = snap)
            )
            
            // Calculate XPath
            val xPath = XPathUtils.generateXPath(node, ancestors, siblingMap)
            
            // Calculate hashes
            val parentBranchHash = if (ancestors.isNotEmpty()) {
                HashUtils.parentBranchHash(ancestors)
            } else {
                null
            }
            val elementHash = HashUtils.elementHash(node, parentBranchHash)
            
            // Merge children recursively
            val newAncestors = ancestors + node
            val mergedChildren = node.children.map { merge(it, newAncestors) }
            val mergedShadowRoots = node.shadowRoots.map { merge(it, newAncestors) }
            val mergedContentDocument = node.contentDocument?.let { merge(it, newAncestors) }
            
            return node.copy(
                snapshotNode = snap,
                axNode = ax,
                isScrollable = isScrollable,
                xPath = xPath,
                elementHash = elementHash,
                parentBranchHash = parentBranchHash,
                children = mergedChildren,
                shadowRoots = mergedShadowRoots,
                contentDocument = mergedContentDocument
            )
        }
        
        val merged = merge(trees.domTree, emptyList())
        lastEnhancedRoot = merged
        return merged
    }

    override fun buildSimplifiedTree(root: EnhancedDOMTreeNode): SimplifiedNode {
        fun simplify(node: EnhancedDOMTreeNode): SimplifiedNode {
            val simplifiedChildren = node.children.map { simplify(it) }
            
            return SimplifiedNode(
                originalNode = node,
                children = simplifiedChildren,
                shouldDisplay = node.nodeType == NodeType.ELEMENT_NODE ||
                               node.nodeType == NodeType.TEXT_NODE,
                interactiveIndex = node.interactiveIndex
            )
        }
        
        return simplify(root)
    }

    override fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String>): String {
        return DomLLMSerializer.serialize(root, includeAttributes)
    }

    override fun findElement(ref: ElementRefCriteria): EnhancedDOMTreeNode? {
        val root = lastEnhancedRoot ?: return null
        
        // Try element hash first (fastest)
        ref.elementHash?.let { hash ->
            var found: EnhancedDOMTreeNode? = null
            fun dfs(n: EnhancedDOMTreeNode) {
                if (found != null) return
                if (n.elementHash == hash) {
                    found = n
                    return
                }
                n.children.forEach { dfs(it) }
                n.shadowRoots.forEach { dfs(it) }
                n.contentDocument?.let { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }
        
        // Try XPath
        ref.xPath?.let { xpath ->
            var found: EnhancedDOMTreeNode? = null
            fun dfs(n: EnhancedDOMTreeNode) {
                if (found != null) return
                if (n.xPath == xpath) {
                    found = n
                    return
                }
                n.children.forEach { dfs(it) }
                n.shadowRoots.forEach { dfs(it) }
                n.contentDocument?.let { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }
        
        // Try backend node ID
        ref.backendNodeId?.let { backendId ->
            var found: EnhancedDOMTreeNode? = null
            fun dfs(n: EnhancedDOMTreeNode) {
                if (found != null) return
                if (n.backendNodeId == backendId) {
                    found = n
                    return
                }
                n.children.forEach { dfs(it) }
                n.shadowRoots.forEach { dfs(it) }
                n.contentDocument?.let { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }
        
        // Try CSS selector (simple cases only)
        ref.cssSelector?.let { selector ->
            // Simple selector matching (tag, #id, .class)
            val tagRegex = Regex("^[a-zA-Z0-9]+")
            val idRegex = Regex("#([a-zA-Z0-9_-]+)")
            val classRegex = Regex("\\.([a-zA-Z0-9_-]+)")
            
            val tag = tagRegex.find(selector)?.value?.lowercase()
            val id = idRegex.find(selector)?.groupValues?.getOrNull(1)
            val classes = classRegex.findAll(selector).map { it.groupValues[1] }.toSet()
            
            fun matches(n: EnhancedDOMTreeNode): Boolean {
                if (tag != null && !n.nodeName.equals(tag, ignoreCase = true)) return false
                if (id != null && n.attributes["id"] != id) return false
                if (classes.isNotEmpty()) {
                    val nodeClasses = n.attributes["class"]?.split(Regex("\\s+"))?.toSet() ?: emptySet()
                    if (!classes.all { it in nodeClasses }) return false
                }
                return true
            }
            
            var found: EnhancedDOMTreeNode? = null
            fun dfs(n: EnhancedDOMTreeNode) {
                if (found != null) return
                if (matches(n)) {
                    found = n
                    return
                }
                n.children.forEach { dfs(it) }
                n.shadowRoots.forEach { dfs(it) }
                n.contentDocument?.let { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }
        
        return null
    }

    override fun toInteractedElement(node: EnhancedDOMTreeNode): DOMInteractedElement {
        return DOMInteractedElement(
            elementHash = node.elementHash ?: HashUtils.simpleElementHash(node),
            xPath = node.xPath,
            bounds = node.snapshotNode?.clientRects,
            isVisible = node.isVisible,
            isInteractable = node.isInteractable
        )
    }
    
    /**
     * Build a map of node ID to ancestors for efficient path calculation.
     */
    private fun buildAncestorMap(root: EnhancedDOMTreeNode): Map<Int, List<EnhancedDOMTreeNode>> {
        val map = mutableMapOf<Int, List<EnhancedDOMTreeNode>>()
        
        fun traverse(node: EnhancedDOMTreeNode, ancestors: List<EnhancedDOMTreeNode>) {
            map[node.nodeId] = ancestors
            val newAncestors = ancestors + node
            node.children.forEach { traverse(it, newAncestors) }
            node.shadowRoots.forEach { traverse(it, newAncestors) }
            node.contentDocument?.let { traverse(it, newAncestors) }
        }
        
        traverse(root, emptyList())
        return map
    }
    
    /**
     * Build a map of parent node ID to children for index calculation.
     */
    private fun buildSiblingMap(root: EnhancedDOMTreeNode): Map<Int, List<EnhancedDOMTreeNode>> {
        val map = mutableMapOf<Int, List<EnhancedDOMTreeNode>>()
        
        fun traverse(node: EnhancedDOMTreeNode) {
            if (node.children.isNotEmpty()) {
                map[node.nodeId] = node.children
            }
            node.children.forEach { traverse(it) }
            node.shadowRoots.forEach { traverse(it) }
            node.contentDocument?.let { traverse(it) }
        }
        
        traverse(root)
        return map
    }
}

/**
 * Convert CDP AXNode to EnhancedAXNode.
 */
private fun AXNode.toEnhanced(): EnhancedAXNode {
    val props = properties?.mapNotNull { prop ->
        try {
            EnhancedAXProperty(
                name = prop.name.toString(),
                value = prop.value?.value
            )
        } catch (e: Exception) {
            null
        }
    }
    
    return EnhancedAXNode(
        axNodeId = nodeId,
        ignored = ignored ?: false,
        role = role?.value?.toString(),
        name = name?.value?.toString(),
        description = description?.value?.toString(),
        properties = props,
        childIds = childIds,
        backendNodeId = backendDOMNodeId,
        frameId = frameId
    )
}
