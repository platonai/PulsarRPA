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
    private val defaultDateTime = Instant.EPOCH.atZone(DateTimes.zoneId).toLocalDateTime()

    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun firstMysqlDateTime(text: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return firstDateTime(text, pattern)
    }

    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun firstDateTime(text: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (text.isNullOrBlank()) {
            return formatDefaultDateTime(pattern)
        }

        val instant = DateTimes.parseBestInstant(text)
        return DateTimeFormatter.ofPattern(pattern).format(instant)
    }

    private fun formatDefaultDateTime(pattern: String): String {
        return DateTimes.format(defaultDateTime, pattern)
    }
}
