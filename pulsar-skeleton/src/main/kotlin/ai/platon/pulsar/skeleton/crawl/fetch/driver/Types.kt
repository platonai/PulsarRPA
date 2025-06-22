package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse
import org.jsoup.Connection
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class JsException(
    val text: String? = null,
    val lineNumber: Int? = null,
    val columnNumber: Int? = null,
    val url: String? = null
)

data class JsEvaluation(
    var value: Any? = null,
    var unserializableValue: String? = null,
    var className: String? = null,
    var description: String? = null,
    var exception: JsException? = null
)

/**
 * The webpage navigation history.
 * */
class NavigateHistory {
    private val _history = Collections.synchronizedList(ArrayList<NavigateEntry>())

    /**
     * Navigate history is small, so search is very fast in a list.
     * */
    val history: List<NavigateEntry> get() = _history

    /**
     *
     * */
    fun isEmpty() = history.isEmpty()

    fun isNotEmpty() = history.isNotEmpty()

    val size get() = history.size

    fun contains(url: String) = history.any { it.url == url }

    fun contains(urlRegex: Regex) = history.any { it.url.contains(urlRegex) }

    fun firstOrNull(url: String) = history.firstOrNull { it.url == url }

    fun firstOrNull(urlRegex: Regex) = history.firstOrNull { it.url.matches(urlRegex) }

    fun lastOrNull(url: String) = history.lastOrNull { it.url == url }

    fun lastOrNull(urlRegex: Regex) = history.lastOrNull { it.url.matches(urlRegex) }

    fun findAll(urlRegex: Regex) = history.filter { it.url.matches(urlRegex) }

    fun add(entry: NavigateEntry) {
        _history.add(entry)
    }

    fun removeAll(url: String) {
        _history.removeAll { it.url == url }
    }

    fun removeAll(urlRegex: Regex) {
        _history.removeAll { it.url.matches(urlRegex) }
    }

    fun clear() {
        _history.clear()
    }
}

object NetworkResourceHelper {
    fun fromJsoup(response: Connection.Response): NetworkResourceResponse {
        val success = response.statusCode() == 200
        val httpStatusCode = response.statusCode()
        //    val stream = response.bodyStream()
        val stream = response.body()
        val headers = response.headers().toMutableMap()
        // All pulsar added headers have a prefix Q-
        headers["Q-client"] = "Jsoup"
        return NetworkResourceResponse(success, 0, "", httpStatusCode, stream, headers)
    }
}
