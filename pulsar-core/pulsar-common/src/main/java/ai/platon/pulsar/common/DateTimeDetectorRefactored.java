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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Enhanced date/time detector with improved structure, error handling, and configurability.
 *
 * This class detects and parses date/time strings from text content, supporting multiple
 * formats commonly found in web articles, URLs, and metadata.
 *
 * Features:
 * - Support for 30+ date/time formats
 * - Multi-language support (English, Chinese)
 * - URL-based date extraction
 * - Configurable validation ranges
 * - Comprehensive error handling and logging
 * - Thread-safe implementation
 *
 * @author Platon AI
 * @since 1.0.0
 */
public class DateTimeDetectorRefactored {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeDetectorRefactored.class);

    // Constants with clear documentation
    public static final int MIN_DATE_TIME_STR_LENGTH = "2015-01-01 12:00".length();
    public static final int MIN_YEAR_MONTH_STR_LENGTH = "201501".length();
    public static final int MIN_DATE_STR_LENGTH = "20150101".length();
    public static final int MAX_META_STR_LENGTH = 200;
    public static final int MAX_DATE_TIME_STR_LENGTH = "EEE, dd MMM yyyy HH:mm:ss zzz".length();
    public static final int MAX_TITLE_LENGTH = 350;
    public static final int MAX_INPUT_LENGTH = 10000; // Prevent excessive processing

    // Configuration constants
    public static final int DEFAULT_OLD_DATE_DAYS = 30;
    public static final int YEAR_LOWER_BOUND = 1990;
    public static final int YEAR_UPPER_BOUND = 2100; // More reasonable than 2030

    // Bad date patterns that indicate auto-generated content
    public static final String[] BAD_DATE_TIME_STRING_CONTAINS = new String[]{
            "GMT+8", "UTC+8", "Processed", "访问", "刷新", "visit"
    };

    // Current date references
    public static final LocalDate CURRENT_DATE = LocalDate.now();
    public static final long CURRENT_DATE_EPOCH_DAYS = CURRENT_DATE.toEpochDay();
    public static final int CURRENT_YEAR = CURRENT_DATE.getYear();
    public static final String CURRENT_YEAR_STR = String.valueOf(CURRENT_YEAR);
    public static final int CURRENT_MONTH = CURRENT_DATE.getMonthValue();

    // Valid year ranges for different contexts
    public static final List<String> VALID_WORK_YEARS = IntStream.range(2010, YEAR_UPPER_BOUND)
            .mapToObj(String::valueOf).collect(Collectors.toList());

    public static final List<String> VALID_WORK_YEARS_SHORT = IntStream.range(10, 100)
            .mapToObj(String::valueOf).collect(Collectors.toList());

    public static final String[] VALID_WORK_YEARS_ARRAY = VALID_WORK_YEARS.toArray(new String[0]);
    public static final String[] VALID_WORK_YEARS_SHORT_ARRAY = VALID_WORK_YEARS_SHORT.toArray(new String[0]);

    // Pre-compiled patterns for better performance
    private static final Set<String> OLD_YEARS;
    private static final Set<String> OLD_MONTHS;
    private static final Pattern OLD_MONTH_URL_DATE_PATTERN;
    private static final Pattern YEAR_MONTH_PATTERN;
    private static final Pattern YEAR_MONTH_DASH_PATTERN;
    private static final Pattern YEAR_DATE_PATTERN;
    private static final Pattern YEAR_DATE_DASH_PATTERN;

    // Common date formats
    public static final String[] COMMON_DATE_FORMATS = new String[]{
            "yyyyMMdd", "yyyy.MM.dd", "yyyy-MM-dd", "yyyy年MM月dd日", "yyyy/MM/dd"
    };

    // Comprehensive date-time formats
    public static final String[] COMMON_DATE_TIME_FORMATS = new String[]{
            // ISO formats
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            // Standard formats with different separators
            "yyyy.MM.dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd hh:mm:ss",
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd hh:mm", "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd hh:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd hh:mm",

            // Chinese formats
            "yyyy年MM月dd日 HH:mm", "yyyy年MM月dd日 hh:mm", "yyyy年MM月dd日 HH:mm:ss",
            "yyyy年MM月dd日 hh:mm:ss",

            // With milliseconds and timezone
            "yyyy/MM/dd HH:mm:ss.SSS zzz", "yyyy/MM/dd HH:mm:ss.SSS", "yyyy/MM/dd HH:mm:ss zzz",

            // European formats
            "dd.MM.yyyy HH:mm:ss zzz", "dd MM yyyy HH:mm:ss zzz", "dd.MM.yyyy zzz",
            "dd.MM.yyyy; HH:mm:ss", "dd.MM.yyyy HH:mm:ss",

            // RFC formats
            "EEE MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy zzz",
            "EEE MMM dd HH:mm:ss zzz yyyy", "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE,dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm:sszzz",
            "EEE, dd MMM yyyy HH:mm:ss", "EEE, dd-MMM-yy HH:mm:ss zzz"
    };

    static {
        // Initialize static data structures
        OLD_YEARS = IntStream.range(YEAR_LOWER_BOUND, CURRENT_YEAR)
                .mapToObj(String::valueOf).collect(Collectors.toSet());

        OLD_MONTHS = IntStream.range(1, CURRENT_MONTH)
                .mapToObj(m -> String.format("%02d", m)).collect(Collectors.toSet());

        String monthPattern = StringUtils.join(OLD_MONTHS, "|");
        if (CURRENT_MONTH <= 2) {
            monthPattern = "\\d{2}";
        }

        // Pre-compile regex patterns for better performance
        OLD_MONTH_URL_DATE_PATTERN = Pattern.compile(".+" + CURRENT_YEAR + "[/\\.-]?(" + monthPattern + ").+");
        YEAR_MONTH_PATTERN = Pattern.compile("20[0-9]{2}[0-1][0-9].*");
        YEAR_MONTH_DASH_PATTERN = Pattern.compile("20[0-9]{2}-[0-1]?[0-9].*");
        YEAR_DATE_PATTERN = Pattern.compile("20[0-9]{2}[0-1][0-9][0-3][0-9].*");
        YEAR_DATE_DASH_PATTERN = Pattern.compile("20[0-9]{2}-[0-1]?[0-9]-[0-3]?[0-9].*");
    }

    private final String[] dateFormats;
    private final String[] dateTimeFormats;
    private ZoneId zoneId;

    /**
     * Default constructor with common date formats and system timezone
     */
    public DateTimeDetectorRefactored() {
        this(COMMON_DATE_FORMATS, COMMON_DATE_TIME_FORMATS);
    }

    /**
     * Constructor with specified timezone
     *
     * @param zoneId the timezone to use for date operations
     */
    public DateTimeDetectorRefactored(ZoneId zoneId) {
        this(COMMON_DATE_FORMATS, COMMON_DATE_TIME_FORMATS, zoneId);
    }

    /**
     * Constructor with custom date formats
     *
     * @param dateFormats array of date format patterns
     * @param dateTimeFormats array of date-time format patterns
     */
    public DateTimeDetectorRefactored(String[] dateFormats, String[] dateTimeFormats) {
        this(dateFormats, dateTimeFormats, ZoneId.systemDefault());
    }

    /**
     * Full constructor with custom formats and timezone
     *
     * @param dateFormats array of date format patterns
     * @param dateTimeFormats array of date-time format patterns
     * @param zoneId the timezone to use for date operations
     */
    public DateTimeDetectorRefactored(String[] dateFormats, String[] dateTimeFormats, ZoneId zoneId) {
        this.dateFormats = validateFormats(dateFormats, "dateFormats");
        this.dateTimeFormats = validateFormats(dateTimeFormats, "dateTimeFormats");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId cannot be null");

        if (CURRENT_YEAR > YEAR_UPPER_BOUND) {
            logger.warn("Current year {} exceeds upper bound {}. Consider updating the year upper bound.",
                       CURRENT_YEAR, YEAR_UPPER_BOUND);
        }
    }

    /**
     * Validates that format arrays are not null or empty
     */
    private String[] validateFormats(String[] formats, String name) {
        Objects.requireNonNull(formats, name + " cannot be null");
        if (formats.length == 0) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return formats.clone();
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId cannot be null");
    }

    /**
     * Detects a possible date-time string in text by looking for year patterns.
     *
     * @param text the text to search
     * @return the extracted date string, or null if no date found
     */
    public String detectPossibleDateTimeString(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Limit processing to first line and reasonable length
        String possibleDate = StringUtils.substringBefore(text, "\n");
        if (possibleDate.length() > MAX_INPUT_LENGTH) {
            possibleDate = possibleDate.substring(0, MAX_INPUT_LENGTH);
        }

        // Look for full year first
        int dateStart = StringUtils.indexOfAny(possibleDate, VALID_WORK_YEARS_ARRAY);

        // If not found, try short year format
        if (dateStart == StringUtils.INDEX_NOT_FOUND) {
            dateStart = StringUtils.indexOfAny(possibleDate, VALID_WORK_YEARS_SHORT_ARRAY);
            if (dateStart != StringUtils.INDEX_NOT_FOUND) {
                possibleDate = "20" + possibleDate.substring(dateStart);
                // Validate the converted date format
                if (!possibleDate.matches("20[0-9]{2}[-\\./年]?\\d+.*")) {
                    return null;
                }
                dateStart = 0;
            }
        }

        if (dateStart < 0 || dateStart >= possibleDate.length()) {
            return null;
        }

        // Extract reasonable length for date parsing
        int dateEnd = Math.min(possibleDate.length(), dateStart + MAX_DATE_TIME_STR_LENGTH);
        possibleDate = possibleDate.substring(dateStart, dateEnd);

        // Normalize separators
        possibleDate = possibleDate.replaceAll("[\\./年月]", "-").trim();

        return possibleDate;
    }

    /**
     * Attempts to detect the best date-time from text using multiple strategies.
     *
     * @param text the text to search
     * @return the detected OffsetDateTime, or null if not found
     */
    public OffsetDateTime detectDateTimeLeniently(String text) {
        OffsetDateTime dateTime = detectDateTime(text);
        if (dateTime == null) {
            dateTime = detectDate(text);
        }
        return dateTime;
    }

    /**
     * Detects date-time from text with comprehensive format support.
     *
     * @param text the text to search
     * @return the detected OffsetDateTime, or null if not found
     */
    public OffsetDateTime detectDateTime(String text) {
        if (text == null || text.length() < MIN_DATE_TIME_STR_LENGTH) {
            return null;
        }

        // Limit input size for performance
        if (text.length() > MAX_INPUT_LENGTH) {
            text = text.substring(0, MAX_INPUT_LENGTH);
        }

        // Normalize whitespace
        text = text.replaceAll("\\p{Zs}", " ").trim();

        // Filter out auto-generated content
        if (containsBadPatterns(text)) {
            return null;
        }

        // Find year pattern
        final int dateTimeStart = StringUtils.indexOfAny(text, VALID_WORK_YEARS_ARRAY);
        if (dateTimeStart == StringUtils.INDEX_NOT_FOUND) {
            return null;
        }

        // Find time component
        final int dateEnd = StringUtils.indexOf(text, " ", dateTimeStart);
        if (dateEnd < 0) {
            return null; // No time component found
        }

        // Extract time component
        final int timeStart = dateEnd + 1;
        int pos = timeStart;
        for (; pos < text.length(); ++pos) {
            char ch = text.charAt(pos);
            if (!Character.isDigit(ch) && ch != ':' && ch != '.' && ch != ' ') {
                break;
            }
        }

        final int dateTimeEnd = pos;
        String possibleDate = StringUtils.substring(text, dateTimeStart, dateTimeEnd);

        // Try to parse the extracted datetime
        if (possibleDate.length() >= MIN_DATE_TIME_STR_LENGTH) {
            return parseDateTimeStrictly(possibleDate);
        }

        return null;
    }

    /**
     * Detects year-month from text.
     *
     * @param text the text to search
     * @return the detected YearMonth, or null if not found
     */
    public YearMonth detectYearMonth(String text) {
        String possibleYearMonth = detectPossibleDateTimeString(text);
        if (possibleYearMonth == null) {
            return null;
        }
        return tryParseYearMonthStrictly(possibleYearMonth);
    }

    /**
     * Attempts to parse year-month string with validation.
     */
    public YearMonth tryParseYearMonthStrictly(String possibleYearMonth) {
        try {
            // Try compact format first: 202312
            if (YEAR_MONTH_PATTERN.matcher(possibleYearMonth).matches()) {
                String normalized = possibleYearMonth.substring(0, 4) + "-" + possibleYearMonth.substring(4, 6);
                return YearMonth.parse(normalized);
            }

            // Try dash format: 2023-12 or 2023-1
            if (YEAR_MONTH_DASH_PATTERN.matcher(possibleYearMonth).matches()) {
                String[] parts = possibleYearMonth.split("-");
                if (parts.length >= 2 && parts[0].length() == 4) {
                    // Normalize month to two digits
                    String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
                    String normalized = parts[0] + "-" + month;
                    return YearMonth.parse(normalized);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to parse year-month: {}", possibleYearMonth, e);
        }

        return null;
    }

    /**
     * Detects date from text.
     *
     * @param text the text to search
     * @return the detected OffsetDateTime, or null if not found
     */
    public OffsetDateTime detectDate(String text) {
        String possibleDate = detectPossibleDateTimeString(text);
        return possibleDate != null ? tryParseDateTimeStrictly(possibleDate) : null;
    }

    /**
     * Attempts to parse date string with validation and normalization.
     */
    public OffsetDateTime tryParseDateTimeStrictly(String possibleDate) {
        try {
            String normalizedDate = normalizeDateString(possibleDate);
            return normalizedDate != null ? parseDateStrictly(normalizedDate, dateFormats) : null;
        } catch (Exception e) {
            logger.debug("Failed to parse date: {}", possibleDate, e);
            return null;
        }
    }

    /**
     * Normalizes various date string formats to standard format.
     */
    private String normalizeDateString(String possibleDate) {
        // Try compact format: 20231225
        if (YEAR_DATE_PATTERN.matcher(possibleDate).matches()) {
            return possibleDate.substring(0, 4) + possibleDate.substring(4, 6) + possibleDate.substring(6, 8);
        }

        // Try dash format: 2023-12-25 or 2023-1-5
        if (YEAR_DATE_DASH_PATTERN.matcher(possibleDate).matches()) {
            String[] parts = possibleDate.split("-");
            if (parts.length >= 3 && parts[0].length() == 4) {
                String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
                String day = parts[2].length() == 1 ? "0" + parts[2] : parts[2];
                if (day.length() > 2) {
                    day = day.substring(0, 2);
                }
                return parts[0] + "-" + month + "-" + day;
            }
        }

        return null;
    }

    /**
     * Parses date strictly using Apache Commons DateUtils.
     */
    public OffsetDateTime parseDateStrictly(String dateStr) {
        return parseDateStrictly(dateStr, dateFormats);
    }

    /**
     * Parses date-time strictly using provided formats.
     */
    public OffsetDateTime parseDateTimeStrictly(String dateStr) {
        return parseDateStrictly(dateStr, dateTimeFormats);
    }

    /**
     * Parses date string using provided formats.
     */
    public OffsetDateTime parseDateStrictly(String dateStr, String... formats) {
        if (dateStr == null || formats == null) {
            return null;
        }

        try {
            Date parsedDate = DateUtils.parseDateStrictly(dateStr, formats);
            return parsedDate != null ? OffsetDateTime.ofInstant(parsedDate.toInstant(), zoneId) : null;
        } catch (ParseException e) {
            logger.debug("Failed to parse date '{}' with formats: {}", dateStr, Arrays.toString(formats), e);
            return null;
        }
    }

    /**
     * Parses date-time with fallback to default value.
     */
    public Instant parseDateTimeStrictly(String dateStr, Instant defaultValue) {
        try {
            Date parsedDate = DateUtils.parseDateStrictly(dateStr, dateTimeFormats);
            return parsedDate != null ? parsedDate.toInstant() : defaultValue;
        } catch (Exception e) {
            logger.debug("Failed to parse date-time '{}', using default: {}", dateStr, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Checks if text contains patterns that indicate auto-generated dates.
     */
    private boolean containsBadPatterns(String text) {
        return Stream.of(BAD_DATE_TIME_STRING_CONTAINS).anyMatch(text::contains);
    }

    /**
     * Determines if text contains dates older than specified days.
     *
     * @param text the text to analyze
     * @param days the threshold in days
     * @param zoneId the timezone for date calculations
     * @return true if text contains old dates, false otherwise
     */
    public boolean containsOldDate(String text, int days, ZoneId zoneId) {
        if (text == null || days <= 0) {
            return false;
        }

        try {
            // Try year-month detection first
            YearMonth yearMonth = detectYearMonth(text);
            if (yearMonth != null) {
                OffsetDateTime dateTime = yearMonth.atEndOfMonth().atTime(0, 0).atZone(zoneId).toOffsetDateTime();
                if (DateTimes.isDaysBefore(dateTime, days)) {
                    return true;
                }
            }

            // Try full date detection
            OffsetDateTime dateTime = detectDate(text);
            return dateTime != null && DateTimes.isDaysBefore(dateTime, days);

        } catch (Exception e) {
            logger.warn("Error checking for old dates in text: {}", text.substring(0, Math.min(text.length(), 100)), e);
            return false;
        }
    }

    /**
     * Convenience method using system default timezone.
     */
    public boolean containsOldDate(String text, int days) {
        return containsOldDate(text, days, ZoneId.systemDefault());
    }

    /**
     * Builder for creating customized DateTimeDetector instances.
     */
    public static class Builder {
        private String[] dateFormats = COMMON_DATE_FORMATS;
        private String[] dateTimeFormats = COMMON_DATE_TIME_FORMATS;
        private ZoneId zoneId = ZoneId.systemDefault();

        public Builder withDateFormats(String... formats) {
            this.dateFormats = formats;
            return this;
        }

        public Builder withDateTimeFormats(String... formats) {
            this.dateTimeFormats = formats;
            return this;
        }

        public Builder withZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public DateTimeDetectorRefactored build() {
            return new DateTimeDetectorRefactored(dateFormats, dateTimeFormats, zoneId);
        }
    }
}