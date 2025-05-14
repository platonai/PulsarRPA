package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.math.geometric.RectD
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.dom.nodes.GeoAnchor
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.plus
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class AbstractJvmWebDriver : JvmWebDriver, WebDriver {
    private val interopScope = CoroutineScope(Dispatchers.Default) + CoroutineName("interop")

    override fun addInitScriptAsync(script: String) = interopScope.future { addInitScript(script) }
    override fun addBlockedURLsAsync(urls: List<String>) = interopScope.future { addBlockedURLs(urls) }
    override fun addProbabilityBlockedURLsAsync(urlPatterns: List<String>) =
        interopScope.future { addProbabilityBlockedURLs(urlPatterns) }

    override fun navigateToAsync(url: String) = interopScope.future { navigateTo(url) }
    override fun navigateToAsync(entry: NavigateEntry) = interopScope.future { navigateTo(entry) }
    override fun setTimeoutsAsync(browserSettings: BrowserSettings) =
        interopScope.future { setTimeouts(browserSettings) }

    override fun currentUrlAsync() = interopScope.future { currentUrl() }
    override fun urlAsync() = interopScope.future { url() }
    override fun documentURIAsync() = interopScope.future { documentURI() }
    override fun baseURIAsync() = interopScope.future { baseURI() }
    override fun referrerAsync() = interopScope.future { referrer() }
    override fun pageSourceAsync() = interopScope.future { pageSource() }
    override fun chatAsync(prompt: String, selector: String) = interopScope.future { chat(prompt, selector) }
    override fun instructAsync(prompt: String) = interopScope.future { instruct(prompt) }
    override fun getCookiesAsync() = interopScope.future { getCookies() }
    override fun deleteCookiesAsync(name: String) = interopScope.future { deleteCookies(name) }
    override fun deleteCookiesAsync(name: String, url: String?, domain: String?, path: String?) =
        interopScope.future { deleteCookies(name, url, domain, path) }

    override fun clearBrowserCookiesAsync() = interopScope.future { clearBrowserCookies() }
    override fun bringToFrontAsync() = interopScope.future { bringToFront() }
    override fun focusAsync(selector: String) = interopScope.future { focus(selector) }
    override fun waitForSelectorAsync(selector: String) = interopScope.future { waitForSelector(selector) }
    override fun waitForSelectorAsync(selector: String, timeoutMillis: Long) =
        interopScope.future { waitForSelector(selector, timeoutMillis) }

    override fun waitForSelectorAsync(selector: String, timeout: Duration) =
        interopScope.future { waitForSelector(selector, timeout) }

    override fun waitForNavigationAsync(oldUrl: String) = interopScope.future { waitForNavigation(oldUrl) }
    override fun waitForNavigationAsync(oldUrl: String, timeoutMillis: Long) =
        interopScope.future { waitForNavigation(oldUrl, timeoutMillis) }

    override fun waitForNavigationAsync(oldUrl: String, timeout: Duration) =
        interopScope.future { waitForNavigation(oldUrl, timeout) }

    override fun waitForPageAsync(url: String, timeout: Duration) = interopScope.future { waitForPage(url, timeout) }
    override fun waitUntilAsync(timeoutMillis: Long, predicate: () -> Boolean) =
        interopScope.future { waitUntil(timeoutMillis, predicate) }

    override fun waitUntilAsync(timeout: Duration, predicate: () -> Boolean) =
        interopScope.future { waitUntil(timeout, predicate) }

    override fun existsAsync(selector: String) = interopScope.future { exists(selector) }
    override fun isVisibleAsync(selector: String) = interopScope.future { isVisible(selector) }
    override fun visibleAsync(selector: String) = interopScope.future { visible(selector) }
    override fun isHiddenAsync(selector: String) = interopScope.future { isHidden(selector) }
    override fun isCheckedAsync(selector: String) = interopScope.future { isChecked(selector) }
    override fun typeAsync(selector: String, text: String) = interopScope.future { type(selector, text) }
    override fun fillAsync(selector: String, text: String) = interopScope.future { fill(selector, text) }
    override fun pressAsync(selector: String, key: String) = interopScope.future { press(selector, key) }
    override fun clickAsync(selector: String, count: Int) = interopScope.future { click(selector, count) }
    override fun clickTextMatchesAsync(selector: String, pattern: String, count: Int) =
        interopScope.future { clickTextMatches(selector, pattern, count) }

    //    override fun clickMatchesAsync(selector: String, pattern: String, count: Int) = interopScope.future { clickMatches(selector, pattern, count) }
    override fun clickAttributeMatchesAsync(selector: String, attrName: String, pattern: String, count: Int) =
        interopScope.future { clickAttributeMatches(selector, attrName, pattern, count) }

    override fun clickNthAnchorAsync(n: Int, rootSelector: String) =
        interopScope.future { clickNthAnchor(n, rootSelector) }

    override fun checkAsync(selector: String) = interopScope.future { check(selector) }
    override fun uncheckAsync(selector: String) = interopScope.future { uncheck(selector) }
    override fun scrollToAsync(selector: String) = interopScope.future { scrollTo(selector) }
    override fun scrollDownAsync(count: Int) = interopScope.future { scrollDown(count) }
    override fun scrollUpAsync(count: Int) = interopScope.future { scrollUp(count) }
    override fun scrollToTopAsync() = interopScope.future { scrollToTop() }
    override fun scrollToBottomAsync() = interopScope.future { scrollToBottom() }
    override fun scrollToMiddleAsync(ratio: Double) = interopScope.future { scrollToMiddle(ratio) }
    override fun scrollToScreenAsync(screenNumber: Double) = interopScope.future { scrollToScreen(screenNumber) }
    override fun mouseWheelDownAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) =
        interopScope.future { mouseWheelDown(count, deltaX, deltaY, delayMillis) }

    override fun mouseWheelUpAsync(count: Int, deltaX: Double, deltaY: Double, delayMillis: Long) =
        interopScope.future { mouseWheelUp(count, deltaX, deltaY, delayMillis) }

    override fun moveMouseToAsync(x: Double, y: Double) = interopScope.future { moveMouseTo(x, y) }
    override fun moveMouseToAsync(selector: String, deltaX: Int, deltaY: Int) =
        interopScope.future { moveMouseTo(selector, deltaX, deltaY) }

    override fun dragAndDropAsync(selector: String, deltaX: Int, deltaY: Int) =
        interopScope.future { dragAndDrop(selector, deltaX, deltaY) }

    override fun outerHTMLAsync() = interopScope.future { outerHTML() }
    override fun outerHTMLAsync(selector: String) = interopScope.future { outerHTML(selector) }
    override fun selectFirstTextOrNullAsync(selector: String): CompletableFuture<String?> =
        interopScope.future { selectFirstTextOrNull(selector) }

    override fun selectFirstTextOptionalAsync(selector: String): CompletableFuture<Optional<String>> =
        interopScope.future { Optional.ofNullable(selectFirstTextOrNull(selector)) }

    override fun selectTextAllAsync(selector: String): CompletableFuture<List<String>> =
        interopScope.future { selectTextAll(selector) }

    override fun selectFirstAttributeOrNullAsync(selector: String, attrName: String): CompletableFuture<String?> =
        interopScope.future { selectFirstAttributeOrNull(selector, attrName) }

    override fun selectFirstAttributeOptionalAsync(
        selector: String,
        attrName: String
    ): CompletableFuture<Optional<String>> =
        interopScope.future { Optional.ofNullable(selectFirstAttributeOrNull(selector, attrName)) }

    override fun selectAttributeAllAsync(selector: String, attrName: String): CompletableFuture<List<String>> =
        interopScope.future { selectAttributeAll(selector, attrName) }

    override fun selectAttributesAsync(selector: String): CompletableFuture<Map<String, String>> =
        interopScope.future { selectAttributes(selector) }

    override fun setAttributeAsync(selector: String, attrName: String, attrValue: String) =
        interopScope.future { setAttribute(selector, attrName, attrValue) }

    override fun setAttributeAllAsync(selector: String, attrName: String, attrValue: String) =
        interopScope.future { setAttributeAll(selector, attrName, attrValue) }

    override fun selectFirstPropertyValueOrNullAsync(selector: String, propName: String): CompletableFuture<String?> =
        interopScope.future { selectFirstPropertyValueOrNull(selector, propName) }

    override fun selectPropertyValueAllAsync(
        selector: String,
        propName: String,
        start: Int,
        limit: Int
    ): CompletableFuture<List<String>> =
        interopScope.future { selectPropertyValueAll(selector, propName, start, limit) }

    override fun setPropertyAsync(selector: String, propName: String, propValue: String) =
        interopScope.future { setProperty(selector, propName, propValue) }

    override fun setPropertyAllAsync(selector: String, propName: String, propValue: String) =
        interopScope.future { setPropertyAll(selector, propName, propValue) }

    override fun selectHyperlinksAsync(selector: String, offset: Int, limit: Int): CompletableFuture<List<Hyperlink>> =
        interopScope.future { selectHyperlinks(selector, offset, limit) }

    override fun selectAnchorsAsync(selector: String, offset: Int, limit: Int): CompletableFuture<List<GeoAnchor>> =
        interopScope.future { selectAnchors(selector, offset, limit) }

    override fun selectImagesAsync(selector: String, offset: Int, limit: Int): CompletableFuture<List<String>> =
        interopScope.future { selectImages(selector, offset, limit) }

    override fun evaluateAsync(expression: String): CompletableFuture<Any?> =
        interopScope.future { evaluate(expression) }

    override fun <T> evaluateAsync(expression: String, defaultValue: T): CompletableFuture<T> =
        interopScope.future { evaluate(expression, defaultValue) }

    override fun evaluateDetailAsync(expression: String): CompletableFuture<Any?> =
        interopScope.future { evaluateDetail(expression) }

    override fun evaluateValueAsync(expression: String): CompletableFuture<Any?> =
        interopScope.future { evaluateValue(expression) }

    override fun <T> evaluateValueAsync(expression: String, defaultValue: T): CompletableFuture<T> =
        interopScope.future { evaluateValue(expression, defaultValue) }

    override fun evaluateValueDetailAsync(expression: String): CompletableFuture<Any?> =
        interopScope.future { evaluateValueDetail(expression) }

    override fun captureScreenshotAsync(): CompletableFuture<String?> =
        interopScope.future { captureScreenshot() }

    override fun captureScreenshotAsync(selector: String) = interopScope.future { captureScreenshot(selector) }
    override fun captureScreenshotAsync(rect: RectD) = interopScope.future { captureScreenshot(rect) }
    override fun clickablePointAsync(selector: String) = interopScope.future { clickablePoint(selector) }
    override fun boundingBoxAsync(selector: String) = interopScope.future { boundingBox(selector) }
    override fun newJsoupSessionAsync() = interopScope.future { newJsoupSession() }
    override fun loadJsoupResourceAsync(url: String) = interopScope.future { loadJsoupResource(url) }
    override fun loadResourceAsync(url: String) = interopScope.future { loadResource(url) }
    override fun delayAsync(millis: Long) = interopScope.future { delay(millis) }
    override fun delayAsync(duration: Duration) = interopScope.future { delay(duration) }
    override fun pauseAsync() = interopScope.future { pause() }
    override fun stopAsync() = interopScope.future { stop() }
}
