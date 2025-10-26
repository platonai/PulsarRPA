package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.cdt.kt.protocol.commands.DOMSnapshot
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMRect
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotNodeEx
import ai.platon.pulsar.common.getLogger

/**
 * Handler for DOMSnapshot domain operations.
 * Captures and processes layout snapshots with style and rect information.
 */
class DomSnapshotHandler(private val devTools: RemoteDevTools) {
    private val logger = getLogger(this)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }

    private val domSnapshot: DOMSnapshot get() = devTools.domSnapshot

    /**
     * Enhanced capture with absolute coordinates and stacking context analysis.
     * This method provides comprehensive layout information for interaction indices.
     */
    suspend fun captureEnhanced(
        includeStyles: Boolean = true,
        includePaintOrder: Boolean = true,
        includeDomRects: Boolean = true,
        includeAbsoluteCoords: Boolean = true
    ): Map<Int, SnapshotNodeEx> {
        val computedStyles = if (includeStyles) REQUIRED_COMPUTED_STYLES else emptyList()
        val capture = try {
            domSnapshot.captureSnapshot(
                computedStyles,
                includePaintOrder = includePaintOrder,
                includeDOMRects = includeDomRects,
                includeBlendedBackgroundColors = null,
                includeTextColorOpacities = null,
            )
        } catch (e: Exception) {
            logger.warn("DOMSnapshot.captureSnapshot failed | err={}", e.toString())
            tracer?.debug("captureSnapshot exception", e)
            return emptyMap()
        }

        val byBackend = mutableMapOf<Int, SnapshotNodeEx>()
        val strings = capture.strings

        var totalRows = 0
        for (doc in capture.documents) {
            val nodeTree = doc.nodes
            val layout = doc.layout

            val backendIds: List<Int> = nodeTree.backendNodeId ?: emptyList()
            val nodeIndex: List<Int> = layout.nodeIndex

            val bounds = layout.bounds
            // The offset rect of nodes. Only available when includeDOMRects is set to true
            val offsetRects = layout.offsetRects
            val scrollRects = layout.scrollRects ?: emptyList()
            val clientRects = layout.clientRects ?: emptyList()
            val paintOrders = if (includePaintOrder) layout.paintOrders ?: emptyList() else emptyList()

            val rows = nodeIndex.size
            totalRows += rows

            // Build style maps per layout row
            val stylesIdx = layout.styles
            val styleMaps: List<Map<String, String>> = if (includeStyles) {
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
            val stackingContexts: List<Int> = layout.stackingContexts.index.let { rareIndices ->
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
            }

            for (row in 0 until rows) {
                val nIdx = nodeIndex.getOrNull(row) ?: continue
                val backendId = backendIds.getOrNull(nIdx) ?: continue

                // Extract cursor and clickability from styles
                val styles = styleMaps.getOrNull(row) ?: emptyMap()
                val cursor = styles["cursor"]
                val isClickable = cursor in setOf("pointer", "hand")

                // CDP layout.bounds are already document-absolute; treat them as absoluteBounds directly
                val boundsRect = DOMRect.fromBoundsArray(bounds.getOrNull(row) ?: emptyList())
                val absoluteBounds = if (includeAbsoluteCoords) boundsRect else null

                val snap = SnapshotNodeEx(
                    isClickable = isClickable,
                    cursorStyle = cursor,
                    bounds = boundsRect,
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

        logger.info("Bounds summary: {}", DomDebug.summarize(byBackend))

        tracer?.debug("DOMSnapshot captured | entries={} rowsApprox={} styles={} paintOrder={}", byBackend.size, totalRows, includeStyles, includePaintOrder)
        return byBackend
    }

    companion object {
        /**
         * Required computed styles for proper visibility, scroll detection, and absolute positioning.
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
