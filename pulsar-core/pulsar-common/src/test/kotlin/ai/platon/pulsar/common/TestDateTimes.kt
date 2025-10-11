package ai.platon.pulsar.common

import ai.platon.pulsar.common.DateTimes.format
import ai.platon.pulsar.common.DateTimes.now
import ai.platon.pulsar.common.DateTimes.parseDuration
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.DateUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.assertThrows
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestDateTimes {
    private val pattern = "yyyy-MM-dd HH:mm:ss"
    val seconds = 3600 * 24 + 3600 * 2 + 60 * 30 + 30L
    val instant = Instant.parse("2021-04-21T00:30:59.520Z")
    val date = LocalDate.parse("2021-04-21")
    val dateTime = LocalDateTime.parse("2021-04-21T00:30:59")
    val duration = Duration.between(instant.minusSeconds(seconds), instant)

    @BeforeTest
    fun setUp() {
        val zoneId = ZoneId.systemDefault()
        val zoneOffset = zoneId.rules.getOffset(Instant.now())
        // Assume the system timezone is UTC+8
        Assumptions.assumeTrue(zoneOffset == ZoneOffset.ofHours(8),
            "This test runs only when the system timezone is UTC+8, will improve in the further")
    }

    @Test
    fun testDateTimeConvert() {
        val zoneId = ZoneId.systemDefault()
        // ParseResult string into local date. LocalDateTime has no timezone component
        var time = LocalDateTime.parse("2014-04-16T13:00:00")
        // Convert to Instant with no time zone offset
        val instant = time.atZone(ZoneOffset.ofHours(0)).toInstant()
        // Easy conversion from Instant to the java.sql.Timestamp object
        val timestamp = Timestamp.from(instant)
        // Convert to LocalDateTime. Use no offset for timezone
        time = LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.ofHours(0))
        // Add time. In this case, add one day.
        time = time.plus(1, ChronoUnit.DAYS)
        // Convert back to instant, again, no time zone offset.
        val output = time.atZone(ZoneOffset.ofHours(0)).toInstant()
        println(output)
        val ldt = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MINUTES), zoneId)
        println(ldt)
        val middleNight = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        println("middle night local date time : $middleNight")
        println("middle night instance : " + Instant.now().truncatedTo(ChronoUnit.DAYS))
        println("duration : " + Duration.between(LocalDateTime.now(), middleNight.plus(1, ChronoUnit.DAYS)))
    }

    @Test
    fun testEpoch() {
        val now = Instant.now()
        println(now.epochSecond)
        println(now.epochSecond / 60)
        assertTrue { now.epochSecond < Int.MAX_VALUE }
    }

    @Test
    fun testDoomsday() {
        val doomsday = DateTimes.doomsday
        println(doomsday.epochSecond)
        println(doomsday.epochSecond / 60)
        assertTrue { doomsday.epochSecond > Int.MAX_VALUE }
        assertTrue { doomsday.epochSecond < Long.MAX_VALUE }
        assertTrue { doomsday.toEpochMilli() < Long.MAX_VALUE }
    }

    @Test
    fun testChronoFields() {
        // println(instant.getLong(ChronoField.MILLI_OF_SECOND))
        assertEquals(520, instant.getLong(ChronoField.MILLI_OF_SECOND))

        // println(date.getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH))
        assertEquals(7, date.getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH))

        // println(dateTime.getLong(ChronoField.MINUTE_OF_DAY))
        assertEquals(30, dateTime.getLong(ChronoField.MINUTE_OF_DAY))
    }

    @Test
    fun testDateTimeFormatter() {
        var time = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(0))
        var formatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time)
        println(formatted)
        time = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(0))
        formatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time)
        println(formatted)
        println(format(0))
        formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        println(formatted)
        println(format(Instant.now(), "yyyy-MM-dd HH:mm:ss"))
        println(now("yyyy/MM/dd"))
        val t = NumberUtils.toInt(format(Instant.now(), "yyyyMMddHH"), 0)
        assertTrue(t > 0)
        println(t)
    }

    @Test
    fun testDateTimeFormatter2() {
        val d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2016-06-16 12:21:18")
        assertEquals("Thu Jun 16 12:21:18 CST 2016", d.toString())
        assertEquals("2016-06-16T04:21:18Z", d.toInstant().toString())

        var t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .parse("2016-06-16 12:21:18") { LocalDateTime.from(it) }
        assertEquals("2016-06-16T12:21:18", t.toString())

        val pattern = "yyyy-MM-dd[ HH[:mm[:ss]]]"
        t = DateTimeFormatter.ofPattern(pattern).parse("2016-06-16 12:21") { LocalDateTime.from(it) }
        assertEquals("2016-06-16T12:21", t.toString())
        t = DateTimeFormatter.ofPattern(pattern).parse("2016-06-16 12") { LocalDateTime.from(it) }
        assertEquals("2016-06-16T12:00", t.toString())
        val ld = DateTimeFormatter.ofPattern(pattern).parse("2016-06-16") { LocalDate.from(it) }
        assertEquals("2016-06-16", ld.toString())
    }

    @Test
    fun testParseBestInstant() {
        val dateRegex = "\\d{4}-\\d{2}-\\d{2}"
        assertTrue { "2016-06-16".matches(dateRegex.toRegex()) }
        assertTrue { "2016-06-16 12:21:18".matches("$dateRegex\\s+\\d{2}:.+".toRegex()) }

        assertEquals("2016-06-16T04:21:18Z", DateTimes.parseBestInstant("2016-06-16 12:21:18").toString())
        assertEquals("2016-06-16T04:21:00Z", DateTimes.parseBestInstant("2016-06-16 12:21").toString())
        assertEquals("2016-06-16T04:00:00Z", DateTimes.parseBestInstant("2016-06-16 12:00").toString())
        assertEquals("2016-06-15T16:00:00Z", DateTimes.parseBestInstant("2016-06-16").toString())

        assertEquals("2016-06-16T04:21:18Z", DateTimes.parseBestInstant("2016-06-16T12:21:18").toString())
        assertEquals("2016-06-16T04:21:00Z", DateTimes.parseBestInstant("2016-06-16T12:21").toString())
        assertEquals("2016-06-16T04:00:00Z", DateTimes.parseBestInstant("2016-06-16T12:00").toString())
    }

    @Test
    fun testTimeZone() {
        val defaultZoneId = ZoneId.systemDefault()
        if (defaultZoneId.id != "Asia/Shanghai") {
            println("Only test time zone when the system time zone is Asia/Shanghai")
            return
        }

        val now = LocalDateTime.now()

        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        println(tz)
        val offset = tz.rawOffset
        println(offset)
        println(TimeZone.getDefault().id)
        println(ZoneId.systemDefault().id)

        val zoneId = DateTimes.zoneId
        assertEquals("Asia/Shanghai", tz.id)
        assertEquals(ZoneOffset.of("+08:00"), DateTimes.zoneOffset)
        println(DateTimes.zoneOffset)
    }

    @Test
    fun testDuration() {
        val epoch = Instant.EPOCH
        val gap = Duration.between(epoch, instant)
        // println(gap.toDays())
        assertEquals(18738, gap.toDays())
        // println(gap)
        assertEquals("PT449712H30M59.52S", gap.toString())
        val days = ChronoUnit.DAYS.between(epoch, instant)
//        println(days)
        assertEquals(18738, days)
        println(Duration.ofDays(365 * 100.toLong()).seconds)
        println(Duration.ofMinutes(60).toMillis())
        println(
            DurationFormatUtils.formatDuration(
                gap.toMillis(),
                "d\' days \'H\' hours \'m\' minutes \'s\' seconds\'"
            )
        )
        println(DurationFormatUtils.formatDuration(gap.toMillis(), "d\'days\' H:mm:ss"))
        val durationToMidnight = Duration.between(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS), LocalDateTime.now())
        println(durationToMidnight.plusDays(1))
        assertEquals(Duration.ofSeconds(1), parseDuration("PT1S", Duration.ZERO))
    }

    @Test
    fun testTemporalDefaultRange() {
        val p = Period.ofDays(30)
        println(p)

        var r = date.range(ChronoField.DAY_OF_MONTH)
        println("DAY_OF_MONTH: $r")

        r = date.range(ChronoField.DAY_OF_WEEK)
        println("DAY_OF_WEEK: $r")

        r = date.range(ChronoField.YEAR)
        println("YEAR: $r")

        r = date.range(ChronoField.DAY_OF_YEAR)
        println("DAY_OF_YEAR: $r")

        r = date.range(ChronoField.YEAR_OF_ERA)
        println("YEAR_OF_ERA: $r")

        r = date.range(ChronoField.EPOCH_DAY)
        println("EPOCH_DAY: $r")

        r = date.range(ChronoField.PROLEPTIC_MONTH)
        println("PROLEPTIC_MONTH: $r")

        r = date.range(ChronoField.ERA)
        println("ERA: $r")
    }

    @Test
    fun testTemporalRange() {
        // kotlin general range
        val range = dateTime.rangeTo(dateTime + Duration.ofSeconds(3600))
        println(range)
        assertTrue { dateTime in range }
    }

    @Test
    fun testDateFormat() {
        var dateString: String? = "Sat May 27 12:21:42 CST 2017"

        val date = Date()
        dateString = DateFormatUtils.format(date, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.pattern)
        println(dateString)
        dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
        println(dateString)
        val now = Instant.now()
        println(now)
        val ldt = LocalDateTime.now()
        println(ldt)

        val timestamp = 0L
        val fmt = "yyyyMMddHHmmss"
        val d = SimpleDateFormat(fmt).format(Date(timestamp))
        println(d)
    }

    @Ignore("A fix is required: unable to parse the date: Sat May 27 12:21:42 CST 2017")
    @Test
    fun testParseDate() {
        var dateString: String? = "Sat May 27 12:21:42 CST 2017"
        try {
            val date = DateUtils.parseDate(dateString, *DateTimeDetector.COMMON_DATE_TIME_FORMATS)
            // Date date = DateUtils.parseDate(dateString);
            dateString = DateFormatUtils.format(
                date,
                DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.pattern,
                TimeZone.getTimeZone("PRC")
            )
            println(dateString)
            dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
            println(dateString)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    @Test
    fun testTruncateLocalDateTime() {
        val dateTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val date = LocalDate.now()
        assertEquals(dateTime.dayOfMonth, date.dayOfMonth)
        assertEquals(dateTime.monthValue, date.monthValue)
    }

    /**
     *
     * Test illegal date format, when a date string is not in the correct format, it will throw an exception
     * */
    @Test
    fun testIllegalDateFormat() {
        var dateString = "2013-39-08 10:39:36"
        val e = assertThrows<DateTimeParseException> {
            val dateTime = DateTimeFormatter.ofPattern(pattern).parse(dateString)
            dateString = DateTimeFormatter.ISO_INSTANT.format(dateTime)
        }
    }

    @Test
    @Ignore("Time costing performance testing")
    fun testSystemClockPerformance() {
        val round = 10000000
        val impreciseNow = System.currentTimeMillis()
        var cost: Long
        var cost2: Long
        var cost3: Long
        var useless: Long
        var uselessTime: Instant?
        var start: Instant
        start = Instant.now()
        for (i in 0 until round) {
            useless = impreciseNow
        }
        cost = Instant.now().toEpochMilli() - start.toEpochMilli()
        start = Instant.now()
        for (i in 0 until round) {
            useless = System.currentTimeMillis()
        }
        cost2 = Instant.now().toEpochMilli() - start.toEpochMilli()
        start = Instant.now()
        for (i in 0 until round) {
            uselessTime = Instant.now()
        }
        cost3 = Instant.now().toEpochMilli() - start.toEpochMilli()
        assertTrue(cost <= cost2)
        assertTrue(cost2 < cost3, "System.currentTimeMillis() should be faster then Instant.now()")
        println("$cost, $cost2, $cost3")
    }

    @Test
    fun testExpiry() {
        val startTime = Instant.now().minusSeconds(120)
        val expiry = Duration.ofSeconds(60)
        assertTrue { DateTimes.isExpired(startTime, expiry) }
    }
}
