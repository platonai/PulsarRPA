package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler
import ai.platon.pulsar.browser.driver.chrome.AccessibilityHandler.AccessibilityTreeResult
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode

/**
 * CDP-backed implementation of DomService using existing RemoteDevTools wiring.
 */
class ChromeCdpDomService(
    private val devTools: RemoteDevTools,
) : DomService {

    private val accessibility = AccessibilityHandler(devTools)
    private val domTree = DomTreeHandler(devTools)
    private val snapshot = DomSnapshotHandler(devTools)
    @Volatile private var lastEnhancedRoot: EnhancedDOMTreeNode? = null

    override fun getAllTrees(target: PageTarget, options: SnapshotOptions): TargetAllTrees {
        // M2: Fetch AX, DOM, Snapshot; collect timings.
        val axResult: AccessibilityTreeResult = if (options.includeAX) {
            accessibility.getFullAXTreeRecursive(target.frameId, depth = null)
        } else AccessibilityTreeResult.EMPTY

        val dom = domTree.getDocument(options.maxDepth)
        val snap = if (options.includeSnapshot) snapshot.capture(options.includeStyles) else emptyList()
        val snapshotByBackendId = if (options.includeSnapshot) snapshot.captureByBackendNodeId(options.includeStyles) else emptyMap()

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

        return TargetAllTrees(
            axTree = enhancedAx,
            domTree = dom,
            snapshot = snap,
            snapshotByBackendId = snapshotByBackendId,
            axByBackendId = axByBackendId,
            axTreeByFrameId = axTreeByFrame,
            devicePixelRatio = 1.0,
            cdpTiming = emptyMap(),
        )
    }

    override fun buildEnhancedDomTree(trees: TargetAllTrees): EnhancedDOMTreeNode {
        // M3: Merge snapshot data onto DOM nodes by backendNodeId (when available).
        fun merge(node: EnhancedDOMTreeNode): EnhancedDOMTreeNode {
            val snap = if (node.backendNodeId != null) trees.snapshotByBackendId[node.backendNodeId] else null
            val mergedChildren = node.children.map { merge(it) }
            return node.copy(
                bounds = snap?.bounds ?: node.bounds,
                offsetRect = snap?.offsetRect ?: node.offsetRect,
                scrollRect = snap?.scrollRect ?: node.scrollRect,
                clientRect = snap?.clientRect ?: node.clientRect,
                paintOrder = snap?.paintOrder ?: node.paintOrder,
                computedStyles = if (!snap?.style.isNullOrEmpty()) snap?.style else node.computedStyles,
                axRole = trees.axByBackendId[node.backendNodeId ?: -1]?.role ?: node.axRole,
                axName = trees.axByBackendId[node.backendNodeId ?: -1]?.name ?: node.axName,
                children = mergedChildren,
            )
        }
        val merged = merge(trees.domTree)
        lastEnhancedRoot = merged
        return merged
    }

    override fun serializeForLLM(root: SimplifiedNode, includeAttributes: List<String>): String {
        // M5: Provide JSON serialization tuned for LLM; simple JSON for now
        return DomLLMSerializer.serialize(root, includeAttributes)
    }

    override fun findElement(ref: ElementRefCriteria): EnhancedDOMTreeNode? {
        val root = lastEnhancedRoot ?: return null
        // For now, support elementHash fast path and a minimal CSS lookup for id/class tags.
        ref.elementHash?.let { h ->
            var found: EnhancedDOMTreeNode? = null
            fun dfs(n: EnhancedDOMTreeNode) {
                if (found != null) return
                if (HashUtils.elementHash(n) == h) { found = n; return }
                n.children.forEach { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }

        ref.cssSelector?.let { sel ->
            // Only handle simple selectors: tag, #id, .class, tag.class1.class2
            val tagRegex = Regex("^[a-zA-Z0-9]+")
            val idRegex = Regex("#([a-zA-Z0-9_-]+)")
            val classRegex = Regex("\\.([a-zA-Z0-9_-]+)")
            val tag = tagRegex.find(sel)?.value?.lowercase()
            val id = idRegex.find(sel)?.groupValues?.getOrNull(1)
            val classes = classRegex.findAll(sel).map { it.groupValues[1] }.toSet()

            fun matches(n: EnhancedDOMTreeNode): Boolean {
                if (tag != null && n.nodeName.lowercase() != tag) return false
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
                if (matches(n)) { found = n; return }
                n.children.forEach { dfs(it) }
            }
            dfs(root)
            if (found != null) return found
        }

        // XPath is not implemented yet
        return null
    }

    override fun toInteractedElement(node: EnhancedDOMTreeNode): DOMInteractedElement {
        // M4: element hash; xPath minimal tag/id path; bounds as clientRect if available
        val hash = HashUtils.elementHash(node)
        val boundsStr = node.clientRect?.joinToString(",")
        // Naive xPath-like representation limited to tag and id
        val xpath = buildString {
            append("//")
            append(node.nodeName.lowercase())
            node.attributes["id"]?.let { append("[@id='" + it + "']") }
        }
        return DOMInteractedElement(elementHash = hash, xPath = xpath, bounds = boundsStr)
    }
}

private fun AXNode.toEnhanced(): EnhancedAXNode = EnhancedAXNode(
    axNodeId = nodeId,
    role = role?.value?.toString(),
    name = name?.value?.toString(),
    frameId = frameId,
    backendNodeId = backendDOMNodeId
)
