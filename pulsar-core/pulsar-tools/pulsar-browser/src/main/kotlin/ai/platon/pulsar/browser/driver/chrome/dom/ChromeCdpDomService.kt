package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler
import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler.AccessibilityTreeResult
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.*
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXProperty
import kotlin.jvm.Volatile
import kotlin.math.abs

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

    @Volatile
    private var lastDomByBackend: Map<Int, EnhancedDOMTreeNode> = emptyMap()

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
        val dom = domTree.getDocument(target, options.maxDepth)
        val domByBackend = domTree.lastBackendNodeLookup()
        timings["dom_tree"] = System.currentTimeMillis() - domStart
        
        // Fetch snapshot
        val snapshotStart = System.currentTimeMillis()
        val snapshotByBackendId = if (options.includeSnapshot) {
            snapshot.captureEnhanced(
                includeStyles = options.includeStyles,
                includePaintOrder = options.includePaintOrder,
                includeDomRects = options.includeDOMRects,
                includeAbsoluteCoords = true // Always include absolute coordinates for better analysis
            )
        } else {
            emptyMap()
        }
        timings["snapshot"] = System.currentTimeMillis() - snapshotStart

        val devicePixelRatio = getDevicePixelRatio()

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
            devicePixelRatio = devicePixelRatio,
            cdpTiming = timings,
            options = options,
            domByBackendId = domByBackend
        )
    }

    override fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode {
        val options = trees.options
        // Build ancestor map for XPath and hash generation
        val ancestorMap = buildAncestorMap(trees.domTree)
        lastAncestorMap = ancestorMap
        lastDomByBackend = trees.domByBackendId

        // Build sibling map for XPath index calculation
        val siblingMap = buildSiblingMap(trees.domTree)

        // Build paint order map for interaction index calculation
        val paintOrderMap = buildPaintOrderMap(trees.snapshotByBackendId)

        // Build stacking context map for z-index analysis
        val stackingContextMap = buildStackingContextMap(trees.snapshotByBackendId)

        // Merge trees recursively with enhanced metrics
        fun merge(node: EnhancedDOMTreeNode, ancestors: List<EnhancedDOMTreeNode>, depth: Int = 0): EnhancedDOMTreeNode {
            val backendId = node.backendNodeId

            // Get snapshot data
            val snap = if (options.includeSnapshot && backendId != null) trees.snapshotByBackendId[backendId] else null

            // Get AX data
            val ax = if (options.includeAX && backendId != null) trees.axByBackendId[backendId] else null

            // Calculate enhanced metrics
            val evaluatedNode = node.copy(snapshotNode = snap, axNode = ax)

            // Calculate scroll ability with enhanced logic
            val isScrollable = if (options.includeScrollAnalysis) {
                calculateScrollability(evaluatedNode, snap, ancestors)
            } else null

            // Calculate visibility with stacking context consideration
            val isVisible = if (options.includeVisibility) {
                calculateVisibility(evaluatedNode, snap, stackingContextMap[backendId])
            } else null

            // Calculate interactivity with paint order
            val isInteractable = if (options.includeInteractivity) {
                calculateInteractivity(evaluatedNode, snap, paintOrderMap[backendId])
            } else null

            // Calculate interactive index based on paint order and stacking context
            val interactiveIndex = if (options.includeInteractivity && snap?.paintOrder != null) {
                calculateInteractiveIndex(snap, stackingContextMap[backendId], paintOrderMap[backendId])
            } else null

            // Calculate absolute position from snapshot absolute bounds
            val absolutePosition = snap?.absoluteBounds

            // Calculate XPath
            val xPath = XPathUtils.generateXPath(node, ancestors, siblingMap)

            // Calculate hashes with enhanced logic
            val parentBranchHash = if (ancestors.isNotEmpty()) {
                HashUtils.parentBranchHash(ancestors)
            } else {
                null
            }
            val elementHash = HashUtils.elementHash(node, parentBranchHash)

            // Merge children recursively with depth tracking
            val merged = node.copy(
                snapshotNode = snap,
                axNode = ax,
                isScrollable = isScrollable,
                isVisible = isVisible,
                isInteractable = isInteractable,
                interactiveIndex = interactiveIndex,
                absolutePosition = absolutePosition,
                xPath = xPath,
                elementHash = elementHash,
                parentBranchHash = parentBranchHash
            )
            val newAncestors = ancestors + merged
            val mergedChildren = node.children.map { merge(it, newAncestors, depth + 1) }
            val mergedShadowRoots = node.shadowRoots.map { merge(it, newAncestors, depth + 1) }
            val mergedContentDocument = node.contentDocument?.let { merge(it, newAncestors, depth + 1) }

            return merged.copy(
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

    override fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String>): DomLLMSerialization {
        // Use enhanced serialization with default options
        val options = DomLLMSerializer.SerializationOptions(
            enablePaintOrderPruning = true,
            enableCompoundComponentDetection = true,
            enableAttributeCasingAlignment = true
        )
        return DomLLMSerializer.serialize(root, includeAttributes, options)
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
            lastDomByBackend[backendId]?.let { return it }
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

    private fun computeVisibility(node: EnhancedDOMTreeNode): Boolean? {
        val snapshot = node.snapshotNode ?: return null
        val styles = snapshot.computedStyles ?: return null
        val display = styles["display"]
        if (display != null && display.equals("none", ignoreCase = true)) return false
        val visibility = styles["visibility"]
        if (visibility != null && visibility.equals("hidden", ignoreCase = true)) return false
        val opacity = styles["opacity"]?.toDoubleOrNull()
        if (opacity != null && opacity <= 0.0) return false
        val pointerEvents = styles["pointer-events"]
        if (pointerEvents != null && pointerEvents.equals("none", ignoreCase = true)) return false
        return true
    }

    private fun computeInteractivity(node: EnhancedDOMTreeNode): Boolean? {
        val snapshot = node.snapshotNode
        if (snapshot?.isClickable == true) {
            return true
        }
        val tag = node.nodeName.uppercase()
        if (tag in setOf("BUTTON", "A", "INPUT", "SELECT", "TEXTAREA", "OPTION")) {
            return true
        }
        val role = node.axNode?.role
        if (role != null && role.lowercase() in setOf("button", "link", "checkbox", "textbox", "combobox")) {
            return true
        }
        return snapshot?.cursorStyle?.equals("pointer", ignoreCase = true)
    }

    private fun getDevicePixelRatio(): Double {
        return try {
            val evaluation = devTools.runtime.evaluate("window.devicePixelRatio")
            val result = evaluation?.result
            val numeric = result?.value?.toString()?.toDoubleOrNull()
            numeric ?: result?.unserializableValue?.toDoubleOrNull() ?: 1.0
        } catch (e: Exception) {
            1.0
        }
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

    /**
     * Build paint order map from snapshot data for interaction index calculation.
     */
    private fun buildPaintOrderMap(snapshotByBackendId: Map<Int, EnhancedSnapshotNode>): Map<Int, Int?> {
        return snapshotByBackendId.mapValues { (_, snapshot) -> snapshot.paintOrder }
    }

    /**
     * Build stacking context map from snapshot data for z-index analysis.
     */
    private fun buildStackingContextMap(snapshotByBackendId: Map<Int, EnhancedSnapshotNode>): Map<Int, Int?> {
        return snapshotByBackendId.mapValues { (_, snapshot) -> snapshot.stackingContexts }
    }

    /**
     * Calculate scrollability with enhanced logic covering iframe/body/html and nested containers.
     */
    private fun calculateScrollability(
        node: EnhancedDOMTreeNode,
        snap: EnhancedSnapshotNode?,
        ancestors: List<EnhancedDOMTreeNode>
    ): Boolean? {
        if (snap == null) return null

        // Use existing ScrollUtils for basic scrollability detection
        val basicScrollable = ScrollUtils.isActuallyScrollable(node)
        if (!basicScrollable) return false

        // Enhanced logic for iframe/body/html special cases
        val tagName = node.nodeName.lowercase()
        val isSpecialElement = tagName in setOf("iframe", "body", "html")

        if (isSpecialElement) {
            // For special elements, check if they have meaningful scrollable content
            val scrollHeight = snap.scrollRects?.height ?: 0.0
            val clientHeight = snap.clientRects?.height ?: 0.0
            return scrollHeight > clientHeight + 1 // Allow 1px tolerance
        }

        // For nested containers, check for duplicate scrollability in ancestors
        val hasScrollableAncestor = ancestors.any { ancestor ->
            ancestor.isScrollable == true && ancestor.snapshotNode?.scrollRects != null
        }

        // If parent is already scrollable, this might be a nested scrollable that should be deduplicated
        return if (hasScrollableAncestor) {
            // Only mark as scrollable if it has significantly different scroll properties
            val ancestorScrollAreas = ancestors
                .filter { it.isScrollable == true }
                .mapNotNull { it.snapshotNode?.scrollRects }
            val currentScrollArea = snap.scrollRects ?: return basicScrollable

            // Check if current element has significantly different scroll area
            ancestorScrollAreas.none { ancestorArea ->
                abs(ancestorArea.x - currentScrollArea.x) < 5 &&
                abs(ancestorArea.y - currentScrollArea.y) < 5 &&
                abs(ancestorArea.width - currentScrollArea.width) < 5 &&
                abs(ancestorArea.height - currentScrollArea.height) < 5
            }
        } else {
            basicScrollable
        }
    }

    /**
     * Calculate visibility with stacking context consideration.
     */
    private fun calculateVisibility(
        node: EnhancedDOMTreeNode,
        snap: EnhancedSnapshotNode?,
        stackingContext: Int?
    ): Boolean? {
        if (snap == null) return null

        // Basic visibility checks from computed styles
        val styles = snap.computedStyles ?: return null
        val display = styles["display"]
        if (display != null && display.equals("none", ignoreCase = true)) return false
        val visibility = styles["visibility"]
        if (visibility != null && visibility.equals("hidden", ignoreCase = true)) return false
        val opacity = styles["opacity"]?.toDoubleOrNull()
        if (opacity != null && opacity <= 0.0) return false
        val pointerEvents = styles["pointer-events"]
        if (pointerEvents != null && pointerEvents.equals("none", ignoreCase = true)) return false

        // Consider stacking context - elements in higher stacking contexts may obscure lower ones
        // For now, just return true if basic checks pass
        // TODO: Implement more sophisticated stacking context analysis
        return true
    }

    /**
     * Calculate interactivity with paint order consideration.
     */
    private fun calculateInteractivity(
        node: EnhancedDOMTreeNode,
        snap: EnhancedSnapshotNode?,
        paintOrder: Int?
    ): Boolean? {
        if (snap == null) return null

        // Check if node is clickable based on cursor style
        if (snap.isClickable == true) return true

        // Check interactivity based on node type and attributes
        val tag = node.nodeName.uppercase()
        if (tag in setOf("BUTTON", "A", "INPUT", "SELECT", "TEXTAREA", "OPTION")) {
            return true
        }

        // Check AX role for interactivity
        val role = node.axNode?.role
        if (role != null && role.lowercase() in setOf("button", "link", "checkbox", "textbox", "combobox")) {
            return true
        }

        // Check cursor style
        return snap.cursorStyle?.equals("pointer", ignoreCase = true)
    }

    /**
     * Calculate interactive index based on paint order and stacking context.
     */
    private fun calculateInteractiveIndex(
        snap: EnhancedSnapshotNode,
        stackingContext: Int?,
        paintOrder: Int?
    ): Int? {
        if (paintOrder == null) return null

        // Higher paint order means element is painted later (on top)
        // Lower stacking context values mean higher z-index
        val stackingFactor = (stackingContext ?: 0) * 1000
        return paintOrder + stackingFactor
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
