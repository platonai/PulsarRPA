package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.getLogger
import com.github.kklisura.cdt.protocol.v2023.types.page.CaptureScreenshotFormat
import com.github.kklisura.cdt.protocol.v2023.types.page.Viewport
import com.google.gson.Gson
import kotlin.math.roundToInt

class Screenshot(
    private val pageHandler: PageHandler,
    private val devTools: RemoteDevTools,
) {
    private val logger = getLogger(this)
    private val isActive get() = AppContext.isActive && devTools.isOpen
    private val page get() = devTools.page.takeIf { isActive }
    private val dom get() = devTools.dom.takeIf { isActive }
    private val debugLevel = System.getProperty("debugLevel")?.toIntOrNull() ?: 0

    fun captureScreenshot(selector: String): String? {
        val nodeId = pageHandler.querySelector(selector)
        if (nodeId == null || nodeId <= 0) {
            logger.info("No such element <{}>", selector)
            return null
        }

//        val vi = pageHandler.firstAttr(selector, "vi")
        val vi: String? = null
        return if (vi != null) {
            captureScreenshotWithVi(nodeId, selector, vi)
        } else {
            captureScreenshotWithoutVi(nodeId, selector)
        }
    }

    fun captureScreenshot(clip: RectD) = captureScreenshot0(0, clip)

    fun captureScreenshot(viewport: Viewport) = captureScreenshot0(0, viewport)

    private fun captureScreenshotWithVi(nodeId: Int, selector: String, vi: String): String? {
        val quad = vi.split(" ").map { it.toDoubleOrNull() ?: 0.0 }
        if (quad.size != 4) {
            logger.warn("Invalid node vi information for selector <{}>", selector)
            return null
        }

        val rect = RectD(quad[0], quad[1], quad[2], quad[3])

        return captureScreenshot0(nodeId, rect)
    }

    private fun captureScreenshotWithoutVi(nodeId: Int, selector: String): String? {
        val nodeClip = calculateNodeClip(nodeId, selector)
        if (nodeClip == null) {
            logger.info("Can not calculate node clip | {}", selector)
            return null
        }

        val rect = nodeClip.rect
        if (rect == null) {
            logger.info("Can not take clip | {}", selector)
            return null
        }

        // val clip = normalizeClip(rect)

        return captureScreenshot0(nodeClip.nodeId, rect)
    }

    private fun captureScreenshot0(nodeId: Int, clip: RectD): String? {
        val viewport = Viewport().apply {
            x = clip.x; y = clip.y
            width = clip.width; height = clip.height
            scale = 1.0
        }
        return captureScreenshot0(nodeId, viewport)
    }

    private fun captureScreenshot0(nodeId: Int, viewport: Viewport): String? {
        val format = CaptureScreenshotFormat.JPEG
        val quality = BrowserSettings.screenshotQuality

        if (debugLevel > 50) {
            println("viewport: ")
            println("" + viewport.x + " " + viewport.y + " " + viewport.width + " " + viewport.height)
        }

//        val cssLayoutViewport = p.layoutMetrics.cssLayoutViewport
//        if (viewport.width > cssLayoutViewport.clientWidth || viewport.height > cssLayoutViewport.clientHeight) {
//        }
//        viewport.width = viewport.width.coerceAtMost(cssLayoutViewport.clientWidth.toDouble())
//        viewport.height = viewport.height.coerceAtMost(cssLayoutViewport.clientHeight.toDouble())

        // The viewport has to be visible before screenshot
        dom?.scrollIntoViewIfNeeded(nodeId, null, null, null)

        val visible = ClickableDOM.create(page, dom, nodeId)?.isVisible() ?: false
        if (!visible) {
            return null
        }

        return page?.captureScreenshot(format, quality, viewport, true, false, false)
    }

    private fun calculateNodeClip(nodeId: Int, selector: String): NodeClip? {
        if (debugLevel > 50) {
            debugNodeClipDebug(nodeId, selector)
        }

        // must scroll to top to calculate the client rect
        pageHandler.evaluate("__pulsar_utils__.scrollToTop()")

        val rect = calculateNodeClip0(nodeId, selector)

        val p = page ?: return null
//        val d = dom ?: return null

        val viewport = p.layoutMetrics.cssLayoutViewport
        val pageX = viewport.pageX
        val pageY = viewport.pageY

        if (debugLevel > 50) {
            println(Gson().toJson(viewport))
        }

        return NodeClip(nodeId, pageX, pageY, rect)
    }

    private fun calculateNodeClip0(nodeId: Int, selector: String): RectD? {
        val clickableDOM = ClickableDOM(page!!, dom!!, nodeId)
        return clickableDOM.boundingBox()
    }

    private fun calculateNodeClip1(nodeId: Int, selector: String): RectD? {
        val clientRect = pageHandler.evaluate("__pulsar_utils__.queryClientRect('$selector')")?.toString()
        if (clientRect == null) {
            logger.info("Can not query client rect for selector <{}>", selector)
            return null
        }

        val quad = clientRect.split(" ").map { it.toDoubleOrNull() ?: 0.0 }
        if (quad.size != 4) {
            return null
        }

        return RectD(quad[0], quad[1], quad[2], quad[3])
    }

    private fun debugNodeClipDebug(nodeId: Int, selector: String) {
        println("\n")
        println("===== $selector $nodeId")

        var clientRects = pageHandler.evaluate("__pulsar_utils__.queryClientRects('$selector')")
        println(clientRects)
        var contentQuads = dom?.getContentQuads(nodeId, null, null)
        println(contentQuads)

        var clientRect = pageHandler.evaluate("__pulsar_utils__.queryClientRect('$selector')")?.toString()

        println("clientRect: ")
        println(clientRect)

        var clickableDOM = ClickableDOM(page!!, dom!!, nodeId)
        println(clickableDOM.boundingBox())
        println(clickableDOM.clickablePoint())

        println("== scrollToTop ==")
        pageHandler.evaluate("__pulsar_utils__.scrollToTop()")

        clientRects = pageHandler.evaluate("__pulsar_utils__.queryClientRects('$selector')")
        println(clientRects)
        contentQuads = dom?.getContentQuads(nodeId, null, null)
        println(contentQuads)

        clientRect = pageHandler.evaluate("__pulsar_utils__.queryClientRect('$selector')")?.toString()

        println("clientRect: ")
        println(clientRect)

        clickableDOM = ClickableDOM(page!!, dom!!, nodeId)
        println(clickableDOM.boundingBox())
        println(clickableDOM.clickablePoint())

        val p = page ?: return
//        val d = dom ?: return null

        val viewport = p.layoutMetrics.cssLayoutViewport
        val pageX = viewport.pageX
        val pageY = viewport.pageY

        println("pageX, pageY: ")
        println("$pageX, $pageY")
    }

    private fun normalizeClip(clip: RectD): RectD {
        val x = clip.x.roundToInt()
        val y = clip.y.roundToInt()
        val width = (clip.width + clip.x - x).roundToInt()
        val height = (clip.height + clip.y - y).roundToInt()
        return RectD(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    }
}
