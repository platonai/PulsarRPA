package ai.platon.pulsar.t

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts

class Influpia {
    private val session = PulsarContexts.createSession()
    
    fun automate() {
        var url = "https://chromewebstore.google.com/detail/unisat-wallet/ppbibelpcjmhbdihakflkdcoccbgbkpo?hl=zh-CN&utm_source=ext_sidebar"
        var options = session.options()
        val be = options.event.browseEventHandlers
        be.onDocumentSteady.addLast { page, driver ->
            driver.clickTextMatches("button", "添加至 Chrome")
            val oldUrl = driver.documentURI()
            driver.navigateTo("chrome-extension://ppbibelpcjmhbdihakflkdcoccbgbkpo/index.html#/welcome")
            driver.waitForNavigation(oldUrl)
            driver.clickTextMatches("button", "Create new wallet")
            driver.waitForNavigation()
            driver.clickTextMatches("button", "下一步")
            driver.waitForNavigation()
            driver.clickTextMatches("button", "下一步")
            driver.waitForNavigation()
            driver.clickTextMatches("button", "下一步")
            driver.waitForNavigation()
            driver.clickTextMatches("button", "完成")
            driver.waitForNavigation()
            driver.clickTextMatches("button", "下一步")
        }
        session.open(url)
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser().withSPA()
    Influpia().automate()
    readlnOrNull()
}
