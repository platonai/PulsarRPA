package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedSnapshotNode
import com.github.kklisura.cdt.protocol.v2023.commands.DOMSnapshot

/**
 * Handler for DOMSnapshot domain operations.
 * Captures and processes layout snapshots with style and rect information.
 */
class DomSnapshotHandler(private val devTools: RemoteDevTools) {
    
    private val domSnapshot: DOMSnapshot get() = devTools.domSnapshot
    
    /**
     * Capture snapshot as a flat list (legacy method).
     * @deprecated Use captureByBackendNodeId for proper node association
     */
    @Deprecated("Use captureByBackendNodeId instead")
    fun capture(includeStyles: Boolean = true): List<EnhancedSnapshotNode> {
        val computed = if (includeStyles) REQUIRED_COMPUTED_STYLES else emptyList()
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
            val layout = doc.layout ?: continue
            val bounds = layout.bounds ?: emptyList()
            val offsetRects = layout.offsetRects ?: emptyList()
            val scrollRects = layout.scrollRects ?: emptyList()
            val clientRects = layout.clientRects ?: emptyList()
            val paintOrders = layout.paintOrders ?: emptyList()

            val n = maxOf(bounds.size, offsetRects.size, scrollRects.size, clientRects.size, paintOrders.size)
            for (i in 0 until n) {
                all += EnhancedSnapshotNode(
                    bounds = DOMRect.fromBoundsArray(bounds.getOrNull(i) ?: emptyList()),
                    clientRects = DOMRect.fromRectArray(clientRects.getOrNull(i) ?: emptyList()),
                    scrollRects = DOMRect.fromRectArray(scrollRects.getOrNull(i) ?: emptyList()),
                    paintOrder = paintOrders.getOrNull(i)
                )
            }
        }
        return all
    }

    /**
     * Build a mapping from backendNodeId to EnhancedSnapshotNode.
     * This is the primary method for associating snapshot data with DOM nodes.
     * 
     * Maps to Python build_snapshot_lookup function.
     * 
     * @param includeStyles Whether to capture computed styles
     * @return Map of backendNodeId to snapshot data
     */
    fun captureByBackendNodeId(includeStyles: Boolean = true): Map<Int, EnhancedSnapshotNode> {
        val computed = if (includeStyles) REQUIRED_COMPUTED_STYLES else emptyList()
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
            val nodeTree = doc.nodes ?: continue
            val layout = doc.layout ?: continue

            val backendIds: List<Int> = nodeTree.backendNodeId ?: emptyList()
            val nodeIndex: List<Int> = layout.nodeIndex ?: emptyList()

            val bounds = layout.bounds ?: emptyList()
            val offsetRects = layout.offsetRects ?: emptyList()
            val scrollRects = layout.scrollRects ?: emptyList()
            val clientRects = layout.clientRects ?: emptyList()
            val paintOrders = layout.paintOrders ?: emptyList()
            val stackingContexts = layout.stackingContexts?.contentDocument ?: emptyList()

            // Build style maps per layout row if requested
            val stylesIdx = layout.styles ?: emptyList()
            val styleMaps: List<Map<String, String>> = if (includeStyles && layout.styles != null) {
                stylesIdx.map { idxs ->
                    // idxs are indices into capture.strings as alternating name/value pairs
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
                List(nodeIndex.size) { emptyMap() }
            }

            val rows = nodeIndex.size
            for (row in 0 until rows) {
                val nIdx = nodeIndex.getOrNull(row) ?: continue
                val backendId = backendIds.getOrNull(nIdx) ?: continue
                
                // Extract cursor and clickability from styles
                val styles = styleMaps.getOrNull(row) ?: emptyMap()
                val cursor = styles["cursor"]
                val isClickable = cursor in setOf("pointer", "hand")
                
                val snap = EnhancedSnapshotNode(
                    isClickable = isClickable,
                    cursorStyle = cursor,
                    bounds = DOMRect.fromBoundsArray(bounds.getOrNull(row) ?: emptyList()),
                    clientRects = DOMRect.fromRectArray(clientRects.getOrNull(row) ?: emptyList()),
                    scrollRects = DOMRect.fromRectArray(scrollRects.getOrNull(row) ?: emptyList()),
                    computedStyles = styles.takeIf { it.isNotEmpty() },
                    paintOrder = paintOrders.getOrNull(row),
                    stackingContexts = stackingContexts.getOrNull(row)
                )
                
                byBackend[backendId] = snap
            }
        }
        return byBackend
    }

    companion object {
        /**
         * Required computed styles for proper visibility and scroll detection.
         * Maps to Python REQUIRED_COMPUTED_STYLES.
         */
        val REQUIRED_COMPUTED_STYLES = listOf(
            "display",
            "visibility",
            "opacity",
            "overflow",
            "overflow-x",
            "overflow-y",
            "cursor",
            "pointer-events",
            "position"
        )
    }
}

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
