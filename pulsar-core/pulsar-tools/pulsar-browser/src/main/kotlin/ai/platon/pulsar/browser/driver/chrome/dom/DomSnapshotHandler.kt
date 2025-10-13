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
     * Enhanced capture with absolute coordinates and stacking context analysis.
     * This method provides comprehensive layout information for interaction indices.
     *
     * @param includeStyles Whether to capture computed styles
     * @param includePaintOrder Whether to include paint order information
     * @param includeDomRects Whether to include DOM rectangles
     * @param includeAbsoluteCoords Whether to calculate absolute coordinates
     * @return Map of backendNodeId to enhanced snapshot data with absolute coordinates
     */
    fun captureEnhanced(
        includeStyles: Boolean = true,
        includePaintOrder: Boolean = true,
        includeDomRects: Boolean = true,
        includeAbsoluteCoords: Boolean = true
    ): Map<Int, EnhancedSnapshotNode> {
        val computed = if (includeStyles) EXTENDED_COMPUTED_STYLES else emptyList()
        val capture = domSnapshot.captureSnapshot(
            computed,
            /* includePaintOrder */ includePaintOrder,
            /* includeDOMRects */ includeDomRects,
            /* includeBlendedBackgroundColors */ null,
            /* includeTextColorOpacities */ null,
        )

        val byBackend = mutableMapOf<Int, EnhancedSnapshotNode>()
        val strings = capture.strings ?: emptyList()

        // Calculate viewport dimensions for absolute coordinate calculation
        val viewportBounds = if (includeAbsoluteCoords) {
            getViewportBounds()
        } else null

        for (doc in capture.documents ?: emptyList()) {
            val nodeTree = doc.nodes ?: continue
            val layout = doc.layout ?: continue

            val backendIds: List<Int> = nodeTree.backendNodeId ?: emptyList()
            val nodeIndex: List<Int> = layout.nodeIndex ?: emptyList()

            val bounds = layout.bounds ?: emptyList()
            val offsetRects = layout.offsetRects ?: emptyList()
            val scrollRects = layout.scrollRects ?: emptyList()
            val clientRects = layout.clientRects ?: emptyList()
            val paintOrders = if (includePaintOrder) layout.paintOrders ?: emptyList() else emptyList()

            val rows = nodeIndex.size

            // Build style maps per layout row
            val stylesIdx = layout.styles ?: emptyList()
            val styleMaps: List<Map<String, String>> = if (includeStyles && layout.styles != null) {
                stylesIdx.map { idxs ->
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
                List(rows) { emptyMap() }
            }

            // Analyze stacking contexts
            val stackingContexts: List<Int> = layout.stackingContexts?.index?.let { rareIndices ->
                if (rareIndices.isEmpty()) {
                    emptyList()
                } else {
                    val flags = IntArray(rows)
                    rareIndices.forEach { idx ->
                        if (idx in 0 until rows) {
                            flags[idx] = 1
                        }
                    }
                    flags.toList()
                }
            } ?: emptyList()

            for (row in 0 until rows) {
                val nIdx = nodeIndex.getOrNull(row) ?: continue
                val backendId = backendIds.getOrNull(nIdx) ?: continue

                // Extract cursor and clickability from styles
                val styles = styleMaps.getOrNull(row) ?: emptyMap()
                val cursor = styles["cursor"]
                val isClickable = cursor in setOf("pointer", "hand")

                // Calculate absolute coordinates if requested
                val absoluteBounds = if (includeAbsoluteCoords && viewportBounds != null) {
                    val boundsRect = DOMRect.fromBoundsArray(bounds.getOrNull(row) ?: emptyList())
                    calculateAbsoluteCoordinates(boundsRect, viewportBounds, styles)
                } else null

                val snap = EnhancedSnapshotNode(
                    isClickable = isClickable,
                    cursorStyle = cursor,
                    bounds = DOMRect.fromBoundsArray(bounds.getOrNull(row) ?: emptyList()),
                    clientRects = DOMRect.fromRectArray(clientRects.getOrNull(row) ?: emptyList()),
                    scrollRects = DOMRect.fromRectArray(scrollRects.getOrNull(row) ?: emptyList()),
                    computedStyles = styles.takeIf { it.isNotEmpty() },
                    paintOrder = paintOrders.getOrNull(row),
                    stackingContexts = stackingContexts.getOrNull(row),
                    absoluteBounds = absoluteBounds
                )

                byBackend[backendId] = snap
            }
        }
        return byBackend
    }

    /**
     * Get viewport bounds for absolute coordinate calculation.
     */
    private fun getViewportBounds(): DOMRect {
        return try {
            val evaluation = devTools.runtime.evaluate("""
                {
                    width: window.innerWidth,
                    height: window.innerHeight,
                    x: 0,
                    y: 0
                }
            """.trimIndent())

            val result = evaluation?.result?.value?.toString()
            if (result != null && result.contains("width") && result.contains("height")) {
                // Parse the JSON result
                val width = result.substringAfter("\"width\":").substringBefore(",").trim().toDoubleOrNull() ?: 1024.0
                val height = result.substringAfter("\"height\":").substringBefore("}").trim().toDoubleOrNull() ?: 768.0
                DOMRect(x = 0.0, y = 0.0, width = width, height = height)
            } else {
                DOMRect(x = 0.0, y = 0.0, width = 1024.0, height = 768.0)
            }
        } catch (e: Exception) {
            DOMRect(x = 0.0, y = 0.0, width = 1024.0, height = 768.0)
        }
    }

    /**
     * Calculate absolute coordinates based on viewport and element styles.
     */
    private fun calculateAbsoluteCoordinates(
        bounds: DOMRect,
        viewportBounds: DOMRect,
        styles: Map<String, String>
    ): DOMRect {
        val position = styles["position"] ?: "static"

        return when (position) {
            "fixed" -> {
                // Fixed positioning is relative to viewport
                bounds
            }
            "absolute" -> {
                // Absolute positioning - need to calculate relative to nearest positioned ancestor
                // For now, return as-is and let the caller handle the calculation
                bounds
            }
            "relative" -> {
                // Relative positioning - offset from normal position
                val top = styles["top"]?.toDoubleOrNull() ?: 0.0
                val left = styles["left"]?.toDoubleOrNull() ?: 0.0
                bounds.copy(
                    x = bounds.x + left,
                    y = bounds.y + top
                )
            }
            else -> {
                // Static positioning - use bounds as-is
                bounds
            }
        }
    }

    /**
     * Capture snapshot as a flat list (legacy method).
     * @deprecated Use captureByBackendNodeId or captureEnhanced for better functionality
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
    fun captureByBackendNodeId(
        includeStyles: Boolean = true,
        includePaintOrder: Boolean = true,
        includeDomRects: Boolean = true
    ): Map<Int, EnhancedSnapshotNode> {
        val computed = if (includeStyles) REQUIRED_COMPUTED_STYLES else emptyList()
        val capture = domSnapshot.captureSnapshot(
            computed,
            /* includePaintOrder */ includePaintOrder,
            /* includeDOMRects */ includeDomRects,
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
            val paintOrders = if (includePaintOrder) layout.paintOrders ?: emptyList() else emptyList()

            val rows = nodeIndex.size

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
                List(rows) { emptyMap() }
            }

            val stackingContexts: List<Int> = layout.stackingContexts?.index?.let { rareIndices ->
                if (rareIndices.isEmpty()) {
                    emptyList()
                } else {
                    val flags = IntArray(rows)
                    rareIndices.forEach { idx ->
                        if (idx in 0 until rows) {
                            flags[idx] = 1
                        }
                    }
                    flags.toList()
                }
            } ?: emptyList()

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
         * Required computed styles for proper visibility, scroll detection, and absolute positioning.
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
            "position",
            "transform",
            "z-index"
        )

        /**
         * Extended computed styles for stacking context and absolute coordinate calculation.
         */
        val EXTENDED_COMPUTED_STYLES = listOf(
            "display",
            "visibility",
            "opacity",
            "overflow",
            "overflow-x",
            "overflow-y",
            "cursor",
            "pointer-events",
            "position",
            "transform",
            "z-index",
            "top",
            "left",
            "right",
            "bottom",
            "width",
            "height",
            "margin-top",
            "margin-left",
            "margin-right",
            "margin-bottom",
            "padding-top",
            "padding-left",
            "padding-right",
            "padding-bottom"
        )
    }
}
