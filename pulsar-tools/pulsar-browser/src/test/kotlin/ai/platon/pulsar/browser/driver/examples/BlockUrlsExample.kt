package ai.platon.pulsar.browser.driver.examples

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import com.github.kklisura.cdt.protocol.events.page.*
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class BlockUrlsExample: BrowserExampleBase() {
    override fun run() {
        page.addScriptToEvaluateOnNewDocument(clientLibJs)

        page.onDomContentEventFired { event: DomContentEventFired ->
            // The page's main html content is ready, but css/js are not ready, document.readyState === 'interactive'
            runtime.evaluate("__utils__.checkPulsarStatus()")
        }

        page.onLoadEventFired { event: LoadEventFired ->
            // The page is completely loaded, document.readyState === 'complete'

            runtime.evaluate("__utils__.scrollDownN();")
            runtime.evaluate("__utils__.emulate();")

            val source = pageSource
            val path = AppPaths.get(AppPaths.WEB_CACHE_DIR, AppPaths.fromUri(testUrl, "", ".htm"))
            AppFiles.saveTo(source, path, true)
            log.debug("Page is saved to file://$path")

            TimeUnit.SECONDS.sleep(5)
            devTools.close()
        }

        page.onFrameAttached { event: FrameAttached ->
            if (isMainFrame(event.frameId)) {
                debugDocumentState(event)
            }
            println("onFrameAttached - ${event.frameId}")
        }

        page.onFrameDetached { event: FrameDetached ->
            if (isMainFrame(event.frameId)) {
                debugDocumentState(event)
            }
            println("onFrameDetached - " + event.frameId)
        }

        page.onFrameNavigated { event: FrameNavigated ->
            if (isMainFrame(event.frame.id)) {
                debugDocumentState(event)
                println(event.javaClass.simpleName + " - " + event.frame.id)
            }
        }

        page.onFrameStartedLoading { event: FrameStartedLoading ->
            if (isMainFrame(event.frameId)) {
                debugDocumentState(event)
                // emulation.canEmulate() //true
            }
        }

        page.onFrameStoppedLoading { event: FrameStoppedLoading ->
            if (isMainFrame(event.frameId)) {
                println("[main] onFrameStoppedLoading - ${event.frameId} - ${pageSource.length}")
            } else {
                println("onFrameStoppedLoading - ${event.frameId}")
            }
            println()
        }

        page.enable()
        page.navigate(testUrl)

        println(Gson().toJson(chrome.getVersion()))
    }

    private fun isMainFrame(frameId: String): Boolean {
        return mainFrame.id == frameId
    }

    private fun debugDocumentState(event: Any, message: String = "") {
        val evaluate = runtime.evaluate("document.readyState")
        println("${event.javaClass.simpleName} ${evaluate.result.value} | message")
    }
}

fun main() {
    BlockUrlsExample().use { it.run() }
}
