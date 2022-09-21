package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.event.EventEmitter
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage

enum class BrowserResponseEvents {
    initPageCategorySniffer,
    initHTMLIntegrityChecker,
    willCreateResponse,
    responseCreated,
    browseTimeout,
}

interface BrowserResponseHandler: EventEmitter<BrowserResponseEvents> {

    val pageCategorySniffer: ChainedPageCategorySniffer

    val htmlIntegrityChecker: ChainedHtmlIntegrityChecker

    fun onInitPageCategorySniffer(sniffer: PageCategorySniffer)

    fun onInitHTMLIntegrityChecker(checker: HtmlIntegrityChecker)

    fun onWillCreateResponse(task: FetchTask, driver: WebDriver)

    fun onResponseCreated(task: FetchTask, driver: WebDriver, response: Response)

    fun checkErrorPage(page: WebPage, status: ProtocolStatus): ProtocolStatus

    /**
     * Normalize the page source.
     *
     * The browser has already converted source code to be UTF-8, so we replace the charset meta tags to be UTF-8.
     * TODO: or we insert a new metadata to indicate the charset
     */
    fun normalizePageSource(url: String, pageSource: String): StringBuilder

    fun onBrowseTimeout(task: NavigateTask)

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error.
     * */
    fun onChromeErrorPageReturn(message: String): BrowserError

    fun onPageSourceIsBroken(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus
}
