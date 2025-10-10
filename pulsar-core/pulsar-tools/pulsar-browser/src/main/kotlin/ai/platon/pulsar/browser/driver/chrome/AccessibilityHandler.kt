package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.common.AppContext
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*
import com.github.kklisura.cdt.protocol.v2023.types.accessibility.AXNode

class AccessibilityHandler(
    private val devTools: RemoteDevTools,
    private val experimental: Boolean = true
) {
    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val pageAPI get() = devTools.page.takeIf { isActive }
    private val domAPI get() = devTools.dom.takeIf { isActive }
    private val cssAPI get() = devTools.css.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }
    private val accessibilityAPI get() = devTools.accessibility.takeIf { isActive }

    fun getAccessibilityTree(selector: String? = null, targetFrameId: String? = null) {
        // 1. Decide params
        val mainFrame = pageAPI?.frameTree?.frame
        if (targetFrameId != null && targetFrameId != mainFrame?.id) {
            // detect frame id
            TODO("Frame will be supported in the later version")
        }

        // 2. Fetch raw AX nodes using getFullAXTree
        val nodes = getFullAXTree(null, targetFrameId)

        // 3. Scrollable detection
//        val scrollableIds = findScrollableElementIds(
//            targetFrameId,
//        );
    }

    fun findScrollableElementIds(targetFrameId: String? = null) {

    }

    /**
     * Fetches the entire accessibility tree for the root Document
     *
     * @param depth The maximum depth at which descendants of the root node should be retrieved. If
     * omitted, the full tree is returned.
     * @param frameId The frame for whose document the AX tree should be retrieved. If omited, the
     * root frame is used.
     */
    private fun getFullAXTree(depth: Int?, frameId: String?): List<AXNode>? {
        return accessibilityAPI?.getFullAXTree(depth, frameId)
    }

}
