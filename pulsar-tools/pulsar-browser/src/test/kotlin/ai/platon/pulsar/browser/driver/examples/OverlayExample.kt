package ai.platon.pulsar.browser.driver.examples

import com.github.kklisura.cdt.protocol.types.dom.RGBA
import com.github.kklisura.cdt.protocol.types.overlay.HighlightConfig
import com.google.gson.GsonBuilder

class OverlayExample: BrowserExampleBase() {

    override val testUrl: String = "https://www.amazon.com/"

    override fun run() {
        page.enable()
        dom.enable()
        overlay.enable()

        overlay.onScreenshotRequested { screenshot ->
            val v = screenshot.viewport
            overlay.highlightRect(v.x.toInt(), v.y.toInt(), v.width.toInt(), v.height.toInt())
        }
//        dom.onGetDocument {
//            val document = it.root
//            val body = document.body
//            val rect = body.boundingBox
//            val highlightConfig = HighlightConfig(RGBA(255, 0, 0, 0.5), RGBA(255, 0, 0, 0.5))
//            overlay.highlightRect(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt(), highlightConfig)
//        }
        page.navigate(testUrl)
        page.onDocumentOpened {
            page.captureScreenshot()
            highlight("#nav-xshop")
        }
    }

    private fun highlight(selector: String) {
        val documentId = dom.document.nodeId
        val nodeId = dom.querySelector(documentId, selector)
        val highlightConfig = HighlightConfig().apply {
            showInfo = true
            showRulers = true
            showStyles = true
            showExtensionLines = true
            shapeColor = RGBA().apply {
                r = 255
                g = 0
                b = 0
                a = 1.0
            }
        }

        overlay.highlightRect(300, 400, 500, 500)
//        Thread.sleep(5000)
        overlay.highlightNode(highlightConfig, nodeId, null, null, selector)
//        Thread.sleep(5000)
        val obj = overlay.getHighlightObjectForTest(nodeId)
        val json = GsonBuilder().setPrettyPrinting().create().toJson(obj)
        println(json)
    }
}

fun main() {
    OverlayExample().use { it.run() }
}
