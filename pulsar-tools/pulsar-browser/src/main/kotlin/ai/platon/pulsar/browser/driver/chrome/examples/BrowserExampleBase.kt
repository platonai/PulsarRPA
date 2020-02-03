package ai.platon.pulsar.browser.driver.chrome.examples

import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher

abstract class BrowserExampleBase(val headless: Boolean = false): AutoCloseable {
    val testUrl = "https://www.baidu.com"
    val launcher = ChromeLauncher()
    val chrome = launcher.launch(headless)
    val tab = chrome.createTab()
    val devTools = chrome.createDevToolsService(tab)

    abstract fun run()

    override fun close() {
        devTools.waitUntilClosed()
    }
}
