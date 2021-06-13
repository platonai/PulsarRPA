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
    private val logger = getLogger(DateTimes::class)

    val PATH_SAFE_FORMAT_1 = SimpleDateFormat("MMdd")
    val PATH_SAFE_FORMAT_2 = SimpleDateFormat("MMdd.HH")
    val PATH_SAFE_FORMAT_3 = SimpleDateFormat("MMdd.HHmm")
    val PATH_SAFE_FORMAT_4 = SimpleDateFormat("MMdd.HHmmss")

    // inaccurate date time
    const val MILLIS_OF_SECOND = 1000L
    const val HOURS_OF_DAY = 24L
    const val HOURS_OF_MONTH = HOURS_OF_DAY * 30
    const val HOURS_OF_YEAR = HOURS_OF_DAY * 365
    val ONE_YEAR_LATER = Instant.now().plus(Duration.ofDays(365))

    const val DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}"
    const val DATE_TIME_REGEX = "${DATE_REGEX}T\\d{2}:.+"
    const val SIMPLE_DATE_TIME_REGEX = "$DATE_REGEX\\s+\\d{2}:.+"

    /**
     * The default zone id, it will be configurable
     * */
    var zoneId = ZoneId.of("Asia/Shanghai")
    /**
     * The default zone offset, it must be consistent with zoneId
     *
     * There is no one-to-one mapping. A ZoneId defines a geographic extent in which a set of different ZoneOffsets is used over time. If the timezone uses daylight saving time, its ZoneOffset will be different between summer and winter.
     * Furthermore, the daylight saving time rules may have changed over time, so the ZoneOffset could be different for e.g. 13/10/2015 compared to 13/10/1980.
     * So you can only find the ZoneOffset for a ZoneId on a particular Instant.
     * See also [Tz_database](https://en.wikipedia.org/wiki/Tz_database)
     * */
    var zoneOffSet = ZoneOffset.of("+08:00")
    /**
     * The time to start the program
     */
    val startTime = Instant.now()
    /**
     * We foresee that 2100 will be the end of the world, care about nothing after the doom
     * */
    val doomsday = Instant.parse("2100-01-01T00:00:00Z")

    val midnight get() = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
    val startOfHour get() = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)

    val elapsed get() = Duration.between(startTime, Instant.now())
    val elapsedToday get() = Duration.between(midnight, LocalDateTime.now())
    val elapsedThisHour get() = Duration.between(startOfHour, LocalDateTime.now())

    fun zoneIdOrDefault(name: String): ZoneId {
        return if (name in ZoneId.getAvailableZoneIds()) {
            ZoneId.of(name)
        } else {
            zoneId
        }
    }

    fun format(time: Long): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId).format(Instant.ofEpochMilli(time))
    }

    fun format(time: Instant): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId).format(time)
    }

    fun format(time: Instant, format: String): String {
        return DateTimeFormatter.ofPattern(format).withZone(zoneId).format(time)
    }

    fun format(localTime: LocalDateTime): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localTime)
    }

    fun format(localTime: LocalDateTime, format: String): String {
        return DateTimeFormatter.ofPattern(format).format(localTime)
    }

    fun format(localTime: OffsetDateTime, format: String): String {
        return DateTimeFormatter.ofPattern(format).format(localTime)
    }

    fun format(epochMilli: Long, format: String): String {
        return format(Instant.ofEpochMilli(epochMilli), format)
    }

    fun formatNow(format: String): String {
        return format(Instant.now(), format)
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

    fun now(format: String): String {
        return format(System.currentTimeMillis(), format)
    }

    @JvmStatic
    fun now(): String {
        return format(LocalDateTime.now())
    }

    fun startOfHour(): Instant {
        return Instant.now().truncatedTo(ChronoUnit.HOURS)
    }

    fun endOfHour(): Instant {
        return Instant.now().truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(1))
    }

    fun startOfDay(): Instant {
        return LocalDate.now().atStartOfDay().toInstant(zoneOffSet)
    }

    fun endOfDay(): Instant {
        return LocalDate.now().atStartOfDay().toInstant(zoneOffSet).plus(Duration.ofHours(24))
    }

    fun timePointOfDay(hour: Int, minute: Int = 0, second: Int = 0): Instant {
        return LocalDate.now().atTime(hour, minute, second).toInstant(zoneOffSet)
    }

    fun elapsedTime(): Duration {
        return elapsedTime(startTime, Instant.now())
    }

    fun elapsedTime(start: Long): Duration {
        return elapsedTime(Instant.ofEpochMilli(start), Instant.now())
    }

    /**
     * The imprecise elapsed time in seconds, at least 1 second
     * */
    fun elapsedSeconds(): Long {
        return elapsedTime().seconds.coerceAtLeast(1)
    }

    /**
     * Calculate the elapsed time between two times specified in milliseconds.
     */
    @JvmOverloads
    fun elapsedTime(start: Instant, end: Instant = Instant.now()): Duration {
        return Duration.between(start, end)
    }

    /**
     * RFC 2616 defines three different date formats that a conforming client must understand.
     */
    @JvmStatic
    fun parseHttpDateTime(text: String, defaultValue: Instant): Instant {
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
    fun formatHttpDateTime(time: Instant): String {
        return DateUtils.formatDate(Date.from(time))
    }

    @JvmOverloads
    @JvmStatic
    fun parseInstant(text: String, defaultValue: Instant = Instant.EPOCH): Instant {
        try {
            // equals to Instant.parse()
            return DateTimeFormatter.ISO_INSTANT.parse(text) { temporal: TemporalAccessor? -> Instant.from(temporal) }
        } catch (ignored: Throwable) {
        }
        return defaultValue
    }

    /**
     * Accept the following format:
     * 1. yyyy-MM-dd[ HH[:mm[:ss]]]
     * 2. ISO_INSTANT, or yyyy-MM-ddTHH:mm:ssZ
     * */
    @JvmOverloads
    @JvmStatic
    fun parseBestInstant(text: String, defaultValue: Instant = Instant.EPOCH): Instant {
        return parseBestInstantOrNull(text) ?: defaultValue
    }

    /**
     * Accept the following format:
     * 1. yyyy-MM-dd[ HH[:mm[:ss]]]
     * 2. ISO_INSTANT, or yyyy-MM-ddTHH:mm:ssZ
     * */
    @JvmStatic
    fun parseBestInstantOrNull(text: String): Instant? {
        try {
            return when {
                text.isBlank() -> null
                text.matches("${DATE_REGEX}T\\d{2}.+Z".toRegex()) -> {
                    Instant.parse(text)
                }
                text.matches(SIMPLE_DATE_TIME_REGEX.toRegex()) -> {
                    val pattern = "yyyy-MM-dd HH[:mm][:ss]"
                    DateTimeFormatter.ofPattern(pattern)
                        .parse(text) { LocalDateTime.from(it) }
                        .atZone(zoneId).toInstant()
                }
                text.matches(DATE_TIME_REGEX.toRegex()) -> {
                    val pattern = "yyyy-MM-dd'T'HH[:mm][:ss]"
                    DateTimeFormatter.ofPattern(pattern)
                        .parse(text) { LocalDateTime.from(it) }
                        .atZone(zoneId).toInstant()
                }
                text.matches(DATE_REGEX.toRegex()) -> {
                    val pattern = "yyyy-MM-dd"
                    DateTimeFormatter.ofPattern(pattern)
                        .parse(text) { LocalDate.from(it) }
                        .atStartOfDay().atZone(zoneId).toInstant()
                }
                else -> null
            }
        } catch (e: Throwable) {
            logger.warn("Failed to parse $text | {}", e)
        }

        return null
    }

    @JvmStatic
    fun parseDuration(duration: String, defaultValue: Duration): Duration {
        return try {
            Duration.parse(duration)
        } catch (ignored: Throwable) {
            defaultValue
        }
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
    fun isDaysBefore(dateTime: OffsetDateTime, days: Int): Boolean {
        return DateTimeDetector.CURRENT_DATE_EPOCH_DAYS - dateTime.toLocalDate().toEpochDay() > days
    }
}

fun Duration.readable() = DateTimes.readableDuration(this)
