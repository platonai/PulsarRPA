package ai.platon.pulsar.common

import ai.platon.pulsar.common.DateTimes.format
import ai.platon.pulsar.common.DateTimes.now
import ai.platon.pulsar.common.DateTimes.parseDuration
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.DateUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.sql.Timestamp
import java.text.ParseException
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.fail

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestDateTimes {
    private val pattern = "yyyy-MM-dd HH:mm:ss"

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
        // System.out.println(now.getLong(ChronoField.MINUTE_OF_DAY));
        println(Int.MAX_VALUE)
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
        formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now())
        println(formatted)
        println(format(Instant.now(), "yyyy-MM-dd HH:mm:ss"))
        println(now("yyyy/MM/dd"))
        val t = NumberUtils.toInt(format(Instant.now(), "yyyyMMddHH"), 0)
        Assert.assertTrue(t > 0)
        println(t)
    }

    @Test
    fun testTimeZone() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        println(tz)
        val offset = tz.rawOffset
        println(offset)
        println(TimeZone.getDefault().id)
        println(ZoneId.systemDefault().id)
    }

    @Test
    fun testDuration() {
        val epoch = Instant.EPOCH
        val now = Instant.now()
        val gap = Duration.between(epoch, now)
        println(gap.toDays())
        println(gap)
        val days = ChronoUnit.DAYS.between(epoch, now)
        println(days)
        println(Duration.ofDays(365 * 100.toLong()).seconds)
        println(Duration.ofMinutes(60).toMillis())
        println(DurationFormatUtils.formatDuration(gap.toMillis(), "d\' days \'H\' hours \'m\' minutes \'s\' seconds\'"))
        println(DurationFormatUtils.formatDuration(gap.toMillis(), "d\'days\' H:mm:ss"))
        val durationToMidnight = Duration.between(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS), LocalDateTime.now())
        println(durationToMidnight.plusDays(1))
        Assert.assertEquals(Duration.ofSeconds(1), parseDuration("PT1S", Duration.ZERO))
    }

    @Test
    fun testDateFormat() {
        var dateString: String? = "Sat May 27 12:21:42 CST 2017"
        try {
            val date = DateUtils.parseDate(dateString, *DateTimeDetector.COMMON_DATE_TIME_FORMATS)
            // Date date = DateUtils.parseDate(dateString);
            dateString = DateFormatUtils.format(date, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.pattern, TimeZone.getTimeZone("PRC"))
            println(dateString)
            dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
            println(dateString)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val date = Date()
        dateString = DateFormatUtils.format(date, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.pattern)
        println(dateString)
        dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
        println(dateString)
        val now = Instant.now()
        println(now)
        val ldt = LocalDateTime.now()
        println(ldt)
    }

    @Test(expected = DateTimeParseException::class)
    fun testIllegalDateFormat() {
        var dateString = "2013-39-08 10:39:36"
        val dateTime = DateTimeFormatter.ofPattern(pattern).parse(dateString)
        dateString = DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }

    @Test
    @Ignore("Time costing performance testing")
    fun testSystemClockPerformance() {
        val round = 10000000
        val impreciseNow = System.currentTimeMillis()
        var cost: Long = 0
        var cost2: Long = 0
        var cost3: Long = 0
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
        Assert.assertTrue(cost <= cost2)
        Assert.assertTrue("System.currentTimeMillis() should be faster then Instant.now()", cost2 < cost3)
        println("$cost, $cost2, $cost3")
    }
}
