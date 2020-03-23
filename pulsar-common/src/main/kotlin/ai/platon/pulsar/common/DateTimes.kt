package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.DateUtils
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.util.*

object DateTimes {
    val PATH_SAFE_FORMAT_1 = SimpleDateFormat("MMdd")
    val PATH_SAFE_FORMAT_2 = SimpleDateFormat("MMdd.HH")
    val PATH_SAFE_FORMAT_3 = SimpleDateFormat("MMdd.HHmm")
    val PATH_SAFE_FORMAT_4 = SimpleDateFormat("MMdd.HHmmss")
    val HOURS_OF_DAY = 24L
    val HOURS_OF_MONTH = HOURS_OF_DAY * 30
    val HOURS_OF_YEAR = HOURS_OF_DAY * 365
    val ONE_YEAR_LATER = Instant.now().plus(Duration.ofDays(365))

    fun format(time: Long): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))
    }

    fun format(time: Instant?): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(time)
    }

    fun format(time: Instant?, format: String?): String {
        return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(time)
    }

    fun format(localTime: LocalDateTime?): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localTime)
    }

    fun format(localTime: LocalDateTime?, format: String?): String {
        return DateTimeFormatter.ofPattern(format).format(localTime)
    }

    fun format(epochMilli: Long, format: String?): String {
        return format(Instant.ofEpochMilli(epochMilli), format)
    }

    @JvmOverloads
    fun readableDuration(duration: Duration, truncatedToUnit: ChronoUnit = ChronoUnit.SECONDS): String {
        return StringUtils.removeStart(duration.truncatedTo(truncatedToUnit).toString(), "PT").toLowerCase()
    }

    fun readableDuration(duration: String): String {
        return StringUtils.removeStart(duration, "PT").toLowerCase()
    }

    fun isoInstantFormat(time: Long): String {
        return DateTimeFormatter.ISO_INSTANT.format(Date(time).toInstant())
    }

    fun isoInstantFormat(date: Date): String {
        return DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
    }

    @JvmStatic
    fun isoInstantFormat(time: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(time)
    }

    fun now(format: String?): String {
        return format(System.currentTimeMillis(), format)
    }

    @JvmStatic
    fun now(): String {
        return format(LocalDateTime.now())
    }

    fun elapsedTime(start: Long): Duration {
        return elapsedTime(Instant.ofEpochMilli(start), Instant.now())
    }

    /**
     * Calculate the elapsed time between two times specified in milliseconds.
     */
    @JvmOverloads
    fun elapsedTime(start: Instant?, end: Instant? = Instant.now()): Duration {
        return Duration.between(start, end)
    }

    /**
     * RFC 2616 defines three different date formats that a conforming client must understand.
     */
    @JvmStatic
    fun parseHttpDateTime(text: String?, defaultValue: Instant): Instant {
        return try {
            val d = DateUtils.parseDate(text)
            d.toInstant()
        } catch (e: Throwable) {
            defaultValue
        }
    }

    fun formatHttpDateTime(time: Long): String {
        return DateUtils.formatDate(Date(time))
    }

    @JvmStatic
    fun formatHttpDateTime(time: Instant?): String {
        return DateUtils.formatDate(Date.from(time))
    }

    @JvmStatic
    fun parseInstant(text: String?, defaultValue: Instant): Instant {
        try { // equals to Instant.parse()
            return DateTimeFormatter.ISO_INSTANT.parse(text) { temporal: TemporalAccessor? -> Instant.from(temporal) }
        } catch (ignored: Throwable) {
        }
        return defaultValue
    }

    @JvmStatic
    fun parseDuration(durationStr: String?, defaultValue: Duration): Duration {
        try {
            return Duration.parse(durationStr)
        } catch (ignored: Throwable) {
        }
        return defaultValue
    }

    @JvmStatic
    fun constructTimeHistory(timeHistory: String?, fetchTime: Instant, maxRecords: Int): String? {
        var history = timeHistory
        val dateStr = isoInstantFormat(fetchTime)
        if (history == null) {
            history = dateStr
        } else {
            val fetchTimes = history.split(",").toTypedArray()
            if (fetchTimes.size > maxRecords) {
                val firstFetchTime = fetchTimes[0]
                val start = fetchTimes.size - maxRecords
                val end = fetchTimes.size
                history = firstFetchTime + ',' + StringUtils.join(fetchTimes, ',', start, end)
            }
            history += ","
            history += dateStr
        }
        return history
    }

    @JvmStatic
    fun isDaysBefore(dateTime: OffsetDateTime?, days: Int): Boolean {
        if (dateTime != null) { // ZonedDateTime ldt = date.atZone(ZoneId.systemDefault());
            if (DateTimeDetector.CURRENT_DATE_EPOCH_DAYS - dateTime.toLocalDate().toEpochDay() > days) {
                return true
            }
        }
        return false
    }
}
