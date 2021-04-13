package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType

interface WebDriver {
    val id: Int
    val url: String
    val browserInstanceId: BrowserInstanceId

    val isCanceled: Boolean
    val isQuit: Boolean
    val isRetired: Boolean

    val name: String
    val browserType: BrowserType
    val supportJavascript: Boolean
    val mockedPageSource: Boolean
    val sessionId: String?
    val currentUrl: String?
    val pageSource: String

    fun navigateTo(url: String)
    fun setTimeouts(driverConfig: BrowserControl)
    fun evaluate(expression: String): Any?
    fun bringToFront()
    fun stopLoading()
    fun evaluateSilently(expression: String): Any?

    fun free()
    fun startWork()
    fun retire()
    fun quit()
    fun cancel()
}
