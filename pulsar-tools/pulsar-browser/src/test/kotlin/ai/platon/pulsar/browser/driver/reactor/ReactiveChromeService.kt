package ai.platon.pulsar.browser.driver.reactor

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.ChromeLauncher
import ai.platon.pulsar.browser.driver.chrome.DevToolsConfig
import org.slf4j.LoggerFactory

class ReactiveChromeService {
    val log = LoggerFactory.getLogger(ReactiveChromeService::class.java)

    val testUrl = "https://item.jd.com/100001071956.html"

    val headless: Boolean = false
    val browserControl = BrowserControl()
    val clientLibJs = browserControl.parseLibJs()
    val launchOptions = ChromeDevtoolsOptions()
            .addArguments("window-size", browserControl.formatViewPort())
            .also { it.headless = headless }
    val launcher = ChromeLauncher()
    val chrome = launcher.launch(launchOptions)
    val tab = chrome.createTab() // TODO: how to avoid this tab creation?
    val devTools = chrome.createDevTools(tab, DevToolsConfig())

    fun run() {

    }
}

fun main() {
    ReactiveChromeService().run()
}
