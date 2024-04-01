package ai.platon.pulsar.browser.driver.examples

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import org.slf4j.LoggerFactory

abstract class BrowserExampleBase(val headless: Boolean = false): AutoCloseable {
    val logger = LoggerFactory.getLogger(BrowserExampleBase::class.java)

    open val testUrl = "https://item.jd.com/100001071956.html"

    val browserSettings = BrowserSettings()
    val preloadJs = browserSettings.scriptLoader.getPreloadJs()
    val launchOptions = ChromeOptions()
            .addArgument("window-size", formatViewPort())
            .also { it.headless = headless }
    val launcher = ChromeLauncher()
    val chrome = launcher.launch(launchOptions)
    val tab = chrome.createTab()
    val devTools = chrome.createDevTools(tab, DevToolsConfig())

    val browser get() = devTools.browser
    val network get() = devTools.network
    val page get() = devTools.page
    val mainFrame get() = page.frameTree.frame
    val runtime get() = devTools.runtime
    val emulation get() = devTools.emulation
    val dom get() = devTools.dom
    val overlay get() = devTools.overlay

    abstract fun run()

    val pageSource: String
        get() {
            val evaluation = runtime.evaluate("document.documentElement.outerHTML")
            return evaluation.result.value.toString()
        }

    private fun formatViewPort(delimiter: String = ","): String {
        val vp = BrowserSettings.SCREEN_VIEWPORT
        return "${vp.width}$delimiter${vp.height}"
    }

    override fun close() {
        devTools.awaitTermination()
    }
}
