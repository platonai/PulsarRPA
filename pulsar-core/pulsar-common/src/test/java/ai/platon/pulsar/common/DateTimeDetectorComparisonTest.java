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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparison tests between original DateTimeDetector and refactored version.
 * Ensures behavioral compatibility while validating improvements.
 */
@DisplayName("DateTimeDetector Compatibility Tests")
public class DateTimeDetectorComparisonTest {

    private DateTimeDetector originalDetector;
    private DateTimeDetectorRefactored refactoredDetector;

    @BeforeEach
    void setUp() {
        originalDetector = new DateTimeDetector();
        refactoredDetector = new DateTimeDetectorRefactored();
    }

    @Test
    @DisplayName("Test basic compatibility - same inputs produce same results")
    void testBasicCompatibility() {
        String[] testInputs = {
            "2023-12-25 14:30:00",
            "2023-12-25",
            "2023年12月25日 14:30:00",
            "2023/12/25 14:30:00",
            "Mon, 25 Dec 2023 14:30:00 GMT",
            "No date here",
            null,
            ""
        };

        for (String input : testInputs) {
            OffsetDateTime originalResult = originalDetector.detectDateTime(input);
            OffsetDateTime refactoredResult = refactoredDetector.detectDateTime(input);

            if (originalResult == null) {
                assertNull(refactoredResult, "Both should return null for input: " + input);
            } else {
                assertNotNull(refactoredResult, "Refactored should not return null when original finds date: " + input);
                assertEquals(originalResult.toInstant(), refactoredResult.toInstant(),
                           "Results should be equivalent for input: " + input);
            }
        }
    }

    @Test
    @DisplayName("Test date detection compatibility")
    void testDateDetectionCompatibility() {
        String[] testInputs = {
            "http://example.com/2023-12-25/article.html",
            "http://example.com/2023/12/25/article.html",
            "http://example.com/20231225/article.html",
            "Published on 2023-12-25",
            "2023年12月25日发布",
            "Invalid date 2023-13-45"
        };

        for (String input : testInputs) {
            OffsetDateTime originalResult = originalDetector.detectDate(input);
            OffsetDateTime refactoredResult = refactoredDetector.detectDate(input);

            if (originalResult == null) {
                assertNull(refactoredResult, "Both should return null for input: " + input);
            } else {
                assertNotNull(refactoredResult, "Refactored should not return null when original finds date: " + input);
                assertEquals(originalResult.toInstant(), refactoredResult.toInstant(),
                           "Date results should be equivalent for input: " + input);
            }
        }
    }

    @Test
    @DisplayName("Test year-month detection compatibility")
    void testYearMonthDetectionCompatibility() {
        String[] testInputs = {
            "2023-12",
            "202312",
            "2023-01",
            "202301",
            "Article from 2023-12 period",
            "Invalid 2023-13"
        };

        for (String input : testInputs) {
            YearMonth originalResult = originalDetector.detectYearMonth(input);
            YearMonth refactoredResult = refactoredDetector.detectYearMonth(input);

            if (originalResult == null) {
                assertNull(refactoredResult, "Both should return null for input: " + input);
            } else {
                assertNotNull(refactoredResult, "Refactored should not return null when original finds year-month: " + input);
                assertEquals(originalResult, refactoredResult,
                           "YearMonth results should be equivalent for input: " + input);
            }
        }
    }

    @Test
    @DisplayName("Test old date detection compatibility")
    void testOldDateDetectionCompatibility() {
        ZoneId zoneId = ZoneId.systemDefault();
        String[] testInputs = {
            "2020-12-25",
            "2023-01-01",
            "2023-12-25",
            "http://example.com/2020/12/25/article.html",
            "Recent article from 2023-12-25"
        };

        int[] dayThresholds = {30, 90, 365, 1000};

        for (String input : testInputs) {
            for (int days : dayThresholds) {
                boolean originalResult = originalDetector.containsOldDate(input, days, zoneId);
                boolean refactoredResult = refactoredDetector.containsOldDate(input, days, zoneId);

                assertEquals(originalResult, refactoredResult,
                           "Old date detection should be consistent for input: " + input + " with days: " + days);
            }
        }
    }

    @Test
    @DisplayName("Test possible date string extraction compatibility")
    void testPossibleDateStringExtractionCompatibility() {
        String[] testInputs = {
            "Article published on 2023-12-25 14:30:00",
            "Date: 2023年12月25日",
            "http://example.com/2023/12/25/article.html",
            "From 2023-12-25 to 2024-01-01",
            "No date in this text",
            "Invalid 2023-13-45 date"
        };

        for (String input : testInputs) {
            String originalResult = originalDetector.detectPossibleDateTimeString(input);
            String refactoredResult = refactoredDetector.detectPossibleDateTimeString(input);

            assertEquals(originalResult, refactoredResult,
                       "Possible date string extraction should be identical for input: " + input);
        }
    }

    @ParameterizedTest
    @DisplayName("Test comprehensive format compatibility")
    @MethodSource("provideComprehensiveTestCases")
    void testComprehensiveFormatCompatibility(String input, String description) {
        // Test date-time detection
        OffsetDateTime originalDateTime = originalDetector.detectDateTime(input);
        OffsetDateTime refactoredDateTime = refactoredDetector.detectDateTime(input);

        assertEquals(
            originalDateTime == null ? null : originalDateTime.toInstant(),
            refactoredDateTime == null ? null : refactoredDateTime.toInstant(),
            "DateTime detection should be compatible for: " + description
        );

        // Test date detection
        OffsetDateTime originalDate = originalDetector.detectDate(input);
        OffsetDateTime refactoredDate = refactoredDetector.detectDate(input);

        assertEquals(
            originalDate == null ? null : originalDate.toInstant(),
            refactoredDate == null ? null : refactoredDate.toInstant(),
            "Date detection should be compatible for: " + description
        );
    }

    @Test
    @DisplayName("Test error handling compatibility")
    void testErrorHandlingCompatibility() {
        // Test null inputs
        assertNull(originalDetector.detectDateTime(null));
        assertNull(refactoredDetector.detectDateTime(null));

        assertNull(originalDetector.detectDate(null));
        assertNull(refactoredDetector.detectDate(null));

        assertNull(originalDetector.detectYearMonth(null));
        assertNull(refactoredDetector.detectYearMonth(null));

        // Test empty inputs
        assertNull(originalDetector.detectDateTime(""));
        assertNull(refactoredDetector.detectDateTime(""));

        // Test invalid dates
        String invalidDate = "2023-13-45 25:70:90";
        assertNull(originalDetector.detectDateTime(invalidDate));
        assertNull(refactoredDetector.detectDateTime(invalidDate));
    }

    @Test
    @DisplayName("Test timezone handling compatibility")
    void testTimezoneHandlingCompatibility() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneId ny = ZoneId.of("America/New_York");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");

        DateTimeDetector originalUTC = new DateTimeDetector(utc);
        DateTimeDetectorRefactored refactoredUTC = new DateTimeDetectorRefactored(utc);

        String[] testInputs = {
            "2023-12-25 14:30:00",
            "2023-12-25T14:30:00Z",
            "2023-12-25T09:30:00-05:00"
        };

        for (String input : testInputs) {
            OffsetDateTime originalResult = originalUTC.detectDateTime(input);
            OffsetDateTime refactoredResult = refactoredUTC.detectDateTime(input);

            if (originalResult == null) {
                assertNull(refactoredResult, "Both should return null for input: " + input);
            } else {
                assertNotNull(refactoredResult, "Refactored should not return null when original finds date: " + input);
                assertEquals(originalResult.toInstant(), refactoredResult.toInstant(),
                           "Timezone handling should be equivalent for input: " + input);
            }
        }
    }

    @Test
    @DisplayName("Test performance comparison")
    void testPerformanceComparison() {
        int iterations = 1000;
        String testInput = "2023-12-25 14:30:00";

        // Warm up
        for (int i = 0; i < 100; i++) {
            originalDetector.detectDateTime(testInput);
            refactoredDetector.detectDateTime(testInput);
        }

        // Test original performance
        long originalStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            originalDetector.detectDateTime(testInput);
        }
        long originalEnd = System.nanoTime();
        long originalDuration = originalEnd - originalStart;

        // Test refactored performance
        long refactoredStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            refactoredDetector.detectDateTime(testInput);
        }
        long refactoredEnd = System.nanoTime();
        long refactoredDuration = refactoredEnd - refactoredStart;

        System.out.println("Original performance: " + (originalDuration / 1_000_000) + "ms for " + iterations + " iterations");
        System.out.println("Refactored performance: " + (refactoredDuration / 1_000_000) + "ms for " + iterations + " iterations");
        System.out.println("Performance ratio: " + String.format("%.2f", (double) refactoredDuration / originalDuration));

        // Refactored should not be significantly slower (less than 2x)
        assertTrue(refactoredDuration < originalDuration * 2,
                   "Refactored version should not be more than 2x slower than original");
    }

    @Test
    @DisplayName("Test improved error handling")
    void testImprovedErrorHandling() {
        // Test cases where refactored version should provide better error handling

        // Large input handling
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            largeInput.append("Text ");
        }
        largeInput.append("2023-12-25 14:30:00");

        // Both should handle large input, but refactored should have better performance
        OffsetDateTime originalResult = originalDetector.detectDateTime(largeInput.toString());
        OffsetDateTime refactoredResult = refactoredDetector.detectDateTime(largeInput.toString());

        assertEquals(originalResult == null ? null : originalResult.toInstant(),
                    refactoredResult == null ? null : refactoredResult.toInstant(),
                    "Large input handling should be compatible");

        // Unicode handling
        String unicodeInput = "文章发布于 2023年12月25日 14:30:00";
        OffsetDateTime originalUnicode = originalDetector.detectDateTime(unicodeInput);
        OffsetDateTime refactoredUnicode = refactoredDetector.detectDateTime(unicodeInput);

        assertEquals(originalUnicode == null ? null : originalUnicode.toInstant(),
                    refactoredUnicode == null ? null : refactoredUnicode.toInstant(),
                    "Unicode handling should be compatible");
    }

    @Test
    @DisplayName("Test builder pattern functionality")
    void testBuilderPatternFunctionality() {
        // Test that builder creates equivalent detector
        DateTimeDetectorRefactored builderDetector = new DateTimeDetectorRefactored.Builder()
                .withDateFormats(DateTimeDetectorRefactored.COMMON_DATE_FORMATS)
                .withDateTimeFormats(DateTimeDetectorRefactored.COMMON_DATE_TIME_FORMATS)
                .withZoneId(ZoneId.systemDefault())
                .build();

        String[] testInputs = {
            "2023-12-25 14:30:00",
            "2023-12-25",
            "2023年12月25日 14:30:00"
        };

        for (String input : testInputs) {
            OffsetDateTime originalResult = originalDetector.detectDateTime(input);
            OffsetDateTime builderResult = builderDetector.detectDateTime(input);

            assertEquals(originalResult == null ? null : originalResult.toInstant(),
                        builderResult == null ? null : builderResult.toInstant(),
                        "Builder should create functionally equivalent detector for input: " + input);
        }
    }

    // Helper method to provide comprehensive test cases
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideComprehensiveTestCases() {
        return Stream.of(
            // Standard formats
            org.junit.jupiter.params.provider.Arguments.of("2023-12-25 14:30:00", "Standard datetime"),
            org.junit.jupiter.params.provider.Arguments.of("2023-12-25", "Standard date"),
            org.junit.jupiter.params.provider.Arguments.of("2023-12-25T14:30:00", "ISO datetime"),
            org.junit.jupiter.params.provider.Arguments.of("2023-12-25T14:30:00Z", "ISO datetime with Z"),

            // Different separators
            org.junit.jupiter.params.provider.Arguments.of("2023/12/25 14:30:00", "Slash separator"),
            org.junit.jupiter.params.provider.Arguments.of("2023.12.25 14:30:00", "Dot separator"),
            org.junit.jupiter.params.provider.Arguments.of("20231225 14:30:00", "No separator"),

            // Chinese formats
            org.junit.jupiter.params.provider.Arguments.of("2023年12月25日 14:30:00", "Chinese format"),
            org.junit.jupiter.params.provider.Arguments.of("2023年12月25日", "Chinese date only"),

            // RFC formats
            org.junit.jupiter.params.provider.Arguments.of("Mon, 25 Dec 2023 14:30:00 GMT", "RFC format with GMT"),
            org.junit.jupiter.params.provider.Arguments.of("Mon, 25 Dec 2023 14:30:00 -0500", "RFC format with offset"),

            // URL formats
            org.junit.jupiter.params.provider.Arguments.of("http://example.com/2023-12-25/article.html", "URL with date"),
            org.junit.jupiter.params.provider.Arguments.of("http://example.com/2023/12/25/article.html", "URL with slashes"),
            org.junit.jupiter.params.provider.Arguments.of("http://example.com/20231225/article.html", "URL without separators"),

            // Mixed content
            org.junit.jupiter.params.provider.Arguments.of("Article published on 2023-12-25 14:30:00 by author", "Mixed content with date"),
            org.junit.jupiter.params.provider.Arguments.of("From 2023-12-25 to 2024-01-01 period", "Date range"),

            // Edge cases
            org.junit.jupiter.params.provider.Arguments.of("", "Empty string"),
            org.junit.jupiter.params.provider.Arguments.of("No date here", "No date"),
            org.junit.jupiter.params.provider.Arguments.of("2023-13-45 25:70:90", "Invalid date"),
            org.junit.jupiter.params.provider.Arguments.of("1899-12-25 14:30:00", "Date before lower bound"),

            // Unicode and special characters
            org.junit.jupiter.params.provider.Arguments.of("文章发布于 2023年12月25日 14:30:00", "Chinese with unicode"),
            org.junit.jupiter.params.provider.Arguments.of("Дата: 2023-12-25 14:30:00", "Russian text"),

            // Multiline
            org.junit.jupiter.params.provider.Arguments.of("First line\n2023-12-25 14:30:00\nLast line", "Multiline with date")
        );
    }
}