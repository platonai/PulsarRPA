package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType

abstract class AbstractWebDriver(
        val browserInstanceId: BrowserInstanceId,
        val id: Int = 0
): Comparable<AbstractWebDriver> {
    var proxyEntry: ProxyEntry? = null

    /**
     * The current loading page url
     * The browser might redirect, so it might not be the same with [currentUrl]
     * */
    var url: String = ""

    abstract val isCanceled: Boolean
    abstract val isQuit: Boolean
    abstract val isRetired: Boolean

    abstract val name: String
    abstract val browserType: BrowserType
    abstract val sessionId: String?
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
