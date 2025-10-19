package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.*

/**
 * Debugging helpers to summarize core DOM types for quick inspection.
 */
object DomDebug {
    // Basic stats for a tree
    data class TreeStats(
        val depth: Int,
        val nodes: Int,
        val leaves: Int
    ) {
        override fun toString(): String = "depth=$depth, nodes=$nodes, leaves=$leaves"
    }

    // ----- Stats calculators -----

    fun stats(root: DOMTreeNodeEx): TreeStats {
        var maxDepth = 0
        var count = 0
        var leaves = 0
        fun dfs(n: DOMTreeNodeEx, d: Int) {
            count++
            if (d > maxDepth) maxDepth = d
            val totalChildren = n.children.size + n.shadowRoots.size + if (n.contentDocument != null) 1 else 0
            if (totalChildren == 0) leaves++
            n.children.forEach { dfs(it, d + 1) }
            n.shadowRoots.forEach { dfs(it, d + 1) }
            n.contentDocument?.let { dfs(it, d + 1) }
        }
        dfs(root, 1)
        return TreeStats(maxDepth, count, leaves)
    }

    fun stats(root: TinyNode): TreeStats {
        var maxDepth = 0
        var count = 0
        var leaves = 0
        fun dfs(n: TinyNode, d: Int) {
            count++
            if (d > maxDepth) maxDepth = d
            if (n.children.isEmpty()) leaves++
            n.children.forEach { dfs(it, d + 1) }
        }
        dfs(root, 1)
        return TreeStats(maxDepth, count, leaves)
    }

    // ----- Summaries -----

    fun summarize(trees: TargetMultiTrees): String {
        val s = stats(trees.domTree)
        return buildString {
            appendLine("TargetAllTrees")
            appendLine("- devicePixelRatio=${trees.devicePixelRatio}")
            appendLine("- timingsMs=${trees.cdpTiming}")
            appendLine("- options=${trees.options}")
            appendLine("- axTree.size=${trees.axTree.size}")
            appendLine("- snapshotByBackendId.size=${trees.snapshotByBackendId.size}")
            appendLine("- domByBackendId.size=${trees.domByBackendId.size}")
            appendLine("- domTree.stats=($s)")
        }
    }

    fun summarize(node: DOMTreeNodeEx, includeTreeStats: Boolean = true): String {
        val attrs = node.attributes
        val id = attrs["id"]?.let { "#${it}" } ?: ""
        val klass = attrs["class"]?.let { "." + it.split(Regex("\\s+")).take(2).joinToString(".") } ?: ""
        val label = (node.nodeName.ifBlank { "?" } + id + klass).trim()
        val hashShort = node.elementHash?.take(12) ?: ""
        val xPathShort = node.xpath?.takeLast(40) ?: ""
        val counts =
            if (includeTreeStats) stats(node).toString() else "children=${node.children.size} shadowRoots=${node.shadowRoots.size} contentDocument=${node.contentDocument != null}"
        return buildString {
            appendLine("DOMTreeNodeEx")
            appendLine("- nodeId=${node.nodeId} backendId=${node.backendNodeId} type=${node.nodeType} name=${node.nodeName}")
            appendLine("- label=$label")
            appendLine("- xPath=${xPathShort}")
            appendLine("- elementHash=${hashShort}")
            appendLine("- scrollable=${node.isScrollable} visible=${node.isVisible} interactable=${node.isInteractable} index=${node.interactiveIndex}")
            appendLine("- bounds=${node.snapshotNode?.clientRects ?: node.absolutePosition}")
            appendLine("- ${counts}")
        }
    }

    fun summarize(el: DOMInteractedElement): String {
        val hashShort = el.elementHash.take(12)
        val xPathShort = el.xPath?.takeLast(60)
        return buildString {
            appendLine("DOMInteractedElement")
            appendLine("- elementHash=${hashShort}")
            appendLine("- xPath=${xPathShort}")
            appendLine("- visible=${el.isVisible} interactable=${el.isInteractable}")
            appendLine("- bounds=${el.bounds}")
        }
    }

    fun summarize(root: TinyNode): String {
        val s = stats(root)
        val original = root.originalNode
        val hashShort = original.elementHash?.take(12)
        return buildString {
            appendLine("SlimNode")
            appendLine("- from nodeId=${original.nodeId} name=${original.nodeName} hash=${hashShort}")
            appendLine("- shouldDisplay=${root.shouldDisplay} interactiveIndex=${root.interactiveIndex}")
            appendLine("- stats=($s)")
        }
    }

    fun summarize(state: DOMState): String {
        val totalEntries = state.selectorMap.size
        val keys = state.selectorMap.keys
        val xpathKeys = keys.count { it.startsWith("xpath:") }
        val backendKeys = keys.count { it.startsWith("backend:") }
        val nodeKeys = keys.count { it.startsWith("node:") }
        val hashKeys = totalEntries - xpathKeys - backendKeys - nodeKeys

        // Deduplicate nodes by nodeId to avoid double counting
        val uniqueNodes = state.selectorMap.values.distinctBy { it.nodeId }
        val uniqueCount = uniqueNodes.size

        val interactable = uniqueNodes.count { it.isInteractable == true }
        val visible = uniqueNodes.count { it.isVisible == true }
        val scrollable = uniqueNodes.count { it.isScrollable == true }
        val withXPath = uniqueNodes.count { !it.xpath.isNullOrBlank() }
        val withHash = uniqueNodes.count { !it.elementHash.isNullOrBlank() }
        val withSnapshot = uniqueNodes.count { it.snapshotNode != null }
        val withBounds = uniqueNodes.count { it.snapshotNode?.clientRects != null || it.snapshotNode?.absoluteBounds != null || it.absolutePosition != null }

        // Build a small sample label list
        fun labelOf(n: DOMTreeNodeEx): String {
            val attrs = n.attributes
            val id = attrs["id"]?.let { "#${it}" } ?: ""
            val klass = attrs["class"]?.let { "." + it.split(Regex("\\s+")).take(2).joinToString(".") } ?: ""
            val name = n.nodeName.ifBlank { "?" }
            return (name + id + klass).trim()
        }
        val sample = uniqueNodes.take(3).joinToString(", ") { labelOf(it) }

        return buildString {
            appendLine("DOMState")
            appendLine("- json.length=${state.json.length}")
            appendLine("- selectorMap.entries=${totalEntries}")
            appendLine("- selectorMap.uniqueNodes=${uniqueCount} (hashKeys=${hashKeys}, xpathKeys=${xpathKeys}, backendKeys=${backendKeys}, nodeKeys=${nodeKeys})")
            appendLine("- nodes: interactable=${interactable}, visible=${visible}, scrollable=${scrollable}, withXPath=${withXPath}, withHash=${withHash}, withSnapshot=${withSnapshot}, withBounds=${withBounds}")
            if (sample.isNotBlank()) appendLine("- sample=${sample}")
        }
    }
}
