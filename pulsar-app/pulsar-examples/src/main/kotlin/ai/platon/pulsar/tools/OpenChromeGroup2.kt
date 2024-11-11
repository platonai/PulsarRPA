package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.runBlocking

fun main() {
    val session = SQLContexts.create()
    val driverFactory = session.getBean(WebDriverFactory::class)
    
    repeat(10) {
        val browserId = BrowserId.NEXT_SEQUENTIAL
        val browser = driverFactory.launchBrowser(browserId)
        
        runBlocking {
            val driver = browser.newDriver()
            driver.navigateTo("chrome://version")
            
            val driver2 = browser.newDriver()
            driver2.navigateTo("https://www.taobao.com")
        }
    }
    
    readlnOrNull()
}
