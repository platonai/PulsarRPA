/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.Date;

import static ai.platon.pulsar.common.DateTimeDetector.CURRENT_DATE_EPOCH_DAYS;

public class DateTimeUtil {

    public static SimpleDateFormat PATH_SAFE_FORMAT_1 = new SimpleDateFormat("MMdd");
    public static SimpleDateFormat PATH_SAFE_FORMAT_2 = new SimpleDateFormat("MMdd.HH");
    public static SimpleDateFormat PATH_SAFE_FORMAT_3 = new SimpleDateFormat("MMdd.HHmm");
    public static SimpleDateFormat PATH_SAFE_FORMAT_4 = new SimpleDateFormat("MMdd.HHmmss");

    public static long HOURS_OF_DAY = 24L;
    public static long HOURS_OF_MONTH = HOURS_OF_DAY * 30;
    public static long HOURS_OF_YEAR = HOURS_OF_DAY * 365;

    public static Instant ONE_YEAR_LATER = Instant.now().plus(Duration.ofDays(365));

    public static String format(long time) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time));
    }

    public static String format(Instant time) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(time);
    }

    public static String format(Instant time, String format) {
        return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(time);
    }

    public static String format(LocalDateTime localTime) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localTime);
    }

    public static String format(LocalDateTime localTime, String format) {
        return DateTimeFormatter.ofPattern(format).format(localTime);
    }

    public static String format(long epochMilli, String format) {
        return format(Instant.ofEpochMilli(epochMilli), format);
    }

    public static String readableDuration(Duration duration) {
        return StringUtils.removeStart(duration.toString(), "PT").toLowerCase();
    }

    public static String readableDuration(@NotNull String duration) {
        return StringUtils.removeStart(duration, "PT").toLowerCase();
    }

    public static String isoInstantFormat(long time) {
        return DateTimeFormatter.ISO_INSTANT.format(new Date(time).toInstant());
    }

    public static String isoInstantFormat(Date date) {
        return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
    }

    public static String isoInstantFormat(Instant time) {
        return DateTimeFormatter.ISO_INSTANT.format(time);
    }

    public static String now(String format) {
        return format(System.currentTimeMillis(), format);
    }

    public static String now() {
        return format(LocalDateTime.now());
    }

    public static Duration elapsedTime(long start) {
        return elapsedTime(Instant.ofEpochMilli(start), Instant.now());
    }

    public static Duration elapsedTime(Instant start) {
        return elapsedTime(start, Instant.now());
    }

    /**
     * Calculate the elapsed time between two times specified in milliseconds.
     */
    public static Duration elapsedTime(Instant start, Instant end) {
        return Duration.between(start, end);
    }

    /**
     * RFC 2616 defines three different date formats that a conforming client must understand.
     */
    public static Instant parseHttpDateTime(String text, Instant defaultValue) {
        try {
            Date d = org.apache.http.client.utils.DateUtils.parseDate(text);
            return d.toInstant();
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static String formatHttpDateTime(long time) {
        return org.apache.http.client.utils.DateUtils.formatDate(new Date(time));
    }

    public static String formatHttpDateTime(Instant time) {
        return org.apache.http.client.utils.DateUtils.formatDate(Date.from(time));
    }

    public static Instant parseInstant(String text, Instant defaultValue) {
        try {
            // equals to Instant.parse()
            return DateTimeFormatter.ISO_INSTANT.parse(text, Instant::from);
        } catch (Throwable ignored) {
        }

        return defaultValue;
    }

    public static Duration parseDuration(String durationStr, Duration defaultValue) {
        try {
            return Duration.parse(durationStr);
        } catch (Throwable ignored) {
        }

        return defaultValue;
    }

    public static String constructTimeHistory(String timeHistory, Instant fetchTime, int maxRecords) {
        String dateStr = isoInstantFormat(fetchTime);

        if (timeHistory == null) {
            timeHistory = dateStr;
        } else {
            String[] fetchTimes = timeHistory.split(",");
            if (fetchTimes.length > maxRecords) {
                String firstFetchTime = fetchTimes[0];
                int start = fetchTimes.length - maxRecords;
                int end = fetchTimes.length;
                timeHistory = firstFetchTime + ',' + StringUtils.join(fetchTimes, ',', start, end);
            }
            timeHistory += ",";
            timeHistory += dateStr;
        }

        return timeHistory;
    }

    public static boolean isDaysBefore(OffsetDateTime dateTime, int days) {
        if (dateTime != null) {
            // ZonedDateTime ldt = date.atZone(ZoneId.systemDefault());
            if (CURRENT_DATE_EPOCH_DAYS - dateTime.toLocalDate().toEpochDay() > days) {
                return true;
            }
        }

        return false;
    }
}
