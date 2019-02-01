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
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DateTimeDetector {

    public static final int MIN_DATE_TIME_STR_LENGTH = "2015-01-01 12:00".length();
    public static final int MIN_YEAR_MONTH_STR_LENGTH = "201501".length();
    public static final int MIN_DATE_STR_LENGTH = "20150101".length();
    public static final int MAX_META_STR_LENGTH = 200;
    public static final int MAX_DATE_TIME_STR_LENGTH = "EEE, dd MMM yyyy HH:mm:ss zzz".length();
    public static final int MAX_TITLE_LENGTH = 350;

    public static final String[] BAD_DATE_TIME_STRING_CONTAINS = new String[]{
            "GMT+8",
            "UTC+8",
            "Processed",
            "访问",
            "刷新",
            "visit"
    };

    public static final int OLD_DATE_DAYS = 30;
    public static final LocalDate CURRENT_DATE = LocalDate.now();
    public static final long CURRENT_DATE_EPOCH_DAYS = CURRENT_DATE.toEpochDay();
    public static final int CURRENT_YEAR = CURRENT_DATE.getYear();
    public static final String CURRENT_YEAR_STR = String.valueOf(CURRENT_YEAR);
    public static final int CURRENT_MONTH = CURRENT_DATE.getMonthValue();
    public static final int YEAR_LOWER_BOUND = 1990;
    public static final List<String> VALID_WORK_YEARS = IntStream.range(2010, 2030)
            .mapToObj(String::valueOf).collect(Collectors.toList());
    public static final List<String> VALID_WORK_YEARS_SHORT = IntStream.range(10, 30)
            .mapToObj(String::valueOf).collect(Collectors.toList());
    public static final String[] VALID_WORK_YEARS_ARRAY = VALID_WORK_YEARS.toArray(new String[0]);
    public static final String[] VALID_WORK_YEARS_SHORT_ARRAY = VALID_WORK_YEARS_SHORT.toArray(new String[0]);
    public static Set<String> OLD_YEARS;
    public static Set<String> OLD_MONTH;
    public static Pattern OLD_MONTH_URL_DATE_PATTERN;
    // 2016-03-05 20:07:51
    // TODO : What's the difference between HH and hh? 24 hours VS 12 hours?
    public static String[] COMMON_DATE_FORMATS = new String[]{
            "yyyyMMdd",
            "yyyy.MM.dd",
            "yyyy-MM-dd",
            "yyyy年MM月dd日",
            "yyyy/MM/dd",
    };
    public static String[] COMMON_DATE_TIME_FORMATS = new String[]{
            "yyyy.MM.dd HH:mm:ss",

            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd hh:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd hh:mm",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",

            "yyyy年MM月dd日 HH:mm",
            "yyyy年MM月dd日 hh:mm",
            "yyyy年MM月dd日 HH:mm:ss",
            "yyyy年MM月dd日 hh:mm:ss",

            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd hh:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd hh:mm:ss",
            "yyyy/MM/dd HH:mm:ss.SSS zzz",
            "yyyy/MM/dd HH:mm:ss.SSS",
            "yyyy/MM/dd HH:mm:ss zzz",

            "MMM dd yyyy HH:mm:ss. zzz",
            "MMM dd yyyy HH:mm:ss zzz",
            "dd.MM.yyyy HH:mm:ss zzz",
            "dd MM yyyy HH:mm:ss zzz",
            "dd.MM.yyyy zzz",
            "dd.MM.yyyy; HH:mm:ss",
            "dd.MM.yyyy HH:mm:ss",

            "EEE MMM dd HH:mm:ss yyyy",
            "EEE MMM dd HH:mm:ss yyyy zzz",
            "EEE MMM dd HH:mm:ss zzz yyyy",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE,dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:sszzz",
            "EEE, dd MMM yyyy HH:mm:ss",
            "EEE, dd-MMM-yy HH:mm:ss zzz"
    };

    static {
        // 1 years ago
        OLD_YEARS = IntStream.range(YEAR_LOWER_BOUND, CURRENT_YEAR).mapToObj(String::valueOf).collect(Collectors.toSet());
        // 2 month ago
        OLD_MONTH = IntStream.range(1, CURRENT_MONTH - 1).mapToObj(m -> String.format("%02d", m)).collect(Collectors.toSet());
        String monthPattern = StringUtils.join(OLD_MONTH, "|");
        if (CURRENT_MONTH <= 2) {
            monthPattern = "\\d{2}";
        }
        // eg : ".+2016[/\.-]?(01|02|03|04|05|06|07|08|09).+"
        OLD_MONTH_URL_DATE_PATTERN = Pattern.compile(".+" + CURRENT_YEAR + "[/\\.-]?(" + monthPattern + ").+");
    }

    private final String[] dateFormats;
    private final String[] dateTimeFormats;
    private ZoneId zoneId = ZoneId.systemDefault();

    public DateTimeDetector() {
        this(COMMON_DATE_FORMATS, COMMON_DATE_TIME_FORMATS);
    }

    public DateTimeDetector(String[] dateFormats, String[] dateTimeFormats) {
        this.dateFormats = dateFormats;
        this.dateTimeFormats = dateTimeFormats;
        if (CURRENT_YEAR > 2030) {
            System.out.println("This program must be refined after 2030");
            System.exit(2030);
        }
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public String detectPossibleDateTimeString(String text) {
        String possibleDate = StringUtils.substringBefore(text, "\n");

        int dateStart = StringUtils.indexOfAny(possibleDate, VALID_WORK_YEARS_ARRAY);
        if (dateStart == StringUtils.INDEX_NOT_FOUND) {
            dateStart = StringUtils.indexOfAny(possibleDate, VALID_WORK_YEARS_SHORT_ARRAY);
            if (dateStart != StringUtils.INDEX_NOT_FOUND) {
                // OK for years
                possibleDate = "20" + possibleDate.substring(dateStart);
                if (possibleDate.matches("20[0-9][0-9][\\-\\./年]?\\d+.+")) {
                    dateStart = 0;
                }
            }
        }

        if (dateStart < 0 || dateStart >= possibleDate.length()) {
            return null;
        }

        int dateEnd = Math.min(possibleDate.length(), dateStart + MAX_DATE_TIME_STR_LENGTH);

        possibleDate = possibleDate.substring(dateStart, dateEnd);
        possibleDate = possibleDate.replaceAll("[\\./年月]", "-").trim();

        return possibleDate;
    }

    public OffsetDateTime detectDateTimeLeniently(String text) {
        OffsetDateTime dateTime = detectDateTime(text);
        if (dateTime == null) {
            dateTime = detectDate(text);
        }

        return dateTime;
    }

    /**
     * Sniffy publish date
     */
    public OffsetDateTime detectDateTime(String text) {
        if (text == null || text.length() < MIN_DATE_TIME_STR_LENGTH) {
            return null;
        }

        // \p{Z} or \p{Separator}: any kind of whitespace or invisible separator.
        text = text.replaceAll("\\p{Zs}", " ").trim();
        // text = text.replaceAll("\\s+", " ").trim();

        // May be automatically generated date time
        final String finalText = text;
        if (Stream.of(BAD_DATE_TIME_STRING_CONTAINS).anyMatch(finalText::contains)) {
            return null;
        }

        final int dateTimeStart = StringUtils.indexOfAny(text, VALID_WORK_YEARS_ARRAY);
        if (dateTimeStart == StringUtils.INDEX_NOT_FOUND) {
            return null;
        }
        // For example : "2017-12-20 11:18:35"
        final int dateEnd = StringUtils.indexOf(text, " ", dateTimeStart);
        if (dateEnd < 0) {
            // TODO : try getTextDocument date
        }

        // Find the datetime string's end
        final int timeStart = dateEnd + 1;
        int pos = timeStart;
        for (; pos < text.length(); ++pos) {
            Character ch = text.charAt(pos);
            if (!Character.isDigit(ch) && ch != ':') {
                break;
            }
        }
        final int dateTimeEnd = pos;

        String possibleDate = StringUtils.substring(text, dateTimeStart, dateTimeEnd);

        // try getTextDocument datetime
        OffsetDateTime dateTime = null;
        if (possibleDate.length() >= MIN_DATE_TIME_STR_LENGTH) {
            dateTime = parseDateTimeStrictly(possibleDate);
        }

        return dateTime;
    }

    public YearMonth detectYearMonth(String text) {
        String possibleYearMonth = detectPossibleDateTimeString(text);
        return possibleYearMonth != null ? tryParseYearMonthStrictly(possibleYearMonth) : null;
    }

    public YearMonth tryParseYearMonthStrictly(String possibleYearMonth) {
        try {
            final Pattern pattern = Pattern.compile("20[0-9][0-9][0-1][0-9].+");
            final Pattern pattern2 = Pattern.compile("20[0-9][0-9]-[0-1]?[0-9].+");
            if (pattern.matcher(possibleYearMonth).matches()) {
                possibleYearMonth = possibleYearMonth.substring(0, 4) + "-" + possibleYearMonth.substring(4, 6);
                return YearMonth.parse(possibleYearMonth);
            } else if (pattern2.matcher(possibleYearMonth).matches()) {
                String[] parts = possibleYearMonth.split("-");
                if (parts.length >= 2 && parts[0].length() == 4) {
                    if (parts[1].length() == 1) {
                        parts[1] = "0" + parts[1];
                    }

                    possibleYearMonth = parts[0] + "-" + parts[1];

                    return YearMonth.parse(possibleYearMonth);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public OffsetDateTime detectDate(String text) {
        String possibleDate = detectPossibleDateTimeString(text);
        return possibleDate != null ? tryParseDateStrictly(possibleDate) : null;
    }

    public OffsetDateTime tryParseDateStrictly(String possibleDate) {
        try {
            final Pattern pattern = Pattern.compile("20[0-9][0-9][0-1][0-9][0-3][0-9].+");
            final Pattern pattern2 = Pattern.compile("20[0-9][0-9]-[0-1]?[0-9]-[0-3]?[0-9].+");

            if (pattern.matcher(possibleDate).matches()) {
                possibleDate = StringUtils.substring(possibleDate, 0, "yyyyMMdd".length());
            } else if (pattern2.matcher(possibleDate).matches()) {
                String[] parts = possibleDate.split("-");
                if (parts.length >= 3 && parts[0].length() == 4) {
                    if (parts[1].length() == 1) {
                        parts[1] = "0" + parts[1];
                    }
                    if (parts[2].length() == 1) {
                        parts[2] = "0" + parts[2];
                    }
                    if (parts[2].length() > 2) {
                        parts[2] = parts[2].substring(0, 2);
                    }

                    possibleDate = parts[0] + "-" + parts[1] + "-" + parts[2];
                }
            } else {
                possibleDate = null;
            }

            return possibleDate == null ? null : parseDateStrictly(possibleDate, dateFormats);
        } catch (Throwable ignored) {
        }

        return null;
    }

    public OffsetDateTime parseDateStrictly(String dateStr) {
        return parseDateStrictly(dateStr, dateFormats);
    }

    public OffsetDateTime parseDateTimeStrictly(String dateStr) {
        return parseDateStrictly(dateStr, dateTimeFormats);
    }

    public OffsetDateTime parseDateStrictly(String dateStr, String... formats) {
        Date parsedDate = null;

        try {
            parsedDate = DateUtils.parseDateStrictly(dateStr, formats);
        } catch (ParseException ignored) {
        }

        return parsedDate == null ? null : OffsetDateTime.ofInstant(parsedDate.toInstant(), zoneId);
    }

    public Instant parseDateTimeStrictly(String dateStr, Instant defaultValue) {
        try {
            return DateUtils.parseDateStrictly(dateStr, dateTimeFormats).toInstant();
        } catch (Throwable ignored) {
        }

        return defaultValue;
    }

    /**
     * For urls who contains date information, for example
     * http://bond.hexun.com/2011-01-07/126641872.html
     * */
    public boolean containsOldDate(String text, int days, ZoneId zoneId) {
        if (text == null) {
            return false;
        }

        YearMonth yearMonth = detectYearMonth(text);
        if (yearMonth == null) {
            return false;
        }

        OffsetDateTime dateTime = yearMonth.atEndOfMonth().atTime(0, 0).atZone(zoneId).toOffsetDateTime();
//    System.out.println(detectDate(text) + "\t\t" + yearMonth + "\t\t" + isDaysBefore(dateTime, days)
//        + "\t\t" + dateTime + "\t\t" + text);

        if (DateTimeUtil.isDaysBefore(dateTime, days)) {
            return true;
        }

        dateTime = detectDate(text);
        return dateTime != null && DateTimeUtil.isDaysBefore(dateTime, days);
    }
}
