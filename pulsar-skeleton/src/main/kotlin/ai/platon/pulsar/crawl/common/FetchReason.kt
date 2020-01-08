package ai.platon.pulsar.crawl.common

import java.util.*

object FetchReason {
    const val UNKNOWN = -1
    const val DO_NOT_FETCH = 0
    const val NEW_PAGE = 1
    const val EXPIRED = 2
    const val SMALL_CONTENT = 3
    const val MISS_FIELD = 4
    const val TEMP_MOVED = 300
    const val RETRY_ON_FAILURE = 301

    val codes = HashMap<Int, String>()
    
    fun toString(code: Int): String {
        return codes.getOrDefault(code, "unknown")
    }

    init {
        codes[DO_NOT_FETCH] = "do_not_fetch"
        codes[NEW_PAGE] = "new_page"
        codes[EXPIRED] = "expired"
        codes[SMALL_CONTENT] = "small"
        codes[MISS_FIELD] = "miss_field"
        codes[TEMP_MOVED] = "temp_moved"
        codes[RETRY_ON_FAILURE] = "retry_on_failure"
    }
}