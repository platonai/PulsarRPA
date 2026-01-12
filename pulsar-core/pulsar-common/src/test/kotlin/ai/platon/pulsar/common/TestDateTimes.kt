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
    fun testDoomsday() {
        val doomsday = DateTimes.doomsday
//        printlnPro(doomsday.epochSecond)
//        printlnPro(doomsday.epochSecond / 60)
        assertTrue { doomsday.epochSecond > Int.MAX_VALUE }
        assertTrue { doomsday.epochSecond < Long.MAX_VALUE }
        assertTrue { doomsday.toEpochMilli() < Long.MAX_VALUE }
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
    fun testExpiry() {
        val startTime = Instant.now().minusSeconds(120)
        val expiry = Duration.ofSeconds(60)
        assertTrue { DateTimes.isExpired(startTime, expiry) }
    }
}

