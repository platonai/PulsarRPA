package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.emulator.DefaultFetchComponents
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyAgent

class OpenChromeGroup {
    private val defaults = DefaultFetchComponents()
    private val fetcher = defaults.webdriverFetcher
    private val privacyManager = defaults.privacyManager
    
    val url = "https://www.taobao.com/"
    
    suspend fun open() {
        repeat(10) {
            val privacyContext = privacyManager.createUnmanagedContext(PrivacyAgent.NEXT_SEQUENTIAL, fetcher)
            privacyContext.open(url)
        }
    }
    
    suspend fun open2() {
        repeat(10) {
            val privacyContext = privacyManager.createUnmanagedContext(PrivacyAgent.NEXT_SEQUENTIAL)
            
            val task = FetchTask.create(url, defaults.conf.toVolatileConfig())
            privacyContext.run(task) { _, driver ->
                fetcher.fetchDeferred(task, driver)
            }
        }
    }
}

suspend fun main() {
    OpenChromeGroup().open()
    readlnOrNull()
}
