package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import java.time.Instant
import java.time.format.DateTimeFormatter

@UDFGroup(namespace = "TIME")
object DateTimeFunctions {
    private val logger = getLogger(DateTimeFunctions::class)
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

        try {
            val instant = DateTimes.parseBestInstant(text)
            return DateTimeFormatter.ofPattern(pattern).withZone(DateTimes.zoneId).format(instant)
        } catch (t: Throwable) {
            logger.warn("Failed handle date time: {}", text)
        }

        return formatDefaultDateTime(pattern)
    }

    private fun formatDefaultDateTime(pattern: String): String {
        return DateTimes.format(defaultDateTime, pattern)
    }
}
