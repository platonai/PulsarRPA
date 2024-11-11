package ai.platon.pulsar.tools

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.emulator.DefaultPrivacyManagedBrowserFetcher
import ai.platon.pulsar.protocol.browser.emulator.context.MultiPrivacyContextManager
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId

fun main() {
    val session = SQLContexts.create()
    val conf = ImmutableConfig()
    val privacyManager = MultiPrivacyContextManager(conf)
    
    repeat(10) {
        val browserId = BrowserId.NEXT_SEQUENTIAL
        val privacyContext = privacyManager.createUnmanagedContext(browserId.privacyAgent)
        val url = "https://www.taobao.com/"
        val fetcher = DefaultPrivacyManagedBrowserFetcher(privacyManager, privacyManager.driverPoolManager)
        fetcher.fetch(url)

//        val task = FetchTask.create(url, conf.toVolatileConfig())
//        runBlocking {
//            privacyContext.run(task) { task, driver ->
//                val response = fetcher.fetch(url)
//                FetchResult(task, response)
//            }
//        }
    }
    
    readlnOrNull()
}
