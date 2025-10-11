package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.v2023.commands.DOMSnapshot

class DomSnapshotHandler(private val devTools: RemoteDevTools) {
    fun capture(includeStyles: Boolean = true): List<EnhancedSnapshotNode> {
    val domSnapshot: DOMSnapshot = devTools.domSnapshot
        val computed = if (includeStyles) listOf("display", "visibility", "overflow", "overflow-x", "overflow-y") else emptyList()
        val capture = domSnapshot.captureSnapshot(
            computed,
            /* includePaintOrder */ true,
            /* includeDOMRects */ true,
            /* includeBlendedBackgroundColors */ null,
            /* includeTextColorOpacities */ null,
        )

        val docs = capture.documents ?: emptyList()
        if (docs.isEmpty()) return emptyList()

        val all = mutableListOf<EnhancedSnapshotNode>()
        for (doc in docs) {
            val layout = doc.layout
            val bounds = layout?.bounds ?: emptyList()
            val offsetRects = layout?.offsetRects ?: emptyList()
            val scrollRects = layout?.scrollRects ?: emptyList()
            val clientRects = layout?.clientRects ?: emptyList()
            val paintOrders = layout?.paintOrders ?: emptyList()

            val n = maxOf(bounds.size, offsetRects.size, scrollRects.size, clientRects.size, paintOrders.size)
            for (i in 0 until n) {
                all += EnhancedSnapshotNode(
                    style = emptyMap(),
                    bounds = bounds.getOrNull(i),
                    offsetRect = offsetRects.getOrNull(i),
                    scrollRect = scrollRects.getOrNull(i),
                    clientRect = clientRects.getOrNull(i),
                    paintOrder = paintOrders.getOrNull(i)
                )
            }
        }
        return all
    }

    /**
     * TODO: Build a mapping from backendNodeId to EnhancedSnapshotNode using NodeTreeSnapshot and LayoutTreeSnapshot.nodeIndex.
     * This requires parsing NodeTreeSnapshot.getBackendNodeId() and correlating positions.
     */
    fun captureByBackendNodeId(includeStyles: Boolean = true): Map<Int, EnhancedSnapshotNode> {
        val domSnapshot: DOMSnapshot = devTools.domSnapshot
        val computed = if (includeStyles) listOf("display", "visibility", "overflow", "overflow-x", "overflow-y") else emptyList()
        val capture = domSnapshot.captureSnapshot(
            computed,
            /* includePaintOrder */ true,
            /* includeDOMRects */ true,
            /* includeBlendedBackgroundColors */ null,
            /* includeTextColorOpacities */ null,
        )

        val byBackend = mutableMapOf<Int, EnhancedSnapshotNode>()
        val strings = capture.strings ?: emptyList()
        for (doc in capture.documents ?: emptyList()) {
            val nodeTree = doc.nodes
            val layout = doc.layout
            if (nodeTree == null || layout == null) continue

            val backendIds: List<Int> = nodeTree.backendNodeId ?: emptyList()
            val nodeIndex: List<Int> = layout.nodeIndex ?: emptyList()

            val bounds = layout.bounds ?: emptyList()
            val offsetRects = layout.offsetRects ?: emptyList()
            val scrollRects = layout.scrollRects ?: emptyList()
            val clientRects = layout.clientRects ?: emptyList()
            val paintOrders = layout.paintOrders ?: emptyList()

            // Build style maps per layout row if requested
            val stylesIdx = layout.styles ?: emptyList()
            val styleMaps: List<Map<String, String>> = if (includeStyles && doc.layout.styles != null) {
                stylesIdx.map { idxs ->
                    // idxs are indices into capture.strings as alternating name/value pairs per ComputedStyle serialization.
                    // In v2023 model, styles come back as indices; reconstruct simple map when possible.
                    val m = mutableMapOf<String, String>()
                    var i = 0
                    while (i + 1 < idxs.size) {
                        val k = strings.getOrNull(idxs[i]) ?: ""
                        val v = strings.getOrNull(idxs[i + 1]) ?: ""
                        if (k.isNotEmpty()) m[k] = v
                        i += 2
                    }
                    m
                }
            } else {
                List(maxOf(bounds.size, offsetRects.size, scrollRects.size, clientRects.size, paintOrders.size)) { emptyMap() }
            }

            val rows = nodeIndex.size
            for (row in 0 until rows) {
                val nIdx = nodeIndex.getOrNull(row) ?: continue
                val backendId = backendIds.getOrNull(nIdx) ?: continue
                val snap = EnhancedSnapshotNode(
                    style = styleMaps.getOrNull(row) ?: emptyMap(),
                    bounds = bounds.getOrNull(row),
                    offsetRect = offsetRects.getOrNull(row),
                    scrollRect = scrollRects.getOrNull(row),
                    clientRect = clientRects.getOrNull(row),
                    paintOrder = paintOrders.getOrNull(row)
                )
                byBackend[backendId] = snap
            }
        }

        return byBackend
    }
}
