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

package fun.platonic.pulsar.common;

import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static fun.platonic.pulsar.common.DateTimeDetector.CURRENT_DATE_EPOCH_DAYS;

public class DateTimeUtil {

    public static SimpleDateFormat FilesystemSafeDateFormat = new SimpleDateFormat("MMdd.HHmmss");

    public static long[] TIME_FACTOR = {60 * 60 * 1000, 60 * 1000, 1000};

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

    public static String elapsedTime(long start) {
        return elapsedTime(start, System.currentTimeMillis());
    }

    public static String elapsedTime(Instant start) {
        return elapsedTime(start.toEpochMilli(), System.currentTimeMillis());
    }

    public static double elapsedSeconds(long start) {
        return (System.currentTimeMillis() - start) / 1000.0;
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

    /**
     * Calculate the elapsed time between two times specified in milliseconds.
     *
     * @param start The start of the time period
     * @param end   The end of the time period
     * @return a string of the form "XhYmZs" when the elapsed time is X hours, Y
     * minutes and Z seconds or null if start > end.
     */
    private static String elapsedTime(long start, long end) {
        if (start > end) {
            return null;
        }

        long[] elapsedTime = new long[TIME_FACTOR.length];

        for (int i = 0; i < TIME_FACTOR.length; i++) {
            elapsedTime[i] = start > end ? -1 : (end - start) / TIME_FACTOR[i];
            start += TIME_FACTOR[i] * elapsedTime[i];
        }

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elapsedTime.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(nf.format(elapsedTime[i]));
        }

        return sb.toString();
    }
}
