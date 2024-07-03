package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.math.geometric.RectD
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.plus
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class AbstractJvmWebDriver: JvmWebDriver, WebDriver {
    private val interopScope = CoroutineScope(Dispatchers.Default) + CoroutineName("interop")

    override fun addInitScriptAsync(script: String) = interopScope.future { addInitScript(script) }
    override fun addBlockedURLsAsync(urls: List<String>) = interopScope.future { addBlockedURLs(urls) }
    override fun navigateToAsync(url: String) = interopScope.future { navigateTo(url) }
    override fun navigateToAsync(entry: NavigateEntry) = interopScope.future { navigateTo(entry) }
    override fun setTimeoutsAsync(browserSettings: BrowserSettings) = interopScope.future { setTimeouts(browserSettings) }
    override fun currentUrlAsync() = interopScope.future { currentUrl() }
    override fun pageSourceAsync() = interopScope.future { pageSource() }
    override fun getCookiesAsync() = interopScope.future { getCookies() }
    override fun bringToFrontAsync() = interopScope.future { bringToFront() }
    override fun waitForSelectorAsync(selector: String) = interopScope.future { waitForSelector(selector) }
    override fun waitForSelectorAsync(selector: String, timeoutMillis: Long) = interopScope.future { waitForSelector(selector, timeoutMillis) }
    override fun waitForSelectorAsync(selector: String, timeout: Duration) = interopScope.future { waitForSelector(selector, timeout) }
    override fun waitForNavigationAsync(oldUrl: String) = interopScope.future { waitForNavigation(oldUrl) }
    override fun waitForNavigationAsync(oldUrl: String, timeoutMillis: Long) = interopScope.future { waitForNavigation(oldUrl, timeoutMillis) }
    override fun waitForNavigationAsync(oldUrl: String, timeout: Duration) = interopScope.future { waitForNavigation(oldUrl, timeout) }
    override fun existsAsync(selector: String) = interopScope.future { exists(selector) }
    override fun isVisibleAsync(selector: String) = interopScope.future { isVisible(selector) }
    override fun visibleAsync(selector: String) = interopScope.future { visible(selector) }
    override fun isHiddenAsync(selector: String) = interopScope.future { isHidden(selector) }
    override fun isCheckedAsync(selector: String) = interopScope.future { isChecked(selector) }
    override fun typeAsync(selector: String, text: String) = interopScope.future { type(selector, text) }
    override fun clickAsync(selector: String, count: Int) = interopScope.future { click(selector, count) }
    override fun clickMatchesAsync(selector: String, pattern: String, count: Int) = interopScope.future { clickTextMatches(selector, pattern, count) }
    override fun clickMatchesAsync(selector: String, attrName: String, pattern: String, count: Int) =
        interopScope.future { clickMatches(selector, attrName, pattern, count) }
    override fun clickNthAnchorAsync(n: Int, rootSelector: String) = interopScope.future { clickNthAnchor(n, rootSelector) }
    override fun checkAsync(selector: String) = interopScope.future { check(selector) }
    override fun uncheckAsync(selector: String) = interopScope.future { uncheck(selector) }
    override fun scrollToAsync(selector: String) = interopScope.future { scrollTo(selector) }
    override fun scrollDownAsync(count: Int) = interopScope.future { scrollDown(count) }
    override fun scrollUpAsync(count: Int) = interopScope.future { scrollUp(count) }
    override fun scrollToTopAsync() = interopScope.future { scrollToTop() }
    override fun scrollToBottomAsync() = interopScope.future { scrollToBottom() }
    override fun scrollToMiddleAsync(ratio: Float) = interopScope.future { scrollToMiddle(ratio) }
    override fun mouseWheelDownAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) =
        interopScope.future { mouseWheelDown(count, deltaX, deltaY, delayMillis) }
    override fun mouseWheelUpAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) =
        interopScope.future { mouseWheelUp(count, deltaX, deltaY, delayMillis) }
    override fun moveMouseToAsync(x: Double, y: Double) = interopScope.future { moveMouseTo(x, y) }
    override fun dragAndDropAsync(selector: String, deltaX: Int, deltaY: Int) =
        interopScope.future { dragAndDrop(selector, deltaX, deltaY) }

    override fun outerHTMLAsync(selector: String) = interopScope.future { outerHTML(selector) }

    override fun selectFirstTextOrNullAsync(selector: String): CompletableFuture<String?> =
        interopScope.future { selectFirstTextOrNull(selector) }
    override fun selectFirstTextOptionalAsync(selector: String): CompletableFuture<Optional<String>> =
        interopScope.future { Optional.ofNullable(selectFirstTextOrNull(selector)) }
    override fun selectTextAllAsync(selector: String): CompletableFuture<List<String>> =
        interopScope.future { selectTextAll(selector) }

    override fun selectFirstAttributeOrNullAsync(selector: String, attrName: String): CompletableFuture<String?> =
        interopScope.future { selectFirstAttributeOrNull(selector, attrName) }
    override fun selectFirstAttributeOptionalAsync(selector: String, attrName: String): CompletableFuture<Optional<String>> =
        interopScope.future { Optional.ofNullable(selectFirstAttributeOrNull(selector, attrName)) }
    override fun selectAttributeAllAsync(selector: String, attrName: String): CompletableFuture<List<String>> =
        interopScope.future { selectAttributeAll(selector, attrName) }

    override fun evaluateAsync(expression: String) = interopScope.future { evaluate(expression) }
    override fun captureScreenshotAsync(selector: String) = interopScope.future { captureScreenshot(selector) }
    override fun captureScreenshotAsync(rect: RectD) = interopScope.future { captureScreenshot(rect) }
    override fun clickablePointAsync(selector: String) = interopScope.future { clickablePoint(selector) }
    override fun boundingBoxAsync(selector: String) = interopScope.future { boundingBox(selector) }
    override fun newJsoupSessionAsync() = interopScope.future { newJsoupSession() }
    override fun loadResourceAsync(url: String) = interopScope.future { loadResource(url) }
    override fun pauseAsync() = interopScope.future { pause() }
    override fun stopAsync() = interopScope.future { stop() }
}
