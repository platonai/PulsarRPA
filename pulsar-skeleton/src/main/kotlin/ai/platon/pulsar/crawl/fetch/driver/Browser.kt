package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.common.event.EventEmitter
import ai.platon.pulsar.crawl.fetch.privacy.BrowserId

/**
 * The Browser defines methods and events to manipulate a real browser.
 */
interface Browser: EventEmitter<BrowserEvents>, AutoCloseable {

    /**
     * The unique browser id
     * */
    val id: BrowserId
    /**
     * The user agent to override, do not override if it's null.
     * */
    val userAgent: String?
    /**
     * The navigation history.
     * */
    val navigateHistory: NavigateHistory
    /**
     * The created drivers by this browser.
     * */
    val drivers: Map<String, WebDriver>
    /**
     * Check if this browser is idle.
     * */
    val isIdle: Boolean
    /**
     * Create a new driver.
     * */
    @Throws(WebDriverException::class)
    fun newDriver(): WebDriver

    /**
     * Destroy the web driver, close the associated browser tabs.
     * */
    fun destroyDriver(driver: WebDriver)

    /**
     * Destroy the browser forcibly, kill the associated browser processes, release all allocated resources,
     * regardless of whether the browser is closed or not.
     * */
    fun destroyForcibly()

    /**
     * Register event handler when a url is about to navigate.
     * */
    fun onWillNavigate(entry: NavigateEntry)
    /**
     * Maintain the browser
     * */
    fun maintain()
}
