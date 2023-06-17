package ai.platon.pulsar.crawl.fetch.driver

import java.util.*

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

class NavigateHistory {
    val history: MutableList<NavigateEntry> = Collections.synchronizedList(mutableListOf())

    fun isEmpty() = history.isEmpty()

    fun isNotEmpty() = history.isNotEmpty()

    val size get() = history.size

    fun contains(url: String): Boolean {
        return history.any { it.url == url }
    }

    fun add(entry: NavigateEntry) {
        history.add(entry)
    }

    fun removeAll(url: String) {
        history.removeAll { it.url == url }
    }

    fun clear() {
        history.clear()
    }
}
