package ai.platon.pulsar.crawl.common

import java.util.*

object FetchState {
    const val UNKNOWN = -1
    const val DO_NOT_FETCH = 0
    const val NEW_PAGE = 1
    const val EXPIRED = 2
    const val SMALL_CONTENT = 3
    const val MISS_FIELD = 4
    const val SCHEDULED = 2
    const val TEMP_MOVED = 300
    const val RETRY = 301

    val codes = HashMap<Int, String>()
    val symbols = HashMap<Int, String>()

    val refreshCodes = listOf(NEW_PAGE, EXPIRED, SCHEDULED, SMALL_CONTENT, MISS_FIELD, RETRY)

    fun toString(code: Int): String {
        return codes.getOrDefault(code, "unknown")
    }

    fun toSymbol(code: Int): String {
        return symbols.getOrDefault(code, "U")
    }

    init {
        codes[UNKNOWN] = "unknown"
        codes[DO_NOT_FETCH] = "do_not_fetch"
        codes[NEW_PAGE] = "new_page"
        codes[EXPIRED] = "expired"
        codes[SMALL_CONTENT] = "small"
        codes[MISS_FIELD] = "miss_field"
        codes[TEMP_MOVED] = "temp_moved"
        codes[RETRY] = "retry"

        symbols[UNKNOWN] = "U"
        symbols[DO_NOT_FETCH] = ""
        symbols[NEW_PAGE] = "N"
        symbols[EXPIRED] = "EX"
        symbols[SMALL_CONTENT] = "SC"
        symbols[MISS_FIELD] = "MF"
        symbols[TEMP_MOVED] = "TM"
        symbols[RETRY] = "R"
    }
}
