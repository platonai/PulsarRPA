package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.DateTimeDetector
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import java.time.Instant
import java.time.format.DateTimeFormatter

@UDFGroup(namespace = "TIME")
object TimeFunctions {
    private val defaultDateTime = Instant.EPOCH.atZone(AppContext.defaultZoneId).toLocalDateTime()

    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun firstMysqlDateTime(text: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (text.isNullOrBlank()) {
            return formatDefaultDateTime(pattern)
        }

        // java 8 formats
        if ('T' in text && text.endsWith("Z")) {
            return DateTimes.format(Instant.parse(text), pattern)
        }

        val s = DateTimeDetector(AppContext.defaultZoneId).detectDateTime(text)?.let { DateTimes.format(it, pattern) }
        return s ?: formatDefaultDateTime(pattern)
    }

    private fun formatDefaultDateTime(pattern: String): String {
        return DateTimes.format(defaultDateTime, pattern)
    }
}
