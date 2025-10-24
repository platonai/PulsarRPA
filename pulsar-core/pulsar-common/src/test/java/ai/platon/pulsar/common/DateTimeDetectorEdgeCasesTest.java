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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and performance tests for DateTimeDetectorRefactored.
 * Focuses on boundary conditions, malformed inputs, and performance scenarios.
 */
@DisplayName("DateTimeDetectorRefactored Edge Cases and Performance Tests")
public class DateTimeDetectorEdgeCasesTest {

    private DateTimeDetectorRefactored detector;
    private static final int PERF_TEST_ITERATIONS = 10000;
    private static final int LARGE_INPUT_SIZE = 100000;

    @BeforeEach
    void setUp() {
        detector = new DateTimeDetectorRefactored();
    }

    // Boundary Value Tests
    @Test
    @DisplayName("Test year boundaries")
    void testYearBoundaries() {
        // Lower boundary
        assertNotNull(detector.detectDate("1990-01-01"));
        assertNull(detector.detectDate("1989-12-31")); // Just below boundary

        // Upper boundary
        assertNotNull(detector.detectDate("2099-12-31"));
        assertNull(detector.detectDate("2100-01-01")); // Just above boundary
    }

    @Test
    @DisplayName("Test month boundaries")
    void testMonthBoundaries() {
        assertNotNull(detector.detectDate("2023-01-01"));
        assertNotNull(detector.detectDate("2023-12-31"));
        assertNull(detector.detectDate("2023-00-15")); // Invalid month 0
        assertNull(detector.detectDate("2023-13-15")); // Invalid month 13
    }

    @Test
    @DisplayName("Test day boundaries for different months")
    void testDayBoundaries() {
        // January
        assertNotNull(detector.detectDate("2023-01-01"));
        assertNotNull(detector.detectDate("2023-01-31"));
        assertNull(detector.detectDate("2023-01-32")); // Invalid day

        // February (non-leap year)
        assertNotNull(detector.detectDate("2023-02-28"));
        assertNull(detector.detectDate("2023-02-29")); // Not a leap year

        // February (leap year)
        assertNotNull(detector.detectDate("2024-02-29")); // Leap year
        assertNull(detector.detectDate("2024-02-30")); // Invalid day

        // April (30 days)
        assertNotNull(detector.detectDate("2023-04-30"));
        assertNull(detector.detectDate("2023-04-31")); // Invalid day

        // December
        assertNotNull(detector.detectDate("2023-12-31"));
        assertNull(detector.detectDate("2023-12-32")); // Invalid day
    }

    @Test
    @DisplayName("Test leap year calculations")
    void testLeapYearCalculations() {
        // Valid leap years
        assertNotNull(detector.detectDate("2024-02-29"));
        assertNotNull(detector.detectDate("2000-02-29"));
        assertNotNull(detector.detectDate("1996-02-29"));

        // Invalid leap years (century years not divisible by 400)
        assertNull(detector.detectDate("1900-02-29"));
        assertNull(detector.detectDate("2100-02-29"));

        // Valid century leap year
        assertNotNull(detector.detectDate("2000-02-29"));
    }

    // Time Boundary Tests
    @Test
    @DisplayName("Test time boundaries")
    void testTimeBoundaries() {
        // Valid times
        assertNotNull(detector.detectDateTime("2023-12-25 00:00:00"));
        assertNotNull(detector.detectDateTime("2023-12-25 23:59:59"));
        assertNotNull(detector.detectDateTime("2023-12-25 12:30:45"));

        // Invalid times
        assertNull(detector.detectDateTime("2023-12-25 24:00:00")); // Invalid hour
        assertNull(detector.detectDateTime("2023-12-25 23:60:00")); // Invalid minute
        assertNull(detector.detectDateTime("2023-12-25 23:59:60")); // Invalid second
        assertNull(detector.detectDateTime("2023-12-25 25:70:80")); // All invalid
    }

    @Test
    @DisplayName("Test 12-hour vs 24-hour format ambiguity")
    void testHourFormatAmbiguity() {
        // Both should be valid - the parser should handle both
        assertNotNull(detector.detectDateTime("2023-12-25 02:30:00")); // Could be AM or PM
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00")); // Clearly PM in 24-hour
    }

    // Malformed Input Tests
    @Test
    @DisplayName("Test severely malformed dates")
    void testSeverelyMalformedDates() {
        assertNull(detector.detectDate("2023-12-25-EXTRA"));
        assertNull(detector.detectDate("2023-12"));
        assertNull(detector.detectDate("2023"));
        assertNull(detector.detectDate("-12-25"));
        assertNull(detector.detectDate("2023--12--25"));
        assertNull(detector.detectDate("2023-121-255"));
        assertNull(detector.detectDate("abcd-ef-gh"));
        assertNull(detector.detectDate("$$$$-$$-$$"));
    }

    @Test
    @DisplayName("Test dates with extra characters")
    void testDatesWithExtraCharacters() {
        // These should fail due to extra characters
        assertNull(detector.detectDate("2023-12-25abc"));
        assertNull(detector.detectDate("abc2023-12-25"));
        assertNull(detector.detectDate("2023-12-25.123"));
        assertNull(detector.detectDate("2023-12-25T14:30:00EXTRA"));
    }

    @Test
    @DisplayName("Test dates with special unicode characters")
    void testDatesWithUnicode() {
        // Zero-width characters
        assertNull(detector.detectDate("2023\u200B-12-25")); // Zero-width space
        assertNull(detector.detectDate("2023-12\uFEFF-25")); // Zero-width no-break space

        // Look-alike characters
        assertNull(detector.detectDate("2O23-12-25")); // Letter O instead of digit 0
        assertNull(detector.detectDate("2023-l2-25")); // Letter l instead of digit 1
    }

    // Edge Case String Processing
    @Test
    @DisplayName("Test empty and whitespace-only inputs")
    void testEmptyAndWhitespaceInputs() {
        assertNull(detector.detectDateTime(null));
        assertNull(detector.detectDateTime(""));
        assertNull(detector.detectDateTime("   "));
        assertNull(detector.detectDateTime("\t\n\r"));
        assertNull(detector.detectDateTime("\u00A0\u2000\u2001\u2002\u2003")); // Various unicode spaces
    }

    @Test
    @DisplayName("Test input with only separators")
    void testInputWithOnlySeparators() {
        assertNull(detector.detectDateTime("---"));
        assertNull(detector.detectDateTime("///"));
        assertNull(detector.detectDateTime("..."));
        assertNull(detector.detectDateTime("   -   /   .   "));
    }

    @Test
    @DisplayName("Test extremely long inputs")
    void testExtremelyLongInputs() {
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longInput.append("No date here. ");
        }
        longInput.append("2023-12-25 14:30:00");

        OffsetDateTime result = detector.detectDateTime(longInput.toString());
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    @DisplayName("Test input at maximum processing limit")
    void testInputAtMaximumLimit() {
        StringBuilder maxInput = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            maxInput.append("Text ");
        }
        maxInput.append("2023-12-25 14:30:00");

        String input = maxInput.toString();
        assertTrue(input.length() > 10000); // Should exceed internal limit

        OffsetDateTime result = detector.detectDateTime(input);
        assertNotNull(result); // Should still find the date
    }

    // Multiline Input Tests
    @Test
    @DisplayName("Test multiline input processing")
    void testMultilineInput() {
        String multiline = "First line with 2023-12-25\nSecond line with 2024-01-01\nThird line";
        String result = detector.detectPossibleDateTimeString(multiline);
        assertEquals("2023-12-25", result); // Should only process first line
    }

    @Test
    @DisplayName("Test multiline with date only in later lines")
    void testMultilineDateInLaterLines() {
        String multiline = "First line no date\nSecond line no date\nThird line 2023-12-25";
        String result = detector.detectPossibleDateTimeString(multiline);
        assertNull(result); // Should not find date (only checks first line)
    }

    // Bad Pattern Detection Tests
    @Test
    @DisplayName("Test bad pattern filtering edge cases")
    void testBadPatternFiltering() {
        // Should be filtered out
        assertNull(detector.detectDateTime("GMT+8 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("UTC+8 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("Processed on 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("访问时间 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("刷新时间 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("visit time 2023-12-25 14:30:00"));

        // Case variations
        assertNull(detector.detectDateTime("gmt+8 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("PROCESSED 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("Visit 2023-12-25 14:30:00"));
    }

    @Test
    @DisplayName("Test bad patterns as part of valid context")
    void testBadPatternsInValidContext() {
        // These should still work if the bad pattern is not indicating auto-generation
        assertNotNull(detector.detectDateTime("Article about GMT+8 timezone, published 2023-12-25 14:30:00"));
        assertNotNull(detector.detectDateTime("Guide to UTC+8 time, written on 2023-12-25 14:30:00"));
    }

    // Year Detection Edge Cases
    @Test
    @DisplayName("Test year detection with ambiguous short years")
    void testAmbiguousShortYears() {
        // Should interpret as 20xx
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("23-12-25"));
        assertEquals("2005-12-25", detector.detectPossibleDateTimeString("05-12-25"));
        assertEquals("2099-12-25", detector.detectPossibleDateTimeString("99-12-25"));

        // Should not match invalid short years
        assertNull(detector.detectPossibleDateTimeString("00-12-25")); // Year 2000 might be too old
        assertNull(detector.detectPossibleDateTimeString("01-12-25")); // Year 2001 might be too old
    }

    // Time Component Edge Cases
    @Test
    @DisplayName("Test time detection with partial time information")
    void testPartialTimeInformation() {
        // Should handle partial times
        assertNotNull(detector.detectDateTime("2023-12-25 14")); // Missing minutes
        assertNotNull(detector.detectDateTime("2023-12-25 14:30")); // Missing seconds
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00"));

        // Should not handle invalid partial times
        assertNull(detector.detectDateTime("2023-12-25 14:")); // Incomplete
        assertNull(detector.detectDateTime("2023-12-25 :30:00")); // Missing hour
    }

    @Test
    @DisplayName("Test time detection with timezone edge cases")
    void testTimezoneEdgeCases() {
        // Valid timezone formats
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 UTC"));
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 GMT"));
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 EST"));
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 +0000"));
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 -0500"));

        // Invalid timezone formats
        assertNull(detector.detectDateTime("2023-12-25 14:30:00 XYZ")); // Invalid TZ
        assertNull(detector.detectDateTime("2023-12-25 14:30:00 +2500")); // Invalid offset
    }

    // Performance Tests
    @Test
    @DisplayName("Test performance with high volume of dates")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testHighVolumePerformance() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < PERF_TEST_ITERATIONS; i++) {
            String date = String.format("2023-12-%02d 14:30:00", (i % 30) + 1);
            OffsetDateTime result = detector.detectDateTime(date);
            assertNotNull(result);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Processed " + PERF_TEST_ITERATIONS + " dates in " + duration + "ms");
        assertTrue(duration < 3000, "Should process 10k dates in under 3 seconds");
    }

    @Test
    @DisplayName("Test performance with large text blocks")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testLargeTextPerformance() {
        // Create large text with date at the end
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < LARGE_INPUT_SIZE; i++) {
            largeText.append("Lorem ipsum dolor sit amet. ");
        }
        largeText.append("Published on 2023-12-25 14:30:00");

        long startTime = System.currentTimeMillis();
        OffsetDateTime result = detector.detectDateTime(largeText.toString());
        long endTime = System.currentTimeMillis();

        assertNotNull(result);
        long duration = endTime - startTime;
        System.out.println("Processed large text (" + largeText.length() + " chars) in " + duration + "ms");
        assertTrue(duration < 5000, "Should process large text in under 5 seconds");
    }

    @Test
    @DisplayName("Test memory efficiency with repeated parsing")
    void testMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();

        // Force garbage collection before test
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Parse many different dates
        for (int i = 0; i < 1000; i++) {
            String date = String.format("2023-%02d-%02d 14:30:00", (i % 12) + 1, (i % 28) + 1);
            detector.detectDateTime(date);
        }

        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("Memory used for 1000 date parsings: " + (memoryUsed / 1024) + "KB");
        assertTrue(memoryUsed < 10 * 1024 * 1024, "Should use less than 10MB of memory");
    }

    // Concurrency Stress Tests
    @Test
    @DisplayName("Test concurrent access stress")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testConcurrentStress() throws InterruptedException {
        int threadCount = 50;
        int operationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Mix of operations
                        if (j % 4 == 0) {
                            detector.detectDateTime("2023-12-25 14:30:00");
                        } else if (j % 4 == 1) {
                            detector.detectDate("2023-12-25");
                        } else if (j % 4 == 2) {
                            detector.detectYearMonth("2023-12");
                        } else {
                            detector.containsOldDate("2023-12-25", 30);
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent stress test");
    }

    // Regex Pattern Stress Tests
    @Test
    @DisplayName("Test regex pattern limits")
    void testRegexPatternLimits() {
        // Very long potential date strings
        StringBuilder longDate = new StringBuilder("2023");
        for (int i = 0; i < 1000; i++) {
            longDate.append("-12-25");
        }

        // Should handle without regex catastrophic backtracking
        String result = detector.detectPossibleDateTimeString(longDate.toString());
        assertNotNull(result);
        assertTrue(result.length() <= DateTimeDetectorRefactored.MAX_DATE_TIME_STR_LENGTH);
    }

    @Test
    @DisplayName("Test complex nested patterns")
    void testComplexNestedPatterns() {
        // Multiple overlapping date patterns
        String complex = "Date1: 2023-12-25, Date2: 2023/12/26, Date3: 2023.12.27, Date4: 20231228";
        String result = detector.detectPossibleDateTimeString(complex);
        assertEquals("2023-12-25", result); // Should find first valid date
    }

    // Error Recovery Tests
    @Test
    @DisplayName("Test graceful degradation with corrupted input")
    void testGracefulDegradation() {
        // Binary data in string
        String corrupted = "2023-12-25\u0000\u0001\u0002\u0003 14:30:00";
        assertNull(detector.detectDateTime(corrupted));

        // Very high unicode characters
        String highUnicode = "2023-12-25 𠀀𠀁𠀂 14:30:00";
        assertNull(detector.detectDateTime(highUnicode));

        // Mixed valid and invalid characters
        String mixed = "20 23 - 12 - 25 14:30:00";
        assertNull(detector.detectDateTime(mixed));
    }

    // Builder Pattern Edge Cases
    @Test
    @DisplayName("Test builder with conflicting configurations")
    void testBuilderConflicts() {
        // Multiple date formats that might conflict
        DateTimeDetectorRefactored complexDetector = new DateTimeDetectorRefactored.Builder()
                .withDateFormats("yyyy-MM-dd", "yyyy-M-d", "MM-dd-yyyy", "dd-MM-yyyy")
                .withDateTimeFormats("yyyy-MM-dd HH:mm:ss", "MM-dd-yyyy HH:mm:ss")
                .withZoneId(ZoneId.of("UTC"))
                .build();

        // Should handle format conflicts gracefully
        assertNotNull(complexDetector.detectDate("2023-12-25"));
        assertNotNull(complexDetector.detectDate("12-25-2023"));
        assertNotNull(complexDetector.detectDate("25-12-2023"));
    }

    @Test
    @DisplayName("Test builder with minimal configuration")
    void testBuilderMinimal() {
        DateTimeDetectorRefactored minimalDetector = new DateTimeDetectorRefactored.Builder()
                .withDateFormats("yyyy-MM-dd")
                .withDateTimeFormats("yyyy-MM-dd HH:mm:ss")
                .build();

        assertNotNull(minimalDetector.detectDate("2023-12-25"));
        assertNotNull(minimalDetector.detectDateTime("2023-12-25 14:30:00"));
        assertNull(minimalDetector.detectDate("2023/12/25")); // Not in formats
    }
}
