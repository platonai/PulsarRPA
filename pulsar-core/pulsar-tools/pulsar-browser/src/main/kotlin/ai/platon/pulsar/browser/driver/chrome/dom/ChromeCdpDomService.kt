package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.AccessibilityHandler.AccessibilityTreeResult
import ai.platon.pulsar.browser.driver.chrome.dom.model.*
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode
import java.nio.file.Files
import java.time.Instant
import kotlin.math.abs

/**
 * CDP-backed implementation of DomService using RemoteDevTools.
 */
class ChromeCdpDomService(
    private val devTools: RemoteDevTools,
) : DomService {
    private val logger = getLogger(this)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }

    private val accessibility = AccessibilityHandler(devTools)
    private val domTree = DomTreeHandler(devTools)
    private val snapshot = DomSnapshotHandler(devTools)

    @Volatile
    private var lastEnhancedRoot: DOMTreeNodeEx? = null

    @Volatile
    private var lastAncestorMap: Map<Int, List<DOMTreeNodeEx>> = emptyMap()

    @Volatile
    private var lastDomByBackend: Map<Int, DOMTreeNodeEx> = emptyMap()

    override fun getAllTrees(target: PageTarget, options: SnapshotOptions): TargetAllTrees {
        val startTime = System.currentTimeMillis()
        val timings = mutableMapOf<String, Long>()

        // Fetch AX tree (resilient)
        val axResult: AccessibilityTreeResult = if (options.includeAX) {
            val axStart = System.currentTimeMillis()
            val result = runCatching {
                accessibility.getFullAXTreeRecursive(target.frameId, depth = null)
            }.onFailure { e ->
                logger.warn("AX tree collection failed | frameId={} | err={}", target.frameId, e.toString())
                tracer?.debug("AX tree exception", e)
            }.getOrDefault(AccessibilityTreeResult.EMPTY)
            timings["ax_tree"] = System.currentTimeMillis() - axStart
            result
        } else AccessibilityTreeResult.EMPTY

        // Fetch DOM tree (resilient)
        val domStart = System.currentTimeMillis()
        val dom = runCatching { domTree.getDocument(target, options.maxDepth) }
            .onFailure { e ->
                logger.warn("DOM tree collection failed | frameId={} | err={}", target.frameId, e.toString())
                tracer?.debug("DOM tree exception", e)
            }
            .getOrElse { DOMTreeNodeEx() }
        val domByBackend = runCatching { domTree.lastBackendNodeLookup() }.getOrDefault(emptyMap())
        timings["dom_tree"] = System.currentTimeMillis() - domStart

        // Fetch snapshot (resilient)
        val snapshotStart = System.currentTimeMillis()
        val snapshotByBackendId = if (options.includeSnapshot) {
            runCatching {
                snapshot.captureEnhanced(
                    includeStyles = options.includeStyles,
                    includePaintOrder = options.includePaintOrder,
                    includeDomRects = options.includeDOMRects,
                    includeAbsoluteCoords = true // absolute coordinates help analysis
                )
            }.onFailure { e ->
                logger.warn("Snapshot collection failed | err={} ", e.toString())
                tracer?.debug("Snapshot exception", e)
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }
        timings["snapshot"] = System.currentTimeMillis() - snapshotStart

        val devicePixelRatio = getDevicePixelRatio()

        // Build AX mappings
        val enhancedAx = axResult.nodes.map { it.toEnhanced() }
        val axByBackendId: Map<Int, AXNodeEx> = buildMap {
            axResult.nodesByBackendNodeId.forEach { (backendId, nodes) ->
                val first = nodes.firstOrNull() ?: return@forEach
                put(backendId, first.toEnhanced())
            }
        }
        val axTreeByFrame: Map<String, List<AXNodeEx>> = axResult.nodesByFrameId.mapValues { (_, list) ->
            list.map { it.toEnhanced() }
        }

        timings["total"] = System.currentTimeMillis() - startTime

        tracer?.debug(
            "Trees collected | axNodes={} snapEntries={} dpr={} timingsMs={} ",
            enhancedAx.size, snapshotByBackendId.size, devicePixelRatio, timings
        )

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

    override fun buildEnhancedDomTree(trees: TargetAllTrees): DOMTreeNodeEx {
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
        fun merge(
            node: DOMTreeNodeEx,
            ancestors: List<DOMTreeNodeEx>,
            depth: Int = 0
        ): DOMTreeNodeEx {
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
            val xPath = runCatching { XPathUtils.generateXPath(node, ancestors, siblingMap) }
                .onFailure { e ->
                    tracer?.debug(
                        "XPath generation failed | nodeId={} | err={} ",
                        node.nodeId,
                        e.toString()
                    )
                }
                .getOrElse { null }

            // Calculate hashes with enhanced logic
            val parentBranchHash = if (ancestors.isNotEmpty()) {
                runCatching { HashUtils.parentBranchHash(ancestors) }
                    .onFailure { e ->
                        tracer?.debug(
                            "Parent branch hash failed | nodeId={} | err={} ",
                            node.nodeId,
                            e.toString()
                        )
                    }
                    .getOrNull()
            } else {
                null
            }
            val elementHash = runCatching { HashUtils.elementHash(node, parentBranchHash) }
                .onFailure { e ->
                    tracer?.debug(
                        "Element hash failed | nodeId={} | err={} ",
                        node.nodeId,
                        e.toString()
                    )
                }
                .getOrNull()

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

    override fun buildSimplifiedTree(root: DOMTreeNodeEx): SlimNode {
        fun simplify(node: DOMTreeNodeEx): SlimNode {
            val simplifiedChildren = node.children.map { simplify(it) }

            return SlimNode(
                originalNode = node,
                children = simplifiedChildren,
                shouldDisplay = node.nodeType == NodeType.ELEMENT_NODE ||
                        node.nodeType == NodeType.TEXT_NODE,
                interactiveIndex = node.interactiveIndex
            )
        }

        return simplify(root)
    }

    override fun buildSlimNodeTree(): SlimNode {
        val trees = getAllTrees()
        return buildSlimNodeTree(trees)
    }

    override fun buildSlimNodeTree(trees: TargetAllTrees): SlimNode {
        val enhanced = buildEnhancedDomTree(trees)
        val hasElements = enhanced.children.isNotEmpty() ||
                enhanced.shadowRoots.isNotEmpty() ||
                enhanced.contentDocument != null
        val simplified = buildSimplifiedTree(enhanced)

        if (!hasElements) {
            // Write a lightweight diagnostic to help root cause empty DOM
            runCatching {
                val diagnostics = mapOf(
                    "timestamp" to Instant.now().toString(),
                    "reason" to "Empty DOM tree collected",
                    "devicePixelRatio" to trees.devicePixelRatio,
                    "axNodeCount" to trees.axTree.size,
                    "snapshotEntryCount" to trees.snapshotByBackendId.size,
                    "timingsMs" to trees.cdpTiming
                )
                val dir = AppPaths.get("logs", "chat-model").toFile()
                if (!dir.exists()) dir.mkdirs()
                val out = dir.resolve("domservice-diagnostics.json")
                Files.writeString(out.toPath(), diagnostics.toString() + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)
            }.onFailure { e -> logger.warn("Failed to write DOM diagnostics | {}", e.toString()) }

            throw IllegalStateException("Empty DOM tree collected (AX=${trees.axTree.size}, SNAP=${trees.snapshotByBackendId.size}). See logs/chat-model/domservice-diagnostics.json")
        }

        return simplified
    }

    override fun serialize(root: SlimNode, includeAttributes: List<String>): DomLLMSerialization {
        // Use enhanced serialization with default options
        val options = DomSerializer.SerializationOptions(
            enablePaintOrderPruning = true,
            enableCompoundComponentDetection = true,
            enableAttributeCasingAlignment = true
        )
        return DomSerializer.serialize(root, includeAttributes, options)
    }

    override fun findElement(ref: ElementRefCriteria): DOMTreeNodeEx? {
        val root = lastEnhancedRoot ?: return null

        // Try element hash first (fastest)
        ref.elementHash?.let { hash ->
            var found: DOMTreeNodeEx? = null
            fun dfs(n: DOMTreeNodeEx) {
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
            var found: DOMTreeNodeEx? = null
            fun dfs(n: DOMTreeNodeEx) {
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
            var found: DOMTreeNodeEx? = null
            fun dfs(n: DOMTreeNodeEx) {
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

            fun matches(n: DOMTreeNodeEx): Boolean {
                if (tag != null && !n.nodeName.equals(tag, ignoreCase = true)) return false
                if (id != null && n.attributes["id"] != id) return false
                if (classes.isNotEmpty()) {
                    val nodeClasses = n.attributes["class"]?.split(Regex("\\s+"))?.toSet() ?: emptySet()
                    if (!classes.all { it in nodeClasses }) return false
                }
                return true
            }

            var found: DOMTreeNodeEx? = null
            fun dfs(n: DOMTreeNodeEx) {
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

    override fun toInteractedElement(node: DOMTreeNodeEx): DOMInteractedElement {
        return DOMInteractedElement(
            elementHash = node.elementHash ?: HashUtils.simpleElementHash(node),
            xPath = node.xPath,
            bounds = node.snapshotNode?.clientRects,
            isVisible = node.isVisible,
            isInteractable = node.isInteractable
        )
    }

    private fun computeVisibility(node: DOMTreeNodeEx): Boolean? {
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

    private fun computeInteractivity(node: DOMTreeNodeEx): Boolean? {
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
            logger.debug("Device pixel ratio fallback | err={}", e.toString())
            1.0
        }
    }

    /**
     * Build a map of node ID to ancestors for efficient path calculation.
     */
    private fun buildAncestorMap(root: DOMTreeNodeEx): Map<Int, List<DOMTreeNodeEx>> {
        val map = mutableMapOf<Int, List<DOMTreeNodeEx>>()

        fun traverse(node: DOMTreeNodeEx, ancestors: List<DOMTreeNodeEx>) {
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
    private fun buildSiblingMap(root: DOMTreeNodeEx): Map<Int, List<DOMTreeNodeEx>> {
        val map = mutableMapOf<Int, List<DOMTreeNodeEx>>()

        fun traverse(node: DOMTreeNodeEx) {
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
    private fun buildPaintOrderMap(snapshotByBackendId: Map<Int, SnapshotNodeEx>): Map<Int, Int?> {
        return snapshotByBackendId.mapValues { (_, snapshot) -> snapshot.paintOrder }
    }

    /**
     * Build stacking context map from snapshot data for z-index analysis.
     */
    private fun buildStackingContextMap(snapshotByBackendId: Map<Int, SnapshotNodeEx>): Map<Int, Int?> {
        return snapshotByBackendId.mapValues { (_, snapshot) -> snapshot.stackingContexts }
    }

    /**
     * Calculate scrollability with enhanced logic covering iframe/body/html and nested containers.
     */
    private fun calculateScrollability(
        node: DOMTreeNodeEx,
        snap: SnapshotNodeEx?,
        ancestors: List<DOMTreeNodeEx>
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
        node: DOMTreeNodeEx,
        snap: SnapshotNodeEx?,
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
        node: DOMTreeNodeEx,
        snap: SnapshotNodeEx?,
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
        snap: SnapshotNodeEx,
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
private fun AXNode.toEnhanced(): AXNodeEx {
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

    return AXNodeEx(
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
