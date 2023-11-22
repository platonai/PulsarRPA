package ai.platon.pulsar.crawl.fetch.driver

import com.github.kklisura.cdt.protocol.v2023.types.network.LoadNetworkResourcePageResult
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class JsException(
    val text: String? = null,
    val lineNumber: Int? = null,
    val columnNumber: Int? = null,
    val url: String? = null
)

class JsEvaluation(
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
    /**
     * Navigate history is small, so search is very fast in a list.
     * */
    val history: Queue<NavigateEntry> = ConcurrentLinkedQueue()

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
        history.add(entry)
    }

    fun removeAll(url: String) {
        history.removeAll { it.url == url }
    }

    fun removeAll(urlRegex: Regex) {
        history.removeAll { it.url.matches(urlRegex) }
    }

    fun clear() {
        history.clear()
    }
}

class NetworkResourceResponse(
    val success: Boolean = false,
    val netError: Int = 0,
    val netErrorName: String = "",
    /** Request not made */
    val httpStatusCode: Int = 0,
    val stream: String? = null,
    val headers: Map<String, Any>? = null,
) {
    companion object {
        fun from(response: org.jsoup.Connection.Response): NetworkResourceResponse {
            val success = response.statusCode() == 200
            val httpStatusCode = response.statusCode()
            //    val stream = response.bodyStream()
            val stream = response.body()
            val headers = response.headers().toMutableMap()
            // All pulsar added headers have a prefix Q-
            headers["Q-client"] = "Jsoup"
            return NetworkResourceResponse(success, 0, "", httpStatusCode, stream, headers)
        }
        
        fun from(res: LoadNetworkResourcePageResult): NetworkResourceResponse {
            val success = res.success ?: false
            val netError = res.netError?.toInt() ?: 0
            val netErrorName = res.netErrorName ?: ""
            val httpStatusCode = res.httpStatusCode?.toInt() ?: 400
            // All pulsar added headers have a prefix Q-
            val headers = res.headers.toMutableMap()
            headers["Q-client"] = "Chrome"
            return NetworkResourceResponse(success, netError, netErrorName, httpStatusCode, res.stream, headers)
        }
    }
}
