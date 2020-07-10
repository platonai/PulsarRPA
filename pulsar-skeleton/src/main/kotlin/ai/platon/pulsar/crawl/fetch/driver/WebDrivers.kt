package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType

abstract class AbstractWebDriver: Comparable<AbstractWebDriver> {
    abstract val browserInstanceId: BrowserInstanceId
    abstract val url: String
    abstract val id: Int
    abstract val name: String
    abstract val browserType: BrowserType
    abstract val sessionId: String?
    abstract val proxyEntry: ProxyEntry?

    abstract val isCanceled: Boolean
    abstract val isQuit: Boolean
    abstract val isRetired: Boolean

    abstract val pageSource: String
    abstract fun navigateTo(url: String)
    abstract fun setTimeouts(driverConfig: BrowserControl)
    abstract fun evaluate(expression: String): Any?
    abstract fun stopLoading()
    abstract fun evaluateSilently(expression: String): Any?

    abstract fun free()
    abstract fun startWork()
    abstract fun retire()
    abstract fun quit()
    abstract fun cancel()
}
