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

    // Bounds stats for a tree
    data class BoundsStats(
        val zero: Int,
        val positive: Int,
        val missing: Int
    ) {
        override fun toString(): String = "zero=$zero, positive=$positive, missing=$missing"
    }

    // ----- Helpers -----

    private fun rectOf(n: DOMTreeNodeEx): DOMRect? {
        val s = n.snapshotNode
        return s?.clientRects ?: s?.absoluteBounds ?: s?.bounds ?: n.absolutePosition
    }

    private fun labelOfNode(n: DOMTreeNodeEx): String {
        val attrs = n.attributes
        val id = attrs["id"]?.let { "#${it}" } ?: ""
        val klass = attrs["class"]?.let { "." + it.split(Regex("\\s+")).take(2).joinToString(".") } ?: ""
        val name = n.nodeName.ifBlank { "?" }
        return (name + id + klass).trim()
    }

    private fun labelOfNano(n: NanoDOMTree): String {
        return buildString {
            val name = n.nodeName ?: "?"
            append(name)
            val id = (n.attributes?.get("id") as? String)?.takeIf { it.isNotBlank() }
            if (!id.isNullOrBlank()) append("#").append(id)
            val cls = (n.attributes?.get("class") as? String)
                ?.trim()?.split(Regex("\\s+")).orEmpty().take(2)
                .filter { it.isNotBlank() }
            if (cls.isNotEmpty()) append(".").append(cls.joinToString("."))
        }
    }

    private fun briefWithBounds(n: DOMTreeNodeEx): String {
        val r = rectOf(n)
        val hashShort = n.elementHash?.take(12)
        return "nodeId=${n.nodeId}, name=${n.nodeName}, label=${labelOfNode(n)}, hash=${hashShort}, bounds=${r}"
    }

    private fun briefWithBounds(n: TinyNode): String {
        val o = n.originalNode
        val r = rectOf(o)
        val hashShort = o.elementHash?.take(12)
        return "nodeId=${o.nodeId}, name=${o.nodeName}, label=${labelOfNode(o)}, hash=${hashShort}, bounds=${r}"
    }

    private fun briefWithBounds(n: NanoDOMTree): String {
        val r = n.clientRects ?: n.absoluteBounds ?: n.bounds
        return "locator=${n.locator}, label=${labelOfNano(n)}, bounds=${r}"
    }

    private fun midTwoWithBounds(root: DOMTreeNodeEx): Pair<DOMTreeNodeEx?, DOMTreeNodeEx?> {
        val eligible = mutableListOf<DOMTreeNodeEx>()
        fun dfs(n: DOMTreeNodeEx) {
            val r = rectOf(n)
            if (hasNonZeroXY(r)) eligible += n
            n.children.forEach { dfs(it) }
            n.shadowRoots.forEach { dfs(it) }
            n.contentDocument?.let { dfs(it) }
        }
        dfs(root)
        return middleTwo(eligible)
    }

    private fun midTwoWithBounds(root: TinyNode): Pair<TinyNode?, TinyNode?> {
        val eligible = mutableListOf<TinyNode>()
        fun dfs(n: TinyNode) {
            val r = rectOf(n.originalNode)
            if (hasNonZeroXY(r)) eligible += n
            n.children.forEach { dfs(it) }
        }
        dfs(root)
        return middleTwo(eligible)
    }

    private fun midTwoWithBounds(root: NanoDOMTree): Pair<NanoDOMTree?, NanoDOMTree?> {
        val eligible = mutableListOf<NanoDOMTree>()
        fun dfs(n: NanoDOMTree?) {
            if (n == null) return
            val r = n.clientRects ?: n.absoluteBounds ?: n.bounds
            if (hasNonZeroXY(r)) eligible += n
            n.children?.forEach { dfs(it) }
        }
        dfs(root)
        return middleTwo(eligible)
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

    // New: stats for NanoDOMTree
    fun stats(root: NanoDOMTree): TreeStats {
        var maxDepth = 0
        var count = 0
        var leaves = 0
        fun dfs(n: NanoDOMTree?, d: Int) {
            if (n == null) return
            count++
            if (d > maxDepth) maxDepth = d
            val children = n.children ?: emptyList()
            if (children.isEmpty()) leaves++
            children.forEach { dfs(it, d + 1) }
        }
        dfs(root, 1)
        return TreeStats(maxDepth, count, leaves)
    }

    // ----- Bounds stats calculators -----

    fun boundsStats(root: DOMTreeNodeEx): BoundsStats {
        var zero = 0
        var positive = 0
        var missing = 0
        fun dfs(n: DOMTreeNodeEx) {
            val r = rectOf(n)
            if (r == null) missing++
            else if (r.width > 0 && r.height > 0) positive++
            else zero++
            n.children.forEach { dfs(it) }
            n.shadowRoots.forEach { dfs(it) }
            n.contentDocument?.let { dfs(it) }
        }
        dfs(root)
        return BoundsStats(zero, positive, missing)
    }

    fun boundsStats(root: TinyNode): BoundsStats {
        var zero = 0
        var positive = 0
        var missing = 0
        fun dfs(n: TinyNode) {
            val o = n.originalNode
            val r = rectOf(o)
            if (r == null) missing++
            else if (r.width > 0 && r.height > 0) positive++
            else zero++
            n.children.forEach { dfs(it) }
        }
        dfs(root)
        return BoundsStats(zero, positive, missing)
    }

    // New: bounds stats for NanoDOMTree using CompactRect precedence clientRects -> absoluteBounds -> bounds
    fun boundsStats(root: NanoDOMTree): BoundsStats {
        var zero = 0
        var positive = 0
        var missing = 0
        fun toNonZero(w: Double?): Double = w ?: 0.0
        fun dfs(n: NanoDOMTree?) {
            if (n == null) return
            val r = n.clientRects ?: n.absoluteBounds ?: n.bounds
            if (r == null) {
                missing++
            } else {
                val w = toNonZero(r.width)
                val h = toNonZero(r.height)
                if (w > 0.0 && h > 0.0) positive++ else zero++
            }
            n.children?.forEach { dfs(it) }
        }
        dfs(root)
        return BoundsStats(zero, positive, missing)
    }

    // ----- Helpers (added) -----
    private fun hasNonZeroXY(r: DOMRect?): Boolean = r?.let { it.x != 0.0 || it.y != 0.0 } == true
    private fun hasNonZeroXY(r: CompactRect?): Boolean = r?.let { (it.x ?: 0.0) != 0.0 || (it.y ?: 0.0) != 0.0 } == true

    private fun <T> middleTwo(list: List<T>): Pair<T?, T?> {
        if (list.isEmpty()) return Pair(null, null)
        if (list.size == 1) return Pair(list[0], null)
        val j = (list.size - 1) / 2
        val k = minOf(j + 1, list.size - 1)
        return Pair(list[j], list[k])
    }

    // ----- Bounds sampling helpers -----

    private fun xyOf(r: DOMRect?): Pair<Double, Double>? = r?.let { it.x to it.y }
    private fun xyOf(r: CompactRect?): Pair<Double, Double>? = r?.let { (it.x ?: 0.0) to (it.y ?: 0.0) }

    private fun round1(v: Double): Double = kotlin.math.round(v * 10.0) / 10.0

    private fun formatXYList(pairs: List<Pair<Double, Double>>, limit: Int = 20): String {
        if (pairs.isEmpty()) return "[]"
        val head = pairs.take(limit)
        val body = head.joinToString(prefix = "[", postfix = "]") { p -> "(${round1(p.first)},${round1(p.second)})" }
        val remain = pairs.size - head.size
        return if (remain > 0) "$body +${remain} more" else body
    }

    private fun collectXY(root: DOMTreeNodeEx): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        fun dfs(n: DOMTreeNodeEx) {
            xyOf(rectOf(n))?.let { list += it }
            n.children.forEach { dfs(it) }
            n.shadowRoots.forEach { dfs(it) }
            n.contentDocument?.let { dfs(it) }
        }
        dfs(root)
        return list
    }

    private fun collectXY(root: TinyNode): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        fun dfs(n: TinyNode) {
            xyOf(rectOf(n.originalNode))?.let { list += it }
            n.children.forEach { dfs(it) }
        }
        dfs(root)
        return list
    }

    private fun collectXY(root: NanoDOMTree): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        fun dfs(n: NanoDOMTree?) {
            if (n == null) return
            xyOf(n.clientRects ?: n.absoluteBounds ?: n.bounds)?.let { list += it }
            n.children?.forEach { dfs(it) }
        }
        dfs(root)
        return list
    }

    private fun filterGt(pairs: List<Pair<Double, Double>>, threshold: Double): List<Pair<Double, Double>> =
        pairs.filter { it.first > threshold && it.second > threshold }

    private fun sortedGt50(pairs: List<Pair<Double, Double>>): List<Pair<Double, Double>> =
        filterGt(pairs, 50.0).sortedWith(compareBy({ it.first }, { it.second }))

    // ----- Summaries -----

    fun summarize(trees: TargetTrees): String {
        val s = stats(trees.domTree)
        val b = boundsStats(trees.domTree)
        val (mid1, mid2) = midTwoWithBounds(trees.domTree)
        // bounds sampling over the DOM tree
        val xyAll = collectXY(trees.domTree)
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)
        return buildString {
            appendLine("TargetTrees")
            appendLine("- devicePixelRatio=${trees.devicePixelRatio}")
            appendLine("- timingsMs=${trees.cdpTiming}")
            appendLine("- options=${trees.options}")
            appendLine("- axTree.size=${trees.axTree.size}")
            appendLine("- snapshotByBackendId.size=${trees.snapshotByBackendId.size}")
            appendLine("- domByBackendId.size=${trees.domByBackendId.size}")
            appendLine("- domTree.stats=($s)")
            appendLine("- domTree.boundsStats=($b)")
            appendLine("- domTree.bounds.zeroNonZero=(zero=${b.zero}, nonZero=${b.positive})")
            if (mid1 != null) appendLine("- domTree.midBoundsNode1={ ${briefWithBounds(mid1)} }")
            if (mid2 != null && mid2 !== mid1) appendLine("- domTree.midBoundsNode2={ ${briefWithBounds(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
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
        val b = if (includeTreeStats) boundsStats(node).toString() else null
        val bz = if (includeTreeStats) boundsStats(node) else null
        val midPair = if (includeTreeStats) midTwoWithBounds(node) else null
        // sampling
        val xyAll = collectXY(node)
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)
        return buildString {
            appendLine("DOMTreeNodeEx")
            appendLine("- nodeId=${node.nodeId} backendId=${node.backendNodeId} type=${node.nodeType} name=${node.nodeName}")
            appendLine("- label=$label")
            appendLine("- xPath=${xPathShort}")
            appendLine("- elementHash=${hashShort}")
            appendLine("- scrollable=${node.isScrollable} visible=${node.isVisible} interactable=${node.isInteractable} index=${node.interactiveIndex}")
            appendLine("- bounds=${node.snapshotNode?.clientRects ?: node.absolutePosition}")
            appendLine("- ${counts}")
            if (b != null) appendLine("- boundsStats=($b)")
            if (bz != null) appendLine("- bounds.zeroNonZero=(zero=${bz.zero}, nonZero=${bz.positive})")
            val (mid1, mid2) = midPair ?: Pair(null, null)
            if (mid1 != null) appendLine("- midBoundsNode1={ ${briefWithBounds(mid1)} }")
            if (mid2 != null && mid2 !== mid1) appendLine("- midBoundsNode2={ ${briefWithBounds(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }

    fun summarize(el: DOMInteractedElement): String {
        val hashShort = el.elementHash.take(12)
        val xPathShort = el.xPath?.takeLast(60)
        val xy = xyOf(el.bounds)
        val gt200 = if (xy != null && xy.first > 200.0 && xy.second > 200.0) 1 else 0
        val gt50Sorted = if (xy != null && xy.first > 50.0 && xy.second > 50.0) listOf(xy) else emptyList()
        return buildString {
            appendLine("DOMInteractedElement")
            appendLine("- elementHash=${hashShort}")
            appendLine("- xPath=${xPathShort}")
            appendLine("- visible=${el.isVisible} interactable=${el.isInteractable}")
            appendLine("- bounds=${el.bounds}")
            appendLine("- bounds.samples.gt200.count=${gt200}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }

    fun summarize(root: TinyNode): String {
        val s = stats(root)
        val b = boundsStats(root)
        val original = root.originalNode
        val hashShort = original.elementHash?.take(12)
        val (mid1, mid2) = midTwoWithBounds(root)
        val xyAll = collectXY(root)
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)
        return buildString {
            appendLine("TinyNode")
            appendLine("- from nodeId=${original.nodeId} name=${original.nodeName} hash=${hashShort}")
            appendLine("- shouldDisplay=${root.shouldDisplay} interactiveIndex=${root.interactiveIndex}")
            appendLine("- stats=($s)")
            appendLine("- boundsStats=($b)")
            appendLine("- bounds.zeroNonZero=(zero=${b.zero}, nonZero=${b.positive})")
            if (mid1 != null) appendLine("- midBoundsNode1={ ${briefWithBounds(mid1)} }")
            if (mid2 != null && mid2 !== mid1) appendLine("- midBoundsNode2={ ${briefWithBounds(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }

    fun summarize(root: NanoDOMTree): String {
        val s = stats(root)
        val b = boundsStats(root)
        val label = buildString {
            val name = root.nodeName ?: "?"
            append(name)
            val id = (root.attributes?.get("id") as? String)?.takeIf { it.isNotBlank() }
            if (!id.isNullOrBlank()) append("#").append(id)
            val cls = (root.attributes?.get("class") as? String)
                ?.trim()?.split(Regex("\\s+")).orEmpty().take(2)
                .filter { it.isNotBlank() }
            if (cls.isNotEmpty()) append(".").append(cls.joinToString("."))
        }
        val (mid1, mid2) = midTwoWithBounds(root)
        val xyAll = collectXY(root)
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)
        return buildString {
            appendLine("NanoDOMTree")
            appendLine("- root locator=${root.locator} label=${label}")
            appendLine("- scrollable=${root.scrollable} interactive=${root.interactive} invisible=${root.invisible}")
            appendLine("- bounds=${root.clientRects ?: root.absoluteBounds ?: root.bounds}")
            appendLine("- stats=($s)")
            appendLine("- boundsStats=($b)")
            appendLine("- bounds.zeroNonZero=(zero=${b.zero}, nonZero=${b.positive})")
            if (mid1 != null) appendLine("- midBoundsNode1={ ${briefWithBounds(mid1)} }")
            if (mid2 != null && mid2 !== mid1) appendLine("- midBoundsNode2={ ${briefWithBounds(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }

    fun summarize(snapshotNode: Map<Int, SnapshotNodeEx>): String {
        var zero = 0
        var positive = 0
        var missing = 0
        var withClientRects = 0
        var withAbsolute = 0
        var withBounds = 0

        val eligible = mutableListOf<Map.Entry<Int, SnapshotNodeEx>>()
        val xyAll = mutableListOf<Pair<Double, Double>>()

        snapshotNode.entries.forEach { (k, s) ->
            val r = s.clientRects ?: s.absoluteBounds ?: s.bounds
            if (s.clientRects != null) withClientRects++
            if (s.absoluteBounds != null) withAbsolute++
            if (s.bounds != null) withBounds++
            xyOf(r)?.let { xyAll += it }
            when {
                r == null -> missing++
                r.width > 0 && r.height > 0 -> {
                    positive++
                    if (hasNonZeroXY(r)) eligible += java.util.AbstractMap.SimpleEntry(k, s)
                }
                else -> {
                    zero++
                    if (hasNonZeroXY(r)) eligible += java.util.AbstractMap.SimpleEntry(k, s)
                }
            }
        }

        fun brief(entry: Map.Entry<Int, SnapshotNodeEx>): String {
            val s = entry.value
            val source = when {
                s.clientRects != null -> "clientRects"
                s.absoluteBounds != null -> "absoluteBounds"
                s.bounds != null -> "bounds"
                else -> "none"
            }
            val r = s.clientRects ?: s.absoluteBounds ?: s.bounds
            return "key=${entry.key}, source=${source}, rect=${r}"
        }

        val (mid1, mid2) = middleTwo(eligible)
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)
        return buildString {
            appendLine("SnapshotNodeExMap")
            appendLine("- entries=${snapshotNode.size}")
            appendLine("- boundsStats=(zero=${zero}, positive=${positive}, missing=${missing})")
            appendLine("- with: clientRects=${withClientRects}, absoluteBounds=${withAbsolute}, bounds=${withBounds}")
            appendLine("- bounds.zeroNonZero=(zero=${zero}, nonZero=${positive})")
            if (mid1 != null) appendLine("- midBoundsEntry1={ ${brief(mid1)} }")
            if (mid2 != null && mid2.key != mid1?.key) appendLine("- midBoundsEntry2={ ${brief(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }

    fun summarize(state: DOMState): String {
        val totalEntries = state.selectorMap.size
        val keys = state.selectorMap.keys
        val xpathKeys = keys.count { it.startsWith("xpath:") }
        val backendKeys = keys.count { it.startsWith("backend:") }
        val nodeKeys = keys.count { it.startsWith("node:") }
        val hashKeys = totalEntries - xpathKeys - backendKeys - nodeKeys

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
        val json = DOMSerializer.toJson(state.microTree)

        val eligible = uniqueNodes.filter { hasNonZeroXY(rectOf(it)) }
        val (mid1, mid2) = middleTwo(eligible)

        val xyAll = uniqueNodes.mapNotNull { xyOf(rectOf(it)) }
        val gt200 = filterGt(xyAll, 200.0)
        val gt50Sorted = sortedGt50(xyAll)

        return buildString {
            appendLine("DOMState")
            appendLine("- json.length=${json.length}")
            appendLine("- selectorMap.entries=${totalEntries}")
            appendLine("- selectorMap.uniqueNodes=${uniqueCount} (hashKeys=${hashKeys}, xpathKeys=${xpathKeys}, backendKeys=${backendKeys}, nodeKeys=${nodeKeys})")
            appendLine("- nodes: interactable=${interactable}, visible=${visible}, scrollable=${scrollable}, withXPath=${withXPath}, withHash=${withHash}, withSnapshot=${withSnapshot}, withBounds=${withBounds}")
            if (sample.isNotBlank()) appendLine("- sample=${sample}")
            if (mid1 != null) appendLine("- midBoundsNode1={ ${briefWithBounds(mid1)} }")
            if (mid2 != null && mid2 !== mid1) appendLine("- midBoundsNode2={ ${briefWithBounds(mid2)} }")
            appendLine("- bounds.samples.gt200.count=${gt200.size}")
            appendLine("- bounds.coords.gt50.sorted=${formatXYList(gt50Sorted)}")
        }
    }
}
