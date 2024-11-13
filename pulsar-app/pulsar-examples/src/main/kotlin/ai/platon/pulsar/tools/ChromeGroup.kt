package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.DefaultBrowserComponents
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyAgent
import kotlinx.coroutines.runBlocking

class ChromeGroup {
    private val components = DefaultBrowserComponents()
    private val fetcher = components.webdriverFetcher
    private val privacyManager = components.privacyManager
    
    val url = "https://www.taobao.com/"
    
    fun open() {
        runBlocking {
            repeat(10) {
                val privacyContext = privacyManager.createUnmanagedContext(PrivacyAgent.NEXT_SEQUENTIAL, fetcher)
                privacyContext.open(url)
            }
        }
    }
    
    fun open2() {
        runBlocking {
            repeat(10) {
                val privacyContext = privacyManager.createUnmanagedContext(PrivacyAgent.NEXT_SEQUENTIAL)
                
                val task = FetchTask.create(url, components.conf.toVolatileConfig())
                privacyContext.run(task) { _, driver ->
                    fetcher.fetchDeferred(task, driver)
                }
            }
        }
    }
}

fun main() {
    ChromeGroup().open()
    readlnOrNull()
}
