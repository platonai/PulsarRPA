package ai.platon.pulsar.browser.driver.examples

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncherV2
import org.slf4j.LoggerFactory

abstract class BrowserExampleBase(val headless: Boolean = false): AutoCloseable {
    val log = LoggerFactory.getLogger(BrowserExampleBase::class.java)

    val testUrl = "https://item.jd.com/100001071956.html"

    val browserControl = BrowserSettings()
    val clientLibJs = browserControl.parseLibJs()
    val launchOptions = ChromeDevtoolsOptions()
            .addArguments("window-size", browserControl.formatViewPort())
            .also { it.headless = headless }
    val launcher = ChromeLauncherV2()
    val chrome = launcher.launch(launchOptions)
    val tab = chrome.createTab()
    val devTools = chrome.createDevToolsService(tab)

    val browser get() = devTools.browser
    val network get() = devTools.network
    val page get() = devTools.page
    val mainFrame get() = page.frameTree.frame
    val runtime get() = devTools.runtime
    val emulation get() = devTools.emulation

    abstract fun run()

    val pageSource: String
        get() {
            val evaluation = runtime.evaluate("document.documentElement.outerHTML")
            return evaluation.result.value.toString()
        }

    override fun close() {
        devTools.waitUntilClosed()
    }
}
