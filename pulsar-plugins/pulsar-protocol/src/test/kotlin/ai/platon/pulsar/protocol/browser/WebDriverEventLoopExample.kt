package ai.platon.pulsar.protocol.browser

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.LoadingWebDriverPool
import ai.platon.pulsar.protocol.browser.experimental.react.WebDriverEventLoop
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {
    val conf = ImmutableConfig()
    val driverControl = WebDriverControl(conf)
    val proxyManager = ProxyPoolMonitor(conf = conf)
    val driverPoolFactory = WebDriverFactory(driverControl, proxyManager, conf)
    val driverPool = LoadingWebDriverPool(driverPoolFactory, conf)
    val loop = WebDriverEventLoop(driverPool, conf)

    GlobalScope.launch {
        loop.run()
    }

    var line = ""
    print("Enter an url> ")
    while (line != "bye") {
        line = readLine()?:"bye"
        println()
        if (line.startsWith("http")) {
            val page = WebPage.newInternalPage(line)
            val task = FetchTask(0, 0, page, conf.toVolatileConfig())
            loop.pendingTasks.add(task)
        }
        print("Enter an url> ")
    }

    loop.use { it.close() }
}
