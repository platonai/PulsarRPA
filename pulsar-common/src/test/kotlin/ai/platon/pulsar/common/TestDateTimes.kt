package ai.platon.pulsar.common;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestDateTime {

    final private String pattern = "yyyy-MM-dd HH:mm:ss";

    @Test
    public void testDateTimeConvert() {
        ZoneId zoneId = ZoneId.systemDefault();

        // ParseResult string into local date. LocalDateTime has no timezone component
        LocalDateTime time = LocalDateTime.parse("2014-04-16T13:00:00");

        // Convert to Instant with no time zone offset
        Instant instant = time.atZone(ZoneOffset.ofHours(0)).toInstant();

        // Easy conversion from Instant to the java.sql.Timestamp object
        Timestamp timestamp = Timestamp.from(instant);

        // Convert to LocalDateTime. Use no offset for timezone
        time = LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.ofHours(0));

        // Add time. In this case, add one day.
        time = time.plus(1, ChronoUnit.DAYS);

        // Convert back to instant, again, no time zone offset.
        Instant output = time.atZone(ZoneOffset.ofHours(0)).toInstant();
        System.out.println(output);

        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MINUTES), zoneId);
        System.out.println(ldt);

        LocalDateTime middleNight = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        System.out.println("middle night local date time : " + middleNight);
        System.out.println("middle night instance : " + Instant.now().truncatedTo(ChronoUnit.DAYS));
        System.out.println("duration : " + Duration.between(LocalDateTime.now(), middleNight.plus(1, ChronoUnit.DAYS)));
    }

    @Test
    public void testEpoch() {
        Instant now = Instant.now();
        System.out.println(now.getEpochSecond());
        System.out.println(now.getEpochSecond() / 60);
        // System.out.println(now.getLong(ChronoField.MINUTE_OF_DAY));
        System.out.println(Integer.MAX_VALUE);

    }

    @Test
    public void testDateTimeFormatter() {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(0));
        String formatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time);
        System.out.println(formatted);

        time = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofHours(0));
        formatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time);
        System.out.println(formatted);

        System.out.println(DateTimeUtil.format(0));

        formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now());
        System.out.println(formatted);

        System.out.println(DateTimeUtil.format(Instant.now(), "yyyy-MM-dd HH:mm:ss"));

        System.out.println(DateTimeUtil.now("yyyy/MM/dd"));

        int t = NumberUtils.toInt(DateTimeUtil.format(Instant.now(), "yyyyMMddHH"), 0);
        assertTrue(t > 0);
        System.out.println(t);
    }

    @Test
    public void testTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        System.out.println(tz);
        int offset = tz.getRawOffset();
        System.out.println(offset);

        System.out.println(TimeZone.getDefault().getID());

        System.out.println(ZoneId.systemDefault().getId());

    }

    @Test
    public void testDuration() {
        Instant epoch = Instant.EPOCH;
        Instant now = Instant.now();

        Duration gap = Duration.between(epoch, now);
        System.out.println(gap.toDays());
        System.out.println(gap);

        long days = ChronoUnit.DAYS.between(epoch, now);
        System.out.println(days);

        System.out.println(Duration.ofDays(365 * 100).getSeconds());

        System.out.println(Duration.ofMinutes(60).toMillis());

        System.out.println(DurationFormatUtils.formatDuration(gap.toMillis(), "d\' days \'H\' hours \'m\' minutes \'s\' seconds\'"));
        System.out.println(DurationFormatUtils.formatDuration(gap.toMillis(), "d\'days\' H:mm:ss"));

        Duration durationToMidnight = Duration.between(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS), LocalDateTime.now());
        System.out.println(durationToMidnight.plusDays(1));

        assertEquals(Duration.ofSeconds(1), DateTimeUtil.parseDuration("PT1S", Duration.ZERO));
    }

    @Test
    public void testDateFormat() {
        String dateString = "Sat May 27 12:21:42 CST 2017";

        try {
            Date date = DateUtils.parseDate(dateString, DateTimeDetector.COMMON_DATE_TIME_FORMATS);
            // Date date = DateUtils.parseDate(dateString);
            dateString = DateFormatUtils.format(date, ISO_DATETIME_TIME_ZONE_FORMAT.getPattern(), TimeZone.getTimeZone("PRC"));
            System.out.println(dateString);

            dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
            System.out.println(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Date date = new Date();
        dateString = DateFormatUtils.format(date, ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
        System.out.println(dateString);

        dateString = DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
        System.out.println(dateString);

        Instant now = Instant.now();
        System.out.println(now);

        LocalDateTime ldt = LocalDateTime.now();
        System.out.println(ldt);
    }

    @Test
    public void testIlligalDateFormat() {
        String dateString = "2013-39-08 10:39:36";
        try {
            TemporalAccessor dateTime = DateTimeFormatter.ofPattern(pattern).parse(dateString);
            dateString = DateTimeFormatter.ISO_INSTANT.format(dateTime);
            System.out.println(dateString);
        } catch (DateTimeParseException e) {
            System.out.println(e.toString());
        }
    }

    @Test
    @Ignore("Time costing performance testing")
    public void testSystemClockPerformance() {
        final int ROUND = 10000000;
        long impreciseNow = System.currentTimeMillis();
        long cost = 0;
        long cost2 = 0;
        long cost3 = 0;
        Long useless;
        Instant uselessTime;

        Instant start;

        start = Instant.now();
        for (int i = 0; i < ROUND; ++i) {
            useless = impreciseNow;
        }
        cost = Instant.now().toEpochMilli() - start.toEpochMilli();

        start = Instant.now();
        for (int i = 0; i < ROUND; ++i) {
            useless = System.currentTimeMillis();
        }
        cost2 = Instant.now().toEpochMilli() - start.toEpochMilli();

        start = Instant.now();
        for (int i = 0; i < ROUND; ++i) {
            uselessTime = Instant.now();
        }
        cost3 = Instant.now().toEpochMilli() - start.toEpochMilli();

        assertTrue(cost <= cost2);
        assertTrue("System.currentTimeMillis() should be faster then Instant.now()", cost2 < cost3);

        System.out.println(cost + ", " + cost2 + ", " + cost3);
    }
}
