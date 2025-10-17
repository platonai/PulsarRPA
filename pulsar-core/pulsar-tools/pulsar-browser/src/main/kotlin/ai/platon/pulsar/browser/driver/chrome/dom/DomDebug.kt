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

    fun stats(root: SlimNode): TreeStats {
        var maxDepth = 0
        var count = 0
        var leaves = 0
        fun dfs(n: SlimNode, d: Int) {
            count++
            if (d > maxDepth) maxDepth = d
            if (n.children.isEmpty()) leaves++
            n.children.forEach { dfs(it, d + 1) }
        }
        dfs(root, 1)
        return TreeStats(maxDepth, count, leaves)
    }

    // ----- Summaries -----

    fun summarize(trees: TargetDetailTrees): String {
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
        val xPathShort = node.xPath?.takeLast(40) ?: ""
        val counts = if (includeTreeStats) stats(node).toString() else "children=${node.children.size} shadowRoots=${node.shadowRoots.size} contentDocument=${node.contentDocument != null}"
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

    fun summarize(root: SlimNode): String {
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
}

