package ai.platon.pulsar.common;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DateTimeDetectorRefactored.
 * Tests cover all major functionality including edge cases, error conditions, and performance scenarios.
 */
@DisplayName("DateTimeDetectorRefactored Tests")
public class DateTimeDetectorRefactoredTest {

    private DateTimeDetectorRefactored detector;
    private ZoneId testZoneId;

    @BeforeEach
    void setUp() {
        detector = new DateTimeDetectorRefactored();
        testZoneId = ZoneId.systemDefault();
    }

    // Constructor and Configuration Tests
    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        DateTimeDetectorRefactored detector = new DateTimeDetectorRefactored();
        assertNotNull(detector);
        assertNotNull(detector.getZoneId());
    }

    @Test
    @DisplayName("Test constructor with ZoneId")
    void testConstructorWithZoneId() {
        ZoneId zoneId = ZoneId.of("UTC");
        DateTimeDetectorRefactored detector = new DateTimeDetectorRefactored(zoneId);
        assertEquals(zoneId, detector.getZoneId());
    }

    @Test
    @DisplayName("Test constructor with custom formats")
    void testConstructorWithCustomFormats() {
        String[] dateFormats = {"yyyy-MM-dd"};
        String[] dateTimeFormats = {"yyyy-MM-dd HH:mm:ss"};
        DateTimeDetectorRefactored detector = new DateTimeDetectorRefactored(dateFormats, dateTimeFormats);
        assertNotNull(detector);
    }

    @Test
    @DisplayName("Test constructor validation - null formats")
    void testConstructorValidation_NullFormats() {
        assertThrows(NullPointerException.class, () ->
            new DateTimeDetectorRefactored(null, new String[]{"yyyy-MM-dd"}));
        assertThrows(NullPointerException.class, () ->
            new DateTimeDetectorRefactored(new String[]{"yyyy-MM-dd"}, null));
    }

    @Test
    @DisplayName("Test constructor validation - empty formats")
    void testConstructorValidation_EmptyFormats() {
        assertThrows(IllegalArgumentException.class, () ->
            new DateTimeDetectorRefactored(new String[]{}, new String[]{"yyyy-MM-dd"}));
        assertThrows(IllegalArgumentException.class, () ->
            new DateTimeDetectorRefactored(new String[]{"yyyy-MM-dd"}, new String[]{}));
    }

    @Test
    @DisplayName("Test ZoneId setter and getter")
    void testZoneIdSetterGetter() {
        ZoneId newZoneId = ZoneId.of("America/New_York");
        detector.setZoneId(newZoneId);
        assertEquals(newZoneId, detector.getZoneId());
    }

    @Test
    @DisplayName("Test ZoneId setter validation")
    void testZoneIdSetterValidation() {
        assertThrows(NullPointerException.class, () -> detector.setZoneId(null));
    }

    // Basic Date Detection Tests
    @Test
    @DisplayName("Test detectPossibleDateTimeString with valid dates")
    void testDetectPossibleDateTimeString_Valid() {
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("Article published on 2023-12-25"));
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("Date: 2023年12月25日"));
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("2023/12/25 - News update"));
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("2023.12.25 | Breaking news"));
    }

    @Test
    @DisplayName("Test detectPossibleDateTimeString with short year format")
    void testDetectPossibleDateTimeString_ShortYear() {
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("23-12-25"));
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("23/12/25"));
    }

    @Test
    @DisplayName("Test detectPossibleDateTimeString with invalid formats")
    void testDetectPossibleDateTimeString_Invalid() {
        assertNull(detector.detectPossibleDateTimeString("No date here"));
        assertNull(detector.detectPossibleDateTimeString("Invalid 2023-13-45 date"));
        assertNull(detector.detectPossibleDateTimeString(""));
        assertNull(detector.detectPossibleDateTimeString(null));
    }

    @Test
    @DisplayName("Test detectPossibleDateTimeString edge cases")
    void testDetectPossibleDateTimeString_EdgeCases() {
        // Test year boundaries
        assertEquals("1990-01-01", detector.detectPossibleDateTimeString("1990-01-01"));
        assertEquals("2099-12-31", detector.detectPossibleDateTimeString("2099-12-31"));

        // Test with text before and after
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("Published on 2023-12-25 and updated later"));

        // Test multiline text (should only process first line)
        assertEquals("2023-12-25", detector.detectPossibleDateTimeString("2023-12-25\n2024-01-01"));
    }

    // DateTime Detection Tests
    @Test
    @DisplayName("Test detectDateTime with valid formats")
    void testDetectDateTime_Valid() {
        OffsetDateTime result = detector.detectDateTime("2023-12-25 14:30:00");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    @DisplayName("Test detectDateTime with Chinese formats")
    void testDetectDateTime_Chinese() {
        OffsetDateTime result = detector.detectDateTime("2023年12月25日 14:30:00");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    @DisplayName("Test detectDateTime with different separators")
    void testDetectDateTime_Separators() {
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00"));
        assertNotNull(detector.detectDateTime("2023/12/25 14:30:00"));
        assertNotNull(detector.detectDateTime("2023.12.25 14:30:00"));
    }

    @Test
    @DisplayName("Test detectDateTime with timezone formats")
    void testDetectDateTime_Timezone() {
        assertNotNull(detector.detectDateTime("2023-12-25 14:30:00 UTC"));
        assertNotNull(detector.detectDateTime("Mon, 25 Dec 2023 14:30:00 GMT"));
    }

    @Test
    @DisplayName("Test detectDateTime with bad patterns")
    void testDetectDateTime_BadPatterns() {
        assertNull(detector.detectDateTime("Processed on 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("GMT+8 2023-12-25 14:30:00"));
        assertNull(detector.detectDateTime("访问时间 2023-12-25 14:30:00"));
    }

    @Test
    @DisplayName("Test detectDateTime with insufficient length")
    void testDetectDateTime_InsufficientLength() {
        assertNull(detector.detectDateTime("2023"));
        assertNull(detector.detectDateTime("2023-12"));
        assertNull(detector.detectDateTime("23-12-25")); // Too short without context
    }

    @Test
    @DisplayName("Test detectDateTime with null and empty input")
    void testDetectDateTime_NullEmpty() {
        assertNull(detector.detectDateTime(null));
        assertNull(detector.detectDateTime(""));
        assertNull(detector.detectDateTime("   "));
    }

    // Date Detection Tests
    @Test
    @DisplayName("Test detectDate with valid formats")
    void testDetectDate_Valid() {
        OffsetDateTime result = detector.detectDate("2023-12-25");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(0, result.getHour()); // Should default to midnight
    }

    @Test
    @DisplayName("Test detectDate with URL formats")
    void testDetectDate_URLFormats() {
        assertNotNull(detector.detectDate("http://example.com/2023-12-25/article.html"));
        assertNotNull(detector.detectDate("http://example.com/2023/12/25/article.html"));
        assertNotNull(detector.detectDate("http://example.com/20231225/article.html"));
    }

    @Test
    @DisplayName("Test detectDate with invalid dates")
    void testDetectDate_Invalid() {
        assertNull(detector.detectDate("2023-13-45")); // Invalid month and day
        assertNull(detector.detectDate("2023-02-30")); // Invalid day for February
        assertNull(detector.detectDate("1899-12-31")); // Before lower bound
    }

    // YearMonth Detection Tests
    @Test
    @DisplayName("Test detectYearMonth with valid formats")
    void testDetectYearMonth_Valid() {
        YearMonth result = detector.detectYearMonth("2023-12");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());

        result = detector.detectYearMonth("202312");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
    }

    @Test
    @DisplayName("Test detectYearMonth with single digit month")
    void testDetectYearMonth_SingleDigitMonth() {
        YearMonth result = detector.detectYearMonth("2023-1");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(1, result.getMonthValue());
    }

    @Test
    @DisplayName("Test tryParseYearMonthStrictly with edge cases")
    void testTryParseYearMonthStrictly_EdgeCases() {
        assertNotNull(detector.tryParseYearMonthStrictly("2023-12"));
        assertNotNull(detector.tryParseYearMonthStrictly("202312"));
        assertNotNull(detector.tryParseYearMonthStrictly("2023-01"));
        assertNull(detector.tryParseYearMonthStrictly("2023-13")); // Invalid month
        assertNull(detector.tryParseYearMonthStrictly("2023-00")); // Invalid month
    }

    // Parsing Tests
    @Test
    @DisplayName("Test parseDateStrictly with custom formats")
    void testParseDateStrictly_CustomFormats() {
        OffsetDateTime result = detector.parseDateStrictly("2023-12-25", "yyyy-MM-dd");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    @DisplayName("Test parseDateStrictly with multiple formats")
    void testParseDateStrictly_MultipleFormats() {
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd"};

        assertNotNull(detector.parseDateStrictly("2023-12-25", formats));
        assertNotNull(detector.parseDateStrictly("2023/12/25", formats));
        assertNotNull(detector.parseDateStrictly("2023.12.25", formats));
        assertNull(detector.parseDateStrictly("25-12-2023", formats)); // Not in formats
    }

    @Test
    @DisplayName("Test parseDateTimeStrictly")
    void testParseDateTimeStrictly() {
        OffsetDateTime result = detector.parseDateTimeStrictly("2023-12-25 14:30:00");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    @DisplayName("Test parseDateTimeStrictly with Instant fallback")
    void testParseDateTimeStrictly_WithFallback() {
        Instant fallback = Instant.parse("2020-01-01T00:00:00Z");

        Instant result = detector.parseDateTimeStrictly("2023-12-25T14:30:00Z", fallback);
        assertNotNull(result);
        assertNotEquals(fallback, result);

        result = detector.parseDateTimeStrictly("invalid-date", fallback);
        assertEquals(fallback, result);
    }

    // Old Date Detection Tests
    @Test
    @DisplayName("Test containsOldDate with valid old dates")
    void testContainsOldDate_Valid() {
        assertTrue(detector.containsOldDate("2020-12-25", 365)); // More than 1 year old
        assertTrue(detector.containsOldDate("2023-01-01", 30)); // More than 30 days old
        assertFalse(detector.containsOldDate("2024-12-25", 365)); // Future date
    }

    @Test
    @DisplayName("Test containsOldDate with year-month")
    void testContainsOldDate_YearMonth() {
        assertTrue(detector.containsOldDate("2020-12", 365));
        assertTrue(detector.containsOldDate("2023-01", 30));
    }

    @Test
    @DisplayName("Test containsOldDate with different zones")
    void testContainsOldDate_DifferentZones() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneId ny = ZoneId.of("America/New_York");

        String oldDate = "2020-12-25";
        assertTrue(detector.containsOldDate(oldDate, 365, utc));
        assertTrue(detector.containsOldDate(oldDate, 365, ny));
    }

    @Test
    @DisplayName("Test containsOldDate edge cases")
    void testContainsOldDate_EdgeCases() {
        assertFalse(detector.containsOldDate(null, 30));
        assertFalse(detector.containsOldDate("", 30));
        assertFalse(detector.containsOldDate("2023-12-25", 0));
        assertFalse(detector.containsOldDate("2023-12-25", -1));
    }

    // Builder Tests
    @Test
    @DisplayName("Test Builder pattern")
    void testBuilder() {
        DateTimeDetectorRefactored customDetector = new DateTimeDetectorRefactored.Builder()
                .withDateFormats("yyyy-MM-dd")
                .withDateTimeFormats("yyyy-MM-dd HH:mm:ss")
                .withZoneId(ZoneId.of("UTC"))
                .build();

        assertNotNull(customDetector);
        assertEquals(ZoneId.of("UTC"), customDetector.getZoneId());

        OffsetDateTime result = customDetector.detectDateTime("2023-12-25 14:30:00");
        assertNotNull(result);
    }

    // Performance and Large Input Tests
    @Test
    @DisplayName("Test performance with large input")
    void testLargeInput() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("Some text without dates. ");
        }
        largeText.append("Date: 2023-12-25 14:30:00");

        OffsetDateTime result = detector.detectDateTime(largeText.toString());
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    @DisplayName("Test input length limiting")
    void testInputLengthLimiting() {
        StringBuilder veryLarge = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            veryLarge.append("Text ");
        }
        veryLarge.append("2023-12-25 14:30:00");

        // Should handle large input without performance issues
        OffsetDateTime result = detector.detectDateTime(veryLarge.toString());
        assertNotNull(result);
    }

    // Unicode and Special Character Tests
    @Test
    @DisplayName("Test unicode whitespace handling")
    void testUnicodeWhitespace() {
        // Various unicode space characters
        assertNotNull(detector.detectDateTime("2023-12-25\u00A014:30:00")); // NBSP
        assertNotNull(detector.detectDateTime("2023-12-25\u200314:30:00")); // EM space
        assertNotNull(detector.detectDateTime("2023-12-25\u200214:30:00")); // EN space
    }

    @Test
    @DisplayName("Test with special unicode characters")
    void testSpecialUnicode() {
        assertNotNull(detector.detectDateTime("文章发布于 2023年12月25日 14:30:00"));
        assertNotNull(detector.detectDateTime("Дата: 2023-12-25 14:30:00"));
    }

    // Error Handling Tests
    @Test
    @DisplayName("Test error handling with malformed dates")
    void testErrorHandling_MalformedDates() {
        assertNull(detector.detectDateTime("2023-12-32 14:30:00")); // Invalid day
        assertNull(detector.detectDateTime("2023-13-25 14:30:00")); // Invalid month
        assertNull(detector.detectDateTime("2023-02-30 14:30:00")); // Invalid February day
        assertNull(detector.detectDateTime("1899-12-25 14:30:00")); // Before lower bound
    }

    @Test
    @DisplayName("Test error handling with invalid input")
    void testErrorHandling_InvalidInput() {
        assertNull(detector.detectDateTime("2023-12-25 25:70:90")); // Invalid time
        assertNull(detector.detectDateTime("2023-12-25 14:70:00")); // Invalid minute
        assertNull(detector.detectDateTime("2023-12-25 14:30:70")); // Invalid second
    }

    // Comprehensive Format Testing
    @ParameterizedTest
    @DisplayName("Test comprehensive date format support")
    @CsvSource({
        "2023-12-25, true",
        "2023/12/25, true",
        "2023.12.25, true",
        "20231225, true",
        "2023年12月25日, true",
        "25-12-2023, false", // Not in default formats
        "12/25/2023, false", // Not in default formats
        "2023-13-25, false", // Invalid month
        "2023-12-32, false", // Invalid day
        "1899-12-25, false", // Before lower bound
        "2101-12-25, false"  // After upper bound
    })
    void testComprehensiveDateFormats(String date, boolean shouldSucceed) {
        OffsetDateTime result = detector.detectDate(date);
        if (shouldSucceed) {
            assertNotNull(result, "Should successfully parse: " + date);
            assertEquals(2023, result.getYear());
            assertEquals(12, result.getMonthValue());
            assertEquals(25, result.getDayOfMonth());
        } else {
            assertNull(result, "Should fail to parse: " + date);
        }
    }

    @ParameterizedTest
    @DisplayName("Test comprehensive date-time format support")
    @CsvSource({
        "2023-12-25 14:30:00, true",
        "2023-12-25T14:30:00, true",
        "2023-12-25T14:30:00Z, true",
        "2023/12/25 14:30:00, true",
        "2023.12.25 14:30:00, true",
        "2023年12月25日 14:30:00, true",
        "Mon, 25 Dec 2023 14:30:00 GMT, true",
        "2023-12-25 14:30, true",
        "2023-12-25 25:70:90, false", // Invalid time
        "2023-13-25 14:30:00, false", // Invalid month
        "1899-12-25 14:30:00, false"  // Before lower bound
    })
    void testComprehensiveDateTimeFormats(String dateTime, boolean shouldSucceed) {
        OffsetDateTime result = detector.detectDateTime(dateTime);
        if (shouldSucceed) {
            assertNotNull(result, "Should successfully parse: " + dateTime);
        } else {
            assertNull(result, "Should fail to parse: " + dateTime);
        }
    }

    // Concurrency Tests
    @Test
    @DisplayName("Test thread safety")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String testDate = "2023-12-" + (j % 30 + 1) + " 14:30:00";
                        OffsetDateTime result = detector.detectDateTime(testDate);
                        assertNotNull(result);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access");
    }

    // Helper Methods
    private static Stream<String> provideValidDateFormats() {
        return Stream.of(
            "2023-12-25",
            "2023/12/25",
            "2023.12.25",
            "20231225",
            "2023年12月25日"
        );
    }

    private static Stream<String> provideValidDateTimeFormats() {
        return Stream.of(
            "2023-12-25 14:30:00",
            "2023-12-25T14:30:00",
            "2023-12-25T14:30:00Z",
            "2023/12/25 14:30:00",
            "2023.12.25 14:30:00",
            "2023年12月25日 14:30:00"
        );
    }
}